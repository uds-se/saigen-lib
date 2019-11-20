package formsolver

import org.apache.jena.graph.Graph
import org.apache.jena.graph.NodeFactory
import org.apache.jena.graph.Triple
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.QueryParseException
import org.apache.jena.query.ResultSet
import org.apache.jena.rdf.model.RDFNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.droidmate.saigen.utils.NLP
import java.io.IOException

internal class AssociationsBuilderWikidata(
    private val graph: Graph,
    private val tags: Array<String>,
    private val namespace: Array<String>,
    private val isThreshold: Boolean
) {
    val elements: MutableList<Element> = mutableListOf()
    val elementsLv2: MutableList<Element> = mutableListOf()
    var nQuery: Int = 0
    var nAssociatedTags: Int = 0
    private var node: RDFNode? = null
    private var useSynPred: Boolean = false
    private var useSynCl: Boolean = false

    private fun <T> runQuery(queryStr: String, processResult: (ResultSet?) -> T): T {
        try {
            logger.debug("Running query: " + queryStr)
            val query = QueryFactory.create(queryStr)
            QueryExecutionFactory.sparqlService(FormSolver.wikiDataEndpoint, query).use { qExec ->
                val queryResult: ResultSet? = try {
                    ++this.nQuery
                    qExec.execSelect()
                } catch (e: Exception) {
                    logger.error("Error querying: ${e.message}", e)
                    null
                }

                return processResult.invoke(queryResult)
            }
        } catch (e: QueryParseException) {
            logger.error("Unable to run query: $e", e)
            throw e
        }
    }

    private fun classAssociations(namespace2: String, tag: String, threshold: Int): String {
        var chosenTag: String
        var chosenTagSupport = "0"
        val synonyms = NLP.getSynonyms(tag)

        var x = 0
        while (x < synonyms.size) {
            val queryTag = Utility.initialToLowerCase(synonyms[x])
            val queryString = CommonData.wikidataPrefix + "SELECT DISTINCT ?final WHERE { ?s ?label \"" + queryTag + "\"@en ." + "?item wdt:P31 ?s ." + "?item rdfs:label ?final ." + "FILTER(LANG(?final) = \"en\") } LIMIT 1"

            val quantity = runQuery(queryString) { result ->
                if (result?.hasNext() == true) {
                    val quantity = this.getQuantity(namespace2, queryTag, false)
                    val sqs = result.next()
                    val obj = sqs.get("final") // node2")
                    this.node = obj

                    logger.debug("found result! " + result)

                    quantity
                } else {
                    0
                }
            }

            if (quantity.toDouble() > chosenTagSupport.toDouble()) {
                chosenTag = queryTag
                chosenTagSupport = "$quantity.$x"
                if (chosenTag.equals(tag, ignoreCase = true) && quantity > threshold) {
                    logger.debug("There are no synonyms for the label $chosenTag")
                    this.useSynCl = false
                    break
                }
            }

            ++x
        }

        return chosenTagSupport
    }

    private fun createClassAssociation(namespace2: String, tag: String, quantity: Int) {
        var internalTag = Utility.initialToLowerCase(tag)
        // internalTag = Utility.initialLetterUpperCase(internalTag)
        val tri = Triple(
            NodeFactory.createURI("$internalTag"), // ?subject_$internalTag"),
            NodeFactory.createURI("a"),
            NodeFactory.createURI(namespace2 + internalTag)
        )
        val concept = ElementClass(namespace2, internalTag, quantity)
        this.graph.add(tri)
        this.elements.add(concept)
        ++this.nAssociatedTags
    }

    private fun getQuantity(namespace2: String, tag: String, predicate: Boolean): Int {
        // val triple = if (predicate) "?node1 $namespace2$tag ?node2 ." else "?$tag a $namespace2${tag.capitalize()}"
        // val triple = "?s ?label \"" + tag + "\"@en ." + "?item wdt:P31 ?s ." + "SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\" . }"
        val triple = "?s ?label \"" + tag + "\"@en ." + "?item wdt:P31 ?s ." + "?item rdfs:label ?final ." + "FILTER(LANG(?final) = \"en\")"

        val cachedResult = namespaceTagCache[triple]
        if (cachedResult != null) {
            return cachedResult
        }

        // val queryString = CommonData.prefix + "SELECT (COUNT(*) as ?count) " + "WHERE {" + triple + "}"
        val queryString = CommonData.wikidataPrefix + "SELECT (COUNT(*) as ?count) " + "WHERE {" + triple + "}"

        val quantity = runQuery(queryString) { result ->
            if (result?.hasNext() == true) {
                val sqs = result.next()
                val node = sqs.get("count")

                node.asLiteral().int
            } else {
                0
            }
        }

        namespaceTagCache[triple] = quantity
        logger.debug("We found $quantity for the ${if (predicate) "predicate" else "class"} association $namespace2${tag.capitalize()}")
        return quantity
    }

    fun buildAssociations(threshold: Int): Graph {
        if (!this.isThreshold) {
            var i = 0
            while (i < this.tags.size) {
                val synonym: Int
                val resultClassNamespace: MutableList<String> = mutableListOf()
                var maxClass = 0.0
                var chosenClass = ""
                var maxClassIndex = 0
                var j = 0

                while (j < this.namespace.size) {
                    this.useSynCl = true
                    resultClassNamespace.add(this.classAssociations(this.namespace[j], this.tags[i], threshold))

                    if (resultClassNamespace[j].toDouble() > maxClass) {
                        maxClass = resultClassNamespace[j].toDouble()
                        chosenClass = resultClassNamespace[j]
                        maxClassIndex = j
                    }

                    if ((j == 0 || j == 1) && maxClass >= threshold.toDouble()) break
                    ++j
                }

                if (maxClass >= threshold.toDouble()) {
                synonym =
                    Integer.parseInt(chosenClass.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
                try {
                    this.tags[i] = NLP.getSynonyms(this.tags[i]).toTypedArray()[synonym]
                } catch (e3: IOException) {
                    e3.printStackTrace()
                }

                    logger.debug("Class creation for " + this.tags[i])
                    this.createClassAssociation(this.namespace[maxClassIndex], this.tags[i], maxClass.toInt())
                }
                ++i
            }
        }
        return this.graph
    }

    companion object {
        @JvmStatic
        val logger: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

        @JvmStatic
        private val namespaceTagCache = mutableMapOf<String, Int>()
    }
}
