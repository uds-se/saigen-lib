package formsolver

import org.apache.jena.graph.Factory
import org.apache.jena.graph.Graph
import org.apache.jena.graph.NodeFactory
import org.apache.jena.graph.Triple
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.ResultSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.droidmate.saigen.utils.NLP
import java.io.IOException

class RelationshipGenerator(
    val graph: Graph,
    private val elements: List<Element>,
    private val elementsLv2: List<Element>
) {
    // private val predicatiLv2: Int = 0
    var nQuery: Int = 0

    /*val numComponentiConnesse: Int
        get() {
            val analizzatore = AnalizzatoreGrafo(this.graph)
            analizzatore.splitConnectedComponents()
            analizzatore.setElements(this.elements)
            val forest = analizzatore.forest
            return forest.size
        }*/

    init {
        this.nQuery = 0
    }

    private fun connessioneClassePred(classe1: ElementClass, pred1: Predicate, grafo: Graph): Boolean {
        ++this.nQuery

        val id = classe1.namespace + classe1.name + " | " + pred1.namespace + pred1.name
        val cachedData = connessioneClassePredCache[id]

        val numerositaQuery1 = if (cachedData != null) {
            cachedData
        } else {
            val queryString1 =
                CommonData.prefix + "SELECT (COUNT(*) AS ?count) \n" + "WHERE {  \n" + "?subject_" + classe1.name + " a " + classe1.namespace + classe1.name + " . \n" + "?subject_" + classe1.name + " " + pred1.namespace + pred1.name + " ?object_" + classe1.name + "_" + pred1.name + " . \n" + "} \n"
            logger.debug("Analyzing Class-Predicate relationship-> Class: " + classe1.name + " Predicate: " + pred1.name)
            val query1 = QueryFactory.create(queryString1)
            val qExec1 = QueryExecutionFactory.sparqlService(FormSolver.endpoint, query1)
            var rs1: ResultSet? = null
            try {
                rs1 = qExec1.execSelect()
            } catch (e2: Exception) {
                logger.error("Exception", e2)
            }

            var num = 0
            if (rs1 != null && rs1.hasNext()) {
                val sqs = rs1.next()
                val node = sqs.get("count")
                num = node.asLiteral().int
            }
            qExec1.close()

            connessionePredPredGenCache[id] = num

            // attribution
            num
        }

        if (numerositaQuery1 > CommonData.tripleRelationLimit) {
            var tri = Triple(
                NodeFactory.createURI("?subject_" + classe1.name),
                NodeFactory.createURI(pred1.namespace + pred1.name),
                NodeFactory.createURI("?object_" + pred1.name)
            )
            grafo.add(tri)
            tri = Triple(
                NodeFactory.createURI("?subject_" + pred1.name),
                NodeFactory.createURI("a"),
                NodeFactory.createURI(classe1.namespace + classe1.name)
            )
            grafo.add(tri)
            return true
        }
        return false
    }

    private fun connessioneClasseClasse(classe1: ElementClass, classe2: ElementClass, grafo: Graph): Boolean {
        val tri: Triple
        ++this.nQuery

        val id1 = classe1.namespace + classe1.name + " | " + classe2.namespace + classe2.name
        val cachedData1 = connessioneClasseClasseCache1[id1]

        val numerositaQuery1 = if (cachedData1 != null) {
            cachedData1
        } else {
            val queryString1 =
                CommonData.prefix + "SELECT (COUNT(*) AS ?count) \n" + "WHERE { \n" + "?node1 a " + classe1.namespace + classe1.name + " . \n" + "?node2 a " + classe2.namespace + classe2.name + " . \n" + "?node1 ?pred ?node2 . \n" + "FILTER (?node1 != ?node2) " + "}"
            logger.debug("Analyzing Class-Class relationship-> Class: " + classe1.name + " Class: " + classe2.name)
            val query1 = QueryFactory.create(queryString1)
            val qexec1 = QueryExecutionFactory.sparqlService(FormSolver.endpoint, query1)
            var rs1: ResultSet? = null
            try {
                rs1 = qexec1.execSelect()
            } catch (e2: Exception) {
                logger.error("Exception", e2)
            }

            var num = 0
            if (rs1 != null && rs1.hasNext()) {
                val sqs = rs1.next()
                val node = sqs.get("count")
                num = node.asLiteral().int
            }
            qexec1.close()

            connessioneClasseClasseCache1[id1] = num

            // attribution
            num
        }

        val id2 = classe1.namespace + classe1.name + " | " + classe2.namespace + classe2.name
        val cachedData2 = connessioneClasseClasseCache2[id2]

        val numerositaQuery2 = if (cachedData2 != null) {
            cachedData2
        } else {
            val queryString2 =
                CommonData.prefix + "SELECT (COUNT(*) AS ?count) \n" + "WHERE { \n" + "?node1 a " + classe1.namespace + classe1.name + " . \n" + "?node2 a " + classe2.namespace + classe2.name + " . \n" + "?node2 ?pred ?node1 . \n" + "FILTER (?node1 != ?node2) " + "}"
            logger.debug("Analyzing Class-Class relationship-> Class: " + classe1.name + " Class: " + classe2.name)
            val query2 = QueryFactory.create(queryString2)
            val qexec2 = QueryExecutionFactory.sparqlService(FormSolver.endpoint, query2)
            var rs2: ResultSet? = null
            try {
                rs2 = qexec2.execSelect()
            } catch (e3: Exception) {
                e3.printStackTrace()
                logger.error("Exception", e3)
            }

            var num = 0
            if (rs2 != null && rs2.hasNext()) {
                val sqs = rs2.next()
                val node = sqs.get("count")
                num = node.asLiteral().int
            }
            qexec2.close()

            connessioneClasseClasseCache2[id2] = num
            // attribution
            num
        }

        if (numerositaQuery1 >= numerositaQuery2 && numerositaQuery1 > CommonData.tripleRelationLimit) {
            tri = Triple(
                NodeFactory.createURI("?subject_" + classe1.name),
                NodeFactory.createURI("?pred_" + classe1.name + "_" + classe2.name),
                NodeFactory.createURI("?subject_" + classe2.name)
            )
            grafo.add(tri)
            return true
        }
        if (numerositaQuery2 > numerositaQuery1 && numerositaQuery2 > 50) {
            tri = Triple(
                NodeFactory.createURI("?subject_" + classe2.name),
                NodeFactory.createURI("?pred_" + classe2.name + "_" + classe1.name),
                NodeFactory.createURI("?subject_" + classe1.name)
            )
            grafo.add(tri)
            return true
        }
        return false
    }

    private fun connessionePredPredGen(pred1: Predicate, pred2: Predicate, grafo: Graph): Boolean {
        ++this.nQuery

        val id = pred1.namespace + pred1.name + " | " + pred2.namespace + pred2.name
        val cachedData = connessionePredPredGenCache[id]

        val numerositaQuery1 = if (cachedData != null) {
            cachedData
        } else {

            // var numerositaQuery1 = 0
            val queryString1 =
                CommonData.prefix + "SELECT (COUNT(*) AS ?count) " + "WHERE { " + "?nodo " + pred1.namespace + pred1.name + " ?ogg1 . " + "?nodo " + pred2.namespace + pred2.name + " ?ogg2 . " + "FILTER (?ogg1 != ?ogg2) " + "}"
            logger.debug("Analyzing Pred-Pred relationship-> Predicate: " + pred1.name + " Predicate: " + pred2.name)
            val query1 = QueryFactory.create(queryString1)
            val qExec1 = QueryExecutionFactory.sparqlService(FormSolver.endpoint, query1)
            var rs1: ResultSet? = null
            try {
                rs1 = qExec1.execSelect()
            } catch (e2: Exception) {
                logger.error("Exception", e2)
            }

            var num = 0
            if (rs1 != null && rs1.hasNext()) {
                val sqs = rs1.next()
                val node = sqs.get("count")

                num = node.asLiteral().int
            }
            qExec1.close()

            connessionePredPredGenCache[id] = num

            // attribution command
            num
        }

        if (numerositaQuery1 > CommonData.tripleRelationLimit) {
            val tri1 = Triple(
                NodeFactory.createURI("?subject_" + pred1.name),
                NodeFactory.createURI((pred2.namespace) + pred2.name),
                NodeFactory.createURI("?object_" + pred2.name)
            )
            grafo.add(tri1)
            val tri2 = Triple(
                NodeFactory.createURI("?subject_" + pred2.name),
                NodeFactory.createURI((pred1.namespace) + pred1.name),
                NodeFactory.createURI("?object_" + pred1.name)
            )
            logger.debug("Added Triple $tri1")
            logger.debug("Added Triple $tri2")
            grafo.add(tri2)
            return true
        }
        return false
    }

    fun analyseCorrelations() {
        var i = 0
        while (i < this.elements.size) {
            var e2: Element
            var e22: Element
            var n: Int
            var synonym: Array<String>
            var j: Int
            val e1 = this.elements[i]
            var matchingE1 = false
            var useSyn = true
            var j2 = 0
            while (j2 < this.elements.size) {
                if (i != j2) {
                    val e23 = this.elements[j2]
                    if (e1 is Predicate && e23 is Predicate && e1 != e23) {
                        matchingE1 = this.connessionePredPredGen(e1, e23, this.graph)
                    }
                    if (e1 is ElementClass && e23 is ElementClass && e1 != e23) {
                        matchingE1 = this.connessioneClasseClasse(e1, e23, this.graph)
                    }
                    if (e1 is ElementClass && e23 is Predicate) {
                        matchingE1 = this.connessioneClassePred(e1, e23, this.graph)
                    }
                    if (e1 is Predicate && e23 is ElementClass) {
                        matchingE1 = this.connessioneClassePred(e23, e1, this.graph)
                    }
                    if (matchingE1) {
                        useSyn = false
                    }
                }
                ++j2
            }
            val tmpNome = e1.name
            val tmpNamespace = e1.namespace
            var maxConnections: Int
            var maxPredicate = ""
            var maxNamespace = ""
            var checkingSyn = false
            if (useSyn) {
                e1.namespace = tmpNamespace
                e1.name = tmpNome
                checkingSyn = true
                synonym = arrayOf()
                try {
                    synonym = NLP.getSynonyms(e1.name).toTypedArray()
                } catch (e3: IOException) {
                    e3.printStackTrace()
                }

                maxConnections = 0
                maxPredicate = ""
                maxNamespace = ""
                var currentConnections2: Int
                val n2 = synonym.size
                n = 0
                while (n < n2) {
                    val syn2 = synonym[n]
                    currentConnections2 = 0
                    logger.debug("Correlation rating for $tmpNome with syn $syn2")
                    if (syn2 !== e1.name) {
                        e1.name = syn2
                        var j3 = 0
                        while (j3 < this.elements.size) {
                            if (i != j3) {
                                e22 = this.elements[j3] // as reference
                                if (e1 is Predicate && e22 is Predicate && e1 != e22) {
                                    matchingE1 = this.connessionePredPredGen(e1, e22, Factory.createDefaultGraph())
                                }
                                if (e1 is ElementClass && e22 is ElementClass && e1 != e22) {
                                    matchingE1 = this.connessioneClasseClasse(e1, e22, Factory.createDefaultGraph())
                                }
                                if (e1 is ElementClass && e22 is Predicate) {
                                    matchingE1 = this.connessioneClassePred(e1, e22, Factory.createDefaultGraph())
                                }
                                if (e1 is Predicate && e22 is ElementClass) {
                                    matchingE1 = this.connessioneClassePred(e22, e1, Factory.createDefaultGraph())
                                }
                                if (matchingE1) {
                                    useSyn = false
                                    ++currentConnections2
                                }
                            }
                            ++j3
                        }
                        if (currentConnections2 > maxConnections) {
                            maxPredicate = e1.name
                            maxNamespace = e1.namespace
                            maxConnections = currentConnections2
                            logger.debug("find relations for $tmpNome with synonym ${e1.name} with $maxConnections")
                            if (maxConnections == this.elements.size - 1) break
                        }
                    }
                    ++n
                }
            }
            if (useSyn) {
                e1.namespace = tmpNamespace
                e1.name = tmpNome
                checkingSyn = true
                maxConnections = 0
                maxPredicate = ""
                maxNamespace = ""
                var currentConnections: Int
                val stringArray = CommonData.namespacesXCuts
                val syn2 = stringArray.size
                var currentConnections2 = 0
                while (currentConnections2 < syn2) {
                    val namespace2 = stringArray[currentConnections2]
                    currentConnections = 0
                    logger.debug("Correlation rating for $tmpNome with namespace $namespace2")
                    if (namespace2 !== e1.namespace) {
                        e1.namespace = namespace2
                        j = 0
                        while (j < this.elements.size) {
                            if (i != j) {
                                e2 = this.elements[j]
                                if (e1 is Predicate && e2 is Predicate && e1 != e2) {
                                    matchingE1 = this.connessionePredPredGen(e1, e2, Factory.createDefaultGraph())
                                }
                                if (e1 is ElementClass && e2 is ElementClass && e1 != e2) {
                                    matchingE1 = this.connessioneClasseClasse(e1, e2, Factory.createDefaultGraph())
                                }
                                if (e1 is ElementClass && e2 is Predicate) {
                                    matchingE1 = this.connessioneClassePred(e1, e2, Factory.createDefaultGraph())
                                }
                                if (e1 is Predicate && e2 is ElementClass) {
                                    matchingE1 = this.connessioneClassePred(e2, e1, Factory.createDefaultGraph())
                                }
                                if (matchingE1) {
                                    useSyn = false
                                    ++currentConnections
                                }
                            }
                            ++j
                        }
                        if (currentConnections > maxConnections) {
                            maxPredicate = e1.name
                            maxNamespace = e1.namespace
                            maxConnections = currentConnections
                            logger.debug("find relations for $tmpNome with namespace ${e1.namespace} with $maxConnections")
                            if (maxConnections == this.elements.size - 1) break
                        }
                    }
                    ++currentConnections2
                }
            }
            if (useSyn) {
                e1.namespace = tmpNamespace
                e1.name = tmpNome
                checkingSyn = true
                synonym = arrayOf()
                try {
                    synonym = NLP.getSynonyms(e1.name).toTypedArray()
                } catch (e4: IOException) {
                    e4.printStackTrace()
                }

                maxConnections = 0
                maxPredicate = ""
                maxNamespace = ""
                var currentConnections: Int
                j = synonym.size // e2.length
                n = 0
                var value: Int
                while (n < j) {
                    val syn = synonym[n] // e2[n]
                    if (syn !== e1.name) {
                        e1.name = syn
                        val stringArray = CommonData.namespacesXCuts
                        val n3 = stringArray.size
                        value = 0 // e22 = 0
                        while (value < n3) { // while (e22 < n3) {
                            val namespace3 = stringArray[value] // val namespace3 = arrstring[e22]
                            currentConnections = 0
                            logger.debug("Correlation rating for $tmpNome with namespace $namespace3 e syn $syn")
                            e1.namespace = namespace3
                            var j4 = 0
                            while (j4 < this.elements.size) {
                                if (i != j4) {
                                    val e24 = this.elements[j4]
                                    if (e1 is Predicate && e24 is Predicate && e1 != e24) {
                                        matchingE1 = this.connessionePredPredGen(e1, e24, Factory.createDefaultGraph())
                                    }
                                    if (e1 is ElementClass && e24 is ElementClass && e1 != e24) {
                                        matchingE1 = this.connessioneClasseClasse(e1, e24, Factory.createDefaultGraph())
                                    }
                                    if (e1 is ElementClass && e24 is Predicate) {
                                        matchingE1 = this.connessioneClassePred(e1, e24, Factory.createDefaultGraph())
                                    }
                                    if (e1 is Predicate && e24 is ElementClass) {
                                        matchingE1 = this.connessioneClassePred(e24, e1, Factory.createDefaultGraph())
                                    }
                                    if (matchingE1) {
                                        useSyn = false
                                        ++currentConnections
                                    }
                                }
                                ++j4
                            }
                            if (currentConnections > maxConnections) {
                                maxPredicate = e1.name
                                maxNamespace = e1.namespace
                                maxConnections = currentConnections
                                logger.debug("find relations for $tmpNome with namespace ${e1.namespace} and synonym ${e1.name} with $maxConnections")
                                if (maxConnections == this.elements.size - 1) break
                            }
                            value++ // e22 = e22 as Int + 1
                        }
                        if (maxConnections == this.elements.size - 1) break
                    }
                    ++n
                }
            }
            if (useSyn && e1 is Predicate) {
                e1.namespace = tmpNamespace
                e1.name = tmpNome
                checkingSyn = true
                maxConnections = 0
                maxPredicate = ""
                maxNamespace = ""
                var j5 = 0
                while (j5 < this.elements.size) {
                    if (i != j5) {
                        /****val e25 = this.elements[j5]
                        val predRid: Array<String>? = null
                        try {
                        var typeInnerQuery = ""
                        var innerQuery = ""
                        if (e25 is Predicate) {
                        innerQuery = "SELECT DISTINCT ?p WHERE { ?c_" + e25.name + " " + e25.namespace + e25.name + " ?o_" + e25.name + " ." + "?c_" + e25.name + " " + "?p ?o ." + "FILTER (!regex(str(?p), '^http://dbpedia.org/resource/')). " + "} "
                        typeInnerQuery = "SELECT DISTINCT ?o WHERE { ?c_" + e25.name + " " + e25.namespace + e25.name + " ?o_" + e25.name + " ." + "?c_" + e25.name + " " + "a ?o ." + "} "
                        } else {
                        innerQuery = "SELECT DISTINCT ?p WHERE { ?c_" + e25.name + " a " + e25.namespace + e25.name + " ." + "?c_" + e25.name + " " + "?p ?o ." + "FILTER (!regex(str(?p), '^http://dbpedia.org/resource/')).  " + "} "
                        typeInnerQuery = "SELECT DISTINCT ?o WHERE { ?c_" + e25.name + " a " + e25.namespace + e25.name + " ." + "?c_" + e25.name + " " + "a ?o ." + "} "
                        }
                        logger.debug("Redundancy analysis for " + tmpNome + " " + e25.name)
                        //val tmp = PredicateRedundancy.applyRedundancy(NodeFactory.createURI(tmpNamespace.toString() + tmpNome), innerQuery, typeInnerQuery)
                        //predRid = tmp.toTypedArray()
                        } catch (e5: IOException) {
                        e5.printStackTrace()
                        }

                        val namespace3 = predRid***/
                        /*
                        val tmp = namespace3!!.size
                        var innerQuery = 0
                        while (innerQuery < tmp) {
                            val pred = namespace3[innerQuery]
                            if (CommonData.cleanURI(pred)[0] != tmpNamespace || CommonData.cleanURI(pred)[1] != tmpNome) {
                                e1.namespace = CommonData.cleanURI(pred)[0]
                                e1.name = CommonData.cleanURI(pred)[1]
                                currentConnections = 0
                                logger.debug("Correlation rating for " + tmpNome + " with redundant " + e1.namespace + e1.name)
                                var z = 0
                                while (z < this.elements.size) {
                                    if (i != z) {
                                        e25 = this.elements[z]
                                        if (e1 is Predicate && e25 is Predicate && e1 != e25) {
                                            machingE1 = this.connessionePredPredGen(e1, e25, Factory.createDefaultGraph())
                                        }
                                        if (e1 is ElementClass && e25 is ElementClass && e1 != e25) {
                                            machingE1 = this.connessioneClasseClasse(e1 as ElementClass, e25, Factory.createDefaultGraph())
                                        }
                                        if (e1 is ElementClass && e25 is Predicate) {
                                            machingE1 = this.connessioneClassePred(e1 as ElementClass, e25, Factory.createDefaultGraph())
                                        }
                                        if (e1 is Predicate && e25 is ElementClass) {
                                            machingE1 = this.connessioneClassePred(e25, e1, Factory.createDefaultGraph())
                                        }
                                        if (machingE1) {
                                            useSyn = false
                                            ++currentConnections
                                        }
                                    }
                                    ++z
                                }
                                if (currentConnections > maxConnections) {
                                    maxPredicate = e1.name
                                    maxNamespace = e1.namespace
                                    maxConnections = currentConnections
                                    logger.debug("trovate relazioni per " + tmpNome + " con ridondanza " + e1.namespace + " conn " + maxConnections)
                                    if (maxConnections == this.elements.size - 1) break
                                }
                            }
                            ++innerQuery
                        } */
                        if (maxConnections == this.elements.size - 1) break
                    }
                    ++j5
                }
            }
            if (useSyn) {
                logger.debug("No links found")
                e1.namespace = tmpNamespace
                e1.name = tmpNome
            } else if (checkingSyn) {
                logger.debug("inserimento delle triple nel modello per maxConnection $maxNamespace$maxPredicate")
                e1.name = maxPredicate
                e1.namespace = maxNamespace
                if (e1 is Predicate) {
                    this.graph.delete(
                        Triple(
                            NodeFactory.createURI("?subject_$tmpNome"),
                            NodeFactory.createURI(tmpNamespace + tmpNome),
                            NodeFactory.createURI("?object_$tmpNome")
                        )
                    )
                    this.graph.add(
                        Triple(
                            NodeFactory.createURI("?subject_" + e1.name),
                            NodeFactory.createURI((e1.namespace) + e1.name),
                            NodeFactory.createURI("?object_" + e1.name)
                        )
                    )
                } else {
                    this.graph.delete(
                        Triple(
                            NodeFactory.createURI("?subject_$tmpNome"),
                            NodeFactory.createURI("a"),
                            NodeFactory.createURI(tmpNamespace.toString() + tmpNome)
                        )
                    )
                    this.graph.add(
                        Triple(
                            NodeFactory.createURI("?subject_" + e1.name),
                            NodeFactory.createURI("a"),
                            NodeFactory.createURI("?object_" + e1.name)
                        )
                    )
                }
                var j6 = 0
                while (j6 < this.elements.size) {
                    if (i != j6) {
                        val e26 = this.elements[j6]
                        if (e1 is Predicate && e26 is Predicate && e1 != e26) {
                            matchingE1 = this.connessionePredPredGen(e1, e26, this.graph)
                        }
                        if (e1 is ElementClass && e26 is ElementClass && e1 != e26) {
                            matchingE1 = this.connessioneClasseClasse(e1, e26, this.graph)
                        }
                        if (e1 is ElementClass && e26 is Predicate) {
                            matchingE1 = this.connessioneClassePred(e1, e26, this.graph)
                        }
                        if (e1 is Predicate && e26 is ElementClass) {
                            matchingE1 = this.connessioneClassePred(e26, e1, this.graph)
                        }
                    }
                    ++j6
                }
            }
            ++i
        }
    }

    /*fun analisiCorrelazioniLivello() {
        var i = 0
        while (i < this.elementsLv2.size) {
            var e2: Element
            val e1 = this.elementsLv2[i]
            var j = 0
            while (j < this.elements.size) {
                e2 = this.elements[j]
                if (e1 is ElementClass && e2 is ElementClass && e1 != e2) {
                    this.connessioneClasseClasseLv(e1, e2)
                }
                ++j
            }
            j = 0
            while (j < this.elementsLv2.size) {
                e2 = this.elementsLv2[j]
                if (e1 is ElementClass && e2 is ElementClass && e1 != e2) {
                    this.connessioneClasseClasseLv(e1, e2)
                }
                ++j
            }
            ++i
        }
    }
    */

    // fun connessioneClasseClasseLv(classe1: ElementClass, classe2: ElementClass) {}

    companion object {
        @JvmStatic
        val logger: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

        @JvmStatic
        val connessionePredPredGenCache = mutableMapOf<String, Int>()

        @JvmStatic
        val connessioneClassePredCache = mutableMapOf<String, Int>()

        @JvmStatic
        val connessioneClasseClasseCache1 = mutableMapOf<String, Int>()

        @JvmStatic
        val connessioneClasseClasseCache2 = mutableMapOf<String, Int>()
    }
}
