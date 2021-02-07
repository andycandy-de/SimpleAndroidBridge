package de.andycandy.android.bridge

import android.util.Log
import android.webkit.WebView

open class JSFunctionParent(val bridge: Bridge, val functionBinding: Long) {

    private var closed: Boolean = false

    protected fun checkClosed() = synchronized(this) {
        if (closed) error("Function is already closed!")
    }

    fun close() {
        synchronized(this) {
            checkClosed()
            closed = true
        }
        bridge.removeFunction(this)
    }

    fun finalize() {
        if (!closed) {
            Log.w("JSFunction", "There is no more reference to this function but the close function is not called!")
            bridge.removeFunction(this)
        }
    }
}

class JSFunction(bridge: Bridge, functionBinding: Long) : JSFunctionParent(bridge, functionBinding) {
    fun call() {
        checkClosed()
        bridge.callJSFunction(this)
    }
}

class JSFunctionWithArg<A>(bridge: Bridge, functionBinding: Long) : JSFunctionParent(bridge, functionBinding) {
    fun call(arg: A) {
        checkClosed()
        bridge.callJSFunction(this, arg)
    }
}
/*
class JSFunctionWithPromise<R>(bridge: Bridge, functionBinding: Long) : JSFunctionParent(bridge, functionBinding) {
    fun call(): Promise<R> {
        checkClosed()
        return bridge.callJSFunctionWithPromise(this)
    }
}*/