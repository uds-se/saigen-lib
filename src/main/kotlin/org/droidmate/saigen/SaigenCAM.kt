package org.droidmate.saigen

import org.droidmate.deviceInterface.exploration.ExplorationAction
import org.droidmate.exploration.actions.click
import org.droidmate.exploration.strategy.widget.ExplorationStrategy
import kotlin.random.Random

class SaigenCAM @JvmOverloads constructor(
    private val mapping: List<CAM>,
    private val delay: Long,
    private val random: Random = Random(0)
) : ExplorationStrategy() {

    override suspend fun chooseAction(): ExplorationAction {
        val candidates = mapping
            .filter { it.matches(currentState) }

        assert(candidates.isNotEmpty()) { "${this.javaClass.simpleName} strategy was invoked without targets. $mapping" }

        val chosenCandidate = candidates.random(random)
        val targets = chosenCandidate.getTargets(currentState)

        return targets.random(random).click(delay)
    }
}