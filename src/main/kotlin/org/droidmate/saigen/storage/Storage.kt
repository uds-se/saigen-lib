package org.droidmate.saigen.storage

import org.droidmate.saigen.SaigenMF
import org.droidmate.saigen.utils.add
import org.droidmate.saigen.utils.getSynonyms
import org.droidmate.saigen.utils.hasValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.SortedSet

class Storage constructor(private val storageProviders: SortedSet<StorageProvider>) {
    companion object {
        @JvmStatic
        val log: Logger by lazy { LoggerFactory.getLogger(SaigenMF::class.java) }
    }

    /**
     * Cache all previously queried values since we are using link remotely and it may affect the
     * overall performance
     */
    private val queryCache = Cache(mutableListOf())

    /**
     * List of terms for which there is no data in any used database
     */
    private val blacklistedWords = mutableSetOf<String>() // mutableSetOf("email", "password", "username", "login", "screen", "nonoun")

    /**
     * Removes terms which have already been queried (of a synonym did so) and had no results
     *
     * @return Terms which have not yet been queried or that have been queried an have a result
     */
    private fun List<String>.nonBlacklisted(): List<String> = this.filterNot { blacklistedWords.contains(it) }

    /**
     * Queries all available providers for data.
     * The providers are queried in order of term size, that is, a provider which contains 3 terms is queried
     * before one which contains 2. As a fallback, DBPedia contains 0 terms and it is the last provider used.
     *
     * The query system works as follows:
     *  1. Remove all terms which have been blacklisted
     *  2. Query a provider.
     *  2.1. If a provider answers part of the query, remove the terms used and recursively
     *       invoke this method with the remaining terms
     *  3. Once all providers have been queried for all words. The words which have no result
     *     are added into the blacklist
     *  4. All results are added to the local cache for easier access
     */
    private fun queryProvidersRecursively(labels: List<String>): Pair<List<String>, List<Record>> {
        if (labels.isEmpty()) {
            return Pair(emptyList(), emptyList())
        }

        log.debug("Querying labels ${labels.joinToString(", ")}")
        val aggregateResult = Pair(mutableListOf<String>(), mutableListOf<Record>())

        // Sorted by number of terms
        for (provider in storageProviders) {
            val result = provider.query(labels)

            aggregateResult.add(result)

            // If provider could answer the result
            if (result.hasValue()) {
                val pendingLabels = labels
                    .filterNot { l -> result.first.any { it == l } }

                val recursiveResult = queryProvidersRecursively(pendingLabels)
                aggregateResult.add(recursiveResult)
                break
            }
        }

        // If the label is pending here, it cannot be found in any storage
        val pendingLabels = labels
            .filterNot { l -> aggregateResult.first.any { it == l } }

        blacklistedWords.addAll(pendingLabels.flatMap { it.getSynonyms() })

        queryCache.addAll(aggregateResult.second)

        return aggregateResult
    }

    /**
     * Queries all databases using a list of terms
     */
    fun query(labels: List<String>): List<QueryResult> {
        // Remove blacklisted terms
        var labelsToProcess = labels.nonBlacklisted()

        if (labelsToProcess.isEmpty()) {
            log.warn("No valid inputs found for labels $labels")
            return emptyList()
        }

        // Query cache
        val results = queryCache.search(labelsToProcess)
            .toMutableList()

        // Remove fields from the cache
        if (results.isNotEmpty()) {
            labelsToProcess = labelsToProcess.filterNot { results.any { r -> r.label.contains(it) } }
        }

        // If all labels have already been found in the cache, continue
        if (labelsToProcess.isEmpty()) {
            return results
        }

        queryProvidersRecursively(labelsToProcess)

        // Filter cache for result
        // Internally this performs an expansion to all synonyms and thus the label matching should work
        // Need to get new blacklist as it could have been updated
        return queryCache.search(labels.nonBlacklisted())
    }
}