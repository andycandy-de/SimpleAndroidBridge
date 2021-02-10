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
startApp(function () {
    var text = document.getElementById("text");
    text.innerHTML = "";
    var appendText = function (s) {
        text.innerHTML = text.innerHTML + "</br>" + s;
    };
    var android = bridge.interfaces.Android;
    appendText(android.helloFullSync("Web"));
    android.helloWebPromise("Web").then(function (s) { appendText(s); });
    android.helloFullPromise("Web").then(function (s) { appendText(s); });
    android.registerFunction(function (i) {
        appendText(i.toLocaleString());
    }).then(function () { return console.log("Function1 registered"); }).catch(function (err) { return console.log(err.toString()); });
    android.registerFunctionWithPromise(function () {
        return new Promise(function (resolve) { resolve("Hello this app runs since " + performance.now()); });
    }).then(function () { return console.log("Function2 registered"); }).catch(function (err) { return console.log(err.toString()); });
    android.registerFunctionWithPromiseAndArg(function (add) {
        return new Promise(function (resolve) {
            var result = add.a + add.b;
            resolve("WEB CALCULATION: " + add.a + " + " + add.b + " = " + result);
        });
    }).then(function () { return console.log("Function3 registered"); }).catch(function (err) { return console.log(err.toString()); });
});
