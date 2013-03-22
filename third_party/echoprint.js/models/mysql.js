/**
 * MySQL database backend. An alternative database backend can be created
 * by implementing all of the methods exported by this module
 */

var fs = require('fs');
var mysql = require('mysql');
var temp = require('temp');
var config = require('../config');
var log = require('winston');

exports.query = query;
exports.getMovie = getMovie;
exports.getMovies = getMovies;
exports.insertMovie = insertMovie;
exports.updateMovie = updateMovie;
exports.disconnect = disconnect;
                

// Initialize the MySQL connection
var client = mysql.createClient({
  user: config.db_user,
  password: config.db_pass,
  database: config.db_database,
  host: config.db_host
});

/**
 *
 */
function query(fp, rows, callback) {
  var fpCodesStr = fp.codes.join(',');
  
  // Get the top N matching movies sorted by score (number of matched codes)
  var sql = 'SELECT movie_id, COUNT(movie_id) AS score ' +
    'FROM codes ' +
    'WHERE code IN (' + fpCodesStr + ') ' +
    'GROUP BY movie_id ' +
    'ORDER BY score DESC ' +
    'LIMIT ' + rows;

  client.query(sql, [], function(err, matches) {
    if (err) return callback(err, null);
    if (!matches || !matches.length) return callback(null, []);
    
    var movie_ids = new Array(matches.length);
    var movie_id_map = {};
    for (var i = 0; i < matches.length; i++) {
      var movie_id = matches[i].movie_id;
      movie_ids[i] = movie_id;
      movie_id_map[movie_id] = i;
    }
    var movie_id_string = movie_ids.join(',');
    
    // Get all of the matching codes and their offsets for the top N matching
    // movies
    sql = 'SELECT code, time_stamp as time, movie_id ' +
      'FROM codes ' +
      'WHERE code IN (' + fpCodesStr + ') ' +
      'AND movie_id IN (' + movie_id_string + ')';
    client.query(sql, [], function(err, codeMatches) {
      if (err) return callback(err, null);
      
      for (var i = 0; i < codeMatches.length; i++) {
        var codeMatch = codeMatches[i];
        var idx = movie_id_map[codeMatch.movie_id];
        if (idx === undefined) continue;
        
        var match = matches[idx];
        if (!match.codes) {
          match.codes = [];
          match.times = [];
        }
        match.codes.push(codeMatch.code);
        match.times.push(codeMatch.time);
      }
      
      callback(null, matches);
    });
  });
}

function getMovie(movie_id, callback) {
  var sql = 'SELECT * FROM movies WHERE id=?';
  client.query(sql, [movie_id], function(err, movies) {
    if (movies && movies.length >= 1) {
      return getEvents(movie_id, movies[0], callback);
    }

    return callback(err, null);
  });
}

function getMovies(callback) {
  var sql = 'SELECT m.id, m.code_version, m.name, m.imdb_url, m.summary, ' +
    'm.length, m.import_date, count(c.movie_id) as codes ' +
    'FROM movies m, codes c ' +
    'WHERE m.id = c.movie_id ' +
    'GROUP BY c.movie_id';
  client.query(sql, [], function(err, movies) {
    callback(err, movies);
  });
}

function getEvents(movie_id, movie, callback) {
  var events = [];

  var sql =
    'SELECT re.time_stamp, re.blurb, r.name as role, r.imdb_url as role_imdb, ' +
      'a.name as actor, a.imdb_url as actor_imdb, a.picture_url as picture_url ' +
      'FROM role_events re, roles r, actors a ' +
      'WHERE re.movie = ? AND re.role = r.id AND r.actor = a.id';
  client.query(sql, [movie_id], function(err, role_events) {
    if (err) {
      return callback(err, null);
    }

    for (var i = 0; i < role_events.length; i++) {
      var role_event = role_events[i];
      events.push({
        time_stamp: role_event.time_stamp,
        type: 'ROLE',
        text: role_event.blurb,
        role: {
          name: role_event.role,
          imdb_url: role_event.role_imdb
        },
        actor: {
          name: role_event.actor,
          imdb_url: role_event.actor_imdb,
          picture_url: role_event.picture_url
        }
      });
    }

    var sql = 'SELECT time_stamp, plot FROM plot_events WHERE movie = ?';
    client.query(sql, [movie_id], function(err, plot_events) {
      if (err) {
        return callback(err, null);
      }
      
      for (var i = 0; i < plot_events.length; i++) {
        var plot_event = plot_events[i];
        events.push({
          time_stamp: plot_event.time_stamp,
          type: 'PLOT',
          text: plot_event.plot
        });
      }

      events.sort(function(a, b) {
        return a.time_stamp - b.time_stamp;
      });

      movie.events = events;
      callback(null, movie);
    });
  });
}

function insertMovie(movie, fingerprint, callback) {
  var sql = 'INSERT INTO movies ' +
    '(code_version, name, imdb_url, summary, length, import_date) ' +
    'VALUES (?, ?, ?, ?, ?, ?)';
  var values = [
    movie.codes.version,
    movie.name,
    movie.imdb_url,
    movie.summary,
    movie.length,
    new Date()
  ];

  client.query(sql, values, function(err, info) {
    if (err) {
      return callback(err, null);
    }

    movie.id = info.insertId;

    insertCodes(movie.id, fingerprint, function(err) {
      if (err) {
        log.error('Error inserting codes: ' + err);
        return callback(err, null);
      }

      insertMetadata(movie.id, movie, callback);
    });
  });
}

function updateMovie(id, movie, callback) {
  var sql = 'UPDATE movies SET name=?, imdb_url=?, summary=?, length=? WHERE id=?';
  var values = [movie.name, movie.imdb_url, movie.summary, movie.length, id];
  client.query(sql, values, function(err, info) {
    if (err) {
      return callback(err, null);
    }

    insertMetadata(id, movie, callback);
  });
}

function insertMetadata(id, movie, callback) {
  insertPlotEvents(id, movie.plot_events, function(err, plot_event_ids) {
    if (err) {
      log.error('Error adding plot events: ' + err);
    }
  });

  insertActors(movie.actors, function(err, actor_ids) {
    if (err) {
      return error('Error addings actors: ' + err);
    }

    for (var i = 0; i < movie.roles.length; i++) {
      var role = movie.roles[i];
      role.actor_id = actor_ids[role.actor];
    }

    insertRoles(movie.roles, function(err, role_ids) {
      if (err) {
        return error('Error inserting roles: ' + err);
      }

      var new_events = [];
      for (var i = 0; i < movie.role_events.length; i++) {
        var role_event = movie.role_events[i];
        if (role_event.role >= 0) {
          role_event.role_id = role_ids[role_event.role];
          new_events.push(role_event);
        }
      }

      insertRoleEvents(id, new_events, function(err) {
        if (err) {
          return error('Error inserting role events: ' + err);
        }

        return success();
      });
    });
  });

  function error(error_string) {
    log.error(error_string);
    callback(error_string, null);
  }

  function success() {
    log.info('Updated movie: ' + movie.name + ' (' + id + ')');
    callback(null, id);
  }
}

function insertCodes(movie_id, fp, callback) {
  // Write out the codes to a file for bulk insertion into MySQL
  var file_name = temp.path({ prefix: 'echoprint-' + movie_id, suffix: '.csv' });
  var sql_file_name = file_name;

  // Hack for cygwin on Russell's computer.
  // Node expects windows paths but mysql expects linux paths.
  if (sql_file_name.indexOf('C:\\cygwin') == 0) {
    sql_file_name = sql_file_name.replace(/\\/g, '/');
    sql_file_name = sql_file_name.slice(9);
  }

  console.log('Writing to file');
  writeCodesToFile(file_name, fp, movie_id, function(err) {
    if (err) return callback(err, null);
    
    console.log('Running SQL query');
    // Bulk insert the codes
    sql = 'LOAD DATA INFILE ? IGNORE INTO TABLE codes';
    client.query(sql, [sql_file_name], function(err, info) {
      // Remove the temporary file
      fs.unlink(file_name, function(err2) {
        if (!err) err = err2;
        callback(err, movie_id);
      });
    });
  });
}

function writeCodesToFile(file_name, fp, movie_id, callback) {
  var i = 0;
  var keepWriting = function() {
    var success = true;
    while (success && i < fp.codes.length) {
      success = file.write(fp.codes[i] + '\t' + fp.times[i] + '\t' + movie_id + '\n');
      i++;
    }

    if (i === fp.codes.length) {
      file.end();
    }
  };

  var file = fs.createWriteStream(file_name);
  file.on('drain', keepWriting);
  file.on('error', callback);
  file.on('close', callback);
  
  keepWriting();
}

function insertPlotEvents(movie_id, plot_events, callback) {
  var sql = 'INSERT INTO plot_events ' +
    '(time_stamp, movie, plot) VALUES (?, ?, ?)';

  var values = [];
  for (var i = 0; i < plot_events.length; i++) {
    var plot_event = plot_events[i];
    values.push([plot_event.time_stamp, movie_id, plot_event.plot]);
  }

  insertMultipleRows(sql, values, callback);
};

function insertActors(actors, callback) {
  var sql = 'INSERT INTO actors ' +
    '(name, imdb_url, picture_url) VALUES (?, ?, ?)';

  var values = [];
  for (var i = 0; i < actors.length; i++) {
    var actor = actors[i];
    values.push([actor.name, actor.imdb_url, actor.picture_url]);
  }

  insertMultipleRows(sql, values, callback);
}

function insertRoles(roles, callback) {
  var sql = 'INSERT INTO roles ' +
    '(name, imdb_url, actor) VALUES (?, ?, ?)';

  var values = [];
  for (var i = 0; i < roles.length; i++) {
    var role = roles[i];
    values.push([role.name, role.imdb_url, role.actor_id]);
  }

  insertMultipleRows(sql, values, callback);
}

function insertRoleEvents(movie_id, role_events, callback) {
  var sql = 'INSERT INTO role_events ' +
    '(time_stamp, movie, role, blurb) VALUES (?, ?, ?, ?)';

  var values = [];
  for (var i = 0; i < role_events.length; i++) {
    var role_event = role_events[i];
    values.push([role_event.time_stamp, movie_id, role_event.role_id, role_event.blurb]);
  }

  insertMultipleRows(sql, values, callback);
}

function insertMultipleRows(sql, rows, callback) {
  var counter = 0;
  var error = null;
  var ids = Array(rows.length);

  if (!rows.length) {
    callback(null, []);
  }

  for (var i = 0; i < rows.length; i++) {
    (function(i) {
      client.query(sql, rows[i], function(err, info) {
        if (err) {
          error = err;
          return complete();
        }

        ids[i] = info.insertId;

        complete();
      });
    })(i);
  }

  function complete() {
    counter++;
    if (counter == ids.length) {
      callback(error, ids);
    }
  }
}

function disconnect(callback) {
  client.end(callback);
}

