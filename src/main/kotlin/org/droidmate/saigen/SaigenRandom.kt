package org.droidmate.saigen

import org.droidmate.deviceInterface.exploration.ExplorationAction
import org.droidmate.exploration.ExplorationContext
import org.droidmate.exploration.actions.queue
import org.droidmate.exploration.actions.setText
import org.droidmate.exploration.strategy.widget.RandomWidget
import org.droidmate.explorationModel.factory.AbstractModel
import org.droidmate.explorationModel.interaction.State
import org.droidmate.explorationModel.interaction.Widget

class SaigenRandom(priority: Int) : RandomWidget(priority, emptyList(), false) {
    lateinit var saigen: SaigenMF

    override suspend fun <M : AbstractModel<S, W>, S : State<W>, W : Widget> hasNext(eContext: ExplorationContext<M, S, W>): Boolean {
        saigen.join()
        return true
    }

    override fun <M : AbstractModel<S, W>, S : State<W>, W : Widget> initialize(initialContext: ExplorationContext<M, S, W>) {
        super.initialize(initialContext)
        saigen = initialContext.getOrCreateWatcher()
    }

    override suspend fun <M : AbstractModel<S, W>, S : State<W>, W : Widget> chooseRandomWidget(
        eContext: ExplorationContext<M, S, W>
    ): ExplorationAction {
        val currentState = eContext.getCurrentState()
        val widgetsToFill = saigen.getInputValues(currentState)

        // Nothing to fill, go random
        return if (widgetsToFill.isEmpty()) {
            super.chooseRandomWidget(eContext)
            // Fill values
        } else {
            ExplorationAction.queue(widgetsToFill.map {
                SaigenMF.concreteIDMap[it.key.id] = 1 // widget was selected and is now being filled

                val toEnter = it.value[random.nextInt(it.value.size)]
                log.info("Entering text: " + toEnter)
                it.key.setText(toEnter)
            })
        }
    }

    override fun ExplorationContext<*, *, *>.getAvailableWidgets(): List<Widget> {
        // TODO: input fields, which are initiallity filled with text, are candidates if their text==initialText
        return getCurrentState().visibleTargets
            .filter { w ->
                !w.isKeyboard &&
                        (!w.isInputField || !explorationTrace.insertedTextValues().contains(w.text.removeSuffix("<newline>")))
            }
            // password fields can't be tested for equality, as the input text looks like "***"
            .filterNot { it.isPassword && it.text.isNotBlank() && it.text != it.hintText }
    }
}