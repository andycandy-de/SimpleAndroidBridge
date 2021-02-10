package de.andycandy.android.bridge

import android.util.Log
import kotlin.reflect.KClass

open class JSFunctionParent(val innerBridge: InnerBridge, val functionBinding: Long) {

    private var closed: Boolean = false

    protected fun checkClosed() = synchronized(this) {
        if (closed) error("Function is already closed!")
    }

    fun close() {
        synchronized(this) {
            checkClosed()
            closed = true
        }
        innerBridge.removeFunction(this)
    }

    fun finalize() {
        if (!closed) {
            Log.w("JSFunction", "There is no more reference to this function but the close function is not called!")
            innerBridge.removeFunction(this)
        }
    }
}

class JSFunction(innerBridge: InnerBridge, functionBinding: Long) : JSFunctionParent(innerBridge, functionBinding) {
    fun call() {
        checkClosed()
        innerBridge.callJSFunction(this)
    }
}

class JSFunctionWithArg<A>(innerBridge: InnerBridge, functionBinding: Long) : JSFunctionParent(innerBridge, functionBinding) {
    fun call(arg: A) {
        checkClosed()
        innerBridge.callJSFunction(this, arg)
    }
}

class JSFunctionWithPromise<R>(innerBridge: InnerBridge, functionBinding: Long, val kClass: KClass<*>) : JSFunctionParent(innerBridge, functionBinding) {
    fun call(): Promise<R> {
        checkClosed()
        return innerBridge.callJSFunctionWithPromise(this)
    }
}

class JSFunctionWithPromiseAndArg<A, R>(innerBridge: InnerBridge, functionBinding: Long, val kClass: KClass<*>) : JSFunctionParent(innerBridge, functionBinding) {
    fun call(arg: A): Promise<R> {
        checkClosed()
        return innerBridge.callJSFunctionWithPromise(this, arg)
    }
}