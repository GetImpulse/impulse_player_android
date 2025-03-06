package io.getimpulse.player.util.extension

fun<E> Collection<E>.indexOfFirstOrNull(predicate: (E) -> Boolean): Int? {
    val index = indexOfFirst(predicate)
    return if (index == -1) {
        null
    } else {
        index
    }
}