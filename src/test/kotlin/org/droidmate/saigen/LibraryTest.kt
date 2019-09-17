package org.droidmate.saigen

import org.droidmate.saigen.Main.Companion.getInputsForLabels
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
        val testLabels = listOf("car")
        val queryResults = getInputsForLabels(testLabels)
        assert(queryResults.size > 0)
    }
}