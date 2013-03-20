/**
 * A simple asynchronous mutex for node.js.
 * Based on code by <https://github.com/elarkin>
 */

var EventEmitter = require('events').EventEmitter;

exports.lock = lock;
exports.release = release;

var Mutex = function() {
  var queue = new EventEmitter();
  var locked = false;
  
  this.lock = function lock(fn) {
    if (locked) {
      queue.once('ready', function() {
        lock(fn);
      });
    } else {
      locked = true;
      fn();
    }
  };
  
  this.release = function release() {
    locked = false;
    queue.emit('ready');
  };
};

var gMutex = null;

function lock(callback) {
  if (!gMutex) {
    gMutex = new Mutex();
  }
  gMutex.lock(callback);
}

function release() {
  if (gMutex) {
    gMutex.release();
  }
}

