package org.droidmate.saigen

import kotlinx.coroutines.runBlocking
import org.droidmate.explorationModel.interaction.State
import org.droidmate.explorationModel.interaction.Widget
import org.droidmate.saigen.storage.DictionaryProvider
import org.droidmate.saigen.storage.LinkProvider
import org.droidmate.saigen.storage.QueryResult
import org.droidmate.saigen.storage.Storage
import org.droidmate.saigen.utils.LabelMatcher
import java.util.UUID

class Lib {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runBlocking {
                println("doh!")
            }
        }

        // API 1: For a given state, return map of input widgets and associated label (noun)
        @JvmStatic
        fun extractWidgetsAndLabels(state: State<Widget>): Map<Widget, String> {
            val matchedWidgets = LabelMatcher.getLabels(state)
            return matchedWidgets
        }

        // TODO: maybe pass this as parameter to getInputsForLabels?
        private val storage = Storage(
            sortedSetOf(
                LinkProvider(),
                DictionaryProvider(
                    mapOf(
                        "user" to listOf("Johnny1999", "Emmmma95"),
                        "password" to listOf("sec", "rets"),
                        "url" to listOf("http://google.com")
                    )
                )
            )
        )

        // API 2: For a given set of labels, return map of the labels to appropriate input samples
        @JvmStatic
        fun getInputsForLabels(labels: List<String>): List<QueryResult> {
            return storage.query(labels)
        }

        // API 3: (for standalone droidmate-saigen)
        @JvmStatic
        fun cachedLabel(widget: Widget): String {
            return LabelMatcher.widgetLabelCache[widget.uid].orEmpty()
        }

        // API 4: (for standalone droidmate-saigen)
        @JvmStatic
        fun cachedLabel(widgetUUID: UUID): String {
            return LabelMatcher.widgetLabelCache[widgetUUID].orEmpty()
        }
    }
}