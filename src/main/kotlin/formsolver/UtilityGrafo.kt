/*
 * Decompiled with CFR 0_123.
 */
package formsolver

import org.apache.jena.graph.Factory
import org.apache.jena.graph.Graph
import org.apache.jena.graph.Node
import org.apache.jena.graph.Triple

object UtilityGrafo {
    fun unisciGrafi(g1: Graph, g2: Graph): Graph {
        val g = Factory.createDefaultGraph()
        val ite = g1.find(Triple.ANY)
        while (ite.hasNext()) {
            g.add(ite.next())
        }
        val ite2 = g2.find(Triple.ANY)
        while (ite2.hasNext()) {
            g.add(ite2.next())
        }
        return g
    }

    fun getIntornoTripla(tripla: Triple?, g: Graph): Graph {
        var temp: Triple
        val intornoTripla = Factory.createDefaultGraph()
        if (tripla == null) {
            return intornoTripla
        }
        val ite = g.find(tripla.subject, Node.ANY, Node.ANY)
        val ite2 = g.find(Node.ANY, Node.ANY, tripla.subject)
        val ite3 = g.find(tripla.`object`, Node.ANY, Node.ANY)
        val ite4 = g.find(Node.ANY, Node.ANY, tripla.`object`)
        intornoTripla.add(tripla)
        var pesoTemp = 0
        while (ite.hasNext()) {
            temp = ite.next()
            if (temp == tripla) continue
            intornoTripla.add(temp)
            ++pesoTemp
        }
        while (ite2.hasNext()) {
            temp = ite2.next()
            if (temp == tripla) continue
            intornoTripla.add(temp)
            ++pesoTemp
        }
        while (ite3.hasNext()) {
            temp = ite3.next()
            if (temp == tripla) continue
            intornoTripla.add(temp)
            ++pesoTemp
        }
        while (ite4.hasNext()) {
            temp = ite4.next()
            if (temp == tripla) continue
            intornoTripla.add(temp)
            ++pesoTemp
        }
        return intornoTripla
    }

    fun stampaGrafo(grafo: Graph) {
        val ite = grafo.find(Triple.ANY)
        while (ite.hasNext()) {
            val tri = ite.next()
            println(tri.toString())
        }
    }
}
