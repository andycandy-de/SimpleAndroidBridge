SimpleAndroidBridge Example App
================================

This example application demonstrates the usage of the SimpleAndroidBridge library, showcasing its capabilities and features. The app consists of a WebView and some native inputs and buttons below.

### NativeCall Types

The three buttons in the web view are designed to demonstrate the following NativeCall Types:

* FullSync: Pauses web rendering while executing native calls.
* WebPromise: Also pauses web rendering while executing native calls.
* FullPromise: Unlike the other two options, FullPromise does not pause web rendering, allowing for multiple concurrent native calls without affecting the web view's responsiveness.

You can test the FullPromise button multiple times to see how the web view remains responsive and efficient.

### Native-to-JavaScript Communication

Below the web view, you'll find native buttons that demonstrate how to call JavaScript code from the native layer and pass data through it. This showcases the library's ability to facilitate seamless communication between the native and web layers.

### Reload and Reinitialization

The reload link within the web view reloads the web content and reinitializes the bridge. To handle this scenario, the MainActivity is registered as an AfterInitializeListener to the bridge, ensuring that bounded JSFunctions are properly released. However, the "Removed function binding error demo" button is intentionally not released, serving as a negative example to illustrate the potential errors that can occur if JSFunctions are not properly released and re-injected during reloads.

### Best Practices

When reloading the web view, it's essential to release all available JSFunctions and inject new functions to the Android layer to avoid errors and ensure smooth functionality.