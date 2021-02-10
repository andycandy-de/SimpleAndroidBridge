package de.andycandy.android.bridge.example

import de.andycandy.android.bridge.*

interface AndroidInterface {

    @NativeCall(CallType.FULL_SYNC)
    fun helloFullSync(name: String): String

    @NativeCall(CallType.WEB_PROMISE)
    fun helloWebPromise(name: String): String

    @NativeCall(CallType.FULL_PROMISE)
    fun helloFullPromise(name: String): Promise<String>

    @NativeCall(CallType.FULL_PROMISE)
    fun registerFunction(function: JSFunctionWithArg<Int>): Promise<Unit>

    @NativeCall(CallType.FULL_PROMISE)
    fun registerFunctionWithPromise(function: JSFunctionWithPromise<String>): Promise<Unit>

    @NativeCall(CallType.FULL_PROMISE)
    fun registerFunctionWithPromiseAndArg(function: JSFunctionWithPromiseAndArg<Add, String>): Promise<Unit>
}

data class Add(val a: Int, val b: Int)