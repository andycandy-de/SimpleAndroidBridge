Changelog for version 1.1.0:

* Migrated Gradle DSL from Groovy to Kotlin
* Replaced deprecated Object.finalize() implementation with a implementation based on PhantomReference for auto cleanup
* Enhanced JSFunction to implement AutoCloseable, allowing for use with try-with-resources statements or AutoCloseable.use {} blocks
* Introduced exception handling for JSFunction in case of web reload, providing better error management
* Promises resolved by the web are executed on the main thread
* Refactored code to improve maintainability, readability, and performance
* Simplified project structure by removing the SimpleAndroidBridgeLibJS subproject, reducing complexity
* Added ProGuard consumer rules
* Changed minSdk to 24 and compileSdk to 35