package de.andycandy.android.bridge.example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.webkit.WebViewAssetLoader
import de.andycandy.android.bridge.Bridge
import de.andycandy.android.bridge.JSFunctionWithArg

class MainActivity : AppCompatActivity() {

    var counter = 0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WebView.setWebContentsDebuggingEnabled(true)

        val webView = findViewById<WebView>(R.id.webView)

        val bridge = Bridge(applicationContext, webView)
        bridge.addJSInterface(AndroidNativeInterface(this))

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                bridge.init()
            }
        }

        webView.loadUrl("https://appassets.androidplatform.net/assets/www/index.html")
    }

    fun registerFunctionToButton(function: JSFunctionWithArg<Int>) {
        findViewById<Button>(R.id.button).setOnClickListener {
            ++counter
            function.call(counter)
        }
    }
}