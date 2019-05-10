package org.droidmate.saigen

import org.droidmate.saigen.utils.NLP
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import kotlin.test.assertEquals

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(JUnit4::class)
class NLPTest {
    @Test
    fun getNounsContactsTest() {
        val nouns = NLP.getNouns("Search contacts")
        assertEquals(listOf("contact"), nouns)
    }

    @Test
    fun getNounsAddressTest() {
        val nouns = NLP.getNouns("The address")
        assertEquals(listOf("address"), nouns)
    }

    @Test
    fun getSynonym() {
        val synonyms = NLP.getSynonyms("state")
        assertEquals(listOf("state", "province", "nation", "dos", "country"), synonyms)
    }
}