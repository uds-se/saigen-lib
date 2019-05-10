package org.droidmate.saigen.storage

import java.util.UUID

data class QueryResult(
    val label: String,
    val values: List<String>,
    val queryId: UUID
)