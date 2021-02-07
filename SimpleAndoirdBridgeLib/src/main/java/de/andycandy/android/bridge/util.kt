package de.andycandy.android.bridge

import android.os.Handler
import android.os.Looper
import kotlin.reflect.KClass
import kotlin.reflect.KType

fun doInMainThread(block: () -> Unit) {
    if (Looper.getMainLooper().thread == Thread.currentThread()) {
        block()
    } else {
        Handler(Looper.getMainLooper()).post(block)
    }
}

fun KType.kClass() = this.classifier as KClass<*>