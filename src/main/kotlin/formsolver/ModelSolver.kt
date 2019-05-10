/*
 * Decompiled with CFR 0_123.
 */
package formsolver

import org.apache.jena.graph.Factory
import org.apache.jena.graph.Graph
import org.apache.jena.graph.Node
import org.apache.jena.graph.NodeFactory
import org.apache.jena.graph.Triple
import org.apache.jena.query.Query
import org.apache.jena.query.QueryExecution
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.QuerySolution
import org.apache.jena.query.ResultSet
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.util.iterator.ExtendedIterator
import org.jgrapht.alg.StoerWagnerMinimumCut
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
import org.droidmate.saigen.utils.NLP
import java.io.IOException

class ModelSolver(private val heuristic: Int) {
    private var graph: SimpleWeightedGraph<String, DefaultWeightedEdge>? = null
    var updatedGraph: Graph? = null
        private set
    // private val elementi: MutableList<Element>? = null

    /*fun isValid(graph: Graph): Boolean {
        return false
    }*/

    fun createWeightedGraphFromModel(jenaGraph: Graph) {
        this.updatedGraph = jenaGraph
        this.graph = SimpleWeightedGraph(DefaultWeightedEdge::class.java)
        val ite1 = jenaGraph.find(Triple.ANY)
        while (ite1.hasNext()) {
            val tri = ite1.next()
            if (!this.graph!!.containsVertex(tri.subject.uri)) {
                this.graph!!.addVertex(tri.subject.uri)
            }
            if (!this.graph!!.containsVertex(tri.`object`.uri)) {
                this.graph!!.addVertex(tri.`object`.uri)
            }
            this.graph!!.addEdge(tri.subject.uri, tri.`object`.uri)

            val trip = WeightedTriple(tri, this.heuristic)
            trip.calculateWeight(jenaGraph)
            this.graph!!.setEdgeWeight(this.graph!!.getEdge(tri.subject.uri, tri.`object`.uri), trip.pesoInv)
            println("Weight " + tri.subject.uri.toString() + tri.`object`.uri.toString() + trip.pesoInv)
        }
    }

    fun solveModel(cutLabels: MutableList<String>): MutableList<String> {
        val analyser = StoerWagnerMinimumCut(this.graph!!)
        val minimalCut = analyser.minCut()
        val ite = minimalCut.iterator()
        val tripleToDelete: MutableList<Triple> = mutableListOf()
        while (ite.hasNext()) {
            var triple: Triple
            val node = ite.next()
            println("Selected node for cutting $node")
            val tripleSubject = this.updatedGraph!!.find(NodeFactory.createURI(node), Node.ANY, Node.ANY)
            val tripleObject = this.updatedGraph!!.find(Node.ANY, Node.ANY, NodeFactory.createURI(node))
            while (tripleSubject.hasNext()) {
                triple = tripleSubject.next()
                if (minimalCut.contains(triple.`object`.uri)) continue
                tripleToDelete.add(triple)
            }
            while (tripleObject.hasNext()) {
                triple = tripleObject.next()
                if (minimalCut.contains(triple.subject.uri)) continue
                tripleToDelete.add(triple)
            }
        }
        if (this.lastChance(minimalCut, tripleToDelete)) {
            return cutLabels
        }
        for (triple in tripleToDelete) {
            println("Triple to replace / delete after lastChance$triple")
            this.updatedGraph!!.delete(triple)
            if (this.isBasePredicate(triple)) {
                // //val newTriple = Triple(NodeFactory.createURI(triple.subject.uri.toString() + "_rec"), triple.predicate, NodeFactory.createURI(triple.`object`.uri.toString() + "_rec"))
                cutLabels.add(triple.predicate.uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            }
            if (!this.isBaseClass(triple)) continue
            val tripleOfClass = this.updatedGraph!!.find(triple.subject, Node.ANY, Node.ANY).toSet().iterator()
            while (tripleOfClass.hasNext()) {
                this.updatedGraph!!.delete(tripleOfClass.next())
            }
            // //val newTriple = Triple(NodeFactory.createURI(triple.subject.uri.toString() + "_rec"), triple.predicate, NodeFactory.createURI(triple.`object`.uri))
            cutLabels.add(triple.`object`.uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
        }
        return cutLabels
    }

    private fun lastChance(minimalCut: Set<String>, cutTriple: List<Triple>): Boolean {
        val tripleToDelete: MutableList<Triple> = mutableListOf()
        var tmpJenaGraph = Factory.createDefaultGraph()
        val iterator = this.updatedGraph!!.find(Triple.ANY)
        while (iterator.hasNext()) {
            tmpJenaGraph.add(iterator.next())
        }
        for (triple in cutTriple) {
            println("Triple to replace / delete before lastChance$triple")
            tmpJenaGraph.delete(triple)
            if (triple.predicate.toString()[0] != '?' && triple.predicate.uri != "a" && !tmpJenaGraph.contains(
                    Node.ANY,
                    triple.predicate,
                    Node.ANY
                )
            ) {
                tripleToDelete.add(triple)
                println("Base predicate")
            }
            if (triple.predicate.uri != "a" || tmpJenaGraph.contains(Node.ANY, Node.ANY, triple.`object`)) continue
            tripleToDelete.add(triple)
            println("Base class")
        }

        tmpJenaGraph = GraphMinimization.minimize(tmpJenaGraph)
        var queryString = CommonData.prefix + "SELECT " + " (COUNT(*) as ?count)" + "\n" + "WHERE { \n"
        val ite = tmpJenaGraph.find(Triple.ANY)
        while (ite.hasNext()) {
            val tri = ite.next()
            queryString =
                queryString + tri.subject.toString() + " " + tri.predicate + " " + tri.`object`.toString() + " . \n"
        }
        queryString = queryString + "} LIMIT ${CommonData.modelLimit}"
        println("Query for status verification required for lastChance execution")
        println(queryString)

        val query2 = QueryFactory.create(queryString)
        val qExec = QueryExecutionFactory.sparqlService(FormSolver.endpoint, query2)
        var rs: ResultSet? = null
        try {
            rs = qExec.execSelect()
        } catch (e2: Exception) {
            System.err.println("Query Timeout")
        }

        var count = 0
        if (rs != null && rs.hasNext()) {
            val sqs = rs.next()
            count = sqs.get("count").asLiteral().int
        }
        if (tripleToDelete.isEmpty() || count == 0) {
            if (tripleToDelete.isEmpty()) {
                println("Empty Triple")
            } else {
                println("Count  zero")
            }
            return false
        }

        var tripleWithSynonymToAnalyse = tripleToDelete
        tripleWithSynonymToAnalyse = this.lastChanceCheckSynonym(tripleWithSynonymToAnalyse)
        tripleWithSynonymToAnalyse = this.lastChanceCheckNamespaces(tripleWithSynonymToAnalyse)
        tripleWithSynonymToAnalyse = this.lastChanceCheckNamespacesSynonyms(tripleWithSynonymToAnalyse)
        tripleWithSynonymToAnalyse = this.lastChanceCheckRedundancy(tripleWithSynonymToAnalyse)
        return tripleWithSynonymToAnalyse.isEmpty()
    }

    private fun isBasePredicate(triple: Triple): Boolean {
        if (triple.predicate.toString()[0] == '?' || triple.predicate.uri == "a") {
            return false
        }
        if (this.updatedGraph!!.contains(Node.ANY, triple.predicate, Node.ANY)) {
            return false
        }
        println("un predicate base")
        return true
    }

    private fun isBaseClass(triple: Triple): Boolean {
        if (triple.predicate.uri != "a") {
            return false
        }
        if (this.updatedGraph!!.contains(Node.ANY, Node.ANY, triple.`object`)) {
            return false
        }
        println("\ufffd a base class")
        return true
    }

    private fun lastChanceCheckNamespaces(tripleWithSynonymToAnalyse: MutableList<Triple>): MutableList<Triple> {
        val namespaces = CommonData.namespacesXCuts
        var maxSupport: Int
        var selectedNamespace: String
        val cutTriple: MutableList<String> = mutableListOf()
        for (triple in tripleWithSynonymToAnalyse) {
            var query2: Query
            var queryStringTripleToAnalyse: String
            var ite: ExtendedIterator<Triple>
            var tri: Triple
            var node: RDFNode
            var rs: ResultSet?
            var tmp: Set<Triple>
            var sqs: QuerySolution
            var namespace2: Int
            var qExec: QueryExecution
            var queryString: String
            var tmpJenaGraph = Factory.createDefaultGraph()
            val iterator = this.updatedGraph!!.find(Triple.ANY)
            while (iterator.hasNext()) {
                tmpJenaGraph.add(iterator.next())
            }
            tmpJenaGraph = GraphMinimization.minimize(tmpJenaGraph)
            if (triple.predicate.toString()[0] != '?' && triple.predicate.toString()[0] != 'a') {
                if (cutTriple.contains(triple.predicate.toString())) continue
                cutTriple.add(triple.predicate.toString())
                maxSupport = 0
                selectedNamespace = ""
                queryStringTripleToAnalyse = ""
                namespace2 = 0
                while (namespace2 < namespaces.size) {
                    val predicate = triple.predicate.localName
                    if (queryStringTripleToAnalyse.isEmpty()) {
                        tmp = tmpJenaGraph.find(Node.ANY, triple.predicate, Node.ANY).toSet()
                        for (auxTriple in tmp) {
                            tmpJenaGraph.delete(auxTriple)
                            queryStringTripleToAnalyse =
                                queryStringTripleToAnalyse + auxTriple.subject.toString() + " " + namespaces[namespace2] + predicate + " " + auxTriple.`object`.toString() + " .\n"
                        }
                    } else {
                        queryStringTripleToAnalyse = queryStringTripleToAnalyse.replace(
                            namespaces[namespace2 - 1] + predicate,
                            namespaces[namespace2] + predicate
                        )
                    }
                    queryString = CommonData.prefix + "SELECT " + " (COUNT(*) as ?count)" + "\n" + "WHERE { \n"
                    ite = tmpJenaGraph.find(Triple.ANY)
                    while (ite.hasNext()) {
                        tri = ite.next()
                        queryString =
                            queryString + tri.subject.toString() + " " + tri.predicate + " " + tri.`object`.toString() + " . \n"
                    }

                    queryString += queryStringTripleToAnalyse
                    queryString = "$queryString}"
                    println("Query: identify all namespaces of a predicate type label")
                    query2 = QueryFactory.create(queryString)
                    qExec = QueryExecutionFactory.sparqlService(FormSolver.endpoint, query2)
                    rs = null
                    try {
                        rs = qExec.execSelect()
                    } catch (e2: Exception) {
                        System.err.println("Timeout global query")
                    }

                    sqs = rs!!.next()
                    node = sqs.get("count")
                    if (rs.hasNext() && node.asLiteral().int > maxSupport) {
                        if (node.asLiteral().int > CommonData.modelLimit) {
                            maxSupport = node.asLiteral().int
                            selectedNamespace = namespaces[namespace2] + predicate
                        } else {
                            println("valid model for " + namespaces[namespace2] + predicate + " but support is " + node.asLiteral().int)
                        }
                    }
                    qExec.close()
                    ++namespace2
                }
                if (selectedNamespace.isEmpty()) continue
                val tmp2 = this.updatedGraph!!.find(Node.ANY, triple.predicate, Node.ANY).toSet()
                for (auxTriple in tmp2) {
                    println("Validated model replacing triple: $auxTriple")
                    this.updatedGraph!!.delete(auxTriple)
                    this.updatedGraph!!.add(
                        Triple(
                            auxTriple.subject,
                            NodeFactory.createURI(selectedNamespace),
                            auxTriple.`object`
                        )
                    )
                }
                tripleWithSynonymToAnalyse.clear()
                return tripleWithSynonymToAnalyse
            }

            if (triple.predicate.toString()[0] == '?' || cutTriple.contains(triple.`object`.toString())) continue

            cutTriple.add(triple.`object`.toString())
            maxSupport = 0
            selectedNamespace = ""
            queryStringTripleToAnalyse = ""
            namespace2 = 0

            while (namespace2 < namespaces.size) {
                val obj = triple.`object`.localName
                if (queryStringTripleToAnalyse.isEmpty()) {
                    tmp = tmpJenaGraph.find(Node.ANY, Node.ANY, triple.`object`).toSet()
                    for (auxTriple in tmp) {
                        tmpJenaGraph.delete(auxTriple)
                        queryStringTripleToAnalyse =
                            queryStringTripleToAnalyse + auxTriple.subject.toString() + " " + auxTriple.predicate.toString() + " " + namespaces[namespace2] + obj + " .\n"
                    }
                } else {
                    queryStringTripleToAnalyse = queryStringTripleToAnalyse.replace(
                        namespaces[namespace2 - 1] + obj,
                        namespaces[namespace2] + obj
                    )
                }

                queryString = CommonData.prefix + "SELECT " + " (COUNT(*) as ?count)" + "\n" + "WHERE { \n"
                ite = tmpJenaGraph.find(Triple.ANY)
                while (ite.hasNext()) {
                    tri = ite.next()
                    queryString =
                        queryString + tri.subject.toString() + " " + tri.predicate + " " + tri.`object`.toString() + " . \n"
                }

                queryString += queryStringTripleToAnalyse
                queryString = "$queryString}"
                println("Query: identify all namespace of a tag from a predicate type")
                query2 = QueryFactory.create(queryString)
                qExec = QueryExecutionFactory.sparqlService(FormSolver.endpoint, query2)
                rs = null
                try {
                    rs = qExec.execSelect()
                } catch (e3: Exception) {
                    System.err.println("Timeout global query")
                }

                sqs = rs!!.next()
                node = sqs.get("count")
                if (rs.hasNext() && node.asLiteral().int > maxSupport) {
                    if (node.asLiteral().int > CommonData.modelLimit) {
                        maxSupport = node.asLiteral().int
                        selectedNamespace = namespaces[namespace2] + obj
                    } else {
                        println("model valid for " + namespaces[namespace2] + obj + " but support is " + node.asLiteral().int)
                    }
                }
                qExec.close()
                ++namespace2
            }

            if (selectedNamespace.isEmpty()) continue
            val tmp3 = this.updatedGraph!!.find(Node.ANY, Node.ANY, triple.`object`).toSet()
            for (auxTriple in tmp3) {
                println("Validated model using triple: $auxTriple")
                this.updatedGraph!!.delete(auxTriple)
                this.updatedGraph!!.add(
                    Triple(
                        auxTriple.subject,
                        auxTriple.predicate,
                        NodeFactory.createURI(selectedNamespace)
                    )
                )
            }
            tripleWithSynonymToAnalyse.clear()
            return tripleWithSynonymToAnalyse
        }
        return tripleWithSynonymToAnalyse
    }

    private fun lastChanceCheckSynonym(tripleWithSynonymToAnalyse: MutableList<Triple>): MutableList<Triple> {
        var maxSupport: Int
        var selectedSynonym: String
        val tagList: MutableList<String> = mutableListOf()
        for (triple in tripleWithSynonymToAnalyse) {
            var tmp: Set<Triple>
            // val wn: WordNet
            var qExec: QueryExecution
            var node: RDFNode
            var queryString: String
            var sqs: QuerySolution
            var ite: ExtendedIterator<Triple>
            var synonyms: Array<String>
            var queryStringTripleToAnalyse: String
            var tri: Triple
            var rs: ResultSet?
            var query2: Query
            var synonym: Int
            var tmpJenaGraph = Factory.createDefaultGraph()
            val iterator = this.updatedGraph!!.find(Triple.ANY)
            while (iterator.hasNext()) {
                tmpJenaGraph.add(iterator.next())
            }
            tmpJenaGraph = GraphMinimization.minimize(tmpJenaGraph)
            if (triple.predicate.toString()[0] != '?' && triple.predicate.toString()[0] != 'a') {
                if (tagList.contains(triple.predicate.toString())) continue
                tagList.add(triple.predicate.toString())
                synonyms = arrayOf()
                try {
                    synonyms = NLP.getSynonyms(triple.predicate.localName).toTypedArray()
                } catch (e2: IOException) {
                    e2.printStackTrace()
                }

                maxSupport = 0
                selectedSynonym = ""
                queryStringTripleToAnalyse = ""
                synonym = 1
                while (synonym < synonyms.size) {
                    val predicate = synonyms[synonym]
                    if (queryStringTripleToAnalyse.isEmpty()) {
                        tmp = tmpJenaGraph.find(Node.ANY, triple.predicate, Node.ANY).toSet()
                        for (auxTriple in tmp) {
                            tmpJenaGraph.delete(auxTriple)
                            queryStringTripleToAnalyse =
                                queryStringTripleToAnalyse + auxTriple.subject.toString() + " " + "dbpedia-owl:" + predicate + " " + auxTriple.`object`.toString() + " .\n"
                        }
                    } else {
                        queryStringTripleToAnalyse = queryStringTripleToAnalyse.replace(
                            "dbpedia-owl:" + synonyms[synonym - 1],
                            "dbpedia-owl:" + synonyms[synonym]
                        )
                    }
                    queryString = CommonData.prefix + "SELECT " + " (COUNT(*) as ?count)" + "\n" + "WHERE { \n"
                    ite = tmpJenaGraph.find(Triple.ANY)
                    while (ite.hasNext()) {
                        tri = ite.next()
                        queryString =
                            queryString + tri.subject.toString() + " " + tri.predicate + " " + tri.`object`.toString() + " . \n"
                    }

                    queryString += queryStringTripleToAnalyse
                    queryString = "$queryString}"
                    println("Query: identify all synonym of a tag from a predicate type")

                    query2 = QueryFactory.create(queryString)
                    qExec = QueryExecutionFactory.sparqlService(FormSolver.endpoint, query2)
                    rs = null
                    try {
                        rs = qExec.execSelect()
                    } catch (e3: Exception) {
                        System.err.println("Timeout global query")
                    }

                    sqs = rs!!.next()
                    node = sqs.get("count")
                    if (rs.hasNext() && node.asLiteral().int > maxSupport) {
                        if (node.asLiteral().int > CommonData.modelLimit) {
                            maxSupport = node.asLiteral().int
                            selectedSynonym = "dbpedia-owl:$predicate"
                        } else {
                            println("model valid for dbpedia-owl:" + predicate + " but support is " + node.asLiteral().int)
                        }
                    }
                    qExec.close()
                    ++synonym
                }
                if (selectedSynonym.isEmpty()) continue
                val tmp2 = this.updatedGraph!!.find(Node.ANY, triple.predicate, Node.ANY).toSet()
                for (auxTriple in tmp2) {
                    println("Validated model using triple: " + auxTriple.toString())
                    this.updatedGraph!!.delete(auxTriple)
                    this.updatedGraph!!.add(
                        Triple(
                            auxTriple.subject,
                            NodeFactory.createURI(selectedSynonym),
                            auxTriple.`object`
                        )
                    )
                }
                tripleWithSynonymToAnalyse.clear()
                return tripleWithSynonymToAnalyse
            }
            if (triple.predicate.toString()[0] == '?' || tagList.contains(triple.`object`.toString())) continue
            tagList.add(triple.`object`.toString())
            synonyms = arrayOf()
            try {
                synonyms = NLP.getSynonyms(triple.`object`.localName).toTypedArray()
            } catch (e4: IOException) {
                e4.printStackTrace()
            }

            maxSupport = 0
            selectedSynonym = ""
            queryStringTripleToAnalyse = ""
            synonym = 1
            while (synonym < synonyms.size) {
                val obj = Utility.initialLetterUpperCase(synonyms[synonym])
                if (queryStringTripleToAnalyse.isEmpty()) {
                    tmp = tmpJenaGraph.find(Node.ANY, Node.ANY, triple.`object`).toSet()
                    for (auxTriple in tmp) {
                        tmpJenaGraph.delete(auxTriple)
                        queryStringTripleToAnalyse =
                            queryStringTripleToAnalyse + auxTriple.subject.toString() + " " + auxTriple.predicate.toString() + " " + "dbpedia-owl:" + obj + " .\n"
                    }
                } else {
                    queryStringTripleToAnalyse = queryStringTripleToAnalyse.replace(
                        "dbpedia-owl:" + synonyms[synonym - 1],
                        "dbpedia-owl:" + synonyms[synonym]
                    )
                }

                queryString = CommonData.prefix + "SELECT " + " (COUNT(*) as ?count)" + "\n" + "WHERE { \n"
                ite = tmpJenaGraph.find(Triple.ANY)
                while (ite.hasNext()) {
                    tri = ite.next()
                    queryString =
                        queryString + tri.subject.toString() + " " + tri.predicate + " " + tri.`object`.toString() + " . \n"
                }

                queryString += queryStringTripleToAnalyse
                queryString = "$queryString}"
                println("Query: identify all synonyms of a tag from a predicate type")
                query2 = QueryFactory.create(queryString)
                qExec = QueryExecutionFactory.sparqlService(FormSolver.endpoint, query2)
                rs = null
                try {
                    rs = qExec.execSelect()
                } catch (e5: Exception) {
                    System.err.println("Timeout gloval query")
                }

                sqs = rs!!.next()
                node = sqs.get("count")
                if (rs.hasNext() && node.asLiteral().int > maxSupport) {
                    if (node.asLiteral().int > CommonData.modelLimit) {
                        maxSupport = node.asLiteral().int
                        selectedSynonym = "dbpedia-owl:$obj"
                    } else {
                        println("model valid for dbpedia-owl:" + obj + " but support is " + node.asLiteral().int)
                    }
                }
                qExec.close()
                ++synonym
            }
            if (selectedSynonym.isEmpty()) continue
            val tmp3 = this.updatedGraph!!.find(Node.ANY, Node.ANY, triple.`object`).toSet()
            for (auxTriple in tmp3) {
                println("Validated model using triple: " + auxTriple.toString())
                this.updatedGraph!!.delete(auxTriple)
                this.updatedGraph!!.add(
                    Triple(
                        auxTriple.subject,
                        auxTriple.predicate,
                        NodeFactory.createURI(selectedSynonym)
                    )
                )
            }
            tripleWithSynonymToAnalyse.clear()
            return tripleWithSynonymToAnalyse
        }
        return tripleWithSynonymToAnalyse
    }

    private fun lastChanceCheckNamespacesSynonyms(tripleWithSynonymToEvaluate: MutableList<Triple>): MutableList<Triple> {
        val namespaces = CommonData.namespacesXCuts
        var maxSupport: Int
        var selectedNamespaceSynonym: String
        val tagList: MutableList<String> = mutableListOf()
        for (triple in tripleWithSynonymToEvaluate) {
            val wn: WordNet
            var synonym: Int
            var tmpJenaGraph: Graph
            var query2: Query
            var namespace2: Int
            var iterator: ExtendedIterator<Triple>
            var synonyms: Array<String>
            var rs: ResultSet?
            var tri: Triple
            var ite: ExtendedIterator<Triple>
            var tmp: Set<Triple>
            var sqs: QuerySolution
            var node: RDFNode
            var queryString: String
            var qexec: QueryExecution
            var queryStringTripleToAnalyse: String
            if (triple.predicate.toString()[0] != '?' && triple.predicate.toString()[0] != 'a') {
                if (tagList.contains(triple.predicate.toString())) continue
                synonyms = arrayOf()
                try {
                    synonyms = NLP.getSynonyms(triple.predicate.localName).toTypedArray()
                } catch (e2: IOException) {
                    e2.printStackTrace()
                }

                tagList.add(triple.predicate.toString())
                maxSupport = 0
                selectedNamespaceSynonym = ""
                synonym = 1
                while (synonym < synonyms.size) {
                    tmpJenaGraph = Factory.createDefaultGraph()
                    iterator = this.updatedGraph!!.find(Triple.ANY)
                    while (iterator.hasNext()) {
                        tmpJenaGraph.add(iterator.next())
                    }
                    tmpJenaGraph = GraphMinimization.minimize(tmpJenaGraph)
                    queryStringTripleToAnalyse = ""
                    namespace2 = 0

                    while (namespace2 < namespaces.size) {
                        val predicate = synonyms[synonym]
                        if (queryStringTripleToAnalyse.isEmpty()) {
                            tmp = tmpJenaGraph.find(Node.ANY, triple.predicate, Node.ANY).toSet()
                            for (tripleAux in tmp) {
                                tmpJenaGraph.delete(tripleAux)
                                queryStringTripleToAnalyse =
                                    queryStringTripleToAnalyse + tripleAux.subject.toString() + " " + namespaces[namespace2] + predicate + " " + tripleAux.`object`.toString() + " .\n"
                            }
                        } else {
                            queryStringTripleToAnalyse = queryStringTripleToAnalyse.replace(
                                namespaces[namespace2 - 1] + synonyms[synonym],
                                namespaces[namespace2].toString() + synonyms[synonym]
                            )
                        }

                        queryString = CommonData.prefix + "SELECT " + " (COUNT(*) as ?count)" + "\n" + "WHERE { \n"
                        ite = tmpJenaGraph.find(Triple.ANY)
                        while (ite.hasNext()) {
                            tri = ite.next()
                            queryString =
                                queryString + tri.subject.toString() + " " + tri.predicate + " " + tri.`object`.toString() + " . \n"
                        }

                        queryString += queryStringTripleToAnalyse
                        queryString = "$queryString}"
                        println("Query: identify all namespace+synonyms from a tag from a predicate type")
                        println(queryString)
                        query2 = QueryFactory.create(queryString)
                        qexec = QueryExecutionFactory.sparqlService(FormSolver.endpoint, query2)
                        rs = null
                        try {
                            rs = qexec.execSelect()
                        } catch (e3: Exception) {
                            System.err.println("Global 15s query timeout reached")
                        }

                        sqs = rs!!.next()
                        node = sqs.get("count")
                        if (rs.hasNext() && node.asLiteral().int > maxSupport) {
                            if (node.asLiteral().int > CommonData.modelLimit) {
                                maxSupport = node.asLiteral().int
                                selectedNamespaceSynonym = namespaces[namespace2] + predicate
                            } else {
                                println("model valid for " + namespaces[namespace2] + predicate + " but support is " + node.asLiteral().int)
                            }
                        }
                        qexec.close()
                        ++namespace2
                    }
                    ++synonym
                }
                if (selectedNamespaceSynonym.isEmpty()) continue
                val tmp2 = this.updatedGraph!!.find(Node.ANY, triple.predicate, Node.ANY).toSet()
                for (auxTriple in tmp2) {
                    println("Validate model using the triple: " + auxTriple.toString())
                    this.updatedGraph!!.delete(auxTriple)
                    this.updatedGraph!!.add(
                        Triple(
                            auxTriple.subject,
                            NodeFactory.createURI(selectedNamespaceSynonym),
                            auxTriple.`object`
                        )
                    )
                }

                tripleWithSynonymToEvaluate.clear()
                return tripleWithSynonymToEvaluate
            }
            if (triple.predicate.toString()[0] == '?' || tagList.contains(triple.`object`.toString())) continue
            synonyms = arrayOf()
            try {
                synonyms = NLP.getSynonyms(triple.`object`.localName).toTypedArray()
            } catch (e4: IOException) {
                e4.printStackTrace()
            }

            tagList.add(triple.`object`.toString())
            maxSupport = 0
            selectedNamespaceSynonym = ""
            synonym = 1
            while (synonym < synonyms.size) {
                tmpJenaGraph = Factory.createDefaultGraph()
                iterator = this.updatedGraph!!.find(Triple.ANY)
                while (iterator.hasNext()) {
                    tmpJenaGraph.add(iterator.next())
                }
                tmpJenaGraph = GraphMinimization.minimize(tmpJenaGraph)
                queryStringTripleToAnalyse = ""
                namespace2 = 0
                while (namespace2 < namespaces.size) {
                    val obj = Utility.initialLetterUpperCase(synonyms[synonym])
                    if (queryStringTripleToAnalyse.isEmpty()) {
                        tmp = tmpJenaGraph.find(Node.ANY, Node.ANY, triple.`object`).toSet()
                        for (auxTriple in tmp) {
                            tmpJenaGraph.delete(auxTriple)
                            queryStringTripleToAnalyse =
                                queryStringTripleToAnalyse + auxTriple.subject.toString() + " " + auxTriple.predicate.toString() + " " + namespaces[namespace2] + obj + " .\n"
                        }
                    } else {
                        queryStringTripleToAnalyse = queryStringTripleToAnalyse.replace(
                            namespaces[namespace2 - 1] + synonyms[synonym],
                            namespaces[namespace2] + synonyms[synonym]
                        )
                    }
                    queryString = CommonData.prefix + "SELECT " + " (COUNT(*) as ?count)" + "\n" + "WHERE { \n"
                    ite = tmpJenaGraph.find(Triple.ANY)
                    while (ite.hasNext()) {
                        tri = ite.next()
                        queryString =
                            queryString + tri.subject.toString() + " " + tri.predicate + " " + tri.`object`.toString() + " . \n"
                    }
                    queryString += queryStringTripleToAnalyse
                    queryString = "$queryString}"
                    println("Query: identify all namespace+synonyms from a tag from a predicate type")
                    query2 = QueryFactory.create(queryString)
                    qexec = QueryExecutionFactory.sparqlService(FormSolver.endpoint, query2)
                    rs = null
                    try {
                        rs = qexec.execSelect()
                    } catch (e5: Exception) {
                        System.err.println("Timeout global query")
                    }

                    sqs = rs!!.next()
                    node = sqs.get("count")
                    if (rs.hasNext() && node.asLiteral().int > maxSupport) {
                        if (node.asLiteral().int > CommonData.modelLimit) {
                            maxSupport = node.asLiteral().int
                            selectedNamespaceSynonym = namespaces[namespace2] + obj
                        } else {
                            println("model valid for " + namespaces[namespace2] + obj + " but support is " + node.asLiteral().int)
                        }
                    }
                    qexec.close()
                    ++namespace2
                }
                ++synonym
            }
            if (selectedNamespaceSynonym.isEmpty()) continue
            val tmp3 = this.updatedGraph!!.find(Node.ANY, triple.predicate, Node.ANY).toSet()
            for (auxTriple in tmp3) {
                println("Validated model using the triple: " + auxTriple.toString())
                this.updatedGraph!!.delete(auxTriple)
                this.updatedGraph!!.add(
                    Triple(
                        auxTriple.subject,
                        auxTriple.predicate,
                        NodeFactory.createURI(selectedNamespaceSynonym)
                    )
                )
            }
            tripleWithSynonymToEvaluate.clear()
            return tripleWithSynonymToEvaluate
        }
        return tripleWithSynonymToEvaluate
    }

    private fun lastChanceCheckRedundancy(tripleWithSynonymToAnalyse: MutableList<Triple>): MutableList<Triple> {
        var maxSupport: Int
        var selectedSynonym: String
        val tagList: MutableList<String> = mutableListOf()
        for (triple in tripleWithSynonymToAnalyse) {
            var tmpJenaGraph = Factory.createDefaultGraph()
            var iterator = this.updatedGraph!!.find(Triple.ANY)
            while (iterator.hasNext()) {
                tmpJenaGraph.add(iterator.next())
            }
            tmpJenaGraph = GraphMinimization.minimize(tmpJenaGraph)
            if (triple.predicate.toString()[0] == '?' || triple.predicate.toString()[0] == 'a' || tagList.contains(
                    triple.predicate.toString()
                )
            ) continue
            tagList.add(triple.predicate.toString())
            val tmpInnerQuery = tmpJenaGraph.find(Node.ANY, triple.predicate, Node.ANY).toSet()
            val tripleInnerQuery = tmpInnerQuery.iterator()
            var innerQueryPar2 = ""
            var typeInnerQueryPar2 = ""
            var i = 0

            while (tripleInnerQuery.hasNext()) {
                val auxTriple = tripleInnerQuery.next()
                if (!innerQueryPar2.contains(auxTriple.subject.toString())) {
                    innerQueryPar2 = innerQueryPar2 + auxTriple.subject.toString() + " ?p ?o ."
                    if (i == 0) {
                        typeInnerQueryPar2 = typeInnerQueryPar2 + "{{" + auxTriple.subject.toString() + " a ?o }"
                        ++i
                    } else {
                        typeInnerQueryPar2 = typeInnerQueryPar2 + " UNION {" + auxTriple.subject.toString() + " a ?o }"
                        ++i
                    }
                }
                tmpJenaGraph.delete(auxTriple)
            }

            if (i > 0) {
                typeInnerQueryPar2 = "$typeInnerQueryPar2}."
            }

            var innerQueryPar1 = "SELECT DISTINCT ?p WHERE { "
            var typeInnerQueryPar1 = "SELECT DISTINCT ?o WHERE { "
            val iteInnerQuery = tmpJenaGraph.find(Triple.ANY)

            while (iteInnerQuery.hasNext()) {
                val tri = iteInnerQuery.next()
                innerQueryPar1 =
                    innerQueryPar1 + tri.subject.toString() + " " + tri.predicate + " " + tri.`object`.toString() + " . \n"
                typeInnerQueryPar1 =
                    typeInnerQueryPar1 + tri.subject.toString() + " " + tri.predicate + " " + tri.`object`.toString() + " . \n"
            }

            var synonyms = arrayOf<String>()
            try {
                val innerQuery: String =
                    innerQueryPar1 + innerQueryPar2 + "FILTER (!regex(str(?p), '^http://dbpedia.org/resource/')). " + "} "
                val typeInnerQuery: String = "$typeInnerQueryPar1$typeInnerQueryPar2}"
                println("Redundancy analysis for " + triple.predicate.localName + " and model " + innerQuery)
                // val tmp = PredicateRedundancy.applyRedundancy(triple.predicate, innerQuery, typeInnerQuery)
                // synonyms = tmp.toTypedArray()
            } catch (e2: IOException) {
                e2.printStackTrace()
            }

            maxSupport = 0
            selectedSynonym = ""
            tmpJenaGraph = Factory.createDefaultGraph()
            iterator = this.updatedGraph!!.find(Triple.ANY)
            while (iterator.hasNext()) {
                tmpJenaGraph.add(iterator.next())
            }

            tmpJenaGraph = GraphMinimization.minimize(tmpJenaGraph)
            var queryStringTripleToAnalyse: String
            var synonym = 0

            while (synonym < synonyms.size) {
                val sqs: QuerySolution
                val node: RDFNode
                queryStringTripleToAnalyse = ""
                val nomePredicate = CommonData.cleanURI(synonyms[synonym])[1]
                val prefixPredicate = CommonData.cleanURI(synonyms[synonym])[0]
                val tmp = tmpJenaGraph.find(Node.ANY, triple.predicate, Node.ANY).toSet()
                for (auxTriple in tmp) {
                    tmpJenaGraph.delete(auxTriple)
                    queryStringTripleToAnalyse =
                        queryStringTripleToAnalyse + auxTriple.subject.toString() + " " + prefixPredicate + nomePredicate + " " + auxTriple.`object`.toString() + " .\n"
                }

                var queryString = CommonData.prefix + "SELECT " + " (COUNT(*) as ?count)" + "\n" + "WHERE { \n"
                val ite = tmpJenaGraph.find(Triple.ANY)
                while (ite.hasNext()) {
                    val tri = ite.next()
                    queryString =
                        queryString + tri.subject.toString() + " " + tri.predicate + " " + tri.`object`.toString() + " . \n"
                }
                queryString += queryStringTripleToAnalyse
                queryString = "$queryString}"
                println("Query: identify all redundancy of a tag from a predicate type")

                val query2 = QueryFactory.create(queryString)
                val qExec = QueryExecutionFactory.sparqlService(FormSolver.endpoint, query2)
                var rs: ResultSet? = null
                try {
                    rs = qExec.execSelect()
                } catch (e3: Exception) {
                    System.err.println("Timeout query globale")
                }

                sqs = rs!!.next()
                node = sqs.get("count")
                if (rs.hasNext() && node.asLiteral().int > maxSupport) {
                    if (node.asLiteral().int > CommonData.modelLimit) {
                        maxSupport = node.asLiteral().int
                        selectedSynonym = prefixPredicate + nomePredicate
                    } else {
                        println("model valid for " + prefixPredicate + nomePredicate + " but support is " + node.asLiteral().int)
                    }
                }
                qExec.close()
                ++synonym
            }
            if (selectedSynonym.isEmpty()) continue
            val tmp = this.updatedGraph!!.find(Node.ANY, triple.predicate, Node.ANY).toSet()
            for (auxTriple in tmp) {
                println("Validated model using triple: " + auxTriple.toString())
                this.updatedGraph!!.delete(auxTriple)
                this.updatedGraph!!.add(
                    Triple(
                        auxTriple.subject,
                        NodeFactory.createURI(selectedSynonym),
                        auxTriple.`object`
                    )
                )
            }
            tripleWithSynonymToAnalyse.clear()
            return tripleWithSynonymToAnalyse
        }
        return tripleWithSynonymToAnalyse
    }
}
