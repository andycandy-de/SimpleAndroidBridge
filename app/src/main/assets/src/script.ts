// @ts-ignore
const bridge = (<Bridge>Bridge)

function startApp(f: () => void) {
    if (bridge.initialized) {
        f()
    } else {
        bridge.afterInitialize = f
    }
}

bridge.init()

startApp(() => {

    let runTimeVal = 0

    const text = document.getElementById("text")!
    const reload = document.getElementById("reload")!
    const info = document.getElementById("info")!
    text.innerHTML = ""

    const appendText = (s: string) => {
        text.innerHTML = `${text.innerHTML}</br>${s}`
    }

    const infoUpdater = () => {
        runTimeVal = Math.floor(performance.now())
        info.innerHTML = `Run time ${runTimeVal}ms, open function bindings ${bridge.getFunctionBinding().length}`
    }

    infoUpdater()
    setInterval(infoUpdater, 31)
    reload.addEventListener("click", () => { window.location.reload() })

    // define the interface as const
    const android = bridge.interfaces.Android

    // call different native call types
    appendText(android.helloFullSync("Web"))
    android.helloWebPromise("Web").then((s) => { appendText(s) })
    android.helloFullPromise("Web").then((s) => { appendText(s) })

    // register functions to the native layer
    android.registerJSFunctionWithArg((i) => {
        appendText(i.toLocaleString())
    })

    android.registerFunctionWithPromise(() => {
        return new Promise<number>((resolve) => {
            appendText(`Sending run time ${runTimeVal}ms to native layer!`)
            resolve(runTimeVal)
        })
    })
    
    android.registerFunctionWithPromiseAndArg((add) => {
        return new Promise<number>((resolve) => {
            const result = add.a + add.b
            appendText(`Sending result ${result} of web calculation from native values ${add.a} + ${add.b} to native layer!`)
            resolve(result)
        })
    })

    android.registerFunction(() => {
        appendText(`Function binding demo is available!`)
    })
})

// Api definitions

interface AndroidInterface {
    helloFullSync(name: string): string
    helloWebPromise(name: string): Promise<string>
    helloFullPromise(name: string): Promise<string>
    registerJSFunctionWithArg(f: JSFunctionWithArg<number>): void
    registerFunctionWithPromise(f: JSFunctionWithPromise<number>): void
    registerFunctionWithPromiseAndArg(f: JSFunctionWithPromiseAndArg<Add, number>): void
    registerFunction(f: JSFunction): void
}

interface Add {
    a: number
    b: number
}

interface JSFunction {
    (): void
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
    init: () => void
    initialized: boolean
    afterInitialize: () => void
    interfaces: {Android: AndroidInterface}
    getFunctionBinding: () => number[]
}