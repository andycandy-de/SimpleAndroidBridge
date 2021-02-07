const runAferInit = (f) => {
    if (Bridge.initialized) {
        f()
    }
    else  {
        Bridge.afterInitialize = f
    }
}

runAferInit(() => {
    const text = document.getElementById("text")
    text.innerHTML = ""
    
    const appendText = (s) => {
        text.innerHTML = text.innerHTML + "</br>" + s
    }
    
    text.innerHTML = Bridge.interfaces.Android.helloFullSync("Web")
    Bridge.interfaces.Android.helloWebPromise("Web").then((s) => {appendText(s)})
    Bridge.interfaces.Android.helloFullPromise("Web").then((s) => {appendText(s)})
    
    Bridge.interfaces.Android.registerFunction((i) => {
        appendText(i)
    }).then(() => console.log("Function registered")).catch((err) => console.log(err.toString()))
})