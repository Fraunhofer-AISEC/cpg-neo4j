package de.fraunhofer.aisec.cpg.ptn4j

class Pair<T, U>(private val t: T, private val u: U) {
    fun getT(): T {
        return t
    }

    fun getU(): U {
        return u
    }
}