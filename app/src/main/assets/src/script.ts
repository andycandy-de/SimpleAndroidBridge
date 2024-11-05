// @ts-ignore
const bridge = (<Bridge>Bridge)

function startApp(f: () => void) {
    if (bridge.initialized) {
        f()
    } else {
        bridge.afterInitialize = f
    }
}

startApp(() => {
    const text = document.getElementById("text")!
    text.innerHTML = ""
    
    const appendText = (s: string) => {
        text.innerHTML = text.innerHTML + "</br>" + s
    }
    const android = bridge.interfaces.Android

    appendText(android.helloFullSync("Web"))
    android.helloWebPromise("Web").then((s) => {appendText(s)})
    android.helloFullPromise("Web").then((s) => {appendText(s)})
    
    android.registerFunction((i) => {
        appendText(i.toLocaleString())
    }).then(() => console.log("Function1 registered")).catch((err) => console.log(err.toString()))

    android.registerFunctionWithPromise(() => {
        return new Promise<string>((resolve) => {resolve("Hello this app runs since " + performance.now())})
    }).then(() => console.log("Function2 registered")).catch((err) => console.log(err.toString()))
    
    android.registerFunctionWithPromiseAndArg((add) => {
        return new Promise<string>((resolve) => {
            const result = add.a + add.b
            resolve(`WEB CALCULATION: ${add.a} + ${add.b} = ${result}`)
        })
    }).then(() => console.log("Function3 registered")).catch((err) => console.log(err.toString()))
})

// Api definitions

interface AndroidInterface {
    helloFullSync(name: string): string
    helloWebPromise(name: string): Promise<string>
    helloFullPromise(name: string): Promise<string>
    registerFunction(f: JSFunctionWithArg<number>): Promise<void>
    registerFunctionWithPromise(f: JSFunctionWithPromise<string>): Promise<void>
    registerFunctionWithPromiseAndArg(f: JSFunctionWithPromiseAndArg<Add, string>): Promise<void>
}

interface Add {
    a: number
    b: number
}

interface JSFunctionWithArg<A> {
    (a: A): void
}

interface JSFunctionWithPromise<R> {
    (): Promise<R>
}

interface JSFunctionWithPromiseAndArg<A, R> {
    (a: A): Promise<R>
}

interface Bridge {
    initialized: boolean
    afterInitialize: () => void
    interfaces: {Android: AndroidInterface}
}