-keepattributes Signature
-keepattributes Exceptions
-keepattributes SetJavaScriptEnabled
-keepattributes JavascriptInterface
-keepattributes InlinedApi
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class de.andycandy.android.bridge.** {*;}