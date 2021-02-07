package de.andycandy.android.bridge

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NativeCall(val value: CallType = CallType.FULL_PROMISE)

enum class CallType{FULL_SYNC, WEB_PROMISE, FULL_PROMISE}