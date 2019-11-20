package formsolver

import org.apache.jena.graph.Graph
import org.apache.jena.graph.Node
import org.apache.jena.graph.NodeFactory
import org.apache.jena.graph.Triple
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.ResultSet
import org.apache.jena.query.ResultSetFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class QueryExecutor internal constructor(
    private var graph: Graph,
    private var elements: List<Element>,
    private var heuristic: Int
) {
    private lateinit var selectionVariability: List<String>
    internal var numComponents: Int = 0
    var numResults: MutableList<Int> = mutableListOf()
    private lateinit var updatedForest: List<Graph>
    internal var validForest: MutableList<Graph> = mutableListOf()
    internal var cutLabels: MutableList<String> = mutableListOf()
    private lateinit var updatedGraph: Graph
    private val wikidataPrefix = "PREFIX wikibase: <http://wikiba.se/ontology#>" + "PREFIX wdt: <http://www.wikidata.org/prop/direct/>" + "PREFIX bd: <http://www.bigdata.com/rdf#>" +  "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"


    internal fun runFinalQuery(maxEntries: Int) {
        CommonData.queryToValuesMap.clear() // might want to comment these two lines when using RelationshipGenerator in FormSolver.kt.
        CommonData.outputQueries = emptyList<String>().toMutableList() //
        val analyser = GraphAnalyser(this.graph)
        analyser.splitConnectedComponents()
        analyser.setElements(this.elements)
        val forest = analyser.forest
        this.updatedForest = forest
        var i = 0
        val obtainedResults = IntArray(forest.size)
        for (g in forest) {
            val tmp_g = GraphMinimization.minimize(g)
            ++this.numComponents
            obtainedResults[i] = 0
            // var variability = this.costruisciStringaVarSelect(tmp_g)
            // if (variability == "") {
            //     variability = "*"
            // }
            var queryString = ""
            val ite = tmp_g.find(Triple.ANY)
            while (ite.hasNext()) {
                val tri = ite.next()
                // queryString =
                //     queryString + tri.subject.toString() + " " + tri.predicate + " " + tri.`object`.toString() + " . \n"
                // using AS to re-label ?itemLabel to queried string to make Storage.queryProvidersRecursively: .filterNot { l -> result.first.any { it == l } }    work correctly.
                // queryString = wikidataPrefix + "SELECT DISTINCT (?itemLabel AS ?" + tri.subject.toString() + ") WHERE { ?s ?label \"" + tri.subject.toString() + "\"@en ." + "?item wdt:P31 ?s ." + "SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" . }} LIMIT $maxEntries"// "SELECT DISTINCT " + variability + "\n" + "WHERE { \n"
                queryString = wikidataPrefix + "SELECT DISTINCT (?final AS ?" + tri.subject.toString() + ") WHERE { ?s ?label \"" + tri.subject.toString() + "\"@en ." + "?item wdt:P31 ?s ." + "?item rdfs:label ?final ." + "FILTER(LANG(?final) = \"en\") } LIMIT $maxEntries"
            }


                // queryString = wikidataPrefix + "SELECT DISTINCT ?itemLabel WHERE { ?s ?label \"" + tri.subject.toString() + "\"@en ." + "?item wdt:P31 ?s ." + "SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" . }} LIMIT $maxEntries"// "SELECT DISTINCT " + variability + "\n" + "WHERE { \n"
            // queryString = "$queryString} LIMIT $maxEntries" // "500"

            logger.debug("Query:")
            logger.debug(queryString)
            CommonData.outputQueries.add(queryString)
            val query2 = QueryFactory.create(queryString)
            val qExec = QueryExecutionFactory.sparqlService(FormSolver.endpoint, query2)
            var rs: ResultSet? = null
            try {
                rs = qExec.execSelect()
            } catch (e2: Exception) {
                logger.error("Global 15s query timeout exceeded")
            }

            if (rs != null && rs.hasNext()) {
                val rsrw = ResultSetFactory.copyResults(rs)
                val rsVariables = rs.resultVars // To get all the keys (Column name)
                val rsValues = mutableListOf<MutableMap<String, String>>()
                // ResultSetFormatter.out(System.out, rsrw, query2)
                while (rsrw.hasNext()) {
                    val rsMap = mutableMapOf<String, String>()
                    val solution = rsrw.next()

                    rsVariables.forEach {
                        rsMap[it] = solution.get(it).toString()
                    }
                    rsValues.add(rsMap)
                }
                obtainedResults[i] = rsrw.size()
                // CommonData.resultValues.addAll(rsValues)
                CommonData.queryToValuesMap[query2] = rsValues
            }
            qExec.close()

            logger.debug("Number of query results: " + obtainedResults[++i - 1])
            if (obtainedResults[i - 1] < CommonData.modelLimit) {
                if (obtainedResults[i - 1] > 0) {
                    logger.debug("Valid model  the support is " + obtainedResults[i - 1])
                }
                --this.numComponents
                this.resolveGraph(g, maxEntries) // recursive call
                continue
            }
            this.validForest.add(tmp_g)
        }
        this.numResults.addAll(obtainedResults.toList())
    }

    private fun resolveGraph(g: Graph, maxEntries: Int): List<String> {
        val modelSolver = ModelSolver(this.heuristic)
        modelSolver.createWeightedGraphFromModel(g)
        this.cutLabels = modelSolver.solveModel(this.cutLabels)
        val newGraph = modelSolver.updatedGraph
        this.updatedGraph = newGraph!!
        this.graph = newGraph
        this.runFinalQuery(maxEntries)
        return this.cutLabels
    }

    /*fun eseguiSingolaQuery(g: Graph): Int {
        try {
            val variabili = this.costruisciStringaVarSelect(g)
            var queryString = CommonData.prefix.toString() + "SELECT " + variabili + "\n" + "WHERE { \n"
            val ite = g.find(Triple.ANY)
            while (ite.hasNext()) {
                val tri = ite.next()
                queryString = queryString + tri.subject.toString() + " " + tri.predicate + " " + tri.`object`.toString() + " . \n"
            }
            queryString = "$queryString} LIMIT 200"
            val query2 = QueryFactory.create(queryString)
            val qexec = QueryExecutionFactory.sparqlService(FormSolver.endpoint, query2)
            var rs: ResultSet? = null
            try {
                rs = qexec.execSelect()
            } catch (exception: Exception) {
                // empty catch block
            }

            var numRes = 0
            while (rs != null && rs.hasNext()) {
                rs.next()
                ++numRes
            }
            qexec.close()
            return numRes
        } catch (variabili: Exception) {
            return 0
        }
    }*/

    private fun costruisciStringaVarSelect(gr: Graph): String {
        var variabiliSelect = ""
        this.selectionVariability = emptyList()
        for (e2 in this.elements) {
            val tri = Triple(Node.ANY, Node.ANY, NodeFactory.createURI("?object_" + e2.name))
            val tri2 = Triple(NodeFactory.createURI("?subject_" + e2.name), Node.ANY, Node.ANY)
            if (e2 is Predicate) {
                if (!gr.contains(Node.ANY, Node.ANY, tri.`object`)) continue
                if (!e2.isRangeElement) {
                    variabiliSelect = variabiliSelect + "?object_" + e2.name + " "
                    continue
                }
                variabiliSelect = variabiliSelect + "?object_" + e2.name + " "
                continue
            }
            if (!gr.contains(tri2.subject, Node.ANY, Node.ANY)) continue
            variabiliSelect = variabiliSelect + "?subject_" + e2.name + " "
        }
        return variabiliSelect
    }

    internal fun queryProssimita(graphIntornoTripla: Graph): Int {
        val g = GraphMinimization.minimize(graphIntornoTripla)
        var queryString = ""
        try {
            var tri: Triple
            var ite = g.find(Triple.ANY)
            var variabili = ""
            while (ite.hasNext()) {
                tri = ite.next()
                variabili =
                    if (tri.`object`.uri[0] == '?') variabili + tri.`object`.uri + " " else variabili + tri.subject.uri + " "
            }
            variabili = "(COUNT(*) AS ?count)"
            queryString = wikidataPrefix + "SELECT DISTINCT " + variabili + "WHERE { \n"
            ite = g.find(Triple.ANY)
            while (ite.hasNext()) {
                tri = ite.next()
                queryString =
                    queryString + tri.subject.toString() + " " + tri.predicate + " " + tri.`object`.toString() + " . \n"
            }
            queryString += "} LIMIT ${CommonData.proximityLimit}"
            val query2 = QueryFactory.create(queryString)
            val qexec = QueryExecutionFactory.sparqlService(FormSolver.endpoint, query2)
            var rs: ResultSet? = null
            try {
                rs = qexec.execSelect()
            } catch (e2: Exception) {
                e2.printStackTrace()
                logger.error("eccezione")
            }

            var numRes = 0
            if (rs != null && rs.hasNext()) {
                val sqs = rs.next()
                val node = sqs.get("count")
                numRes = node.asLiteral().int
            }
            qexec.close()
            return numRes
        } catch (e3: Exception) {
            e3.printStackTrace()
            logger.error(queryString)
            return 0
        }
    }

    companion object {
        @JvmStatic
        val logger: Logger by lazy { LoggerFactory.getLogger(this::class.java) }
    }
}
