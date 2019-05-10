package org.droidmate.saigen

import org.droidmate.deviceInterface.exploration.ExplorationAction
import org.droidmate.exploration.actions.queue
import org.droidmate.exploration.actions.setText
import org.droidmate.exploration.strategy.widget.RandomWidget
import org.droidmate.explorationModel.interaction.Widget

class SaigenRandom(randomSeed: Long) : RandomWidget(randomSeed, true, true, emptyList()) {
    val saigen by lazy { eContext.getOrCreateWatcher<SaigenMF>() }

    override suspend fun chooseRandomWidget(): ExplorationAction {
        val widgetsToFill = saigen.getInputValues(currentState)

        // Nothing to fill, go random
        return if (widgetsToFill.isEmpty()) {
            super.chooseRandomWidget()
            // Fill values
        } else {
            val nrEntries = widgetsToFill.values.first().size
            val idx = random.nextInt(nrEntries)

            logger.info("Entering text: ${widgetsToFill.map { it.value[idx] }}")

            eContext.queue(widgetsToFill.map {
                it.key.setText(it.value[idx])
            })
        }
    }

    override suspend fun computeCandidates(): Collection<Widget> {
        return super.computeCandidates()
            .filterNot { eContext.explorationTrace.insertedTextValues().contains(it.text) }
    }
}