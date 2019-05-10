package formsolver

import org.apache.jena.query.Query
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object CommonData {
    @JvmStatic
    val logger: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    var namespacesXModel = arrayOf("dbpedia-owl:")
    var namespacesXCuts = arrayOf("dbpedia-owl:", "rdfs:", "foaf:", "yago:", "skos:", "bibo:", "fb:")
    var prefix =
        "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\nPREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\nPREFIX foaf: <http://xmlns.com/foaf/0.1/>\nPREFIX yago: <http://dbpedia.org/class/yago/>\nPREFIX dbpprop: <http://dbpedia.org/property/>\nPREFIX dc: <http://purl.org/dc/elements/1.1/>\nPREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\nPREFIX geonames: <http://www.geonames.org/ontology#>\nPREFIX skos: <http://www.w3.org/2004/02/skos/core#>\nPREFIX bibo: <http://purl.org/ontology/bibo/>\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\nPREFIX fb: <http://rdf.freebase.com/ns/>\nPREFIX dcterm: <http://purl.org/dc/terms>\nPREFIX dcmitype: <http://purl.org/dc/dcmitype/>\nPREFIX mmd: <http://musicbrainz.org/ns/mmd-1.0#>\nPREFIX aws: <http://soap.amazon.com/>"
    var modelLimit =
        5 // 10 //500 If the number is too big it takes too long and the results are not that different if at all.
    var tripleRelationLimit = 1 // 1
    var redundancyLimit: Double = 0.45 // 0.45
    var proximityLimit = 1000 // 1000
    var outputQueries: MutableList<String> = mutableListOf()
    // var resultValues: MutableList<Any> = mutableListOf()
    var queryToValuesMap = mutableMapOf<Query, MutableList<MutableMap<String, String>>>()

    fun cleanURI(uri: String): List<String> {
        val cleanedUri: MutableList<String> = mutableListOf()
        if (uri.contains("http://www.w3.org/1999/02/22-rdf-syntax-ns#")) {
            cleanedUri.add("rdf:")
            cleanedUri.add(uri.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("http://dbpedia.org/ontology")) {
            cleanedUri.add("dbpedia-owl:")
            cleanedUri.add(uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size - 1])
            return cleanedUri
        }
        if (uri.contains("http://xmlns.com/foaf/0.1")) {
            cleanedUri.add("foaf:")
            cleanedUri.add(uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size - 1])
            return cleanedUri
        }
        if (uri.contains("http://dbpedia.org/class/yago")) {
            cleanedUri.add("yago:")
            cleanedUri.add(uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size - 1])
            return cleanedUri
        }
        if (uri.contains("http://dbpedia.org/property")) {
            cleanedUri.add("dbpprop:")
            cleanedUri.add(uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size - 1])
            return cleanedUri
        }
        if (uri.contains("http://purl.org/dc/elements/1.1")) {
            cleanedUri.add("dc:")
            cleanedUri.add(uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size - 1])
            return cleanedUri
        }
        if (uri.contains("http://www.w3.org/2003/01/geo/wgs84_pos#")) {
            cleanedUri.add("geo:")
            cleanedUri.add(uri.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("http://www.geonames.org/ontology#")) {
            cleanedUri.add("geonames:")
            cleanedUri.add(uri.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("http://www.w3.org/2004/02/skos/core#")) {
            cleanedUri.add("skos:")
            cleanedUri.add(uri.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("http://purl.org/ontology/bibo")) {
            cleanedUri.add("bibo:")
            cleanedUri.add(uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size - 1])
            return cleanedUri
        }
        if (uri.contains("http://www.w3.org/2000/01/rdf-schema#")) {
            cleanedUri.add("rdfs:")
            cleanedUri.add(uri.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("http://rdf.freebase.com/ns")) {
            cleanedUri.add("fb:")
            cleanedUri.add(uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size - 1])
            return cleanedUri
        }
        if (uri.contains("http://purl.org/dc/terms")) {
            cleanedUri.add("dcterm:")
            cleanedUri.add(uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size - 1])
            return cleanedUri
        }
        if (uri.contains("http://purl.org/dc/dcmitype")) {
            cleanedUri.add("dcmitype:")
            cleanedUri.add(uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size - 1])
            return cleanedUri
        }
        if (uri.contains("http://musicbrainz.org/ns/mmd-1.0#")) {
            cleanedUri.add("mmd:")
            cleanedUri.add(uri.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("http://soap.amazon.com")) {
            cleanedUri.add("aws:")
            cleanedUri.add(uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[uri.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size - 1])
            return cleanedUri
        }
        logger.debug("Name space in CommonData non trovato $uri")
        System.exit(1)
        return cleanedUri
    }

    fun buildURI(uri: String): List<String> {
        val cleanedUri: MutableList<String> = mutableListOf()
        if (uri.contains("rdf:")) {
            cleanedUri.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#")
            cleanedUri.add(uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("dbpedia-owl:")) {
            cleanedUri.add("http://dbpedia.org/ontology/")
            cleanedUri.add(uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("foaf:")) {
            cleanedUri.add("http://xmlns.com/foaf/0.1/")
            cleanedUri.add(uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("yago:")) {
            cleanedUri.add("http://dbpedia.org/class/yago/")
            cleanedUri.add(uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("dbpprop:")) {
            cleanedUri.add("http://dbpedia.org/property/")
            cleanedUri.add(uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("dc:")) {
            cleanedUri.add("http://purl.org/dc/elements/1.1/")
            cleanedUri.add(uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("geo:")) {
            cleanedUri.add("http://www.w3.org/2003/01/geo/wgs84_pos#")
            cleanedUri.add(uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("geonames:")) {
            cleanedUri.add("http://www.geonames.org/ontology#")
            cleanedUri.add(uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("skos:")) {
            cleanedUri.add("http://www.w3.org/2004/02/skos/core#")
            cleanedUri.add(uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("bibo:")) {
            cleanedUri.add("http://purl.org/ontology/bibo/")
            cleanedUri.add(uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("rdfs:")) {
            cleanedUri.add("http://www.w3.org/2000/01/rdf-schema#")
            cleanedUri.add(uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("fb:")) {
            cleanedUri.add("http://rdf.freebase.com/ns/")
            cleanedUri.add(uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("dcterm:")) {
            cleanedUri.add("http://purl.org/dc/terms/")
            cleanedUri.add(uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("dcmitype:")) {
            cleanedUri.add("http://purl.org/dc/dcmitype/")
            cleanedUri.add(uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("mmd:")) {
            cleanedUri.add("http://musicbrainz.org/ns/mmd-1.0#")
            cleanedUri.add(uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        if (uri.contains("aws:")) {
            cleanedUri.add("http://soap.amazon.com/")
            cleanedUri.add(uri.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            return cleanedUri
        }
        logger.debug("Name space in CommonData non trovato $uri")
        System.exit(1)
        return cleanedUri
    }
}
