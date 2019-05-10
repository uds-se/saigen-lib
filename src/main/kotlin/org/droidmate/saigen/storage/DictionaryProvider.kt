package org.droidmate.saigen.storage

import org.droidmate.saigen.utils.NLP

class DictionaryProvider constructor(private val dict: Map<String, List<String>>) : StorageProvider {
    private val dictSynonyms = dict.keys
        .flatMap { key -> NLP.getSynonyms(key).map { Pair(it, key) } }
        .toMap()

    override val terms: List<String> = dict.keys.toList()

    override fun query(labels: List<String>): Pair<List<String>, List<Record>> {
        // If que provider doesn't answer all labels return empty
        val usedLabels = labels.filter { terms.contains(it) }

        val entries = usedLabels
            .map { label ->
                Record(
                    this::class.java.simpleName,
                    listOf(
                        Field(
                            dictSynonyms[label].orEmpty(),
                            dict[dictSynonyms[label]].orEmpty()
                        )
                    )
                )
            }

        return Pair(usedLabels, entries)
    }
}
