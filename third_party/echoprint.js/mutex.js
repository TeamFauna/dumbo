/**
 * A simple asynchronous mutex for node.js.
 * Based on code by <https://github.com/elarkin>
 */

var EventEmitter = require('events').EventEmitter;

exports.createMutex = createMutex;

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

function createMutex() {
  var m = new Mutex();
  return {
    lock: m.lock,
    release: m.release
  };
}

