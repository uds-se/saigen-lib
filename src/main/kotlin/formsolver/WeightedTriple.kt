/*
 * Decompiled with CFR 0_123.
 */
package formsolver

import org.apache.jena.graph.Factory
import org.apache.jena.graph.Graph
import org.apache.jena.graph.Node
import org.apache.jena.graph.Triple

class WeightedTriple(val tripla: Triple, internal var euristica: Int) : Comparable<WeightedTriple> {
    val peso: Int
    var pesoInv: Double = 0.toDouble()

    init {
        this.peso = 1
        this.pesoInv = 1.0
    }

    fun calculateWeight(g: Graph) {
        var temp: Triple
        var temp2: Triple
        var pesoConn = 1.0
        var pesoSupporto = 0.0
        var pesoMisto = 0.0
        this.pesoInv = 1.0
        var numTriple = 0
        val intornoTripla = Factory.createDefaultGraph()
        val ite = g.find(this.tripla.subject, Node.ANY, Node.ANY)
        val ite2 = g.find(Node.ANY, Node.ANY, this.tripla.subject)
        intornoTripla.add(this.tripla)
        while (ite.hasNext()) {
            temp = ite.next()
            if (temp == this.tripla) continue
            intornoTripla.add(temp)
            pesoConn += 1.0
        }
        while (ite2.hasNext()) {
            temp = ite2.next()
            if (temp == this.tripla) continue
            intornoTripla.add(temp)
            pesoConn += 1.0
        }
        val ite3 = g.find(this.tripla.`object`, Node.ANY, Node.ANY)
        val ite4 = g.find(Node.ANY, Node.ANY, this.tripla.`object`)
        while (ite3.hasNext()) {
            temp2 = ite3.next()
            if (temp2 == this.tripla) continue
            intornoTripla.add(temp2)
            pesoConn += 1.0
        }
        while (ite4.hasNext()) {
            temp2 = ite4.next()
            if (temp2 == this.tripla) continue
            intornoTripla.add(temp2)
            pesoConn += 1.0
        }
        val iteTri = g.find(Triple.ANY)
        while (iteTri.hasNext()) {
            iteTri.next()
            ++numTriple
        }
        pesoConn = numTriple.toDouble() / pesoConn
        pesoConn = this.arrotonda(pesoConn, 2)
        if (this.euristica == 0) {
            this.pesoInv = pesoConn
        }
        if (this.euristica == 1 || this.euristica == 2) {
            val esec = QueryExecutorWikidata(intornoTripla, emptyList(), this.euristica)
            pesoSupporto = esec.queryProssimita(intornoTripla).toDouble()
            if (pesoSupporto > CommonData.proximityLimit.toDouble()) {
                pesoSupporto = CommonData.proximityLimit.toDouble()
            }
            this.pesoInv = pesoSupporto
        }
        if (this.euristica == 2) {
            this.pesoInv = pesoConn + pesoSupporto
            pesoMisto = this.pesoInv
        }
    }

    override fun compareTo(other: WeightedTriple): Int {
        return -this.peso - other.peso
    }

    fun arrotonda(numero: Double, nCifreDecimali: Int): Double {
        return Math.round(numero * Math.pow(10.0, nCifreDecimali.toDouble())).toDouble() / Math.pow(
            10.0,
            nCifreDecimali.toDouble()
        )
    }
}
