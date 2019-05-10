package formsolver

import org.apache.jena.graph.Factory
import org.apache.jena.graph.Graph
import org.apache.jena.graph.Triple
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FormSolver(
    private val tags: Array<String>,
    private val useThreshold: Boolean,
    private val associationThreshold: Int,
    private val heuristic: Int
) {
    private var graph = Factory.createDefaultGraph()
    private val namespaces = CommonData.namespacesXModel
    private var elements: MutableList<Element> = mutableListOf()
    private var elementsLv2: MutableList<Element> = mutableListOf()
    private var graphElements: MutableList<MutableList<Element>> = mutableListOf()
    private var graphHeuristic: MutableList<Int> = mutableListOf()
    private var graphToPrint: MutableList<Graph> = mutableListOf()

    fun runProcess(maxEntries: Int)/*: List<String>*/ {
        val evaluator = Evaluator()
        evaluator.setNrTags(this.tags.size)
        evaluator.setSearchResults(maxEntries) // 200)
        val start = System.currentTimeMillis()
        evaluator.setStartTime(start)

        val genAss = AssociationsBuilder(this.graph, this.tags, this.namespaces, this.useThreshold)
        this.graph = genAss.buildAssociations(this.associationThreshold)
        evaluator.addNQuery(genAss.nQuery)
        evaluator.setNAssociatedTags(genAss.nAssociatedTags)
        this.elements = genAss.elements
        this.elementsLv2 = genAss.elementsLv2

        val step2 = System.currentTimeMillis()
        evaluator.setEndTimeStep1(step2)
        logger.debug("Generation of associations is over")

        val genRel = RelationshipGenerator(this.graph, this.elements, this.elementsLv2)
        genRel.analyseCorrelations()
        evaluator.addNQuery(genRel.nQuery)
        this.graph = genRel.graph

        val step3 = System.currentTimeMillis()
        evaluator.setEndTimeStep2(step3)
        logger.debug("Generation of associations is over")

        val queryExecutor = QueryExecutor(this.graph, this.elements, this.heuristic)
        queryExecutor.runFinalQuery(maxEntries)
        evaluator.setObtainedResult(queryExecutor.numResults)
        evaluator.numComponents = queryExecutor.numComponents

        /*val step4 = System.currentTimeMillis()
        evaluator.setEndTime(step4)

        evaluator.evaluate()
        this.graphElements.add(this.elements)
        this.graphHeuristic.add(this.heuristic)
        this.graphToPrint.add(GraphMinimization.minimize(this.forestToGraph(queryExecutor.validForest)))*/

        // this.printGraph(modelNumber) //remove to run without interruption in my project
        /*logger.debug("Cut labels " + queryExecutor.cutLabels.joinToString())
        return queryExecutor.cutLabels*/
    }

    /*private fun printGraph(modelNumber: Int) {
        var x = 0
        while (x < this.graphToPrint.size) {
            gui.GraphWindow(this.graphToPrint[x], this.graphElements[x], this.graphHeuristic[x], "Model$modelNumber")
            ++x
        }
    }*/

    /*private fun rimuoviPredicatiDoppi() {
        var ite = this.graph.find(Node.ANY, Node.ANY, Node.ANY)
        val predicati : MutableList<Node> = mutableListOf()
        val predicatiDoppi : MutableList<Node> = mutableListOf()
        var triple: Triple?
        while (ite.hasNext()) {
            triple = ite.next()
            if (!predicati.contains(triple!!.predicate)) {
                predicati.add(triple.predicate)
                continue
            }
            predicatiDoppi.add(triple.predicate)
        }
        if (predicatiDoppi.size > 1) {
            val predicatiToRemove : MutableList<Triple> = mutableListOf()
            var x = 0
            while (x < predicatiDoppi.size) {
                ite = this.graph.find(Node.ANY, predicatiDoppi[x] as Node, Node.ANY)
                var triple1 = ite.next()
                var triple2: Triple?
                while (ite.hasNext()) {
                    triple2 = ite.next()
                    if (this.graph.find(triple1.subject, Node.ANY, Node.ANY).toList().size < this.graph.find(triple2!!.subject, Node.ANY, Node.ANY).toList().size) {
                        predicatiToRemove.add(triple1)
                        triple1 = triple2
                        continue
                    }
                    predicatiToRemove.add(triple2)
                }
                var y = 0
                while (y < predicatiToRemove.size) {
                    this.graph.delete(predicatiToRemove[y] as Triple)
                    ++y
                }
                ++x
            }
        }
    }*/

    private fun forestToGraph(forest: List<Graph>): Graph {
        val g = Factory.createDefaultGraph()
        for (component in forest) {
            val ite = component.find(Triple.ANY)
            while (ite.hasNext()) {
                val triple = ite.next()
                g.add(triple)
            }
        }
        return g
    }

    companion object {
        @JvmStatic
        val logger: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

        @JvmStatic
        var endpoint = "http://dbpedia.org/sparql/"
    }
}
