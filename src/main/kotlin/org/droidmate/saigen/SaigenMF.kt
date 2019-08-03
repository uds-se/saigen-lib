package org.droidmate.saigen

import kotlinx.coroutines.*
import org.droidmate.deviceInterface.exploration.Rectangle
import org.droidmate.exploration.ExplorationContext
import org.droidmate.exploration.modelFeatures.ModelFeature
import org.droidmate.exploration.modelFeatures.reporter.drawRectangle
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
import java.nio.file.Files
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import javax.imageio.ImageIO
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.io.File
import java.io.IOException
import java.nio.file.StandardOpenOption


class SaigenMF : ModelFeature() {
    companion object {
        @JvmStatic
        private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

        val uidMap = mutableMapOf<UUID, Pair<Boolean, Boolean>>()
        val queryMap = mutableMapOf<Pair<UUID, String>, List<String>>() // key: <queryID, label>, values: from DBPedia...
        val allQueriedLabels = mutableSetOf<String>()

        lateinit var context: ExplorationContext
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
        SaigenMF.context = context
    }

    private suspend fun getLastQueryData(): List<QueryResult> {
        val queriedData = this.lastQuery?.await() ?: return emptyList()

        queriedData.forEach { result ->
            log.trace("GroupId: ${result.queryId}\tLabel: ${result.label}\tValues: ${result.values.joinToString(" | ")}")
            log.debug("GroupId: ${result.queryId}\tLabel: ${result.label}\tValues: ${result.values.joinToString(" | ")}")

            if (!queryMap.containsKey(Pair(result.queryId, result.label))) {
                log.debug("[yyy adding new query] for label " + result.label)
                queryMap[Pair(result.queryId, result.label)] = result.values
            }
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

        // Here: build set of all unique widgets seen so far
        for (dw in dataWidgets) {
            if (!uidMap.containsKey(dw.uid)) {
                // log.debug("[XXX]adding new uid")
                uidMap[dw.uid] = Pair(false, false)
            }
        }
        //log.debug("uidMap[" + uidMap.size + "]: " + uidMap.toString())

        val toFill = dataWidgets
            .filter { it.notFilled() }
            .map { Pair(it, queryResult.firstOrNull { p -> p.label == LabelMatcher.cachedLabel(it) }?.values.orEmpty()) }
            .toMap()
        log.trace("Data widgets to fill: ${toFill.count()}")

        return toFill
    }

    //For randomly filled widgets, interactions does not contain "ActionQueue-START", "ActionQueue-End", so the image will be named after the TextInsert action. If, however, we find an "ActionQueue-START", then the image file will have the name of that action.
    suspend fun drawOnScreenshots(interactions: List<Interaction>) {
        var rects: MutableList<Rectangle> = mutableListOf<Rectangle>()
        var actionId = -1

        assert(interactions.isNotEmpty())

        if (interactions[0].actionType == "ActionQueue-START") {
            actionId = interactions[0].actionId
        }

        for (interaction in interactions) {
            if (interaction.actionType == "TextInsert") {
                if (actionId == -1) {
                    actionId = interaction.actionId
                }
                rects.add(interaction.targetWidget!!.visibleBounds)
            }
        }

        if (rects.size > 0) {
            val targetDir = SaigenMF.context.model.config.imgDst
            val fileName = "$actionId.jpg"
            val dstFile = targetDir.resolve(fileName)
            while (!Files.exists(dstFile) || !Files.isReadable(dstFile)) {
                delay(100L)
            }

            //delay(2000) // problem: sometimes file exists, but has not been written yet?... race condition // possibly not required anymore after adding isReadable() check

            // Now open for editing, draw rectangle around all recently filled edit boxes
            val myPicture = ImageIO.read(dstFile.toFile())
            val g = myPicture.graphics as Graphics2D
            g.stroke = BasicStroke(10f)
            g.color = Color.BLUE
            for (rect in rects) {
                g.drawRectangle(rect)
            }
            try {
                if (ImageIO.write(myPicture, "jpg", File(dstFile.toString()))) {
                    log.debug("-- saved")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }


        }
    }

    override suspend fun onNewAction(
        traceId: UUID,
        interactions: List<Interaction>,
        prevState: State,
        newState: State
    ) {
        async { drawOnScreenshots(interactions) }

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


        nouns.map { allQueriedLabels.add(it) }

        // Populate the cache while dm processes
        this.lastQuery = async { storage.query(nouns) }
    }
}