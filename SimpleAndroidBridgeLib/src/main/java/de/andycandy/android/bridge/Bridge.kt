package de.andycandy.android.bridge

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.reflect.TypeToken
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Collections
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KVariance
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.superclasses

class Bridge(context: Context, webView: WebView, name: String = "Bridge") {

    private val innerBridge = InnerBridge(context, webView, name)

    fun init() = innerBridge.init()

    fun addAfterInitializeListener(block: () -> Unit) = innerBridge.addAfterInitializeListener(block)

    fun addJSInterface(jsInterface: JSInterface) = innerBridge.addJSInterface(jsInterface)
}

@SuppressLint("SetJavaScriptEnabled")
class InnerBridge(private val context: Context, private val webView: WebView, private val name: String = "Bridge") {

    private val gson = Gson()

    private val interfaces = Collections.synchronizedMap(mutableMapOf<String, JSInterfaceData>())

    private val pendingPromises = Collections.synchronizedMap(mutableMapOf<Long, Pair<Promise<*>, KClass<*>>>())

    private val currentPendingPromiseID: AtomicLong = AtomicLong(0)

    private val executor = Executors.newSingleThreadExecutor()

    private val referenceQueue = ReferenceQueue<JSFunctionParent>()

    private val functionBindingMap = Collections.synchronizedMap(mutableMapOf<UUID, JSFunctionPhantomReference>())

    private val afterInitializeListeners = Collections.synchronizedList(mutableListOf<Runnable>())

    init {
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(this, name)
        executor.execute { checkJSFunctionReference() }
    }

    @JavascriptInterface
    fun init() {
        val initScript = readInitScript()
        executeJavaScript(initScript + "; initBridge(${name},${createInterfacesJS()});")
    }

    @JavascriptInterface
    fun nativeAfterInitialize() {
        pendingPromises.clear()
        functionBindingMap.clear()
        currentPendingPromiseID.set(0)
        afterInitializeListeners.toList().forEach(Runnable::run)
    }

    @JavascriptInterface
    fun nativeCall(callAsString: String): String {
        try {
            val call = gson.fromJson(callAsString, Call::class.java)
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
            return gson.toJson(Answer(hasError = false, isVoid = false, value = gson.toJsonTree(answer)))
        } catch (e: InvocationTargetException) {
            return gson.toJson(Answer(hasError = true, error = Error.create(e.targetException)))
        } catch (e: Exception) {
            return gson.toJson(Answer(hasError = true, error = Error.create(e)))
        }
    }

    @JavascriptInterface
    fun finishPromise(promiseBinding: Long, answerAsString: String) {
        val answer = gson.fromJson(answerAsString, Answer::class.java)
        val promisePair = pendingPromises[promiseBinding]!!
        pendingPromises.remove(promiseBinding)

        @Suppress("UNCHECKED_CAST")
        val promise = promisePair.first as Promise<Any?>
        val kClass = promisePair.second

        doInMainThread {
            when {
                answer.hasError -> promise.reject(Exception("${answer.error?.message}${answer.error?.let { " ${it.stackTrace}" }}"))
                kClass == Unit::class -> promise.resolve(Unit)
                else -> promise.resolve(gson.fromJson(answer.value, kClass.java))
            }
        }
    }

    @JavascriptInterface
    fun releaseDeadPromise(promiseBinding: Long) {
        pendingPromises.remove(promiseBinding)
    }

    fun addAfterInitializeListener(block: () -> Unit) {
        afterInitializeListeners.add(block)
    }

    fun addJSInterface(jsInterface: JSInterface) {
        val nativeCalls = findAndMapNativeCalls(jsInterface)
        interfaces[jsInterface.name] = JSInterfaceData(jsInterface.name, jsInterface, nativeCalls)
        executeJavaScript("if (${name}.initialized) { ${name}.interfaces=${createInterfacesJS()}; }")
    }

    fun removeFunction(functionUUID: UUID) {
        val functionBinding = functionBindingMap[functionUUID]?.functionBinding ?: return
        executeJavaScript("${name}.removeFunction(${functionBinding});")
        functionBindingMap.remove(functionUUID)
    }

    fun callJSFunction(functionUUID: UUID) {
        val functionBinding = functionBindingMap[functionUUID]?.functionBinding ?: error("Functionbinding is not available. This happens when the Bridge was reinitialized!")
        executeJavaScript("${name}.executeFunction(${functionBinding});")
    }

    fun <A> callJSFunction(functionUUID: UUID, arg: A) {
        val functionBinding = functionBindingMap[functionUUID]?.functionBinding
            ?: error("Functionbinding is not available. This happens when the Bridge was reinitialized!")
        executeJavaScript("${name}.executeFunction(${functionBinding},${gson.toJson(arg)});")
    }

    fun <R> callJSFunctionWithPromise(functionUUID: UUID, kClass: KClass<*>) : Promise<R> {
        val functionBinding = functionBindingMap[functionUUID]?.functionBinding
            ?: error("Functionbinding is not available. This happens when the Bridge was reinitialized!")
        val promise = Promise<R>()
        val pendingPromiseID = currentPendingPromiseID.getAndIncrement()
        pendingPromises[pendingPromiseID] = Pair(promise, kClass)
        executeJavaScript("${name}.executeFunctionWithPromiseBinding(${functionBinding},${pendingPromiseID});")
        return promise
    }

    fun <A, R> callJSFunctionWithPromise(functionUUID: UUID, kClass: KClass<*>, arg: A): Promise<R> {
        val functionBinding = functionBindingMap[functionUUID]?.functionBinding
            ?: error("Functionbinding is not available. This happens when the Bridge was reinitialized!")
        val promise = Promise<R>()
        val pendingPromiseID = currentPendingPromiseID.getAndIncrement()
        pendingPromises[pendingPromiseID] = Pair(promise, kClass)
        executeJavaScript("${name}.executeFunctionWithPromiseBinding(${functionBinding},${pendingPromiseID},${gson.toJson(arg)});")
        return promise
    }

    private fun handlePromise(promise: Promise<*>, call: Call) {
        val functionBinding = call.promiseFunctionBinding!!
        val functionUUID = UUID.randomUUID()
        val functionWithArg = JSFunctionWithArg<Any?>(this, functionUUID)
        functionBindingMap[functionUUID] = JSFunctionPhantomReference(functionWithArg, referenceQueue, functionUUID, functionBinding)
        promise.then {
            val answer = if (it is Unit) {
                Answer(hasError = false, isVoid = true)
            } else {
                Answer(hasError = false, isVoid = false, value = gson.toJsonTree(it))
            }
            functionWithArg(answer)
            functionWithArg.close()
        }.catch { err ->
            val answer = Answer(hasError = true, error = Error.create(err))
            functionWithArg(answer)
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
        return """function(${args}) {
            return ${name}.${function}({
                "interfaceName":"${jsInterfaceData.name}",
                "functionName":"${kFunction.name}",
                "arguments":[${args}]
            });
        }
        """.trimIndent().replace("\n", "").replace(Regex("\\s+"), " ")
    }

    private fun createArgList(call: Call, kFunction: KFunction<*>): List<Any?> {
        val valueParameter = kFunction.parameters.filter { it.kind == KParameter.Kind.VALUE }
        if (call.arguments.size != valueParameter.size) error("Error parsing arguments!")
        return valueParameter.mapIndexed { i, p ->
            parseFromJson(call.arguments[i], p.type)
        }
    }

    private fun parseFromJson(jsonElement: JsonElement, kType: KType) = when {
        JSFunctionParent::class.isSuperclassOf(kType.kClass()) -> createJSFunction(jsonElement, kType)
        jsonElement.isJsonNull -> null
        Enum::class.isSuperclassOf(kType.kClass()) -> parseJsonEnumWithType(jsonElement, kType)
        jsonElement.isJsonPrimitive -> parseJsonPrimitive(jsonElement, kType)
        else -> parseJsonWithType(jsonElement, kType)
    }

    private fun createJSFunction(jsonElement: JsonElement, kType: KType) : JSFunctionParent {
        val functionUUID = UUID.randomUUID()
        val functionBinding = jsonElement.asLong
        val jsFunction = when (kType.kClass()) {
            JSFunction::class -> JSFunction(this, functionUUID)
            JSFunctionWithArg::class -> JSFunctionWithArg<Any?>(this, functionUUID)
            JSFunctionWithPromise::class -> {
                val kClass = kType.arguments.first().let {
                    if (it.variance != KVariance.INVARIANT) error("Unsupported variance!")
                    it.type!!.kClass()
                }
                JSFunctionWithPromise<Any?>(this, functionUUID, kClass)
            }
            JSFunctionWithPromiseAndArg::class -> {
                val kClass = kType.arguments.last().let {
                    if (it.variance != KVariance.INVARIANT) error("Unsupported variance!")
                    it.type!!.kClass()
                }
                JSFunctionWithPromiseAndArg<Any?, Any?>(this, functionUUID, kClass)
            }
            else -> error("Unknown function class!")
        }
        functionBindingMap[functionUUID] = JSFunctionPhantomReference(jsFunction, referenceQueue, functionUUID, functionBinding)
        return jsFunction
    }

    private fun parseJsonEnumWithType(jsonElement: JsonElement, kType: KType): Any {
        val stringVal = jsonElement.asString
        val enum = kType.kClass().java.enumConstants!!
            .map { it as Enum<*> }
            .find { it.name == stringVal }

        enum ?: error("Unknown enum constant $stringVal for class ${kType.kClass().simpleName}!")

        return enum
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
        Char::class -> jsonElement.asString[0]
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
        val found = (jsInterface::class.superclasses + jsInterface::class)
            .asSequence()
            .map { it.memberFunctions }
            .flatten()
            .filter { it.annotations.filterIsInstance<NativeCall>().isNotEmpty() }
            .map { it.name to it }.toList()

        return found.distinctBy { it.first }
            .also { list -> if (found.size != list.count()) error("Duplicate function definitions found ${(found - list.toList()).map { it.first }.distinct()}") }
            .onEach { validateFunction(it.second) }
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

    private fun checkJSFunctionReference() {
        while (!Thread.currentThread().isInterrupted) {
            try {
                val jsFunctionPhantomReference = (referenceQueue.remove() as JSFunctionPhantomReference)
                val functionUUID = jsFunctionPhantomReference.functionUUID
                synchronized(this) {
                    if (functionBindingMap.contains(functionUUID)) {
                        Log.w("InnerBridge", "There is no more reference to this function but the close function is not called!")
                        removeFunction(functionUUID)
                    }
                }
            } catch (e: InterruptedException) {
                return
            }
        }
    }
}

data class Call(val interfaceName: String, val functionName: String, val arguments: List<JsonElement>, val promiseFunctionBinding: Long?)

data class Answer(val hasError: Boolean, val error: Error? = null, val isVoid: Boolean? = true, val value: JsonElement = JsonNull.INSTANCE)

data class Error(val message: String?, val stackTrace: String) {
    companion object {
        fun create(throwable: Throwable) = Error(throwable.message, throwable.stackTraceToString())
    }
}

data class JSInterfaceData(val name: String, val jsInterface: JSInterface, val nativeCalls: Map<String, KFunction<*>>)

class JSFunctionPhantomReference(jsFunction: JSFunctionParent, referenceQueue: ReferenceQueue<JSFunctionParent>, val functionUUID: UUID, val functionBinding: Long) : PhantomReference<JSFunctionParent>(jsFunction, referenceQueue)