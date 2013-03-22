var urlParser = require('url');
var log = require('winston');
var fingerprinter = require('./fingerprinter');
var database = require('../models/mysql.js');
var server = require('../server');
var config = require('../config');

exports.query = query;
exports.insert = insert;
exports.list = list;
exports.update = update;
exports.get = get;

/**
 * Querying for the closest matching movie.
 */
function query(req, res) {
  var url = urlParser.parse(req.url, true);
  var debug = url.query.debug;
  var codes = req.body;

  if (!isValidCode(codes)) {
    return server.respond(req, res, 500, { error: 'Missing code' });
  }
  
  if (!isCorrectCodeVersion(codes)) {
    return server.respond(req, res, 500, { error: 'Missing or invalid version' });
  }
  
  fingerprinter.decodeCodeString(codes.string, function(err, fingerprint) {
    if (err) {
      log.error('Failed to decode codes for query: ' + err);
      return server.respond(req, res, 500, { error: 'Invalid code' });
    }
    
    fingerprinter.query(codes, fingerprint, function(err, result) {
      if (err) {
        log.warn('Failed to complete query: ' + err);
        return server.respond(req, res, 500, { error: 'Lookup failed' });
      }

      var success = fingerprinter.isSuccess(result.status);

      if (!result.match) {
        log.debug('Query failed: ' + result.status);
        return server.respond(req, res, 200, { success: success, status: result.status });
      }
      
      database.getMovie(result.match.movie_id, function(err, movie) {
        if (err) {
          return server.respond(req, res, 500, { error: 'Failed to get movie metadata: ' + err });
        }

        result.match.metadata = movie;

        var duration = new Date() - req.start;

        log.debug('Completed lookup in ' + duration + 'ms. success=' +
          success + ', status=' + result.status);
      
        return server.respond(req, res, 200, { success: success,
          status: result.status, match: result.match || null });
      });
    });
  });
};

/**
 * Insert a new movie to the database.
 */
function insert(req, res) {
  var movie = req.body;

  validateMovie(req, res, movie, true, function() {
    fingerprinter.decodeCodeString(movie.codes.string, function(err, fingerprint) {
      if (err || !fingerprint.codes.length) {
        log.error('Failed to decode codes for insert: ' + err);
        return server.respond(req, res, 500, { error: 'Invalid code' });
      }

      // Check if the movie already exists to avoid duplication.
      fingerprinter.query(movie.codes, fingerprint, function(err, result) {
        if (err) {
          server.respond(req, res, 500, { error: 'Query failed: ' + err });
        }

        if (result.success) {
          updateMovie(req, res, result.match.movie_id, movie);
        } else {
          database.insertMovie(movie, fingerprint, function(err, movie_id) {
            if (err) {
              server.respond(req, res, 500, { error: ' Movie insertion failed: ' + err });
            }

            var duration = new Date() - req.start;
            log.debug('Inserted new movie in ' + duration + 'ms. movie.id=' + movie_id);
        
            return server.respond(req, res, 200, { success: true, movie_id: movie_id });
          });
        }
      });
    });
  });
};

/**
 * Updates metadata for a movie.
 */
function update(req, res) {
  var movie = req.body;

  validateMovie(req, res, movie, false, function() {
    updateMovie(req, res, movie.id, movie);
  });
}

/**
 * Lists all movies in the database.
 */
function list(req, res) {
  database.getMovies(function(err, movies) {
    var result = {
      success: true,
      movies: movies
    };
    return server.respond(req, res, 200, result);
  });
}

/**
 * Get a movie and all events from the database.
 */
function get(req, res) {
  var movie = req.body;
  var id = parseInt(movie.id, 10);

  database.getMovie(id, function(err, movie) {
    if (err) {
      return server.respond(req, res, 500, 'Error metching movie metadata: ' + err);
    }
    
    var match = {};
    match.metadata = movie;
    server.respond(req, res, 200, { success: true, match: match });
  });
}

function validateMovie(req, res, movie, is_insertion, callback) {
  if (is_insertion) {
    if (!isValidCode(movie.codes)) {
      return server.respond(req, res, 500, { error: 'Missing or invalid code fields' });
    }
    
    if (!isCorrectCodeVersion(movie.codes)) {
      return server.respond(req, res, 500, { error: 'Invalid version' });
    }
  } else {
    movie.id = parseInt(movie.id, 10);
    if (!(movie.id > 0)) {
      return server.respond(req, res, 500, { error: 'Invalid movie id: ' + move.id });
    }
  }

  if (!movie.plot_events) {
    movie.plot_events = [];
  }

  if (!movie.actors) {
    movie.actors = [];
  }

  if (!movie.roles) {
    movie.roles = [];
  }

  if (!movie.role_events) {
    movie.role_events = [];
  }

  callback();
}

function updateMovie(req, res, id, movie) {
  database.updateMovie(movie.id, movie, function(err, result) {
    if (err) {
      log.error('Failed to update movie: ' + err);
      return server.respond(req, res, 500, { error: 'Update failed' });
    }

    var duration = new Date() - req.start;
    log.debug('Updated movie in ' + duration + 'ms. movie.id=' + id);
    return server.respond(req, res, 200, { success: true });
  });
}

function isValidCode(codes) {
  return codes && codes.string && codes.version;
}

function isCorrectCodeVersion(codes) {
  return codes.version == config.codever;
}

