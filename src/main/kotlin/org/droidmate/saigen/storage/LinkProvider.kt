package org.droidmate.saigen.storage

import formsolver.CommonData
import formsolver.FormSolver
import org.apache.jena.query.Query

class LinkProvider @JvmOverloads constructor(
    private val provider: String = "dbpedia", // "dbpedia", "wikidata"
    /**
     * Maximum number of entries which should be queried by link
     */
    private val maxEntries: Int = 100,
    /**
     * Threshold for a predicate association (Link)
     */
    private val associationThreshold: Int = 50,
    override val terms: List<String> = emptyList()
) : StorageProvider {
    companion object {
        /**
         * Use threshold for association (Link)
         */
        @JvmStatic
        private val useThreshold = false
        /**
         * Type of heuristic to be used (Link)
         * // TODO Not sure this is being used
         */
        @JvmStatic
        private val heuristic = 2
    }

    override fun query(labels: List<String>): Pair<List<String>, List<Record>> {
        // Empty Link data
        CommonData.queryToValuesMap.clear()

        runLink(labels)

        return fetchResults()
    }

    /**
     * Link returns a list of queried entries.
     *
     * Each queried entry is represented in a map where the queried label is the _Key_
     * and the queried data is the _Value_
     *
     * @return List of stored fields. Each field contains all queried input values.
     */
    private fun List<Map<String, String>>.toFieldList(): List<Field> {
        return this.take(maxEntries)
            .flatMap { entry ->
                entry.map { result -> Pair(result.key, result.value) }
            }.groupBy { it.first }
            .mapValues { group -> group.value.map { it.second } }
            .map { Field(it.key, it.value) }
    }

    /**
     * Obtains all labels for all fields in a query result
     */
    private fun Map<Query, List<Map<String, String>>>.labels() = this
        .values.firstOrNull().orEmpty()
        .take(1)
        .toFieldList()
        .flatMap { it.identifiers }
        .distinct()

    /**
     * Fetches the labels used in Link's queries and the query results
     */
    private fun fetchResults(): Pair<List<String>, List<Record>> {
        if (CommonData.queryToValuesMap.isEmpty()) {
            return Pair(emptyList(), emptyList())
        }

        // Convert from Link to LinkEntry
        val queriedData = CommonData.queryToValuesMap
            .map { entry -> Record(entry.key.toString(), entry.value.toFieldList()) }

        // Update used labels
        val queriedLabels = CommonData.queryToValuesMap.labels()
        return Pair(queriedLabels, queriedData)
    }

    private fun runLink(labels: List<String>) {
        // Run Link
        val solver = FormSolver(provider, labels.toTypedArray(), useThreshold, associationThreshold, heuristic)
        solver.runProcess(maxEntries)
    }
}