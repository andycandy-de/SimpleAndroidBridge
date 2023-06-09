package de.andycandy.android.bridge

import android.util.Log
import androidx.annotation.Keep

@Keep
class Promise<R> {

    enum class State {PENDING, RESOLVED, REJECTED}

    var state = State.PENDING
        private set

    private var thenList: MutableList<Callable<R>>? = mutableListOf()

    private var catchList: MutableList<Callable<Throwable>>? = mutableListOf()

    private var value: R? = null

    private var error: Throwable? = null

    fun resolve(r: R) {
        synchronized(this) {
            if (state != State.PENDING) error("Promise already finished!")
            state = State.RESOLVED
            value = r
            val list = thenList
            thenList = null
            return@synchronized list
        }?.forEach { it ->
            try {
                it.call(r)
            } catch (e: Exception) {
                Log.e("Promise", "Error in then block", e)
            }
        }
    }

    fun reject(e: Throwable) {
        synchronized(this) {
            if (state != State.PENDING) error("Promise already finished!")
            state = State.REJECTED
            error = e
            val list = catchList
            catchList = null
            return@synchronized list
        }?.forEach {
            try {
                it.call(e)
            } catch (e: Exception) {
                Log.e("Promise", "Error in catch block", e)
            }
        }
    }

    fun then(block: (R) -> Unit): Promise<R> {
        synchronized(this) {

            when (state) {
                State.RESOLVED -> block(uncheckedCast(value))
                State.PENDING -> thenList!!.add( toCallable { block(it) })
                else -> Unit
            }
        }
        return this
    }

    private fun <T> uncheckedCast(t: T?): T {
        @Suppress("UNCHECKED_CAST")
        return t as T
    }

    fun catch(block: (Throwable) -> Unit): Unit {
        synchronized(this) {

            when (state) {
                State.REJECTED -> block(error!!)
                State.PENDING -> catchList!!.add( toCallable { block(it) })
                else -> Unit
            }
        }
    }

    fun finalize() {
        if (state == State.PENDING) {
            Log.w("Promise", "Promise is not resolved or rejected!")
        }
    }

    private interface Callable<T>{
        fun call(t: T)
    }

    private fun <T> toCallable (block: (T) -> Unit) : Callable<T> {
        return object : Callable<T> {
            override fun call(t: T) {
                block(t)
            }
        }
    }
}