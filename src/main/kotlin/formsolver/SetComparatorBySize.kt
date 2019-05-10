/*
 * Decompiled with CFR 0_123.
 */
package formsolver

class SetComparatorBySize<E> : Comparator<E> {
    override fun compare(set1: E, set2: E): Int {
        if ((set1 as Set<*>).size < (set2 as Set<*>).size) {
            return -1
        }
        return if (set1.size > set2.size) {
            1
        } else 0
    }
}
