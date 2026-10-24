package io.github.filipembcruz.rumo.data

import kotlin.math.roundToInt

/**
 * What the same hours could have become. All values are rough, motivational estimates.
 * [unitHours] is how many hours one unit of the goal takes.
 */
data class Equivalence(val title: String, val unitHours: Double, val asPercent: Boolean, val unit: String)

object Equivalences {
    val all = listOf(
        Equivalence("Um novo idioma até o nível B2", 600.0, true, "do caminho"),
        Equivalence("Uma graduação completa", 3200.0, true, "do curso"),
        Equivalence("Livros lidos", 6.0, false, "livros"),
        Equivalence("Cursos online de 40 h", 40.0, false, "cursos"),
        Equivalence("Treinos de 1 hora", 1.0, false, "treinos"),
    )

    fun describe(e: Equivalence, hours: Double): String {
        val ratio = hours / e.unitHours
        return if (e.asPercent) {
            if (ratio >= 1) "${"%.1f".format(ratio)}× completo" else "${(ratio * 100).roundToInt()}% ${e.unit}"
        } else {
            "${ratio.roundToInt()} ${e.unit}"
        }
    }
}
