package io.getimpulse.player.model

@Suppress("EnumEntryName")
internal enum class Speed(
    val key: Int,
    val value: Float,
) {
    x0_25(1, 0.25f),
    x0_50(2, 0.5f),
    x0_75(3, 0.75f),
    x1_00(4, 1.0f),
    x1_25(5, 1.25f),
    x1_50(6, 1.5f),
    x1_75(7, 1.75f),
    x2_00(8, 2.0f),
    ;

    companion object {
        fun all() = entries.sortedBy { it.key }
        fun from(index: Int) = entries.firstOrNull { it.key == index }
    }
}