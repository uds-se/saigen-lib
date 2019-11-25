package org.droidmate.saigen

import org.droidmate.saigen.Lib.Companion.getInputsForLabels
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(JUnit4::class)

class LibraryTest {

    @Test
    fun extractWidgetsAndLabelsTest() {
    }

    @Test
    fun getInputsForLabelsTest() {
        // for "teller", wikidata only finds one result, which gets scrapped because of threshold. Then, dbpedia is queried which finds a result.
        val testLabels = listOf("actor") // "toy", "cocktail", "teller", "car", "drink"
        val queryResults = getInputsForLabels(testLabels)
        print(queryResults)
        assert(queryResults.size > 0)
    }
}
