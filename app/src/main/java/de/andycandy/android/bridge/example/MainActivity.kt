package de.andycandy.android.bridge.example

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.webkit.WebViewAssetLoader
import de.andycandy.android.bridge.Bridge
import de.andycandy.android.bridge.JSFunctionWithArg
import de.andycandy.android.bridge.JSFunctionWithPromise
import de.andycandy.android.bridge.JSFunctionWithPromiseAndArg

class MainActivity : AppCompatActivity() {

    var counter = 0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WebView.setWebContentsDebuggingEnabled(true)

        val webView = findViewById<WebView>(R.id.webView)

        val bridge = Bridge(applicationContext, webView)
        bridge.addJSInterface(AndroidNativeInterface(this@MainActivity))

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                bridge.init()
            }
        }

        webView.loadUrl("https://appassets.androidplatform.net/assets/www/index.html")
    }

    fun registerFunctionToButton1(function: JSFunctionWithArg<Int>) {
        findViewById<Button>(R.id.button).setOnClickListener {
            ++counter
            function.call(counter)
        }
    }

    fun registerFunctionToButton2(function: JSFunctionWithPromise<String>) {
        findViewById<Button>(R.id.button2).setOnClickListener {
            function.call().then {
                runOnUiThread { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
            }.catch {
                runOnUiThread { Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun registerFunctionToButton3(function: JSFunctionWithPromiseAndArg<Add, String>) {
        findViewById<Button>(R.id.button3).setOnClickListener {
            function.call(Add((Math.random() * 10).toInt(), (Math.random() * 10).toInt())).then {
                runOnUiThread { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
            }.catch {
                runOnUiThread { Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show() }
            }
        }
    }
}
