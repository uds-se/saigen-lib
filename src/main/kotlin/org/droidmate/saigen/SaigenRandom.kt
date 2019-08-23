package org.droidmate.saigen

import org.droidmate.deviceInterface.exploration.ExplorationAction
import org.droidmate.deviceInterface.exploration.Swipe
import org.droidmate.exploration.actions.availableActions
import org.droidmate.exploration.actions.queue
import org.droidmate.exploration.actions.setText
import org.droidmate.exploration.strategy.widget.RandomWidget
import org.droidmate.explorationModel.debugOutput
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
            eContext.queue(widgetsToFill.map {
                SaigenMF.concreteIDMap[it.key.id] = 1 // widget was selected and is now being filled

                val toEnter = it.value[random.nextInt(it.value.size)]
                logger.info("Entering text: " + toEnter)
                it.key.setText(toEnter)
            })
        }
    }

    override suspend fun computeCandidates(): Collection<Widget> {
        return super.computeCandidates() // TODO: input fields, which are initiallity filled with text, are candidates if their text==initialText
            .filterNot { it.isPassword && it.text.isNotBlank() && it.text != it.hintText} // password fields can't be tested for equality, as the input text looks like "***"
            .filterNot { eContext.explorationTrace.insertedTextValues().contains(it.text.removeSuffix("<newline>")) } // removes trailing <newline> if it was added by setText with sendEnter=true
    }
}