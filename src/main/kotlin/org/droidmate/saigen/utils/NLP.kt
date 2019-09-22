package org.droidmate.saigen.utils

import edu.stanford.nlp.process.Morphology
import edu.stanford.nlp.tagger.maxent.MaxentTagger
import edu.washington.cs.knowitall.morpha.MorphaStemmer
import formsolver.WordNet
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun String.getSynonyms() = NLP.getSynonyms(this)

object NLP {
    @JvmStatic
    val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    @JvmStatic
    private val nounReplacements = mapOf(
        /** Reason: On apps, 'search' is usually used as a verb but Stanford NLP tags it as a noun */
        "search" to "",
        /** Reason: WordNet doesn't have a synonym for 'where'. But Thesaurus.com has 'location' as a SYN for it. */
        "where" to "location",
        /** Reason: Thesaurus.com has 'song' as a SYN for 'music'. */
        "music" to "song"
    )

    /*@JvmStatic
    private val pluralToSingularReplacements = mapOf(
        "address" to "address"
    )*/

    @JvmStatic
    private val cache = mutableMapOf<String, List<String>>()

    @JvmStatic
    private val tagger = MaxentTagger("models/english-left3words-distsim.tagger") // src/main/resources/models/english-left3words-distsim.tagger") // models/english-left3words-distsim.tagger")

    @JvmStatic
    fun getSynonyms(word: String): List<String> {
        if (!cache.containsKey(word)) {
            try {
                val words = WordNet.getSynonyms(word)
                    .map { it.replace("_", " ") }
                    .map {
                        it.map { c ->
                            if (c in 'A'..'Z')
                                " $c"
                            else
                                "$c"
                        }.joinToString("")
                    }
                    .filterNot { it.contains(" ") }
                    .distinct()
                words.forEach { cache[it] = words }
            } catch (e: Exception) {
                cache[word] = emptyList()
                log.error("Unable to get synonym for $word", e)
            }
        }

        return cache[word].orEmpty()
    }

    /** Here are guided tempering which can help increase effectiveness of retrieving the correct semantics
     *  However, it should be avoided as much as possible since it's not automated, and represents human bias.
     */
    private fun String.replaceWrongNouns(): String {
        var tmp = this
        nounReplacements.forEach { oldWord, newWord -> tmp = tmp.replace(oldWord, newWord) }
        return tmp
    }

    /**
     * Remove invalid characters and
     * replace nouns which Tagger and WordNet cannot identify
     */
    private fun String.preProcessLabel(): String = this.toLowerCase()
        .replaceWrongNouns()

    /**
     * Here changed into split word and tag
     * Filtering and keeping the members that are nouns only
     * and putting each separated word (without _NN* tags) into a list
     *
     * Reference for PenTreeBankConstituents e.g.
     *   NN-Noun singular,
     *   NNS-Noun Plural,
     *   NNP-Proper Noun singular,
     *   NNPS-Proper Noun plural,
     *   NP-Noun Phrase
     * http://www.surdeanu.info/mihai/teaching/ista555-fall13/readings/PennTreebankConstituents.html#NNP
     */
    private fun String.extractNouns(): List<String> = try {
        this.split(" ")
            .filter { "NN" in it || "NP" in it }
            .map { it.split("_") }
            // .map { it.substringBefore("_") }
            .map {
                try {
                    Morphology().lemma(it[0], it[1]).orEmpty()
                } catch (e: Exception) {
                    log.trace("Unable to lemma for word: ${it[0]}", e)
                    it[0]
                }
            }
    } catch (e: Exception) {
        log.trace("Unable to get nouns from word: $this", e)
        emptyList()
    }

    /**
     * Converts a noun into singular
     *
     * Got illegal group reference using hypertino inflector. Changes to Morpha.
     *  var single = English.singular(it) // com.hypertino.inflector.English, converts plural to singular.
     */
    private fun String.toSingular(): String = try {
        MorphaStemmer.stem(this)
    } catch (e: Exception) {
        log.error("Unable to extract singular from word '$this'. Error: $e", e)
        this
    }

    /**
     * Tagger function from Standford NLP group
     *
     * It takes a string and returns only the nouns to be used as the label for the descriptive widget
     *
     * Done:
     *  1. convert plural to singular,
     *  2. lowerCase() before tagging,
     *  3. remove duplicates
     */
    fun getNouns(label: String): List<String> {
        if (label.isEmpty())
            return emptyList()

        val logger = StringBuilder()

        val preProcessedLabel = label.preProcessLabel()
        val taggedLabel = tagger.tagString(preProcessedLabel)
        logger.appendln("Tagged label: $taggedLabel")

        val nounList = taggedLabel.extractNouns()
        logger.appendln("List of nouns: ${nounList.joinToString(", ")}")

        return nounList
            .map { it.toSingular() }
            .distinct()
    }
}