# SimpleAndroidBridge

Build a bridge! Seamless Integration between Android and Webapps

Effortlessly build a robust bridge between your Android and Webapp with the SimpleAndroidBridge library. This library allows you to render a web application in an Android WebView and create a JSInterface to enable smooth communication between the two platforms. Share complex objects, promises, and callback functions, all while your web application remains responsive and efficient.

✓ Share Objects - Android ⇄ Web

✓ Share Promise - Android ⇄ Web

✓ Callback Functions - Android ← Web

✓ Call Functions non-blocking - Android ⇄ Web

✓ Type safety with typescript - Android + Web

## Features

### Share Complex Objects with Ease

The built-in JavaScript bridge in the Android SDK only supports primitive types. With SimpleAndroidBridge, you can share complex objects between Android and web with ease. Simply define the types as arguments or return values in your Android native functions, and the library will automatically convert JavaScript objects to Kotlin objects and vice versa.

```kotlin
// Kotlin
class AndroidNativeInterface(val contactService: ContactService): DefaultJSInterface("Android") {

    @NativeCall(CallType.FULL_SYNC)
    fun searchContact(contactFilter: ContactFilter): List<Contact> {
        return contactService.search(contactFilter)
    }
}

data class ContactFilter(val surname: String? = null, val firstname: String? = null)
data class Contact(val surname: String? = null, val fistname: String? = null,
    val mail: String? = null, val phonenumber: String? = null)
```

```js
// Javascript
console.log(Bridge.interfaces.Android.searchContact({surname: "Pitt"}))
```

### Promises Made Easy

The JavaScript bridge in the Android SDK executes functions in a blocking manner, causing the web application to freeze until the native function returns. However, with this library, you can define a Promise return type, allowing for non-blocking execution. By utilizing the 'doInBackground' function, the Android code is executed in a background thread, preventing the web application from being blocked.

```kotlin
// Kotlin
class AndroidNativeInterface(val contactService: ContactService): DefaultJSInterface("Android") {

    @NativeCall(CallType.FULL_PROMISE)
    fun searchContact(contactFilter: ContactFilter) = doInBackground<List<Contact>> { promise ->
        try {
            promise.resolve(contactService.search(contactFilter))
        } catch (e: Exception) {
            promise.reject(e)
        }
    }
}

data class ContactFilter(val surname: String? = null, val firstname: String? = null)
data class Contact(val surname: String? = null, val fistname: String? = null,
    val mail: String? = null, val phonenumber: String? = null)
```

```js
// Javascript
Bridge.interfaces.Android.searchContact({surname: "Pitt"}).then((list) => {
    console.log(list);
});
```

### Callback Functions

If you're familiar with JavaScript, you're likely no stranger to callback functions. SimpleAndroidBridge takes this concept a step further, allowing you to inject these JavaScript callback functions directly into the Android layer, thereby creating a seamless interaction between your web application and Android.

```kotlin
// Kotlin
class AndroidNativeInterface(val button: Button): DefaultJSInterface("Android") {

    @NativeCall(CallType.FULL_SYNC)
    fun registerOnClickAction(jsFunction: JSFunction) {
        button.setOnClickListener { jsFunction() }
    }
}
```

```js
// Javascript
Bridge.interfaces.Android.registerOnClickAction(() => {
    console.log("Button Clicked!")
})
```

To pass an argument to a JavaScript function, use the `JSFunctionWithArg` type, which is specifically designed to accept an argument.

```kotlin
// Kotlin
class AndroidNativeInterface(val button: Button): DefaultJSInterface("Android") {

    var i = 0
    
    @NativeCall(CallType.FULL_SYNC)
    fun registerOnClickAction(jsFunction: JSFunctionWithArg<Int>) {
        button.setOnClickListener { jsFunction(++i) }
    }
}
```

```js
// Javascript
Bridge.interfaces.Android.registerOnClickAction((i) => {
    console.log("Button Clicked! " + i)
})
```

To pass multiple arguments to a function, consider creating a data class.

For functions that need to return a result to the Android layer, you can use either `JSFunctionWithPromise` for functions without arguments or `JSFunctionWithPromiseAndArg` for functions that accept an argument.

```kotlin
// Kotlin
class AndroidNativeInterface(val button: Button): DefaultJSInterface("Android") {

    @NativeCall(CallType.FULL_SYNC)
    fun registerOnClickAction(jsFunction: JSFunctionWithPromiseAndArg<Add, Int>) {
        button.setOnClickListener {
            val add = Add((Math.random() * 10).toInt(), (Math.random() * 10).toInt())
            jsFunction(add)
                .then{ Log.d("AndroidNativeInterface", "Web calculated: ${add.a} + ${add.b} = $it") }
                .catch{ Log.e("AndroidNativeInterface", "ERROR IN WEB LAYER: $it") }
        }
    }
    
    data class Add(a: Int, b: Int)
}
```

```js
// Javascript
Bridge.interfaces.Android.registerOnClickAction((add) => {
    return new Promise((resolve) => { resolve(add.a + add.b) })
})
```

---
**NOTE**

To release a `JSFunction` and clear its binding, simply call the `close` function. `JSFunction` implements the `AutoCloseable` interface, enabling you to utilize try-with-resources or `AutoCloseable.use {}` blocks to automatically manage the function's lifecycle and ensure proper cleanup.
```kotlin
function.use { it() }
```

Additionally, if your web application or WebView supports reloading, it's recommended to add an AfterInitializeListener to the Bridge. This listener will help release any available `JSFunctions`, ensuring a clean state after initialization.

---

### Different native call types

This library supports different native call types which let you decide how to call the native code.

#### Full sync

The `CallType.FULL_SYNC` call type invokes native code in a blocking manner, causing JavaScript execution to pause until the native Android function returns. As a result, the web view remains unresponsive until the native execution is complete. *(Not recommended for long-running tasks)*

```kotlin
// Kotlin
@NativeCall(CallType.FULL_SYNC)
fun searchContact(contactFilter: ContactFilter): List<Contact> {
    return contactService.search(contactFilter)
}
```

```js
// Javascript
console.log(Bridge.interfaces.Android.searchContact({surname: "Pitt"}))
```

#### Web promise

The `CallType.WEB_PROMISE` call type functions similarly to the `FULL_SYNC` call, with the key difference being that the JavaScript call returns a promise. However, the native Android function is still invoked in a blocking manner. *(Recommended if you're uncertain about the task duration and may need to migrate to `FULL_PROMISE` in the future)*

```kotlin
// Kotlin
@NativeCall(CallType.WEB_PROMISE)
fun searchContact(contactFilter: ContactFilter): List<Contact> {
    return contactService.search(contactFilter)
}
```

```js
// Javascript
Bridge.interfaces.Android.searchContact({surname: "Pitt"}).then((list) => {
    console.log(list);
});
```

#### Full promise

The `CallType.FULL_PROMISE` call type enables you to execute native Android code in a background thread, allowing JavaScript execution to continue uninterrupted. As a result, the web view remains responsive and free to perform its tasks. *(Recommended for long-running tasks)*

```kotlin
// Kotlin
@NativeCall(CallType.FULL_PROMISE)
fun searchContact(contactFilter: ContactFilter) = doInBackground<List<Contact>> { promise ->
    try {
        promise.resolve(contactService.search(contactFilter))
    } catch (e: Exception) {
        promise.reject(e)
    }
}
```

```js
// Javascript
Bridge.interfaces.Android.searchContact({surname: "Pitt"}).then((list) => {
    console.log(list);
});
```

## Setup

### Add the library to your android project

Add maven central to the repositories block.
```gradle
repositories {
    google()
    mavenCentral()
}
```

Add the library to the dependencies block.
```gradle
dependencies {
    implementation 'com.github.andycandy-de:simple-android-bridge:1.1.1'
}
```

### Create a javascript interface

```kotlin
class AndroidNativeInterface: DefaultJSInterface("Android") {

    @NativeCall(CallType.FULL_SYNC)
    fun helloFullSync(name: String): String {
        return "hello $name"
    }

    @NativeCall(CallType.WEB_PROMISE)
    fun helloWebPromise(name: String): String {
        return "hello $name"
    }

    @NativeCall(CallType.FULL_PROMISE)
    fun helloFullPromise(name: String) = doInBackground<String> { promise ->
        promise.resolve("hello $name")
    }
}
```

### Create the bridge and add the interface

```kotlin
val bridge = Bridge(applicationContext, webView)
bridge.addJSInterface(AndroidNativeInterface())
```

### Initialize the bridge (Javascript or Android)

Android code

```kotlin
// Bridge can be initialized by calling the 'init' function inside
// the 'onPageStarted' function of a WebViewClient

webView.webViewClient = object : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        bridge.init()
    }
}
```

Javascript code

```js
// Bridge can be initialized by calling the 'init' function in
// Javascript. Register function to 'Bridge.afterInitialize' to
// start the webapp after the bridge is initialized.

function startApp(f) {
    if (Bridge.initialized) {
        f()
    } else {
        Bridge.afterInitialize = f
    }
}

Bridge.init()

startApp(() => {
    // Start your webapp
});
```

### Access the interface in the webapp

```js
console.log(Bridge.interfaces.Android.helloFullSync("Web"))
```

## License

MIT License

Copyright (c) 2020 andycandy-de<br>
Copyright (c) 2021 andycandy-de<br>
Copyright (c) 2024 andycandy-de

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
