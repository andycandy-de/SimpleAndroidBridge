package de.andycandy.android.bridge.example

import de.andycandy.android.bridge.CallType
import de.andycandy.android.bridge.DefaultJSInterface
import de.andycandy.android.bridge.JSFunctionWithArg
import de.andycandy.android.bridge.NativeCall

class AndroidNativeInterface(val mainActivity: MainActivity): DefaultJSInterface("Android") {

    @NativeCall(CallType.FULL_SYNC)
    fun helloFullSync(name: String): String {
        return "hello $name"
    }

    @NativeCall(CallType.WEB_PROMISE)
    fun helloWebPromise(name: String): String {
        return "hello $name"
    }

    @NativeCall(CallType.FULL_PROMISE)
    fun helloFullPromise(name: String) = doInBackground<String> { promise ->
        promise.resolve("hello $name")
    }

    @NativeCall(CallType.FULL_PROMISE)
    fun registerFunction(function: JSFunctionWithArg<Int>) = doInBackground<Unit> { promise ->
        mainActivity.registerFunctionToButton(function)
        promise.resolve(Unit)
    }
}