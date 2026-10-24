package io.github.filipembcruz.rumo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.filipembcruz.rumo.data.Achievement
import io.github.filipembcruz.rumo.data.ActivityLog
import io.github.filipembcruz.rumo.data.AppUse
import io.github.filipembcruz.rumo.data.Cat
import io.github.filipembcruz.rumo.data.CatUse
import io.github.filipembcruz.rumo.data.DetailModel
import io.github.filipembcruz.rumo.data.Goal
import io.github.filipembcruz.rumo.data.GoalProgress
import io.github.filipembcruz.rumo.data.GrowthModel
import io.github.filipembcruz.rumo.data.Insight
import io.github.filipembcruz.rumo.data.InsightKind
import io.github.filipembcruz.rumo.data.Limit
import io.github.filipembcruz.rumo.data.LimitStatus
import io.github.filipembcruz.rumo.data.PeriodModel
import io.github.filipembcruz.rumo.data.Range
import io.github.filipembcruz.rumo.data.Schedule
import io.github.filipembcruz.rumo.data.SiteUse
import io.github.filipembcruz.rumo.data.TargetKind
import io.github.filipembcruz.rumo.data.TodayModel
import io.github.filipembcruz.rumo.service.BlockReason
import io.github.filipembcruz.rumo.ui.InterventionScreen
import io.github.filipembcruz.rumo.ui.LifeGoal
import io.github.filipembcruz.rumo.ui.LocalLocale
import io.github.filipembcruz.rumo.ui.LocalStrings
import io.github.filipembcruz.rumo.ui.Strings
import io.github.filipembcruz.rumo.ui.UiFlags
import io.github.filipembcruz.rumo.ui.screens.DetailScreen
import io.github.filipembcruz.rumo.ui.screens.FocusActions
import io.github.filipembcruz.rumo.ui.screens.FocusModel
import io.github.filipembcruz.rumo.ui.screens.FocusScreen
import io.github.filipembcruz.rumo.ui.screens.GrowthScreen
import io.github.filipembcruz.rumo.ui.screens.HomeActions
import io.github.filipembcruz.rumo.ui.screens.HomeScreen
import io.github.filipembcruz.rumo.ui.screens.InsightsScreen
import io.github.filipembcruz.rumo.ui.screens.OnboardingScreen
import io.github.filipembcruz.rumo.ui.theme.R2
import io.github.filipembcruz.rumo.ui.theme.RumoTheme
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.util.Locale

private const val M = 60_000L
private const val H = 3_600_000L

object Sample {
    val apps = listOf(
        AppUse("com.instagram.android", 92 * M, 34, Cat.SOCIAL),
        AppUse("com.brave.browser", 71 * M, 18, Cat.BROWSER),
        AppUse("com.google.android.youtube", 48 * M, 9, Cat.VIDEO),
        AppUse("com.whatsapp", 37 * M, 41, Cat.COMMUNICATION),
        AppUse("com.duolingo", 21 * M, 3, Cat.EDUCATION),
        AppUse("com.spotify.music", 12 * M, 4, Cat.MUSIC),
        AppUse("notion.id", 9 * M, 5, Cat.PRODUCTIVITY),
    )
    val sites = listOf(
        SiteUse("youtube.com", 28 * M, 6, Cat.VIDEO),
        SiteUse("reddit.com", 19 * M, 11, Cat.SOCIAL),
        SiteUse("github.com", 12 * M, 4, Cat.PRODUCTIVITY),
        SiteUse("g1.globo.com", 7 * M, 3, Cat.NEWS),
        SiteUse("wikipedia.org", 5 * M, 2, Cat.EDUCATION),
    )
    val cats = listOf(
        CatUse(Cat.SOCIAL, 111 * M), CatUse(Cat.VIDEO, 76 * M), CatUse(Cat.COMMUNICATION, 37 * M),
        CatUse(Cat.EDUCATION, 26 * M), CatUse(Cat.PRODUCTIVITY, 21 * M), CatUse(Cat.MUSIC, 12 * M), CatUse(Cat.NEWS, 7 * M),
    )
    val hourly = listOf(0L, 0, 0, 0, 0, 0, 2, 9, 14, 11, 6, 8, 19, 14, 7, 9, 12, 15, 21, 33, 41, 29, 18, 3).map { it * M }
    val limits = listOf(
        LimitStatus(Limit(1, TargetKind.APP, "com.instagram.android", 90, true), 92 * M),
        LimitStatus(Limit(2, TargetKind.CATEGORY, "video", 120, false), 76 * M),
        LimitStatus(Limit(3, TargetKind.SITE, "reddit.com", 30, true), 19 * M),
    )
    val today = TodayModel(
        total = 290 * M, goalMs = 240 * M, yesterday = 251 * M, avg7 = 263 * M, pickups = 87, firstUse = null,
        productiveMs = 47 * M, distractingMs = 187 * M, focusScore = 58, hourly = hourly, apps = apps, sites = sites, cats = cats,
        focusUntil = 0, streak = 3, sitesEnabled = true,
        insight = Insight(InsightKind.TOP_DISTRACTING, "com.instagram.android", 92 * M), limits = limits,
    )
    val heat = (0 until 7).map { d -> (0 until 24).map { h -> if (h < 7) 0f else ((h * 7 + d * 13) % 23 + (if (h in 19..22) 18 else 0)).toFloat() } }
    val period = PeriodModel(
        range = Range.WEEK, from = LocalDate.of(2026, 10, 19), to = LocalDate.of(2026, 10, 25),
        bars = listOf(251L, 302, 198, 276, 344, 290, 0).map { it * M },
        barLabels = listOf("M", "T", "W", "T", "F", "S", "S"), currentIndex = 5,
        total = 1661 * M, avg = 277 * M, prevTotal = 1820 * M, goalMs = 240 * M, apps = apps, sites = sites, cats = cats,
        heat = heat, hourly = hourly.map { it * 6 }, weekdayAvg = listOf(251L, 302, 198, 276, 344, 290, 210).map { it * M },
        pickups = 512, productiveMs = 4 * H, distractingMs = 15 * H, days = 6, longestDay = LocalDate.of(2026, 10, 23), longestMs = 344 * M,
    )
    val growth = GrowthModel(
        streak = 3, bestStreak = 9,
        goals = listOf(
            GoalProgress(Goal(1, "Learn Spanish", "language", 600.0, 0), 86.5, 4.2),
            GoalProgress(Goal(2, "Read 12 books", "books", 72.0, 0), 31.0, 2.0),
        ),
        recent = listOf(
            ActivityLog(1, "2026-10-24", 1, "Spanish lesson", 45, 0),
            ActivityLog(2, "2026-10-23", 2, "Read: Atomic Habits", 30, 0),
        ),
        weekProductive = listOf(62L, 45, 80, 30, 95, 47, 0).map { it * M },
        weekDistracting = listOf(150L, 190, 120, 170, 210, 187, 0).map { it * M },
        weekLabels = listOf("S", "M", "T", "W", "T", "F", "S"),
        yearDistractingMs = 812 * H, projectedYearMs = 1040 * H, productiveWeekMs = 359 * M, focusCount = 6,
        achievements = listOf(
            Achievement("first_focus", true, 1f), Achievement("under_goal", true, 1f), Achievement("streak_3", true, 1f),
            Achievement("streak_7", false, 0.43f), Achievement("focus_10", false, 0.6f), Achievement("productive_10h", false, 0.8f),
        ),
    )
    val focus = FocusModel(
        until = 0, start = 0, cats = setOf("social", "video", "games"), apps = setOf("com.instagram.android"), strict = false,
        schedules = listOf(Schedule(1, "Bedtime", 22 * 60 + 30, 7 * 60, 0b1111111, setOf("social", "video"), emptySet(), true, "moon")),
        limits = limits, pauseApps = setOf("com.instagram.android", "com.zhiliaoapp.musically"),
        goals = listOf(Goal(1, "Learn Spanish", "language", 600.0, 0)), accessibility = true,
    )
    val detail = DetailModel(
        kind = TargetKind.APP, id = "com.instagram.android", cat = Cat.SOCIAL, today = 92 * M, avgDaily = 78 * M, total30 = 2340 * M,
        opensToday = 34, opens30 = 812,
        daily = (0 until 30).map { ((it * 37) % 60 + 50).toLong() * M }, dayLabels = (0 until 30).map { if (it % 7 == 0) "${it + 1}" else "" },
        hourly = hourly, limit = Limit(1, TargetKind.APP, "com.instagram.android", 90, true), pause = true, weekTotal = 540 * M, prevWeekTotal = 610 * M,
    )
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h1900dp-xhdpi")
class ScreenshotTest {
    @Before
    fun setup() {
        UiFlags.animations = false
    }

    private fun shot(name: String, dark: Boolean = false, pt: Boolean = false, content: @Composable () -> Unit) {
        captureRoboImage("build/screens/$name.png") {
            CompositionLocalProvider(LocalStrings provides Strings(pt), LocalLocale provides if (pt) Locale("pt", "BR") else Locale.ENGLISH) {
                RumoTheme(if (dark) "dark" else "light") {
                    Box(Modifier.fillMaxSize().background(R2.colors.bg)) { content() }
                }
            }
        }
    }

    @Test fun home() = shot("01_home") { HomeScreen(Sample.today, "Filipe", HomeActions()) }
    @Test fun homeDarkPt() = shot("02_home_dark_pt", dark = true, pt = true) { HomeScreen(Sample.today, "Filipe", HomeActions()) }
    @Test fun insights() = shot("03_insights") {
        InsightsScreen(Sample.period, Range.WEEK, listOf(LifeGoal.LANGUAGE, LifeGoal.BOOKS, LifeGoal.FITNESS), true, {}, {}, {}, {}, {}, {})
    }
    @Test fun focus() = shot("04_focus") { FocusScreen(Sample.focus, FocusActions()) }
    @Test fun growth() = shot("05_growth") {
        GrowthScreen(Sample.growth, listOf(LifeGoal.LANGUAGE, LifeGoal.BOOKS, LifeGoal.FITNESS), {}, {}, {})
    }
    @Test fun detail() = shot("06_detail") { DetailScreen(Sample.detail, "Instagram", true, {}, {}, {}, {}, {}) }
    @Test fun onboardingWelcome() = shot("07_onboarding_welcome") {
        OnboardingScreen(0, true, "en", "", emptySet(), 180, false, false, false, 0, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
    @Test fun onboardingGoals() = shot("08_onboarding_goals", pt = true) {
        OnboardingScreen(1, true, "pt", "Filipe", setOf("language", "books"), 180, false, false, false, 0, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
    @Test fun onboardingReality() = shot("09_onboarding_reality") {
        OnboardingScreen(4, true, "en", "Filipe", setOf("language", "books", "fitness"), 180, true, false, false, 263 * M, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
    @Test fun intervention() = shot("10_block", dark = true) {
        InterventionScreen(BlockReason.LIMIT, TargetKind.APP, "com.instagram.android", "com.instagram.android", 92 * M, 90, "", 34,
            "Learn Spanish", false, 8, 0, {}, {}, {}, true, {})
    }
    @Test fun focusDark() = shot("11_focus_dark", dark = true) { FocusScreen(Sample.focus, FocusActions()) }
}
