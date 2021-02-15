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
Console.log(Bridge.interfaces.Android.search({surname: "Pitt"}))
```

## Setup

### Add the libary to your android project

```
implementation 'com.github.andycandy-de:simple-android-bridge:1.0.0-BETA-b01'
```

### Create a interface

```
interface AndroidInterface {

    @NativeCall(CallType.FULL_SYNC)
    fun helloFullSync(name: String): String

    @NativeCall(CallType.WEB_PROMISE)
    fun helloWebPromise(name: String): String

    @NativeCall(CallType.FULL_PROMISE)
    fun helloFullPromise(name: String): Promise<String>
}
```

### Add a implementation class

```
class AndroidNativeInterface: DefaultJSInterface("Android"), AndroidInterface {

    override fun helloFullSync(name: String): String {
        return "hello $name"
    }

    override fun helloWebPromise(name: String): String {
        return "hello $name"
    }

    override fun helloFullPromise(name: String) = doInBackground<String> { promise ->
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
