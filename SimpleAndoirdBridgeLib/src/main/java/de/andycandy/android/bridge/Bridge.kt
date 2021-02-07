package de.andycandy.android.bridge

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KVariance

class Bridge(private val context: Context, private val webView: WebView, private val name: String = "Bridge") {

    init {
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(this, name)
    }

    private val gson = Gson()
    private val interfaces = mutableMapOf<String, JSInterfaceData>()

    private val afterInitializeListeners = mutableListOf<AfterInitializeListener>()

    @JavascriptInterface
    fun init() {
        val initScript = readInitScript()
        executeJavaScript(initScript + ";initBridge(${name},${createInterfacesJS()});")
    }

    @JavascriptInterface
    fun nativeAfterInitialize() {
        afterInitializeListeners.forEach(AfterInitializeListener::afterInitialize)
    }

    @JavascriptInterface
    fun nativeCall(callAsString: String): String {
        try {
            val call = gson.fromJson<Call>(callAsString, Call::class.java)
            val jsInterfaceData = findInterfaceData(call.interfaceName)
            val kFunction = findInterfaceFunction(jsInterfaceData, call.functionName)
            val args = listOf(jsInterfaceData.jsInterface) + createArgList(call, kFunction)

            val answer = kFunction.call(*(args.toTypedArray()))
            if (answer is Promise<*>) {
                handlePromise(answer, call)
                return gson.toJson(Answer(hasError = false, isVoid = true))
            }
            if (answer is Unit) {
                return gson.toJson(Answer(hasError = false, isVoid = true))
            }
            return gson.toJson(Answer(hasError = false, isVoid = false, value = answer))
        } catch (e: InvocationTargetException) {
            return gson.toJson(Answer(hasError = true, error = Error.create(e.targetException)))
        } catch (e: Exception) {
            return gson.toJson(Answer(hasError = true, error = Error.create(e)))
        }
    }

    fun addAfterInitializeListener(afterInitializeListener: AfterInitializeListener) {
        afterInitializeListeners.add(afterInitializeListener)
    }

    fun addJSInterface(jsInterface: JSInterface) {
        val nativeCalls = findAndMapNativeCalls(jsInterface)
        interfaces[jsInterface.name] = JSInterfaceData(jsInterface.name, jsInterface, nativeCalls)
    }

    fun removeFunction(jsFunctionParent: JSFunctionParent) {
        executeJavaScript("${name}.removeFunction(${jsFunctionParent.functionBinding})")
    }

    fun callJSFunction(jsFunctionParent: JSFunctionParent) {
        executeJavaScript("${name}.getFunction(${jsFunctionParent.functionBinding})()")
    }

    fun <A> callJSFunction(jsFunctionParent: JSFunctionParent, arg: A) {
        executeJavaScript("${name}.getFunction(${jsFunctionParent.functionBinding})(${gson.toJson(arg)})")
    }

    private fun handlePromise(promise: Promise<*>, call: Call) {
        val functionWithArg = JSFunctionWithArg<Any?>(this, call.promiseFunctionBinding!!)
        promise.then {
            val answer = if (it is Unit) {
                Answer(hasError = false, isVoid = true)
            } else {
                Answer(hasError = false, isVoid = false, value = it)
            }
            functionWithArg.call(answer)
            functionWithArg.close()
        }.catch { err ->
            val answer = Answer(hasError = true, error = Error.create(err))
            functionWithArg.call(answer)
            functionWithArg.close()
        }
    }

    private fun createInterfacesJS(): String {
        return interfaces.values.joinToString(",", "{", "}") { "\"${it.name}\":${createFunctionsJS(it)}" }
    }

    private fun createFunctionsJS(jsInterfaceData: JSInterfaceData): String {
        return jsInterfaceData.nativeCalls.values
            .joinToString(",", "{", "}") { "\"${it.name}\":${createFunctionJS(jsInterfaceData, it)}" }
    }

    private fun createFunctionJS(jsInterfaceData: JSInterfaceData, kFunction: KFunction<*>): String {
        val nativeCall = kFunction.annotations.filterIsInstance<NativeCall>().first()
        val valueParameter = kFunction.parameters.filter { it.kind == KParameter.Kind.VALUE }
        val args = (valueParameter.indices).map { 'a' + it }.joinToString(",")
        val function = when (nativeCall.value) {
            CallType.FULL_SYNC -> "nativeCallFullSync"
            CallType.WEB_PROMISE -> "nativeCallWebPromise"
            CallType.FULL_PROMISE -> "nativeCallFullPromise"
        }
        return """(${args})=>{
            const call={
                "interfaceName":"${jsInterfaceData.name}",
                "functionName":"${kFunction.name}",
                "arguments":[${args}]
            };
            return ${name}.${function}(call);
        }
        """.trimIndent().replace("\n", "")
    }

    private fun createArgList(call: Call, kFunction: KFunction<*>): List<Any?> {
        val valueParameter = kFunction.parameters.filter { it.kind == KParameter.Kind.VALUE }
        if (call.arguments.size != valueParameter.size) error("Error parsing arguments!")
        return valueParameter.mapIndexed { i, p ->
            parseFromJson(call.arguments[i], p.type)
        }
    }

    private fun parseFromJson(jsonElement: JsonElement, type: KType) = when {
        jsonElement.isJsonNull -> null
        jsonElement.isJsonPrimitive -> parseJsonPrimitive(jsonElement, type)
        else -> parseJsonWithType(jsonElement, type)
    }

    private fun parseJsonWithType(jsonElement: JsonElement, kType: KType): Any {
        return gson.fromJson(jsonElement, createType(kType))
    }

    private fun createType(kType: KType): Type {

        val rawClass = kType.kClass()
        val rawType = TypeToken.get(rawClass.javaObjectType).type

        if (kType.arguments.isEmpty()) {
            return rawType
        }

        val types = kType.arguments.map {
            if (it.variance != KVariance.INVARIANT) error("Unsupported variance!")
            createType(it.type!!)
        }.toTypedArray()

        return TypeToken.getParameterized(rawType, *types).type
    }

    private fun parseJsonPrimitive(jsonElement: JsonElement, type: KType) = when (type.kClass()) {
        String::class -> jsonElement.asString
        Char::class -> jsonElement.asCharacter
        Number::class -> jsonElement.asNumber
        Short::class -> jsonElement.asShort
        Byte::class -> jsonElement.asByte
        Int::class -> jsonElement.asInt
        Long::class -> jsonElement.asLong
        Float::class -> jsonElement.asFloat
        Double::class -> jsonElement.asDouble
        Boolean::class -> jsonElement.asBoolean
        BigInteger::class -> jsonElement.asBigInteger
        BigDecimal::class -> jsonElement.asBigDecimal
        else -> error("Unreachable!")
    }

    private fun findInterfaceData(interfaceName: String): JSInterfaceData {
        interfaces[interfaceName]?.let {
            return it
        }
        throw NoSuchMethodException("Unable to find interface ${interfaceName}!")
    }

    private fun findInterfaceFunction(jsInterfaceData: JSInterfaceData, functionName: String): KFunction<*> {
        jsInterfaceData.nativeCalls[functionName]?.let {
            return it
        }
        throw NoSuchMethodException("Unable to find method $functionName of interface ${jsInterfaceData.name} with a @JSCall annotation!")
    }

    private fun findAndMapNativeCalls(jsInterface: JSInterface): Map<String, KFunction<*>> {
        return jsInterface::class.members.filterIsInstance<KFunction<*>>()
            .filter { it.annotations.filterIsInstance<NativeCall>().isNotEmpty() }
            .onEach { validateFunction(it) }
            .map { it.name to it }
            .toMap()
    }

    private fun validateFunction(kFunction: KFunction<*>) {
        val nativeCall = kFunction.annotations.filterIsInstance<NativeCall>().first()
        if (nativeCall.value == CallType.FULL_PROMISE && kFunction.returnType.kClass() != Promise::class) {
            error("Functions with the ${CallType.FULL_PROMISE} call type must return a promise object! ${kFunction.name} has a wrong return type!")
        }
        kFunction.parameters.filter { it.kind == KParameter.Kind.VALUE }
            .forEach { validateType(kFunction, it.type) }
    }

    private fun validateType(kFunction: KFunction<*>, type: KType) {
        type.arguments.mapNotNull { it.variance }.filter { it != KVariance.INVARIANT }
            .forEach { _ ->
                error("Unsupported variance in function ${kFunction.name} ${type.kClass().simpleName}")
            }
        type.arguments.mapNotNull {it.type}.forEach {
            validateType(kFunction, it)
        }
    }

    private fun readInitScript(): String {
        context.assets.open("init.min.js").bufferedReader().use {
            return it.readText()
        }
    }

    private fun executeJavaScript(js: String) = doInMainThread {
        webView.evaluateJavascript(js) { }
    }
}

interface AfterInitializeListener {
    fun afterInitialize()
}

data class Call(val interfaceName: String, val functionName: String, val arguments: List<JsonElement>, val promiseFunctionBinding: Long?)

data class Answer(val hasError: Boolean, val error: Error? = null, val isVoid: Boolean? = true, val value: Any? = null)

data class Error(val message: String?, val stackTrace: String) {
    companion object {
        fun create(throwable: Throwable) = Error(throwable.message, throwable.stackTraceToString())
    }
}

data class JSInterfaceData(val name: String, val jsInterface: JSInterface, val nativeCalls: Map<String, KFunction<*>>)