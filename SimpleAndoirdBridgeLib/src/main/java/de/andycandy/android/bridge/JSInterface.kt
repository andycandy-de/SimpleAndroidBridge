package de.andycandy.android.bridge

import androidx.annotation.Keep
import java.util.concurrent.Executors
@Keep
interface JSInterface {
    val name: String
}

@Keep
open class DefaultJSInterface(override val name: String) : JSInterface {

    private val executor = Executors.newCachedThreadPool()

    fun <T> doInBackground(block: (Promise<T>) -> Unit): Promise<T> {
        val promise = Promise<T>()
        executor.execute { block(promise) }
        return promise
    }
}