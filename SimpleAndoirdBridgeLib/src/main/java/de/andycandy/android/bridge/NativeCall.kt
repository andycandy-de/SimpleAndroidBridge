package de.andycandy.android.bridge

import androidx.annotation.Keep

@Keep
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NativeCall(val value: CallType = CallType.FULL_PROMISE)
@Keep
enum class CallType{FULL_SYNC, WEB_PROMISE, FULL_PROMISE}