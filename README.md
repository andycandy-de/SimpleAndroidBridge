# SimpleAndroidBridge

Build a bridge! This library is created to create a powerful interface between Android and Webapp.

✓ Share Objects - Android ⇄ Web

✓ Share Promise - Android ⇄ Web

✓ Callback Functions - Android ← Web

✓ Call Functions non-blocking - Android ⇄ Web

✓ Type safety with typescript - Android + Web

## Features

### Share Objects

The javascript bridge which is built in the android sdk just accepts primitive types. That is not enogth? This librabry allows you to share complex objects between web and android. Just define the types as arguments or return in the android native functions. This library automatically converts a javascript object to a kotlin object and vice versa.

```
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

```
// Javascript
console.log(Bridge.interfaces.Android.search({surname: "Pitt"}))
```

### Share Promise

The javascript bridge which is built in the android sdk executes all functions in a blocking way. The webapp is fully blocked until the native function returns. With this libray you can define a Promise return type. With the 'doInBackground' function the android code is executed in a backgroud thread and the webapp is not blocked.

```
// Kotlin
class AndroidNativeInterface(val contactService: ContactService): DefaultJSInterface("Android") {

    @NativeCall(CallType.FULL_PROMISE)
    fun searchContact(contactFilter: ContactFilter): doInBackground<List<Contact>> { promise ->
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

```
// Javascript
Bridge.interfaces.Android.search({surname: "Pitt"}).then((list) => {
    console.log(list);
});
```

## Setup

### Add the libary to your android project

```
implementation 'com.github.andycandy-de:simple-android-bridge:1.0.0-BETA-b01'
```

### Create a javscript interface

```
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

```
val bridge = Bridge(applicationContext, webView)
bridge.addJSInterface(AndroidNativeInterface())
```

### Initialize the bridge (Javascript or Android)

Android code

```
// Bridge can be initialized by calling the 'init' function inside
// the 'onPageFinished' function of a WebViewClient

webView.webViewClient = object : WebViewClient() {

    override fun onPageFinished(view: WebView?, url: String?) {
        bridge.init()
    }
}
```

Javascript code

```
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

```
console.log(Bridge.interfaces.Android.helloFullSync("Web"))
```

## License

MIT License

Copyright (c) 2020 andycandy-de

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
