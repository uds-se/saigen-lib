package org.droidmate.saigen.utils

fun <A, B> Pair<MutableList<A>, MutableList<B>>.add(other: Pair<List<A>, List<B>>?) {
    if (other == null) {
        return
    }

    this.first.addAll(other.first)
    this.second.addAll(other.second)
}

fun <A, B> Pair<List<A>, List<B>>.hasValue(): Boolean = this.first.isNotEmpty()