package formsolver

import org.apache.jena.graph.Factory
import org.apache.jena.graph.Graph
import org.apache.jena.graph.Node
import org.apache.jena.graph.Triple

class GraphAnalyser internal constructor(private val graph: Graph) {
    private val analysed: Graph = Factory.createDefaultGraph()
    internal var forest: MutableList<Graph> = mutableListOf()
        private set
    private var elements: MutableList<Element> = mutableListOf()

    internal fun setElements(elements: List<Element>) {
        this.elements = elements.toMutableList()
    }

    internal fun splitConnectedComponents() {
        this.forest = mutableListOf()
        val ite1 = this.graph.find(Triple.ANY)
        var i = -1
        while (ite1.hasNext()) {
            val tri = ite1.next()
            if ((i < 0 || this.analysed.contains(tri)) && i != -1) continue
            ++i
            var g = Factory.createDefaultGraph()
            g.add(tri)
            this.analysed.add(tri)
            g = this.exploreTriple(tri, g)
            this.forest.add(g)
        }
    }

    /*fun getGraphFromTriple(tri: Triple): Graph {
        val g = Factory.createDefaultGraph()
                .also { it.add(tri) }

        return this.exploreTriple(tri, g)
    }*/

    private fun exploreTriple(tri: Triple, graphAtt: Graph): Graph {
        var internalGraphAtt = graphAtt
        var tmp: Triple
        val tripleSubject = this.graph.find(tri.subject, Node.ANY, Node.ANY)
        val tripleObject = this.graph.find(Node.ANY, Node.ANY, tri.`object`)
        val tripleObjectInv = this.graph.find(tri.`object`, Node.ANY, Node.ANY)
        val tripleSubjectInv = this.graph.find(Node.ANY, Node.ANY, tri.subject)
        while (tripleSubject.hasNext()) {
            tmp = tripleSubject.next()
            if (this.analysed.contains(tmp)) continue
            graphAtt.add(tmp)
            this.analysed.add(tmp)
            internalGraphAtt = this.exploreTriple(tmp, graphAtt)
        }
        while (tripleObject.hasNext()) {
            tmp = tripleObject.next()
            if (this.analysed.contains(tmp)) continue
            graphAtt.add(tmp)
            this.analysed.add(tmp)
            internalGraphAtt = this.exploreTriple(tmp, graphAtt)
        }
        while (tripleObjectInv.hasNext()) {
            tmp = tripleObjectInv.next()
            if (this.analysed.contains(tmp)) continue
            graphAtt.add(tmp)
            this.analysed.add(tmp)
            internalGraphAtt = this.exploreTriple(tmp, graphAtt)
        }
        while (tripleSubjectInv.hasNext()) {
            tmp = tripleSubjectInv.next()
            if (this.analysed.contains(tmp)) continue
            graphAtt.add(tmp)
            this.analysed.add(tmp)
            internalGraphAtt = this.exploreTriple(tmp, graphAtt)
        }
        return internalGraphAtt
    }

    /*fun isSameComponent(e1: Element, e2: Element): Boolean {
        for (g in this.forest) {
            if (e1 !is Classe || e2 !is Classe)
                continue

            val ite1 = g.find(NodeFactory.createURI(e1.name), Node.ANY, Node.ANY)
            val iteAlt1 = g.find(Node.ANY, Node.ANY, NodeFactory.createURI(e1.name))
            val ite2 = g.find(NodeFactory.createURI(e2.name), Node.ANY, Node.ANY)
            val iteAlt2 = g.find(Node.ANY, Node.ANY, NodeFactory.createURI(e2.name))

            if (!(ite1.hasNext() or iteAlt1.hasNext()) || !(ite2.hasNext() or iteAlt2.hasNext()))
                continue

            return true
        }

        return false
    }*/

    /*fun printForest() {
        for (g in this.forest) {
            println("----------------------------")
            val ite = g.find(Triple.ANY)
            while (ite.hasNext()) {
                val tri = ite.next()
                println(tri)
            }
        }
    }*/
}
