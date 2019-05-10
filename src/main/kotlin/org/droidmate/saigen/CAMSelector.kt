package org.droidmate.saigen

import org.droidmate.exploration.SelectorFunction

@Suppress("UNCHECKED_CAST")
val camSelector: SelectorFunction = { context, pool, data ->
    val bundleArray = data as Array<Any>
    val cams = bundleArray.first() as List<CAM>
    val curState = context.getCurrentState()
    val saigen = context.getOrCreateWatcher<SaigenMF>()
    saigen.join()

    when {
        saigen.getInputValues(curState).isNotEmpty() -> null
        cams.any { it.matches(curState) } -> pool.getFirstInstanceOf(SaigenCAM::class.java)
        else -> null
    }
}