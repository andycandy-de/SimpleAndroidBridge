package de.andycandy.android.bridge.example

import de.andycandy.android.bridge.DefaultJSInterface
import de.andycandy.android.bridge.JSFunction
import de.andycandy.android.bridge.JSFunctionWithArg
import de.andycandy.android.bridge.JSFunctionWithPromise
import de.andycandy.android.bridge.JSFunctionWithPromiseAndArg

class AndroidNativeInterface(private val mainActivity: MainActivity): DefaultJSInterface("Android"), AndroidInterface {

    override fun helloFullSync(name: String): String {
        return "Hello $name FullSync from Thread ${Thread.currentThread().name}"
    }

    override fun helloWebPromise(name: String): String {
        return "Hello $name WebPromise from Thread ${Thread.currentThread().name}"
    }

    override fun helloFullPromise(name: String) = doInBackground<String> { promise ->
        promise.resolve("Hello $name FullPromise from Thread ${Thread.currentThread().name}")
    }

    override fun longRunningTaskFullSync(sleepTimeInSec: Int): String {
        Thread.sleep(sleepTimeInSec.toLong() * 1000)
        return "${Thread.currentThread().name} finished the task after ${sleepTimeInSec}s!"
    }

    override fun longRunningTaskWebPromise(sleepTimeInSec: Int): String {
        Thread.sleep(sleepTimeInSec.toLong() * 1000)
        return "${Thread.currentThread().name} finished the task after ${sleepTimeInSec}s!"
    }

    override fun longRunningTaskFullPromise(sleepTimeInSec: Int) = doInBackground<String> { promise ->
        Thread.sleep(sleepTimeInSec.toLong() * 1000)
        promise.resolve("${Thread.currentThread().name} finished the task after ${sleepTimeInSec}s!")
    }

    override fun registerJSFunctionWithArg(function: JSFunctionWithArg<Int>) {
        mainActivity.registerFunctionToButton1(function)
    }

    override fun registerFunctionWithPromise(function: JSFunctionWithPromise<Int>) {
        mainActivity.registerFunctionToButton2(function)
    }

    override fun registerFunctionWithPromiseAndArg(function: JSFunctionWithPromiseAndArg<Add, Int>) {
        mainActivity.registerFunctionToButton3(function)
    }

    override fun registerFunction(function: JSFunction) {
        mainActivity.registerFunctionToButton4(function)
    }

    override fun toNumSystem(number: Int, numSystem: NumSystem): String {
        return numSystem.convert(number)
    }
}