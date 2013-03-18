/**
 * MySQL database backend. An alternative database backend can be created
 * by implementing all of the methods exported by this module
 */

var fs = require('fs');
var mysql = require('mysql');
var temp = require('temp');
var config = require('../config');

exports.query = query;
exports.getMovie = getMovie;
//exports.getMovieByName = getMovieByName;
//exports.getMovieEvents = getMovieEvents;
exports.insertMovie = insertMovie;
exports.insertCodes = insertCodes;
exports.insertPlotEvents = insertPlotEvents;
exports.insertActors = insertActors;
exports.insertRoles = insertRoles;
exports.insertRoleEvents = insertRoleEvents;
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
  
  // Get the top N matching tracks sorted by score (number of matched codes)
  var sql = 'SELECT movie_id, COUNT(movie_id) AS score ' +
    'FROM codes ' +
    'WHERE code IN (' + fpCodesStr + ') ' +
    'GROUP BY movie_id ' +
    'ORDER BY score DESC ' +
    'LIMIT ' + rows;

  client.query(sql, [], function(err, matches) {
    if (err) return callback(err, null);
    if (!matches || !matches.length) return callback(null, []);
    
    var trackIDs = new Array(matches.length);
    var trackIDMap = {};
    for (var i = 0; i < matches.length; i++) {
      var trackID = matches[i].track_id;
      trackIDs[i] = trackID;
      trackIDMap[trackID] = i;
    }
    var trackIDsStr = trackIDs.join('","');
    
    // Get all of the matching codes and their offsets for the top N matching
    // tracks
    sql = 'SELECT code, time_stamp, movie_id' +
      'FROM codes ' +
      'WHERE code IN (' + fpCodesStr + ') ' +
      'AND movie_id IN ("' + trackIDsStr + '")';
    client.query(sql, [], function(err, codeMatches) {
      if (err) return callback(err, null);
      
      for (var i = 0; i < codeMatches.length; i++) {
        var codeMatch = codeMatches[i];
        var idx = trackIDMap[codeMatch.track_id];
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

function getMovie(movieID, callback) {
  var sql = 'SELECT * FROM movies WHERE id=?';
  client.query(sql, [movieID], function(err, movies) {
    if (movies && movies.length >= 1) {
      return callback(null, movies[0]);
    }
    return callback(err, null);
  });
}

function getEvents(movieID, callback, startTime, stopTime) {
  var sql = 'SELECT id, time_stamp, role, null as plot FROM role_events WHERE movie=? AND time_stamp >= ? AND time_stamp < ?' +
            'UNION' +
            'SELECT id, time_stamp, null, plot FROM actor_events WHERE movie=? AND time_stamp >= ? AND time_stamp < ?';
  client.query(sql, [movieID, startTime, stopTime, movieID, startTime, stopTime],
      function(err, events) {
    if (events && events.length >= 1) {
      return callback(null, events);
    }
    return callback(err, null);
  });
}

function insertMovie(movie, callback) {
  var sql = 'INSERT INTO movies ' +
    '(code_version, name, imdb_url, length, import_date) ' +
    'VALUES (?, ?, ?, ?, ?)';
  var values = [movie.codes.version, movie.name, movie.imdb_url, movie.codes.length, new Date()];
  client.query(sql, values, function(err, info) {
    if (err) {
      return callback(err, null);
    }

    if (info.affectedRows !== 1) {
      return callback('Movie insert failed', null);
    }

    var movie_id = info.insertId;
    callback(null, movie_id);
  });
}

function insertCodes(movie_id, fp, callback) {
  // Write out the codes to a file for bulk insertion into MySQL
  var temp_name = temp.path({ prefix: 'echoprint-' + movie_id, suffix: '.csv' });
  
  // Hack for cygwin on Russell's computer.  Will make better later... maybe.
  //if (tempName.indexOf('C:\\cygwin') == 0) {
    //filename.replace('\\', '/');
    //filename = filename.slice(2);
    //var tempName = '/cygwin/tmp/testing.csv';
  //}

  console.log('Writing to file');
  writeCodesToFile(temp_name, fp, movie_id, function(err) {
    if (err) return callback(err, null);
    
    console.log('Running SQL query');
    // Bulk insert the codes
    sql = 'LOAD DATA INFILE ? IGNORE INTO TABLE codes';
    client.query(sql, [temp_name], function(err, info) {
      // Remove the temporary file
      fs.unlink(temp_name, function(err2) {
        if (!err) err = err2;
        callback(err, movie_id);
      });
    });
  });
}

function writeCodesToFile(filename, fp, movie_id, callback) {
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

  var file = fs.createWriteStream(filename);
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
    '(name, imdb_url) VALUES (?, ?)';

  var values = [];
  for (var i = 0; i < actors.length; i++) {
    values.push([actors[i].name, actors[i].imdb_url]);
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

