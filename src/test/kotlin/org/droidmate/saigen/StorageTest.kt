package org.droidmate.saigen

import org.droidmate.saigen.storage.DictionaryProvider
import org.droidmate.saigen.storage.LinkProvider
import org.droidmate.saigen.storage.Storage
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import kotlin.test.assertEquals

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(JUnit4::class)
class StorageTest {
    private val storage = Storage(sortedSetOf(LinkProvider(maxEntries = 5)))

    @Test
    fun getAddressesTest() {
        val queryResult = storage.query(listOf("address"))

        assertEquals(1, queryResult.size)

        val addresses = queryResult.first().values

        assertEquals(listOf("Noida Link Rd", "798 Route 304",
            "1 Burden Court", "6610 Browns Mill Road", "10810-142 Street"),
            addresses)
    }

    @Test
    fun queryMultipleTermsTest() {
        val queryLabels = listOf("address", "name", "city", "state", "phone", "email")
        val queryResult = storage.query(queryLabels)

        val foundLabels = queryLabels.filterNot { it == "phone" || it == "email" }
        assertEquals(foundLabels, queryResult.map { it.label })
    }

    @Test
    fun queryMultipleProvidersTest() {
        val customNames = listOf("first name", "second name")
        val internalStorage = Storage(sortedSetOf(LinkProvider(), DictionaryProvider(mapOf("name" to customNames))))
        val queryLabels = listOf("address", "name", "city", "email", "phone", "fruit")
        val queryResult = internalStorage.query(queryLabels)

        val foundLabels = queryLabels.filterNot { it == "phone" || it == "email" || it == "fruit" }

        assertEquals(foundLabels, queryResult.map { it.label })
        assertEquals(customNames, queryResult.first { it.label == "name" }.values)
    }
}