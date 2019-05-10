package formsolver

import org.apache.jena.graph.Factory
import org.apache.jena.graph.Graph
import org.apache.jena.graph.Node
import org.apache.jena.graph.NodeFactory
import org.apache.jena.graph.Triple
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object GraphMinimization {

    @JvmStatic
    val logger: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    fun minimize(graph: Graph): Graph {
        var set: MutableSet<String> = mutableSetOf()
        // //val undirectedGraph = GraphMinimization.graphToUndirectedGraph(graph)
        val collection: MutableList<Set<String>> = mutableListOf()
        val objects: MutableList<String> = mutableListOf()
        var ite = graph.find(Triple.ANY)
        while (ite.hasNext()) {
            val tri = ite.next()
            val `object` = tri.`object`.uri
            if (objects.contains(`object`)) continue
            objects.add(`object`)
            set.clear()
            val ite1 = graph.find(Node.ANY, Node.ANY, NodeFactory.createURI(`object`))
            while (ite1.hasNext()) {
                val triple = ite1.next()
                val subject = triple.subject.uri
                set.add(subject.toString())
            }
            if (set.isEmpty()) continue
            collection.add(set)
        }
        val p = GraphMinimization.generateMHS(collection)
        val ite2 = p.iterator()
        if (ite2.hasNext()) {
            set = ite2.next().toMutableSet()
            for (subject in set) {
                if (objects.contains(subject)) continue
                objects.add(subject)
            }
        }
        val newGraph = Factory.createDefaultGraph()
        var i = 0
        while (i < objects.size) {
            var j = 0
            while (j < objects.size) {
                ite = graph.find(NodeFactory.createURI(objects[i]), Node.ANY, NodeFactory.createURI(objects[j]))
                while (ite.hasNext()) {
                    newGraph.add(ite.next())
                }
                ++j
            }
            ++i
        }
        return newGraph
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val list: MutableList<Set<String>> = mutableListOf()
        val s1 = listOf("a", "b")
        val s2 = listOf("a", "b")
        val s3 = listOf("c", "d")
        val set1 = s1.toSet()
        val set2 = s2.toSet()
        val set3 = s3.toSet()
        list.add(set1)
        list.add(set2)
        list.add(set3)
        val p = GraphMinimization.generateMHS(list)
        for (set in p) {
            logger.debug("Set Dimension: " + set.size)
            val ite1 = set.iterator()
            while (ite1.hasNext()) {
                logger.debug("Element : " + ite1.next())
            }
        }
    }

    /*private fun graphToUndirectedGraph(graph: Graph): SimpleGraph<String, DefaultEdge> {
        val undirectedGraph = SimpleGraph<String, DefaultEdge>(DefaultEdge::class.java)
        val ite = graph.find(Triple.ANY)
        while (ite.hasNext()) {
            val tri = ite.next()
            if (!undirectedGraph.containsVertex(tri.subject.uri)) {
                undirectedGraph.addVertex(tri.subject.uri)
            }
            if (!undirectedGraph.containsVertex(tri.`object`.uri)) {
                undirectedGraph.addVertex(tri.`object`.uri)
            }
            undirectedGraph.addEdge(tri.subject.uri, tri.`object`.uri)
        }
        return undirectedGraph
    }
*/
    private fun <T> generateMHS(_collection: List<Set<T>>): Set<Set<T>> {
        val setOfMHS: MutableSet<Set<T>> = mutableSetOf()
        val singletons: MutableSet<T> = mutableSetOf()
        val singletonHS: MutableSet<T> = mutableSetOf()
        val n = _collection.size
        val itCollection = _collection.iterator()
        while (itCollection.hasNext()) {
            singletons.addAll(itCollection.next())
        }
        val m = singletons.size
        val listColumns = GraphMinimization.setToList(singletons)
        var matrix = GraphMinimization.generatePresenceMatrix(_collection, listColumns)
        val tmp: MutableList<T> = mutableListOf()
        var j = 0
        while (j < m) {
            var ok = true
            var i = 0
            while (i < n) {
                if (!matrix[i][j]) {
                    ok = false
                }
                ++i
            }
            if (ok) {
                singletonHS.add(listColumns[j])
                tmp.add(listColumns[j])
            }
            ++j
        }
        var i = 0
        while (i < tmp.size) {
            if (listColumns.contains(tmp[i])) {
                listColumns.remove(tmp[i])
            }
            ++i
        }

        matrix = GraphMinimization.generatePresenceMatrix(_collection, listColumns)
        val powerSetOfSets = GraphMinimization.powerSet(GraphMinimization.listToSet(listColumns))
        powerSetOfSets.remove(emptySet())
        var h = 0
        while (h < listColumns.size) {
            val tempSingletonSet: MutableSet<T> = mutableSetOf()
            tempSingletonSet.add(listColumns[h])
            powerSetOfSets.remove(tempSingletonSet)
            ++h
        }

        val listPowerSet = GraphMinimization.sortSetsBySize(powerSetOfSets)
        val cardinalityPowerSet = listPowerSet.size
        var k = 0
        while (k < cardinalityPowerSet) {
            val s_i = listPowerSet[k]
            if (s_i.size <= n) {
                var isHS = true
                var r = 0
                while (r < _collection.size && isHS) {
                    var covered = false
                    var c = 0
                    while (c < listColumns.size && !covered) {
                        if (s_i.contains(listColumns[c]) && matrix[r][c]) {
                            covered = true
                        }
                        ++c
                    }
                    if (!covered) {
                        isHS = false
                    }
                    ++r
                }
                if (isHS) {
                    var included = false
                    val itSetOfMHS = setOfMHS.iterator()
                    var h2: Set<T>
                    while (itSetOfMHS.hasNext() && !included) {
                        h2 = itSetOfMHS.next()
                        if (!s_i.containsAll(h2) || s_i == h2) continue
                        included = true
                    }
                    if (!included) {
                        setOfMHS.add(s_i)
                    }
                }
            }
            ++k
        }
        val itSingletonHS = singletonHS.iterator()
        while (itSingletonHS.hasNext()) {
            val tempAddingSingletonHS: MutableSet<T> = mutableSetOf()
            tempAddingSingletonHS.add(itSingletonHS.next())
            setOfMHS.add(tempAddingSingletonHS)
        }
        return setOfMHS
    }

    private fun <T> setToList(set: Set<T>): MutableList<T> {
        val list: MutableList<T> = mutableListOf()
        val itTemp = set.iterator()
        while (itTemp.hasNext()) {
            list.add(itTemp.next())
        }
        return list
    }

    private fun <T> listToSet(List: List<T>): Set<T> {
        val set: MutableSet<T> = mutableSetOf()
        val itTemp = List.iterator()
        while (itTemp.hasNext()) {
            set.add(itTemp.next())
        }
        return set
    }

    private fun <T> generatePresenceMatrix(ListRows: List<Set<T>>, ListColumns: List<T>): Array<BooleanArray> {
        val n = ListRows.size
        val m = ListColumns.size
        val matrix = Array(n) { BooleanArray(m) }
        var i = 0
        while (i < n) {
            var j = 0
            while (j < m) {
                matrix[i][j] = ListRows[i].contains(ListColumns[j])
                ++j
            }
            ++i
        }
        return matrix
    }

    private fun <T> powerSet(originalSet: Set<T>): MutableSet<Set<T>> {
        val sets: MutableSet<Set<T>> = mutableSetOf()
        if (originalSet.isEmpty()) {
            sets.add(emptySet())
            return sets
        }
        val list = ArrayList(originalSet)
        val head = list[0]
        val rest = list.subList(1, list.size).toSet()
        for (set in GraphMinimization.powerSet(rest)) {
            val newSet: MutableSet<T> = mutableSetOf()
            newSet.add(head)
            newSet.addAll(set)
            sets.add(newSet)
            sets.add(set)
        }
        return sets
    }

    /*private fun <T> sortSetsBySize(setOfSets: Set<Set<T>>): Array<Set<T>> {
        var i = 0
        val arrayS = arrayOfNulls<Set<*>>(setOfSets.size)
        run {
            val arrayS: Array<Set<*>>
            while (i > 0);
        }
        setOfSets
        run { ++i }
        Arrays.sort<Set>(arrayS, SetComparatorBySize())
        return arrayS
    }*/

    private fun <T> sortSetsBySize(setOfSets: Set<Set<T>>): List<Set<T>> {
        return setOfSets.toSortedSet(SetComparatorBySize()).toList()
    }
}
