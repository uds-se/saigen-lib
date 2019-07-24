package org.droidmate.saigen

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import org.droidmate.exploration.ExplorationContext
import org.droidmate.exploration.modelFeatures.ModelFeature
import org.droidmate.explorationModel.ExplorationTrace
import org.droidmate.explorationModel.interaction.Interaction
import org.droidmate.explorationModel.interaction.State
import org.droidmate.explorationModel.interaction.Widget
import org.droidmate.saigen.storage.DictionaryProvider
import org.droidmate.saigen.storage.LinkProvider
import org.droidmate.saigen.storage.QueryResult
import org.droidmate.saigen.storage.Storage
import org.droidmate.saigen.utils.LabelMatcher
import org.droidmate.saigen.utils.getSynonyms
import org.droidmate.saigen.utils.isVisibleDataWidget
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.coroutines.CoroutineContext

class SaigenMF : ModelFeature() {
    companion object {
        @JvmStatic
        private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }
    }

    override val coroutineContext: CoroutineContext = CoroutineName("SaigenMF") + Job()

    // private val storage = Storage(sortedSetOf(LinkProvider()))
    private val storage = Storage(sortedSetOf(LinkProvider(), DictionaryProvider(mapOf("user" to listOf("Johnny1999", "Emmmma95"), "password" to listOf("sec", "rets"), "url" to listOf("http://google.com")))))


    /**
     * Initialized on the onAppExplorationStarted
     */
    private lateinit var trace: ExplorationTrace

    /**
     * Labels for which no query is found
     */
    private val blacklistedWords = mutableListOf<String>() // "email", "password", "username", "login", "screen", "nonoun")

    /**
     * Value of the last storage query, updated when reaching a state
     */
    private var lastQuery: Deferred<List<QueryResult>>? = null

    private fun Widget.isBlacklisted(): Boolean {
        val label = LabelMatcher.cachedLabel(this)

        return label.isEmpty() || // no cachedLabel for the widget
                blacklistedWords.contains(label) // in the blacklist
    }

    /**
     * Checks is the value of the current widget has not yet been filled
     */
    private fun Widget.isFilled(): Boolean {
        assert(this.isVisibleDataWidget()) { "Widget $this should have been a data widget" }

        log.debug("MARKER InsertedTextValues: " + trace.insertedTextValues())

        if (this.isPassword && this.text.isNotBlank() && this.text != this.hintText) { // password fields can't be tested for equality, as the input text looks like "***"
            return true
        }

        return this.text.isNotBlank() && // has text
                trace.insertedTextValues()
                    .map { it.replace(";", "<semicolon>").replace(Regex("\\r\\n|\\r|\\n"), "<newline>").trim() }
                    .any { it == this.text.removeSuffix("<newline>") } // removes trailing <newline> if it was added by setText with sendEnter=true
    }

    private fun Widget.notFilled(): Boolean {
        return !this.isFilled() &&
                !this.isBlacklisted()
    }

    override fun onAppExplorationStarted(context: ExplorationContext) {
        this.trace = context.explorationTrace
    }

    private suspend fun getLastQueryData(): List<QueryResult> {
        val queriedData = this.lastQuery?.await() ?: return emptyList()

        queriedData.forEach { result ->
            log.trace("GroupId: ${result.queryId}\tLabel: ${result.label}\tValues: ${result.values.joinToString(" | ")}")
        }

        return queriedData
    }

    suspend fun getInputValues(newState: State): Map<Widget, List<String>> {
        val queryResult = getLastQueryData()

        if (queryResult.isEmpty()) {
            return emptyMap()
        }

        val queryLabels = queryResult.map { it.label }
        log.debug("Queried labels: ${queryLabels.joinToString(",")}")

        val dataWidgets = newState.widgets
            .filter { it.isVisibleDataWidget() }
        log.trace("Data widgets on state: ${dataWidgets.count()}")

        // Add unused terms to the blacklist
        val unusedLabels = dataWidgets
            .map { LabelMatcher.cachedLabel(it) }
            .filterNot { queryLabels.contains(it) }
            .flatMap { it.getSynonyms() }

        log.debug("Blacklisting unused labels: ${unusedLabels.joinToString(",")}")
        blacklistedWords.addAll(unusedLabels)

        val toFill = dataWidgets
            .filter { it.notFilled() }
            .map { Pair(it, queryResult.firstOrNull { p -> p.label == LabelMatcher.cachedLabel(it) }?.values.orEmpty()) }
            .toMap()
        log.trace("Data widgets to fill: ${toFill.count()}")

        return toFill
    }

    override suspend fun onNewAction(
        traceId: UUID,
        interactions: List<Interaction>,
        prevState: State,
        newState: State
    ) {
        val matchedWidgets = LabelMatcher.getLabels(newState)

        if (matchedWidgets.isEmpty())
            return

        log.trace("Non filled UI elements on current state:")
        matchedWidgets
            .map { "Label: ${it.key}\tLabel: ${it.value}" }
            .joinToString("\n")

        val nouns = matchedWidgets
            .map { it.value }
            .distinct()
        log.trace("Requested terms: ${nouns.joinToString()}")

        // Populate the cache while dm processes
        this.lastQuery = async { storage.query(nouns) }
    }
}