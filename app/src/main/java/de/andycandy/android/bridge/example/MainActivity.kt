package de.andycandy.android.bridge.example

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.captionBarPadding
import androidx.compose.foundation.layout.mandatorySystemGesturesPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import de.andycandy.android.bridge.Bridge
import de.andycandy.android.bridge.JSFunction
import de.andycandy.android.bridge.JSFunctionWithArg
import de.andycandy.android.bridge.JSFunctionWithPromise
import de.andycandy.android.bridge.JSFunctionWithPromiseAndArg
import de.andycandy.android.bridge.example.ui.theme.SimpleAndroidBridgeExampleTheme

class MainActivity : ComponentActivity() {

    private var function1: JSFunctionWithArg<Int>? = null
    private var function2: JSFunctionWithPromise<Int>? = null
    private var function3: JSFunctionWithPromiseAndArg<Add, Int>? = null
    private var function4: JSFunction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SimpleAndroidBridgeExampleTheme {
                Column (
                    Modifier
                        .statusBarsPadding()
                        .captionBarPadding()
                        .navigationBarsPadding()
                        .mandatorySystemGesturesPadding()
                ) {
                    Column (
                        Modifier
                            .weight(1f)
                            .padding(12.dp)
                    )
                    {
                        Text("WebView", modifier = Modifier.padding(8.dp), fontSize = 16.sp)
                        WebViewScreen(this@MainActivity, Modifier.weight(1f)
                            .border(8.dp, Color.DarkGray, RoundedCornerShape(12.dp)))
                    }
                    Column {
                        Row {
                            var counter by remember {
                                mutableIntStateOf(0)
                            }
                            var runSince by remember {
                                mutableStateOf("?")
                            }
                            Button(
                                onClick = {
                                    ++counter
                                    function1?.call(counter)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "Counter: $counter")
                            }
                            Button(
                                onClick = {
                                    function2?.call()?.then {
                                        runSince = (it / 1000).toString()
                                        val message = "The web app runs since ${it}ms"
                                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                                    }?.catch {
                                        Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "Run time: ${runSince}s")
                            }
                        }
                        Row {
                            var value1 by remember {
                                mutableStateOf("${((Math.random() * 1000) - 500).toInt()}")
                            }
                            var value2 by remember {
                                mutableStateOf("${((Math.random() * 1000) - 500).toInt()}")
                            }
                            var result by remember {
                                mutableStateOf("?")
                            }
                            Row (modifier = Modifier.weight(1f)) {
                                OutlinedTextField(value1, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), onValueChange = {
                                    if (it.matches("^-?[0-9]*$".toRegex()) && it.length < 6)
                                        value1 = it
                                })
                                OutlinedTextField(value2, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), onValueChange = {
                                    if (it.matches("^-?[0-9]*$".toRegex()) && it.length < 6)
                                        value2 = it
                                })
                            }
                            Button(
                                onClick = {
                                    val v1 =
                                        if (value1.matches("^-?$".toRegex())) 0 else value1.toInt()
                                    val v2 =
                                        if (value2.matches("^-?$".toRegex())) 0 else value2.toInt()
                                    function3?.call(Add(v1, v2))?.then {
                                        result = "$it"
                                        val message = "WEB CALCULATION: $v1 + $v2 = $result"
                                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                                    }?.catch {
                                        Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "Sum: $result")
                            }
                        }
                        Row {
                            Button(
                                onClick = {
                                    try {
                                        function4?.call()
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                                        Log.e("MainActivity", e.message, e)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "Removed function binding error demo")
                            }
                        }
                    }
                }
            }
        }
    }

    fun registerFunctionToButton1(function: JSFunctionWithArg<Int>) {
        function1 = function
    }

    fun registerFunctionToButton2(function: JSFunctionWithPromise<Int>) {
        function2 = function
    }

    fun registerFunctionToButton3(function: JSFunctionWithPromiseAndArg<Add, Int>) {
        function3 = function
    }

    fun registerFunctionToButton4(function: JSFunction) {

        // For the demo this function is just set the first time
        // This is a negative example to demonstrate what happens
        // when the WebView reloads and the function is not
        // released properly

        function4 = function4 ?: function
    }

    fun clearFunctions() {
        function1?.close()
        function2?.close()
        function3?.close()
        function1 = null
        function2 = null
        function3 = null
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(mainActivity: MainActivity, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->

            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .build()

            return@AndroidView WebView(context).apply {
                settings.javaScriptEnabled = true

                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.setSupportZoom(false)

                val bridge = Bridge(context, this)
                bridge.addJSInterface(AndroidNativeInterface(mainActivity))
                bridge.addAfterInitializeListener { mainActivity.clearFunctions() }

                webViewClient = object : WebViewClient() {

                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request.url)
                    }
                }

                loadUrl("https://appassets.androidplatform.net/assets/www/index.html")
            }
        }
    )
}