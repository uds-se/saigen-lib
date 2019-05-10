package org.droidmate.saigen.storage

import org.droidmate.explorationModel.toUUID

data class Cache(private val data: MutableList<Record>) {
    fun addAll(entries: List<Record>) = data.addAll(entries)

    private fun largestMatch(keys: List<String>): Record? {
        return data
            .maxBy { entry -> keys.count { key -> entry[key] != null } }
            .takeIf { entry -> keys.count { key -> entry?.get(key) != null } > 0 }
    }

    fun search(keys: List<String>): List<QueryResult> {
        val resultSet = mutableListOf<Record>()

        val keysToProcess = keys.toMutableList()
        while (keysToProcess.isNotEmpty()) {
            val entry = largestMatch(keysToProcess)

            if (entry != null) {
                resultSet.add(entry)
                keysToProcess.removeAll { entry.fields.contains(it) }
            } else {
                break
            }
        }

        return keys
            .map { label ->
                val firstMatch = resultSet.firstOrNull { it.fields.contains(label) }
                QueryResult(
                    label,
                    firstMatch?.get(label).orEmpty(),
                    firstMatch?.query.toString().toUUID()
                )
            }
            .filter { it.values.isNotEmpty() }
    }
}