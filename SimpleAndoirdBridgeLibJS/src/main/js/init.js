var initBridge = function (bridge, interfaces) {
  // check preconditions
  if (!bridge) return;
  if (!interfaces) return;
  if (bridge.initialized) return;

  function NativeError(message, nativeMessage, nativeStackTrace) {
    Object.defineProperty(this, 'name', {
      enumerable: false,
      writable: false,
      value: 'NativeError'
    });

    this.message = message;
    this.nativeMessage = nativeMessage;
    this.nativeStackTrace = nativeStackTrace;
    this.native = true;

    if (Error.hasOwnProperty('captureStackTrace')) { // V8
      Error.captureStackTrace(this, NativeError);
    } else {
      Object.defineProperty(this, 'stack', {
        enumerable: false,
        writable: false,
        value: (new Error(message)).stack
      });
    }

    this.toString = function () {
      if (!this.stack) {
        return this.message + "\n" + "Caused by: " + this.nativeStackTrace;
      } else {
        return this.stack + "\n" + "Caused by: " + this.nativeStackTrace;
      }
    }.bind(this);
  }

  if (typeof Object.setPrototypeOf === 'function') {
    Object.setPrototypeOf(NativeError.prototype, Error.prototype);
  } else {
    NativeError.prototype = Object.create(Error.prototype, {
      constructor: {value: NativeError}
    });
  }


  // enrich bridge
  var functionBindings = [];
  var currentFunctionBinding = 0;

  var addFunctionBinding = function (f) {
    functionBindings[currentFunctionBinding] = f;
    return currentFunctionBinding++;
  };

  bridge.getFunction = function (functionBinding) {
    return functionBindings[functionBinding];
  };

  bridge.removeFunction = function (functionBinding) {
    delete functionBindings[functionBinding];
  };

  bridge.getFunctionBinding = function () {
    return Object.keys(functionBindings);
  };

  bridge.executeFunction = function (functionBinding, arg) {
    var f = bridge.getFunction(functionBinding);
    setTimeout(function () {
      f(arg);
    });
  };

  bridge.executeFunctionWithPromiseBinding = function (functionBinding, promiseBinding, arg) {
    var f = bridge.getFunction(functionBinding);
    setTimeout(function () {
      var promise = f(arg);
      promise.then(function (ret) {
        var answer = {hasError: false, isVoid: typeof ret === "undefined", value: ret};
        bridge.finishPromise(promiseBinding, JSON.stringify(answer));
      }).catch(function (err) {
        var answer = {hasError: true, error: {message: err.toString(), stackTrace: err.stack}};
        bridge.finishPromise(promiseBinding, JSON.stringify(answer));
      });
    });
  };

  bridge.interfaces = interfaces;

  var prepareCall = function (call) {
    call.arguments = call.arguments.map(function (arg) {
      if (typeof arg === "function") {
        return addFunctionBinding(arg);
      } else {
        return arg;
      }
    });
  };

  var callNativeInterface = function (call) {
    prepareCall(call);
    var callAsString = JSON.stringify(call);
    var answerAsString = bridge.nativeCall(callAsString);
    return JSON.parse(answerAsString);
  };

  bridge.nativeCallFullSync = function (call) {
    var answer = callNativeInterface(call);
    if (answer.hasError) {
      throw new NativeError("Error in Native Layer: ", answer.error.message, answer.error.stackTrace);
    }
    if (!answer.isVoid) {
      return answer.value;
    }
  };

  bridge.nativeCallWebPromise = function (call) {
    return new Promise(function (resolve, reject) {
      setTimeout(function () {
        try {
          resolve(bridge.nativeCallFullSync(call));
        } catch (e) {
          reject(e);
        }
      });
    });
  };

  bridge.nativeCallFullPromise = function (call) {
    return new Promise(function (resolve, reject) {
      var functionBinding = addFunctionBinding(function (answer) {
        if (answer.hasError) {
          reject(new NativeError("Error in Native Layer: ", answer.error.message, answer.error.stackTrace));
        } else if (answer.isVoid) {
          resolve();
        } else {
          resolve(answer.value);
        }
      });
      setTimeout(function () {
        try {
          call.promiseFunctionBinding = functionBinding;
          bridge.nativeCallFullSync(call);
        } catch (e) {
          bridge.removeFunction(functionBinding);
          reject(e);
        }
      });
    });
  };

  // set flag to true
  bridge.initialized = true;

  // call listener
  if (bridge.afterInitialize) {
    bridge.afterInitialize();
  }

  bridge.nativeAfterInitialize();
};