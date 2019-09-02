package org.droidmate.saigen.storage

import org.droidmate.saigen.utils.getSynonyms

data class Field(
    private val linkFieldName: String,
    private val linkValues: List<String>
) {
    private fun String.sanitize() =
        this.substringBefore("^^")
            .substringAfterLast("/")
            .replace("_", " ") // converts Goose_Bay into Goose Bay
            .substringBeforeLast("@") // prevent 798 Route 304@en | Albertus Magnus High School@en
            .substringBefore("(") // prevent: New York (state) | Georgia (U.S. state)
            .substringBefore(",") // prevent: Bardonia, New York | Alexandria, Indiana
            .trim()

    private val queriedName by lazy { linkFieldName.substringAfter("_").toLowerCase() } // toLowerCase() required because for classAssociations, it's capitalized

    val identifiers by lazy { queriedName.getSynonyms() }

    val queriedValues by lazy { linkValues.map { it.sanitize() } }

    fun containsKey(key: String): Boolean {
        return identifiers.any { it == key }
    }
}