package formsolver

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Evaluator {
    private var nQuery = 0
    var numComponents = 0
    private var nTags: Int = 0
    private var nAssociatedTags: Int = 0
    private var startTime: Double = 0.0
    private var endTime: Double = 0.0
    private var step1: Double = 0.0
    private var step2: Double = 0.0
    // //private val passo3: Double = 0.0
    // //private var precPredicati: Boolean = false
    // //private var cGlobali: Boolean = false
    // //private var relazioniLv2: Boolean = false
    private var obtainedResults: List<Int> = emptyList()
    private var searchResults: Int = 0
    private var totalResults: Int = 0

    fun addNQuery(n: Int) {
        this.nQuery += n
    }

    fun setSearchResults(ris: Int) {
        this.searchResults = ris
    }

    fun setObtainedResult(ris: List<Int>) {
        this.obtainedResults = ris
        this.totalResults = 0
        var temp = 0
        var i = 0
        while (i < this.obtainedResults.size) {
            this.totalResults += this.obtainedResults[i]
            temp += this.searchResults
            ++i
        }
        this.searchResults = temp
    }

    fun setNrTags(n: Int) {
        this.nTags = n
    }

    fun setNAssociatedTags(n: Int) {
        this.nAssociatedTags = n
    }

    fun setStartTime(millis: Long) {
        this.startTime = millis.toDouble() / 1000.0
    }

    fun setEndTimeStep1(millis: Long) {
        this.step1 = millis.toDouble() / 1000.0
    }

    fun setEndTimeStep2(millis: Long) {
        this.step2 = millis.toDouble() / 1000.0
    }

    fun setEndTime(millis: Long) {
        this.endTime = millis.toDouble() / 1000.0
    }

    /*fun setPrecedenza(b: Boolean) {
        this.precPredicati = b
    }

    fun setCGlobali(b: Boolean) {
        this.cGlobali = b
    }

    fun setRelazioniLv2(b: Boolean) {
        this.relazioniLv2 = b
    }*/

    fun round(number: Double, nrDecimals: Int): Double {
        return Math.round(number * Math.pow(10.0, nrDecimals.toDouble())).toDouble() / Math.pow(
            10.0,
            nrDecimals.toDouble()
        )
    }

    /*fun calcolaPunteggio(): Int {
        val step1 = (this.nAssociatedTags / this.nTags).toDouble()
        var step2 = (100 - this.nQuery).toDouble() / 100.0
        if (step2 < 0.0) {
            step2 = 0.0
        }
        val passo3 = 1.0 / this.numComponents.toDouble()
        var passo4 = (30.0 - (this.endTime - this.startTime)) / 30.0
        if (passo4 < 0.0) {
            passo4 = 0.0
        }
        var risultatoFinale = this.round((step1 + step2 + passo3 + passo4) / 4.0, 2)
        risultatoFinale *= 100.0
        val moltiplicatore = this.totalResults.toDouble() / this.searchResults.toDouble()
        risultatoFinale *= moltiplicatore
        return risultatoFinale.toInt()
    }*/

    fun evaluate() {
        val tempoImpiegato = this.endTime - this.startTime
        logger.debug("-------------------------------------------------")
        logger.debug("Timing:")
        logger.debug("(Associations -> " + this.round(this.step1 - this.startTime, 2) + " s ) ")
        logger.debug("(Relationships -> " + this.round(this.step2 - this.step1, 2) + " s ) ")
        logger.debug("(Final query -> " + this.round(this.endTime - this.step2, 2) + " s ) ")
        logger.debug("Total Time " + this.round(tempoImpiegato, 2) + " secondi")
        logger.debug("-------------------------------------------------")
    }

    companion object {
        @JvmStatic
        val logger: Logger by lazy { LoggerFactory.getLogger(this::class.java) }
    }
}
