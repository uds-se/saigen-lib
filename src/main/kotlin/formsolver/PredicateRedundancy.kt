package formsolver

import org.apache.jena.graph.Node
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.droidmate.saigen.utils.NLP
import java.io.IOException
import java.util.Comparator
import java.util.SortedSet
import java.util.TreeSet
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Map.Entry
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.listOf
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.toTypedArray

object PredicateRedundancy {
    @JvmStatic
    val logger: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    private var endpoint = "http://dbpedia-live.openlinksw.com/sparql" // "http://ra.lta.disco.unimib.it:8890/sparql" //
    private var candidates: MutableMap<String, Double> = mutableMapOf()
    private var orderedCandidates: MutableMap<String, Double> = mutableMapOf()
    lateinit var namespaces: Array<String>
    lateinit var syns: Array<String>
    lateinit var prefix: String
    private lateinit var Types: MutableList<String>
    private lateinit var SuperTypes: MutableList<String>
    private var innerQuery: String =
        "SELECT DISTINCT ?p WHERE { ?c dbpedia-owl:isbn ?oggetto_isbn .?c dbpedia-owl:series ?oggetto_series .?c ?p ?o .FILTER (!regex(str(?p), '^http://dbpedia.org/resource/')).  } "

    @Throws(IOException::class)
    fun applyRedundancy(predicate: Node, innerQuery: String, typeQuery: String): List<String> {
        namespaces = CommonData.namespacesXCuts
        prefix = CommonData.prefix
        try {
            val tmp = NLP.getSynonyms(predicate.localName)
            syns = tmp.toTypedArray()
        } catch (e2: IOException) {
            e2.printStackTrace()
        }

        // PredicateRedundancy.innerQuery = innerQuery
        var rendundantPredicate: MutableList<String> = mutableListOf()
        var syn = 0
        while (syn < syns.size) {
            var namespace2 = 0
            while (namespace2 < namespaces.size) {
                val resource = namespaces[namespace2] + syns[syn]
                val types = getOrderedTypes(typeQuery)
                var tmpRendundantPredicate: List<String> = listOf()
                rendundantPredicate = mutableListOf()
                val noType: MutableList<String> = mutableListOf()
                var type = 0
                while (type < types.size) {
                    var found = true
                    val currentTypes = types[type]
                    rendundantPredicate = mutableListOf()
                    var t = 0
                    while (t < currentTypes.size) {
                        val currentType = currentTypes[t]
                        if (Types.indexOf(currentType) > -1 && noType.contains(SuperTypes[Types.indexOf(currentType)])) {
                            logger.debug("Hello")
                            logger.debug("NoType candidates for $resource")
                        } else {
                            candidates.clear()
                            orderedCandidates.clear()
                            val resources = arrayOf(resource)
                            logger.debug("Search for redundancies for" + predicate.uri + " con " + namespaces[namespace2] + syns[syn] + " di tipo " + currentType)
                            findCD(resources, currentType)
                            if (candidates.isEmpty()) {
                                logger.debug("No candidates for $resource")
                                noType.add(currentType)
                            } else {
                                orderedCandidates.putAll(candidates)
                                val tmp = sortByValuesASC(orderedCandidates)
                                var distance = tmp.first().value
                                var tmpResource: String
                                val iterator = tmp.iterator()
                                var entryIter: Entry<String, Double>? = null
                                var i = 0
                                while (iterator.hasNext() && rendundantPredicate.size < 6) {
                                    ++i
                                    entryIter = iterator.next()
                                    tmpResource = entryIter!!.key
                                    distance = entryIter.value
                                    found = false
                                    logger.debug("risorsa pi\ufffd somigliante " + tmpResource + " distance is " + orderedCandidates[tmpResource])
                                    if (orderedCandidates[tmpResource]!! <= CommonData.redundancyLimit || rendundantPredicate.contains(
                                            tmpResource
                                        )
                                    ) continue
                                    rendundantPredicate.add(tmpResource)
                                }
                                if (rendundantPredicate.size > 0)
                                    break
                            }
                        }
                        ++t
                    }
                    if (found) {
                        return tmpRendundantPredicate
                    }
                    if (rendundantPredicate.size > 0) {
                        tmpRendundantPredicate = rendundantPredicate
                    }
                    ++type
                }
                ++namespace2
            }
            ++syn
        }
        val originalPredicate = CommonData.buildURI(predicate.uri)[0].toString() + CommonData.buildURI(predicate.uri)[1]
        rendundantPredicate.add(originalPredicate)
        return rendundantPredicate
    }

    private fun findCD(resources: Array<String>, currentType: String) {
        var queryTuples = ""
        var queryFilter = "(?p != <http://dbpedia.org/ontology/wikiPageWikiLink>) && \n"
        var queryParams = ""
        var resource = 0
        while (resource < resources.size) {
            queryTuples =
                queryTuples + "?s " + resources[resource] + " ?o . \n" + "?s ?p ?o . \n" + "?s a <" + currentType + ">. \n"
            queryParams = "$queryParams?p (COUNT(?s) as ?count) "
            queryFilter =
                if (resource == resources.size - 1) queryFilter + "(?p != " + resources[resource] + ")\n" else queryFilter + "(?p != " + resources[resource] + " ) && \n"
            ++resource
        }
        var sparqlQueryString1 =
            prefix + "SELECT DISTINCT " + queryParams + "WHERE {" + queryTuples + "{ " + innerQuery + "} " + "FILTER(" + queryFilter + "). " + "} " + "GROUP BY ?p " + "ORDER BY ASC (?count) "
        var query2 = QueryFactory.create(sparqlQueryString1)
        var qexec = QueryExecutionFactory.sparqlService(endpoint, query2)
        var results = qexec.execSelect()
        val predicateCounts = HashMap<String, Int>()
        while (results.hasNext()) {
            val queryResult = results.next()
            val count = queryResult.get("count").asLiteral().int
            val link = queryResult.getResource("p").toString()
            predicateCounts[link] = count
        }
        qexec.close()
        if (predicateCounts.size > 0) {
            var resource2 = 0
            while (resource2 < resources.size) {
                queryTuples = "?s " + resources[resource2] + " ?o . \n ?s a <" + currentType + ">. \n"
                queryParams = "(COUNT(?s) as ?count) "
                sparqlQueryString1 = prefix + "SELECT DISTINCT " + queryParams + "WHERE {" + queryTuples + "} "
                query2 = QueryFactory.create(sparqlQueryString1)
                qexec = QueryExecutionFactory.sparqlService(endpoint, query2)
                results = qexec.execSelect()
                var count = 0.0
                if (results.hasNext()) {
                    val queryResult = results.next()
                    count = queryResult.get("count").asLiteral().int.toDouble()
                }
                qexec.close()
                for ((key, value) in predicateCounts) {
                    if (count != 0.0) {
                        candidates[key] = value.toDouble() / count
                        continue
                    }
                    candidates[key] = count
                }
                ++resource2
            }
        }
    }

    internal fun sortByValuesASC(map: Map<String, Double>): SortedSet<Entry<String, Double>> {
        val sortedSet = TreeSet<Entry<String, Double>>(Comparator<Entry<String, Double>> { e1, e2 ->
            if (e1.value <= e2.value) {
                1
            } else -1
        })
        sortedSet.addAll(map.entries)
        return sortedSet
    }

    internal fun sortByValuesDESC(map: Map<String, Double>): SortedSet<Entry<String, Double>> {
        val sortedSet = TreeSet<Entry<String, Double>>(Comparator<Entry<String, Double>> { e1, e2 ->
            if (e1.value <= e2.value) {
                -1
            } else 1
        })
        sortedSet.addAll(map.entries)
        return sortedSet
    }

    private fun getOrderedTypes(typeQuery: String): List<List<String>> {
        var query2 = QueryFactory.create(prefix + typeQuery)
        var qexec = QueryExecutionFactory.sparqlService(endpoint, query2)
        var results = qexec.execSelect()
        val modelTypes: MutableList<String> = mutableListOf()
        while (results.hasNext()) {
            val queryResult = results.next()
            modelTypes.add(queryResult.getResource("o").toString())
        }
        qexec.close()
        val sparqlQueryString =
            prefix + "SELECT DISTINCT ?o ?s " + "WHERE {" + "?o rdfs:subClassOf ?s  ." + "{ " + typeQuery + "} " + "} "
        query2 = QueryFactory.create(sparqlQueryString)
        qexec = QueryExecutionFactory.sparqlService(endpoint, query2)
        results = qexec.execSelect()
        val types: MutableList<String> = mutableListOf()
        val superTypes: MutableList<String> = mutableListOf()
        while (results.hasNext()) {
            val queryResult = results.next()
            types.add(queryResult.getResource("o").toString())
            superTypes.add(queryResult.getResource("s").toString())
        }
        Types = types
        SuperTypes = superTypes
        qexec.close()
        val unorderedTypes = HashMap<String, Double>()
        var i = 0
        while (i < types.size) {
            var count = 1
            var end = true
            val type = types[i]
            var superType = superTypes[i]
            while (end) {
                if (types.contains(superType)) {
                    ++count
                    superType = superTypes[types.indexOf(superType)]
                    continue
                }
                end = false
            }
            if (unorderedTypes.containsKey(type)) {
                if (unorderedTypes[type]!! < count) {
                    unorderedTypes[type] = count.toDouble()
                }
            } else {
                unorderedTypes[type] = count.toDouble()
            }
            ++i
        }
        i = 0
        while (i < superTypes.size) {
            val type = superTypes[i]
            if (type != "http://www.w3.org/2002/07/owl#Thing" && !types.contains(type) && modelTypes.contains(type)) {
                unorderedTypes[type] = 0.0
            }
            ++i
        }
        unorderedTypes["http://www.w3.org/2002/07/owl#Thing"] = -1.0
        val orderedTypeList: MutableList<String> = mutableListOf()
        val orderedTypes = sortByValuesDESC(unorderedTypes)
        val iter = orderedTypes.iterator()
        var i2 = -2
        var List = -1
        val typeLists: MutableList<MutableList<String>> = mutableListOf()
        while (iter.hasNext()) {
            val tmp: MutableList<String>
            val entry = iter.next()
            if (entry.value.toInt() == i2) {
                tmp = typeLists[List]
                tmp.add(entry.key)
                typeLists.removeAt(List)
                typeLists.add(List, tmp)
            } else {
                i2 = entry.value.toInt()
                tmp = mutableListOf()
                tmp.add(entry.key)
                typeLists.add(tmp)
                ++List
            }
            logger.debug(" " + entry.value + " " + entry.key)
        }
        return typeLists
    }
}
