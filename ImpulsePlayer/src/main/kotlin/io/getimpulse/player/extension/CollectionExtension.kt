package io.getimpulse.player.extension

fun<E> Collection<E>.indexOfFirstOrNull(predicate: (E) -> Boolean): Int? {
    val index = indexOfFirst(predicate)
    return if (index == -1) {
        null
    } else {
        index
    }
}