package org.droidmate.saigen.storage

data class Record(
    val query: String,
    private val data: List<Field>
) {
    operator fun get(key: String): List<String>? {
        return data
            .firstOrNull { linkField -> linkField.containsKey(key) }
            ?.queriedValues
    }

    val fields
        get() = data.flatMap { it.identifiers }
            .distinct()
}