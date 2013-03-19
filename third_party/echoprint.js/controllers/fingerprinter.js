var zlib = require('zlib');
var log = require('winston');
var Mutex = require('../mutex');
var config = require('../config');
var database = require('../models/mysql');

// Constants
var CHARACTERS = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ';
var SECONDS_TO_TIMESTAMP = 43.45;
var MAX_ROWS = 30;
var MIN_MATCH_PERCENT = 0.1;
var MATCH_SLOP = 2;

// Exports
exports.decodeCodeString = decodeCodeString;
exports.query = query;
exports.insert = insert;

// Globals
var gMutex = Mutex.getMutex();

/**
 * Takes a base64 encoded representation of a zlib-compressed code string
 * and passes a fingerprint object to the callback.
 */
function decodeCodeString(codeStr, callback) {
  // Fix url-safe characters
  codeStr = codeStr.replace(/-/g, '+').replace(/_/g, '/');
  
  // Expand the base64 data into a binary buffer
  var compressed = new Buffer(codeStr, 'base64');
  
  // Decompress the binary buffer into ascii hex codes
  zlib.inflate(compressed, function(err, uncompressed) {
    if (err) return callback(err, null);
    // Convert the ascii hex codes into codes and time offsets
    var fp = inflateCodeString(uncompressed);
    log.debug('Inflated ' + codeStr.length + ' byte code string into ' +
      fp.codes.length + ' codes');
    
    callback(null, fp);
  });
}

/**
 * Takes an uncompressed code string consisting of zero-padded fixed-width
 * sorted hex integers and converts it to the standard code string.
 */
function inflateCodeString(buf) {
  // 5 hex bytes for hash, 5 hex bytes for time (40 bits per tuple)
  var count = Math.floor(buf.length / 5);
  var endTimestamps = count / 2;
  var i;
  
  var codes = new Array(count / 2);
  var times = new Array(count / 2);
  
  for (i = 0; i < endTimestamps; i++) {
    times[i] = parseInt(buf.toString('ascii', i * 5, i * 5 + 5), 16);
  }
  for (i = endTimestamps; i < count; i++) {
    codes[i - endTimestamps] = parseInt(buf.toString('ascii', i * 5, i * 5 + 5), 16);
  }
  
  // Sanity check
  for (i = 0; i < codes.length; i++) {
    if (isNaN(codes[i]) || isNaN(times[i])) {
      log.error('Failed to parse code/time index ' + i);
      return { codes: [], times: [] };
    }
  }
  
  return { codes: codes, times: times };
}

/**
 * Clamp this fingerprint to a maximum N seconds worth of codes.
 */
function cutFPLength(fp, maxSeconds) {
  if (!maxSeconds) maxSeconds = 60;
  
  var newFP = {};
  for(var key in fp) {
    if (fp.hasOwnProperty(key))
     newFP[key] = fp[key];
   }
  
  var firstTimestamp = fp.times[0];
  var sixtySeconds = maxSeconds * SECONDS_TO_TIMESTAMP + firstTimestamp;
  
  for (var i = 0; i < fp.times.length; i++) {
    if (fp.times[i] > sixtySeconds) {
      log.debug('Clamping ' + fp.codes.length + ' codes to ' + i + ' codes');
      
      newFP.codes = fp.codes.slice(0, i);
      newFP.times = fp.times.slice(0, i);
      return newFP;
    }
  }
  
  newFP.codes = fp.codes.slice(0);
  newFP.times = fp.times.slice(0);
  return newFP;
}

/**
 * Finds the closest matching movie, if any, to a given fingerprint.
 */
function query(codes, fingerprint, callback) {
  fingerprint = cutFPLength(fingerprint);
  
  if (!fingerprint.codes.length)
    return callback('No valid fingerprint codes specified', null);
  
  log.debug('Starting query with ' + fingerprint.codes.length + ' codes');
  
  database.query(fingerprint, MAX_ROWS, function(err, matches) {
    if (err) return callback(err, null);
    
    if (!matches || !matches.length) {
      log.debug('No matched movies');
      return callback(null, { status: 'NO_RESULTS' });
    }
    
    log.debug('Matched ' + matches.length + ' movies, top code overlap is ' +
      matches[0].score);
    
    // If the best result matched fewer codes than our percentage threshold,
    // report no results
    if (matches[0].score < fingerprint.codes.length * MIN_MATCH_PERCENT)
      return callback(null, { status: 'MULTIPLE_BAD_HISTOGRAM_MATCH' });
    
    // Compute more accurate scores for each movie by taking time offsets into
    // account
    var newMatches = [];
    var newCount = 0;
    for (var i = 0; i < matches.length; i++) {
      var match = matches[i];
      match.ascore = getActualScore(fingerprint, match, MATCH_SLOP);
      if (match.ascore)
        newMatches[newCount++] = match;
    }
    matches = newMatches;
    
    if (!matches.length) {
      log.debug('No matched movies after score adjustment');
      return callback(null, { status: 'NO_RESULTS_HISTOGRAM_DECREASED' });
    }
    
    // If we only had one movie match, just use the threshold to determine if
    // the match is good enough
    if (matches.length === 1) {
      if (matches[0].ascore / fingerprint.codes.length >= MIN_MATCH_PERCENT) {
        // Fetch metadata for the single match
        return getMovieMetadata(matches[0], matches,
          'SINGLE_GOOD_MATCH_HISTOGRAM_DECREASED', callback);
      } else {
        log.debug('Single bad match with actual score ' + matches[0].ascore +
          '/' + fingerprint.codes.length);
        return callback(null, { status: 'SINGLE_BAD_MATCH' });
      }
    }
    
    var origTopScore = matches[0].ascore;
    
    // Sort by the new adjusted score
    matches.sort(function(a, b) { return b.ascore - a.score; });
    
    var topMatch = matches[0];
    var newTopScore = topMatch.ascore;
    
    log.debug('Actual top score is ' + newTopScore + ', next score is ' +
      matches[1].ascore);
    
    // If the best result actually matched fewer codes than our percentage
    // threshold, report no results
    if (newTopScore < fingerprint.codes.length * MIN_MATCH_PERCENT)
      return callback(null, { status: 'MULTIPLE_BAD_HISTOGRAM_MATCH' });
    
    // If the actual score was not close enough, then no match
    if (newTopScore <= origTopScore / 2)
      return callback(null, { status: 'MULTIPLE_BAD_HISTOGRAM_MATCH' });
    
    // If the difference in actual scores between the first and second matches
    // is not significant enough, then no match 
    if (newTopScore - matches[1].ascore < newTopScore / 2)
      return callback(null, { status: 'MULTIPLE_BAD_HISTOGRAM_MATCH' });
    
    // Fetch metadata for the top movie
    getMovieMetadata(topMatch, matches,
      'MULTIPLE_GOOD_MATCH_HISTOGRAM_DECREASED', callback);
  });
}

/**
 * Attach movie metadata to a query match.
 */
function getMovieMetadata(match, allMatches, status, callback) {
  database.getMovie(match.movie_id, function(err, movie) {
    if (err) {
      return callback(err, null);
    }
    
    match.metadata = movie;
    match.offset.time = (match.offset.offset + match.offset.min_time) / SECONDS_TO_TIMESTAMP;

    database.getEvents(match.movie_id, function(err, events) {
      if (err) {
        return callback(err, null);
      }

      match.metadata.events = events;
      callback(null, { success: true, status: status, match: match }, allMatches);
    });
  });
}

/**
 * Build a mapping from each code in the given fingerprint to an array of time
 * offsets where that code appears, with the slop factor accounted for in the 
 * time offsets. Used to speed up getActualScore() calculation.
 */
function getCodesToTimes(match, slop) {
  var codesToTimes = {};

  for (var i = 0; i < match.codes.length; i++) {
    var code = match.codes[i];
    var time = Math.floor(match.times[i] / slop);
    
    if (codesToTimes[code] === undefined)
      codesToTimes[code] = [];
    codesToTimes[code].push(time);
  }
  
  return codesToTimes;
}

/**
 * Computes the actual match score for a movie by taking time offsets into
 * account.
 */
function getActualScore(fp, match, slop) {
  var MAX_DIST = 32767;
  
  if (match.codes.length < config.code_threshold) {
    return 0;
  }
  
  var timeDiffs = {};
  var offsetHistogram = {};
  var i, j;
  
  var matchCodesToTimes = getCodesToTimes(match, slop);

  // Iterate over each {code,time} tuple in the query
  for (i = 0; i < fp.codes.length; i++) {
    var code = fp.codes[i];
    var time = Math.floor(fp.times[i] / slop);
    var minDist = MAX_DIST;
    
    // Find the distance of the nearest instance of this code in the match
    var matchTimes = matchCodesToTimes[code];
    if (matchTimes) {
      for (j = 0; j < matchTimes.length; j++) {
        var dist = matchTimes[j] - time;
        dist *= slop;

        if (offsetHistogram[dist] === undefined) {
          offsetHistogram[dist] = 0;
        }
        offsetHistogram[dist]++;

        if (Math.abs(dist) < minDist)
          minDist = Math.abs(dist);
      }
      if (minDist < MAX_DIST) {
        // Increment the histogram bucket for this distance
        if (timeDiffs[minDist] === undefined)
          timeDiffs[minDist] = 0;
        timeDiffs[minDist]++;
      }
    }
  }

  match.histogram = timeDiffs;

  // Calculate the most likely offset of the query from the match.
  var offsets = [];
  var min_time = Math.min.apply(null, fp.times);
  for (var i in offsetHistogram) {
    if (!offsetHistogram.hasOwnProperty(i)) {
      continue;
    }

    offsets.push({
      offset: parseInt(i, 10),
      min_time: min_time,
      amount: offsetHistogram[i]
    });
  }

  if (offsets.length > 0) {
    offsets.sort(function(a, b) { return b.amount - a.amount; });
    match.offset = offsets[0];
  }
  
  // Convert the histogram into an array, sort it, and sum the top two
  // frequencies to compute the adjusted score
  var keys = Object.keys(timeDiffs);
  var array = new Array(keys.length);
  for (i = 0; i < keys.length; i++)
    array[i] = [ keys[i], timeDiffs[keys[i]] ];
  array.sort(function(a, b) { return b[1] - a[1]; });
  

  if (array.length > 1)
    return array[0][1] + array[1][1];
  else if (array.length === 1)
    return array[0][1];
  return 0;
}

/**
 * Takes a movie fingerprint (includes codes and time offsets plus any
 * available metadata), adds it to the database and returns a movie_id
 */
function insert(movie, fp, callback) {
  log.info('Ingesting movie "' + movie.name +
    '", ' + movie.length + ' seconds, ' + fp.codes.length + ' codes');
  
  if (!isValidFP(fp)) {
    return callback('Missing required movie fields', null);
  }
  
  // Acquire a lock while modifying the database
  gMutex.lock(function() {
    query(movie.codes, fp, function(err, res) {
      if (err) {
        return error('Query failed: ' + err);
      }

      if (res.success) {
      } else {
        database.insertMovie(movie, function(err, movie_id) {
          if (err) {
            return error('Error inserting movie: ' + err);
          }

          database.insertCodes(movie_id, fp, function(err) {
            if (err) {
              log.error('Error inserting codes: ' + err);
            }
          });

          database.insertPlotEvents(movie_id, movie.plot_events, function(err) {
            if (err) {
              log.error('Error inserting plot events: ' + err);
            }
          });

          database.insertActors(movie.actors, function(err, actor_ids) {
            if (err) {
              return error('Error inserting actors: ' + err);
            }

            for (var i = 0; i < movie.roles.length; i++) {
              var role = movie.roles[i];
              role.actor_id = actor_ids[role.actor];
            }

            database.insertRoles(movie.roles, function(err, role_ids) {
              if (err) {
                return error('Error inserting roles: ' + err);
              }

              for (var i = 0; i < movie.role_events.length; i++) {
                var role_event = movie.role_events[i];
                role_event.role_id = role_ids[role_event.role];
              }

              database.insertRoleEvents(movie_id, movie.role_events, function(err) {
                if (err) {
                  return error('Error inserting role events: ' + err);
                }

                return success(movie_id);
              });
            });
          });
        });
      }
    });
  });

  function error(error_string) {
    gMutex.release();
    log.error(error_string);
    callback(error_string, null);
  }

  function success(movie_id) {
    gMutex.release();
    log.info('Created movie_id: ' + movie_id + ' for ' + movie.name);
    callback(null, { movie_id: movie_id });
  }
}

function isValidFP(fp) {
  return fp.codes.length && fp.times.length;
}

