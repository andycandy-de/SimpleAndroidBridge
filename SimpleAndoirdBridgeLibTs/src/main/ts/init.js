((bridge) => {

    // check preconditions
    if (!bridge) return
    if (bridge.initialized) return

    //set flag to true
    bridge.initialized = true

    //call listener
    if (bridge.afterInitialize) {
        bridge.afterInitialize()
    }
})()