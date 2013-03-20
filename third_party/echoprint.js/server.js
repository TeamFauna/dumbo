/**
 * Simple HTTP server module
 */

var http = require('http');
var urlParser = require('url');
var qs = require('querystring');
var log = require('winston');
var jade = require('jade');
var config = require('./config');
var api = require('./controllers/api');
var debug = require('./controllers/debug');

exports.init = init;
exports.renderView = renderView;
exports.respond = respond;

var TIMEOUT = 1000 * 60;

/**
 * Initialize the HTTP endpoints.
 */
function init() {
  http.createServer(function(req, res) {
    req.start = new Date();
    req.timer = setTimeout(function() { timeout(req, res); }, TIMEOUT);
    
    var url = urlParser.parse(req.url, true);
    var path = url.pathname.split('/', 16);

    var body = '';
    req.on('data', function(data) {
      body += data;
    });

    req.on('end', function() {
      if (body.length > 0) {
        try {
          req.body = JSON.parse(body);
        } catch(e) {
          return respond(req, res, 500, { error: 'Request body was not JSON: ' + e});
          req.body = {};
        }
      }

      if (path[1] == 'query') {
        api.query(req, res);
      } else if (path[1] == 'insert') {
        api.insert(req, res);
      } else {
        respond(req, res, 404, { error: 'Invalid API endpoint' });
      }
    });
  }).addListener('clientError', function(ex) {
    log.warn('Client error: ' + ex);
  }).listen(config.web_port, "127.0.0.1");
}

/**
 * Render a view template and send it as an HTTP response.
 */
function renderView(req, res, statusCode, view, options, headers) {
  jade.renderFile(__dirname + '/views/' + view, options, function(err, html) {
    if (err) {
      log.error('Failed to render ' + view + ': ' + err);
      return respond(req, res, 500, 'Internal server error', headers);
    }
    
    respond(req, res, 200, html, headers);
  });
}

/**
 * Send an HTTP response to a client.
 */
function respond(req, res, statusCode, body, headers) {
  // Destroy the response timeout timer
  clearTimeout(req.timer);
  
  statusCode = statusCode || 200;
  
  if (!headers) {
    headers = {};
  }
  
  if (typeof body !== 'string') {
    body = JSON.stringify(body);
    headers['Content-Type'] = 'application/json';
  } else {
    headers['Content-Type'] = 'text/html';
  }
  
  if (body) {
    var contentLength = Buffer.byteLength(body, 'utf8');
    headers['Content-Length'] = contentLength;
  }

  var remoteAddress = (req.socket &&
      (req.socket.remoteAddress || (req.socket.socket && req.socket.socket.remoteAddress)));
  
  var referrer = req.headers.referer || req.headers.referrer || '';
  if (referrer.length > 128) {
    referrer = referrer.substr(0, 128) + ' ...';
  }
  
  var url = req.url;
  if (url.length > 128) {
    url = url.substr(0, 128) + ' ...';
  }

  log.info(
    remoteAddress +
      ' - - [' + (new Date()).toUTCString() + ']' +
      ' "' + req.method + ' ' + url +
      ' HTTP/' + req.httpVersionMajor + '.' + req.httpVersionMinor + '" ' +
      statusCode + ' "' + referrer +
      '" "' + (req.headers['user-agent'] || '') + '"');
  
  try {
    res.writeHead(200, headers);
    res.end(body);
  } catch (ex) {
    log.error('Error sending response to ' + remoteAddress + ': ' + ex);
  }
}

/**
 * Handles server timeouts by logging an error and responding with a 503.
 */
function timeout(req, res) {
  var remoteAddress = (req.socket &&
      (req.socket.remoteAddress || (req.socket.socket && req.socket.socket.remoteAddress)));
  log.error('Timed out while responding to a request from ' + remoteAddress);
  
  try {
    res.writeHead(503);
    res.end();
  } catch (ex) { }
}

