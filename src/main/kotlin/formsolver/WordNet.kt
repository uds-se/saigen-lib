/*
 * Decompiled with CFR 0_123.
 */
package formsolver

import edu.mit.jwi.Dictionary
import edu.mit.jwi.item.POS
import org.slf4j.Logger
import org.slf4j.LoggerFactory
// import java.io.File
import java.io.IOException
import java.net.URL

object WordNet {

    @JvmStatic
    private val logger: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    // this does not work with files packed in jar: https://stackoverflow.com/questions/36174897/issue-with-relative-paths-of-resources-files-in-an-executable-jar-using-maven
    // private val path = "WordNet-3.0" + File.separator + "dict"
    private val path: String = System.getenv("WORDNET_DICT")
    private val url = URL("file", null, path)
    private val dict = Dictionary(url)

    init {
        dict.open()
    }

    @Throws(IOException::class)
    fun getSynonyms(label: String): List<String> {
        val allSynonyms: MutableList<String> = mutableListOf()
        allSynonyms.add(label)
        // val wnhome = System.getenv("WNHOME")
        logger.debug("wordNet label $label")
        if (label.isBlank()) {
            return emptyList()
        }

        val idxWord = dict.getIndexWord(label, POS.NOUN)
        if (idxWord != null) {
            val ids = idxWord.wordIDs.size
            var id = 0
            while (id < ids) {
                val wordID = idxWord.wordIDs[id]
                val word = dict.getWord(wordID)
                val synSet = word.synset
                var syn = ""
                var senseNumber = -1
                val synonyms: MutableList<String> = mutableListOf()
                for (w in synSet.words) {
                    var x: Int
                    var split: List<String> = syn.split("_".toRegex())
                    if (w.senseKey.lemma.equals(label, ignoreCase = true)) continue
                    if (senseNumber == -1) {
                        senseNumber = dict.getSenseEntry(w.senseKey).senseNumber
                    }
                    if (dict.getSenseEntry(w.senseKey).senseNumber > senseNumber) continue
                    syn = w.senseKey.lemma.toLowerCase()
                    if (syn.contains("_") && (split.dropLastWhile { it.isEmpty() }.toTypedArray()).size > 1) {
                        syn = split[0]
                        x = 1
                        while (x < split.size) {
                            syn += Utility.initialLetterUpperCase(split[x])
                            ++x
                        }
                    }

                    split = syn.split("-".toRegex())
                    if (syn.contains("-") && (split.dropLastWhile { it.isEmpty() }.toTypedArray()).size > 1) {
                        syn = split[0]
                        x = 1
                        while (x < split.size) {
                            syn += Utility.initialLetterUpperCase(split[x])
                            ++x
                        }
                    }
                    split = syn.split("'".toRegex())
                    if (syn.contains("'") && (split.dropLastWhile { it.isEmpty() }.toTypedArray()).size > 1) {
                        syn = split[0]
                        x = 1
                        while (x < split.size) {
                            syn += split[x]
                            ++x
                        }
                    }
                    if (dict.getSenseEntry(w.senseKey).senseNumber < senseNumber) {
                        // synonyms.removeAll(synonyms) // Dunno why this line is here. It introduces a bug: Not all synonyms from WordNet are used. http://wordnetweb.princeton.edu/perl/webwn?s=flora&sub=Search+WordNet&o2=&o0=1&o8=1&o1=1&o7=&o5=&o9=&o6=&o3=&o4=&h= . "plant" is missing when we include this line.
                        if (!allSynonyms.contains(syn)) {
                            synonyms.add(syn)
                        }
                    } else if (!allSynonyms.contains(syn)) {
                        synonyms.add(syn)
                    }
                    senseNumber = dict.getSenseEntry(w.senseKey).senseNumber
                }
                allSynonyms.addAll(synonyms)
                ++id
            }
        }
        // if (allSynonyms.size > 0) {
        // 	logger.debug("The synonyms for the " + label + " label are: " + allSynonyms.toString())
        // }
        /**
         * Here I add sanitization since org.apache.jena.query doesn't accept queries with dot(.)
         * Honestly it should use regex
         */
        val allSynonymsSanitized = mutableListOf<String>()
        allSynonyms.forEach { synonym ->
            var syn = synonym
            syn = syn.replace(".", "")
            allSynonymsSanitized.add(syn)
        }

        if (allSynonymsSanitized.size > 0) {
            logger.debug("The synonyms for the $label label are: $allSynonymsSanitized")
        }

        return allSynonymsSanitized
    }
}
