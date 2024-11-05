"use strict";
// @ts-ignore
var bridge = Bridge;
function startApp(f) {
    if (bridge.initialized) {
        f();
    }
    else {
        bridge.afterInitialize = f;
    }
}
bridge.init();
startApp(function () {
    var runTimeVal = 0;
    var text = document.getElementById("text");
    var reload = document.getElementById("reload");
    var info = document.getElementById("info");
    text.innerHTML = "";
    var appendText = function (s) {
        text.innerHTML = "".concat(text.innerHTML, "</br>").concat(s);
    };
    var infoUpdater = function () {
        runTimeVal = Math.floor(performance.now());
        info.innerHTML = "Run time ".concat(runTimeVal, "ms, open function bindings ").concat(bridge.getFunctionBinding().length);
    };
    infoUpdater();
    setInterval(infoUpdater, 31);
    reload.addEventListener("click", function () { window.location.reload(); });
    // define the interface as const
    var android = bridge.interfaces.Android;
    // call different native call types
    appendText(android.helloFullSync("Web"));
    android.helloWebPromise("Web").then(function (s) { appendText(s); });
    android.helloFullPromise("Web").then(function (s) { appendText(s); });
    // register functions to the native layer
    android.registerJSFunctionWithArg(function (i) {
        appendText(i.toLocaleString());
    });
    android.registerFunctionWithPromise(function () {
        return new Promise(function (resolve) {
            appendText("Sending run time ".concat(runTimeVal, "ms to native layer!"));
            resolve(runTimeVal);
        });
    });
    android.registerFunctionWithPromiseAndArg(function (add) {
        return new Promise(function (resolve) {
            var result = add.a + add.b;
            appendText("Sending result ".concat(result, " of web calculation from native values ").concat(add.a, " + ").concat(add.b, " to native layer!"));
            resolve(result);
        });
    });
    android.registerFunction(function () {
        appendText("Function binding demo is available!");
    });
});
