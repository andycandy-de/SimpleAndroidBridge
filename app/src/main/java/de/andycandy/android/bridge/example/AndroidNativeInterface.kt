package de.andycandy.android.bridge.example

import de.andycandy.android.bridge.*

class AndroidNativeInterface(private val mainActivity: MainActivity): DefaultJSInterface("Android"), AndroidInterface {

    override fun helloFullSync(name: String): String {
        return "hello $name"
    }

    override fun helloWebPromise(name: String): String {
        return "hello $name"
    }

    override fun helloFullPromise(name: String) = doInBackground<String> { promise ->
        promise.resolve("hello $name")
    }

    override fun registerFunction(function: JSFunctionWithArg<Int>) = doInBackground<Unit> { promise ->
        mainActivity.registerFunctionToButton1(function)
        promise.resolve(Unit)
    }

    override fun registerFunctionWithPromise(function: JSFunctionWithPromise<String>) = doInBackground<Unit> { promise ->
        mainActivity.registerFunctionToButton2(function)
        promise.resolve(Unit)
    }

    override fun registerFunctionWithPromiseAndArg(function: JSFunctionWithPromiseAndArg<Add, String>) = doInBackground<Unit> { promise ->
        mainActivity.registerFunctionToButton3(function)
        promise.resolve(Unit)
    }
}