package org.droidmate.saigen.storage

interface StorageProvider : Comparable<StorageProvider> {
    val terms: List<String>

    /**
     * Queries the underlying storage for input values
     *
     * @return A pair with the labels used and the query results, or
     *         null if the provider cannot answer the query
     */
    fun query(labels: List<String>): Pair<List<String>, List<Record>>

    /**
     * Sort the providers according to the number of terms they have
     */
    override fun compareTo(other: StorageProvider): Int {
        return terms.size.compareTo(other.terms.size) * -1
    }
}