package de.andycandy.android.bridge

import android.util.Log
import java.util.function.Consumer

class Promise<R> {

    enum class State {PENDING, RESOLVED, REJECTED}

    var state = State.PENDING
        private set

    private var thenList: MutableList<Consumer<R>>? = mutableListOf<Consumer<R>>()

    private var catchList: MutableList<Consumer<Throwable>>? = mutableListOf<Consumer<Throwable>>()

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
                it.accept(r)
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
                it.accept(e)
            } catch (e: Exception) {
                Log.e("Promise", "Error in catch block", e)
            }
        }
    }

    fun then(block: (R) -> Unit): Promise<R> {
        synchronized(this) {

            when (state) {
                State.RESOLVED -> block(uncheckedCast(value))
                State.PENDING -> thenList!!.add( Consumer {
                    block(it)
                })
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
                State.PENDING -> catchList!!.add( Consumer {
                    block(it)
                })
                else -> Unit
            }
        }
    }
}