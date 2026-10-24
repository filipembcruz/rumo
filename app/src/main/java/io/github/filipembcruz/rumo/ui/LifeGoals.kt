package io.github.filipembcruz.rumo.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.roundToInt

/**
 * Things the user would rather do with their time. [hours] is the rough effort for one
 * "unit" (a whole skill when [countable] is false, one item when true). Motivational estimates.
 */
enum class LifeGoal(val id: String, val icon: ImageVector, val hours: Double, val countable: Boolean, val color: Color) {
    LANGUAGE("language", Icons.Rounded.Translate, 600.0, false, Color(0xFF6B4EFF)),
    DEGREE("degree", Icons.Rounded.School, 3200.0, false, Color(0xFF4FA8FF)),
    CODE("code", Icons.Rounded.Code, 1000.0, false, Color(0xFF22C99A)),
    MUSIC("music", Icons.Rounded.MusicNote, 300.0, false, Color(0xFFD16BFF)),
    BOOKS("books", Icons.AutoMirrored.Rounded.MenuBook, 6.0, true, Color(0xFFFFB238)),
    FITNESS("fitness", Icons.Rounded.FitnessCenter, 1.0, true, Color(0xFFFF6B6B)),
    SLEEP("sleep", Icons.Rounded.Bedtime, 8.0, true, Color(0xFF3E3A8C)),
    FAMILY("family", Icons.Rounded.Favorite, 2.0, true, Color(0xFFFF5C8A));

    /** "34%" of a skill, or "12" items. */
    fun amount(hoursSpent: Double): String = if (countable) {
        (hoursSpent / hours).roundToInt().toString()
    } else {
        val r = hoursSpent / hours
        if (r >= 1) "%.1f×".format(r) else "${(r * 100).roundToInt()}%"
    }

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id }
    }
}
