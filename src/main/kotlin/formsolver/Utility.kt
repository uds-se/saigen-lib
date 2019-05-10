/*
 * Decompiled with CFR 0_123.
 */
package formsolver

import org.apache.jena.graph.Graph
import org.apache.jena.graph.Triple
import org.apache.jena.query.ResultSet
import java.io.BufferedWriter
import java.io.FileWriter
import java.util.Date

object Utility {
    var analizzato: Graph? = null

    fun initialLetterUpperCase(string: String): String {
        var str = string
        str = str.trim { it <= ' ' }
        str = str.substring(0, 1).toUpperCase() + str.substring(1)
        return str
    }

    fun initialToLowerCase(string: String): String {
        var str = string
        str = str.trim { it <= ' ' }
        str = str.substring(0, 1).toLowerCase() + str.substring(1)
        return str
    }

    fun editLabels(array: Array<String>, maiuscole: Boolean): Array<String> {
        if (maiuscole) {
            var i = 0
            while (i < array.size) {
                array[i] = Utility.initialLetterUpperCase(array[i])
                ++i
            }
        } else {
            var i = 0
            while (i < array.size) {
                array[i] = Utility.initialToLowerCase(array[i])
                ++i
            }
        }
        return array
    }

    fun stampaForesta(foresta: List<Graph>) {
        for (g in foresta) {
            println("----------------------------")
            val ite = g.find(Triple.ANY)
            while (ite.hasNext()) {
                val tri = ite.next()
                println(tri)
            }
        }
    }

    fun writeXmlFile(s: String, titolo: String) {
        try {
            val t = Date()
            val fstream = FileWriter("risultati/" + titolo + "-" + t.time + ".xml")
            val out = BufferedWriter(fstream)
            out.write(s)
            out.close()
        } catch (e2: Exception) {
            System.err.println("Error: " + e2.message)
        }
    }

    fun verificaNumerosita(rs: ResultSet, limit: Int): Boolean {
        var contatore = 0
        while (rs.hasNext()) {
            rs.next()
            ++contatore
        }
        return contatore > limit
    }

    fun getNumerosita(rs: ResultSet): Int {
        var contatore = 0
        while (rs.hasNext()) {
            rs.next()
            ++contatore
        }
        return contatore
    }

    fun getStringFromGrafo(g: Graph): String {
        val ite = g.find(Triple.ANY)
        var queryString = ""
        while (ite.hasNext()) {
            val tri = ite.next()
            queryString =
                queryString + tri.subject.toString() + " " + tri.predicate + " " + tri.`object`.toString() + " . \n"
        }
        return queryString
    }
}
