const initBridge = (bridge, interfaces) => {

    // check preconditions
    if (!bridge) { return; }
    if (!interfaces) { return; }
    if (bridge.initialized) { return; }

    class NativeError extends Error {

        constructor(message, nativeMessage, nativeStackTrace) {
            super(message);
            this.nativeMessage = nativeMessage;
            this.nativeStackTrace = nativeStackTrace;
            this.native = true;
        }

        toString() {
            if (!this.stack) {
                return this.message + "\n"+ "Caused by: " + this.nativeStackTrace;
            } else {
                return this.stack + "\n"+ "Caused by: " + this.nativeStackTrace;
            }
        }
    }

    class FinalizationRegistryWrapper {

        constructor(callbackFn) {
            if (typeof FinalizationRegistry !== "undefined") {
                this.registry = new FinalizationRegistry(callbackFn);
            }
        }

        register(target, heldValue, unregisterToken) {
            if(typeof this.registry !== "undefined") {
                return this.registry.register(target, heldValue, unregisterToken);
            }
        }

        unregister(unregisterToken) {
            if(typeof this.registry !== "undefined") {
                return this.registry.unregister(unregisterToken);
            }
        }
    }

    // enrich bridge
    const promiseFinalizationRegistry = new FinalizationRegistryWrapper((promiseBinding) => {
        bridge.releaseDeadPromise(promiseBinding);
    });
    const functionBindings = [];
    let currentFunctionBinding = 0;

    const addFunctionBinding = (f) => {
        functionBindings[currentFunctionBinding] = f;
        return currentFunctionBinding++;
    };

    bridge.getFunction = (functionBinding) => {
        return functionBindings[functionBinding];
    };

    bridge.removeFunction = (functionBinding) => {
        delete functionBindings[functionBinding];
    };

    bridge.getFunctionBinding = () => {
        return Object.keys(functionBindings);
    };

    bridge.executeFunction = (functionBinding, arg) => {
        const f = bridge.getFunction(functionBinding);
        setTimeout(() => {
            f(arg);
        });
    };
    
    bridge.executeFunctionWithPromiseBinding = (functionBinding, promiseBinding, arg) => {
        const f = bridge.getFunction(functionBinding);
        setTimeout(() => {
            try {
                const promise = f(arg);
                promiseFinalizationRegistry.register(promise, promiseBinding, promise);
                promise.then((ret) => {
                    const answer = { hasError: false, isVoid: typeof ret === "undefined", value: ret };
                    bridge.finishPromise(promiseBinding, JSON.stringify(answer));
                    promiseFinalizationRegistry.unregister(promise);
                }).catch((err) => {
                    const answer = { hasError: true, error: { message: err.toString(), stackTrace: err.stack } };
                    bridge.finishPromise(promiseBinding, JSON.stringify(answer));
                    promiseFinalizationRegistry.unregister(promise);
                });
            } catch (err) {
                const answer = { hasError: true, error: { message: `Unable to get a Promise! function: ${f.toString
                ()} error: ${err.toString()}` } };
                bridge.finishPromise(promiseBinding, JSON.stringify(answer));
                promiseFinalizationRegistry.unregister(promise);
            }
        });
    };

    bridge.interfaces = interfaces;

    const prepareCall = (call) => {
        call.arguments = call.arguments.map((arg) => {
            if (typeof arg === "function") {
                return addFunctionBinding(arg);
            } else {
                return arg;
            }
        });
    };

    const callNativeInterface = (call) => {
        prepareCall(call);
        const callAsString = JSON.stringify(call);
        const answerAsString = bridge.nativeCall(callAsString);
        return JSON.parse(answerAsString);
    };

    bridge.nativeCallFullSync = (call) => {
        const answer = callNativeInterface(call);
        if (answer.hasError) {
            throw new NativeError("Error in Native Layer: ", answer.error.message, answer.error.stackTrace);
        }
        if (!answer.isVoid) {
            return answer.value;
        }
    };

    bridge.nativeCallWebPromise = (call) => {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                try {
                    resolve(bridge.nativeCallFullSync(call));
                } catch (e) {
                    reject(e);
                }
            });
        });
    };

    bridge.nativeCallFullPromise = (call) => {
        return new Promise((resolve, reject) => {
            const functionBinding = addFunctionBinding((answer) => {
                if (answer.hasError) {
                    reject(new NativeError("Error in Native Layer: ", answer.error.message, answer.error.stackTrace));
                } else if (answer.isVoid) {
                    resolve();
                } else {
                    resolve(answer.value);
                }
            });
            setTimeout(() => {
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

    bridge.nativeAfterInitialize();

    // call listener
    if (typeof bridge.afterInitialize === "function") {
        bridge.afterInitialize();
    }
};