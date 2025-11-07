package de.andycandy.android.bridge.example

import de.andycandy.android.bridge.CallType
import de.andycandy.android.bridge.JSFunction
import de.andycandy.android.bridge.JSFunctionWithArg
import de.andycandy.android.bridge.JSFunctionWithPromise
import de.andycandy.android.bridge.JSFunctionWithPromiseAndArg
import de.andycandy.android.bridge.NativeCall
import de.andycandy.android.bridge.Promise

interface AndroidInterface {

    @NativeCall(CallType.FULL_SYNC)
    fun helloFullSync(name: String): String

    @NativeCall(CallType.WEB_PROMISE)
    fun helloWebPromise(name: String): String

    @NativeCall(CallType.FULL_PROMISE)
    fun helloFullPromise(name: String): Promise<String>

    @NativeCall(CallType.FULL_SYNC)
    fun longRunningTaskFullSync(sleepTimeInSec: Int): String

    @NativeCall(CallType.WEB_PROMISE)
    fun longRunningTaskWebPromise(sleepTimeInSec: Int): String

    @NativeCall(CallType.FULL_PROMISE)
    fun longRunningTaskFullPromise(sleepTimeInSec: Int): Promise<String>

    @NativeCall(CallType.FULL_SYNC)
    fun registerJSFunctionWithArg(function: JSFunctionWithArg<Int>)

    @NativeCall(CallType.FULL_SYNC)
    fun registerFunctionWithPromise(function: JSFunctionWithPromise<Int>)

    @NativeCall(CallType.FULL_SYNC)
    fun registerFunctionWithPromiseAndArg(function: JSFunctionWithPromiseAndArg<Add, Int>)

    @NativeCall(CallType.FULL_SYNC)
    fun registerFunction(function: JSFunction)

    @NativeCall(CallType.FULL_SYNC)
    fun toNumSystem(number: Int, numSystem: NumSystem): String
}

data class Add(val a: Int, val b: Int)

enum class NumSystem(val base: Int) {

    BIN(2),
    OCT(8),
    HEX(16);

    fun convert(value: Int): String {
        var current = value
        var result = ""
        while (current != 0) {
            result = "${toChar(current % base)}$result"
            current /= base
        }
        return result.ifEmpty { "0" }
    }

    private fun toChar(value: Int) = if (value < 10) {
       '0' + value
    } else {
       'A' + value - 10
    }
}