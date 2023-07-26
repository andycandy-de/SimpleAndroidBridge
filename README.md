# SimpleAndroidBridge

Build a bridge! This library is created to create a powerful interface between Android and Webapp. You can render the Webapp in an Android webview. Create a JSInterface to access the Android layer from the Webapp. You can also call web functions from the android layer.

✓ Share Objects - Android ⇄ Web

✓ Share Promise - Android ⇄ Web

✓ Callback Functions - Android ← Web

✓ Call Functions non-blocking - Android ⇄ Web

✓ Type safety with typescript - Android + Web

## Features

### Share Objects

The javascript bridge which is built in the android sdk just accepts primitive types. That is not enogth? This librabry allows you to share complex objects between web and android. Just define the types as arguments or return in the android native functions. This library automatically converts a javascript object to a kotlin object and vice versa.

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

### Share Promise

The javascript bridge which is built in the android sdk executes all functions in a blocking way. The webapp is fully blocked until the native function returns. With this library you can define a Promise return type. With the 'doInBackground' function the android code is executed in a background thread and the webapp is not blocked.

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

If you know Javascript, you also know callback functions. With this Library you can
inject javascript callback functions into the Android layer.

```kotlin
// Kotlin
class AndroidNativeInterface(val button: Button): DefaultJSInterface("Android") {

    @NativeCall(CallType.FULL_SYNC)
    fun registerOnClickAction(jsFunction: JSFunction) {
        button.setOnClickListener { jsFunction.call() }
    }
}
```

```js
// Javascript
Bridge.interfaces.Android.registerOnClickAction(() => {
    console.log("Button Clicked!")
})
```

You want to pass an argument to a Javascript function. Just use the type JSFunctionWithArg
which accepts an argument.

```kotlin
// Kotlin
class AndroidNativeInterface(val button: Button): DefaultJSInterface("Android") {

    var i = 0
    
    @NativeCall(CallType.FULL_SYNC)
    fun registerOnClickAction(jsFunction: JSFunctionWithArg<Int>) {
        button.setOnClickListener { jsFunction.call(++i) }
    }
}
```

```js
// Javascript
Bridge.interfaces.Android.registerOnClickAction((i) => {
    console.log("Button Clicked! " + i)
})
```

To pass more than one argument to a function you can create a data class.

There are also function which can pass a result to the Android layer. Just use the class
JSFunctionWithPromise or JSFunctionWithPromiseAndArg.

```kotlin
// Kotlin
class AndroidNativeInterface(val button: Button): DefaultJSInterface("Android") {

    @NativeCall(CallType.FULL_SYNC)
    fun registerOnClickAction(jsFunction: JSFunctionWithPromiseAndArg<Add, Int>) {
        button.setOnClickListener {
            val add = Add((Math.random() * 10).toInt(), (Math.random() * 10).toInt())
            jsFunction.call(add)
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

The resolve and reject of a Promise from a JSFunctionWithPromise is executed in a background thread.
If you don't want to call the JSFunction anymore just call the function 'close' to clear the function binding.

---

### Different native call types

This library supports different native call types which let you decide how to call the native code.

#### Full sync

The call type `CallType.FULL_SYNC` calls the native code in a blocking way. The javascript execution waits until the native android function returns. The drawback is the the web view doesn't interact until the native execution terminates. *(Not recommended for long running tasks)*

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

The call type `CallType.WEB_PROMISE` works exactly like the **Full sync** call does. The difference is that the return of the javascript call is a promise. But the native android function is still called in a blocking way. *(Recommended if you are unsure about duration and you might need to migrate to FULL_PROMISE)*

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

The call type `CallType.FULL_PROMISE` allowes you to call the native android code in a background thread. So the javascript execution is not blocked and web view is free to perform its work. *(Recommended for long running tasks)*

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
    implementation 'com.github.andycandy-de:simple-android-bridge:1.0.2'
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

Copyright (c) 2022 andycandy-de

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
