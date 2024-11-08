package de.andycandy.android.bridge

import java.util.UUID
import kotlin.reflect.KClass

open class JSFunctionParent(protected val innerBridge: InnerBridge, protected val functionUUID: UUID) : AutoCloseable {

    private var closed: Boolean = false

    protected fun checkClosed() = synchronized(this) {
        if (closed) error("Function is already closed!")
    }

    override fun close() = synchronized(this) {
        checkClosed()
        closed = true
        innerBridge.removeFunction(functionUUID)
    }
}

class JSFunction(innerBridge: InnerBridge, functionUUID: UUID) : JSFunctionParent(innerBridge, functionUUID) {

    fun call() = synchronized(this) {
        checkClosed()
        innerBridge.callJSFunction(functionUUID)
    }

    operator fun invoke() = call()
}

class JSFunctionWithArg<A>(innerBridge: InnerBridge, functionUUID: UUID) : JSFunctionParent(innerBridge, functionUUID) {

    fun call(arg: A) = synchronized(this) {
        checkClosed()
        innerBridge.callJSFunction(functionUUID, arg)
    }

    operator fun invoke (arg: A) = call(arg)
}

class JSFunctionWithPromise<R>(innerBridge: InnerBridge, functionUUID: UUID, private val kClass: KClass<*>) : JSFunctionParent(innerBridge, functionUUID) {

    fun call(): Promise<R> = synchronized(this) {
        checkClosed()
        return innerBridge.callJSFunctionWithPromise(functionUUID, kClass)
    }

    operator fun invoke() = call()
}

class JSFunctionWithPromiseAndArg<A, R>(innerBridge: InnerBridge, functionUUID: UUID, private val kClass: KClass<*>) : JSFunctionParent(innerBridge, functionUUID) {

    fun call(arg: A): Promise<R> = synchronized(this) {
        checkClosed()
        return innerBridge.callJSFunctionWithPromise(functionUUID, kClass, arg)
    }

    operator fun invoke(arg: A) = call(arg)
}