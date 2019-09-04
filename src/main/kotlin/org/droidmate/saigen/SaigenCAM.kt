package org.droidmate.saigen

import com.natpryce.konfig.Configuration
import org.droidmate.configuration.ConfigProperties
import org.droidmate.deviceInterface.exploration.ExplorationAction
import org.droidmate.exploration.ExplorationContext
import org.droidmate.exploration.actions.click
import org.droidmate.exploration.strategy.AExplorationStrategy
import org.droidmate.explorationModel.factory.AbstractModel
import org.droidmate.explorationModel.interaction.State
import org.droidmate.explorationModel.interaction.Widget
import kotlin.random.Random

class SaigenCAM constructor(
    private val priority: Int,
    private val mapping: List<CAM>
) : AExplorationStrategy() {
    private var delay: Long = 0
    private var random: Random = Random(0)

    override fun getPriority(): Int {
        return priority
    }

    override fun initialize(cfg: Configuration) {
        delay = cfg[ConfigProperties.Exploration.widgetActionDelay]
        val randomSeed = cfg[ConfigProperties.Selectors.randomSeed].let {
            if (it == -1L) java.util.Random().nextLong()
            else it
        }
        random = Random(randomSeed)
    }

    override suspend fun <M : AbstractModel<S, W>, S : State<W>, W : Widget> hasNext(eContext: ExplorationContext<M, S, W>): Boolean {
        val curState = eContext.getCurrentState()
        val saigen = eContext.getOrCreateWatcher<SaigenMF>()
        saigen.join()

        return when {
            saigen.getInputValues(curState).isNotEmpty() -> false
            mapping.any { it.matches(curState) } -> true
            else -> false
        }
    }

    override suspend fun <M : AbstractModel<S, W>, S : State<W>, W : Widget> computeNextAction(eContext: ExplorationContext<M, S, W>): ExplorationAction {
        val currentState = eContext.getCurrentState()
        val candidates = mapping
            .filter { it.matches(currentState) }

        assert(candidates.isNotEmpty()) { "${this.javaClass.simpleName} strategy was invoked without targets. $mapping" }

        val chosenCandidate = candidates.random(random)
        val targets = chosenCandidate.getTargets(currentState)

        return targets.random(random).click(delay)
    }
}