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
      
      var duration = new Date() - req.start;
      var success = !!result.success;

      log.debug('Completed lookup in ' + duration + 'ms. success=' +
        success + ', status=' + result.status);
      
      return server.respond(req, res, 200, { success: success,
        status: result.status, match: result.match || null });
    });
  });
};

/**
 * Insert a new movie to the database.
 */
function insert(req, res) {
  var movie = req.body;
  
  if (!isValidCode(movie.codes)) {
    return server.respond(req, res, 500, { error: 'Missing or invalid code fields' });
  }
  
  if (!isCorrectCodeVersion(movie.codes)) {
    return server.respond(req, res, 500, { error: 'Invalid version' });
  }
  
  fingerprinter.decodeCodeString(movie.codes.string, function(err, fingerprint) {
    if (err || !fingerprint.codes.length) {
      log.error('Failed to decode codes for insert: ' + err);
      return server.respond(req, res, 500, { error: 'Invalid code' });
    }

    fingerprinter.insert(movie, fingerprint, function(err, result) {
      if (err) {
        log.error('Failed to insert movie: ' + err);
        return server.respond(req, res, 500, { error: 'Insertion failed' });
      }
      
      var duration = new Date() - req.start;
      log.debug('Inserted new movie in ' + duration + 'ms. movie.id=' +
        result.movie_id);
      
      result.success = true;
      return server.respond(req, res, 200, result);
    });
  });
};

/**
 * Updates metadata for a movie.
 */
function update(req, res) {
  var movie = req.body;

  var id = parseInt(movie.id, 10);
  if (!(id > 0)) {
    return server.respond(req, res, 500, { error: 'Invalid movie id: ' + id });
  }

  database.updateMovie(id, movie, function(err, result) {
    if (err) {
      log.error('Failed to update movie: ' + err);
      return server.respond(req, res, 500, { error: 'Update failed' });
    }

    var duration = new Date() - req.start;
    log.debug('Updated movie in ' + duration + 'ms. movie.id=' + id);
    result.success = true;
    return server.respond(req, res, 200, result);
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
      return server.respond(req, res, 500, 'Error metching metadata: ' + err);
    }
    
    var match = {};
    match.metadata = movie;

    database.getEvents(id, function(err, events) {
      if (err) {
        return server.respond(req, res, 500, 'Error metching metadata: ' + err);
      }

      match.metadata.events = events;
      server.respond(req, res, 200, { success: true, match: match });
    });
  });
}

function isValidCode(codes) {
  return codes && codes.string && codes.version && !isNaN(parseInt(codes.length, 10));
}

function isCorrectCodeVersion(codes) {
  return codes.version == config.codever;
}

