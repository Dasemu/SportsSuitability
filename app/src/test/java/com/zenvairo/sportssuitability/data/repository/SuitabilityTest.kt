package com.zenvairo.sportssuitability.data.repository

import com.zenvairo.sportssuitability.data.local.Spot
import com.zenvairo.sportssuitability.data.local.SportType
import com.zenvairo.sportssuitability.data.prefs.AppSettings
import com.zenvairo.sportssuitability.data.prefs.Limit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuitabilityTest {

    private val settings = AppSettings()

    private fun skiSpot() = Spot(
        id = 1, name = "Estación", region = null,
        latitude = 42.7, longitude = -0.4, sportType = SportType.SKI, altitudeM = 1800.0
    )

    private fun surfSpot(facing: Double? = null) = Spot(
        id = 2, name = "Playa", region = null,
        latitude = 43.3, longitude = -2.2, sportType = SportType.SURF, coastFacingDeg = facing
    )

    /** A day that satisfies every ski rule, so individual limits can be tested in isolation. */
    private fun goodSkiDay() = RuleInput(
        windKmh = 15.0,
        gustKmh = 30.0,
        airTempC = -5.0,
        rainMm = 0.0,
        snowfallCm = 12.0,
        snowDepthCm = 90.0,
        visibilityM = 20_000.0,
        freezingLevelM = 1200.0,
        cloudCoverPct = 10.0
    )

    // -- Hard limits -------------------------------------------------------

    @Test
    fun `ski wind above the configured hard limit blocks the day`() {
        val verdict = evaluate(SportType.SKI, goodSkiDay().copy(windKmh = 82.0), settings)

        assertEquals(Suitability.NO_GO, verdict.suitability)
        assertFalse(verdict.canGo)
        assertTrue(verdict.blockers.any { it.contains("82") })
    }

    @Test
    fun `ski wind just below the limit does not block`() {
        val verdict = evaluate(SportType.SKI, goodSkiDay().copy(windKmh = 69.0), settings)

        assertTrue(verdict.blockers.isEmpty())
        assertTrue(verdict.suitability != Suitability.NO_GO)
    }

    @Test
    fun `raising the wind limit lets a previously blocked day through`() {
        val windy = goodSkiDay().copy(windKmh = 82.0)
        val relaxed = settings.copy(
            ski = settings.ski.copy(windHardMaxKmh = Limit(enabled = true, value = 90.0))
        )

        assertEquals(Suitability.NO_GO, evaluate(SportType.SKI, windy, settings).suitability)
        assertTrue(evaluate(SportType.SKI, windy, relaxed).blockers.isEmpty())
    }

    @Test
    fun `disabling a hard limit turns it into a scoring factor only`() {
        val windy = goodSkiDay().copy(windKmh = 82.0)
        val disabled = settings.copy(
            ski = settings.ski.copy(windHardMaxKmh = Limit(enabled = false, value = 70.0))
        )

        val verdict = evaluate(SportType.SKI, windy, disabled)
        assertTrue(verdict.blockers.isEmpty())
        // The wind still drags the score down even though it no longer vetoes the day.
        assertTrue(verdict.score!! < evaluate(SportType.SKI, goodSkiDay(), disabled).score!!)
    }

    @Test
    fun `thin snowpack blocks the day and a deeper one does not`() {
        val thin = goodSkiDay().copy(snowDepthCm = 12.0)
        assertEquals(Suitability.NO_GO, evaluate(SportType.SKI, thin, settings).suitability)
        assertTrue(evaluate(SportType.SKI, goodSkiDay(), settings).blockers.isEmpty())
    }

    @Test
    fun `every hard limit crossed at once is reported, not just the first`() {
        val awful = goodSkiDay().copy(windKmh = 95.0, gustKmh = 120.0, rainMm = 20.0)
        val verdict = evaluate(SportType.SKI, awful, settings)

        assertEquals(Suitability.NO_GO, verdict.suitability)
        assertEquals(3, verdict.blockers.size)
    }

    // -- Scoring -----------------------------------------------------------

    @Test
    fun `the recommended snow depth is the point where depth scores full marks`() {
        val atRecommended = goodSkiDay().copy(snowDepthCm = settings.ski.snowDepthRecommendedCm)
        val wellAbove = goodSkiDay().copy(snowDepthCm = settings.ski.snowDepthRecommendedCm * 3)

        assertEquals(
            evaluate(SportType.SKI, atRecommended, settings).score,
            evaluate(SportType.SKI, wellAbove, settings).score
        )
    }

    @Test
    fun `raising the recommended depth makes the same snowpack score worse`() {
        val day = goodSkiDay().copy(snowDepthCm = 60.0)
        val demanding = settings.copy(ski = settings.ski.copy(snowDepthRecommendedCm = 150.0))

        assertTrue(
            evaluate(SportType.SKI, day, demanding).score!! <
                evaluate(SportType.SKI, day, settings).score!!
        )
    }

    @Test
    fun `minScoreToGo decides where a mediocre day falls`() {
        val mediocre = goodSkiDay().copy(snowDepthCm = 32.0, snowfallCm = 0.0, windKmh = 45.0)
        val score = evaluate(SportType.SKI, mediocre, settings).score!!

        val lenient = settings.copy(minScoreToGo = score - 5)
        val strict = settings.copy(minScoreToGo = score + 5)

        assertTrue(evaluate(SportType.SKI, mediocre, lenient).canGo)
        assertFalse(evaluate(SportType.SKI, mediocre, strict).canGo)
    }

    @Test
    fun `no readings at all yields unknown rather than a bad score`() {
        val verdict = evaluate(SportType.SKI, RuleInput(), settings)

        assertEquals(Suitability.UNKNOWN, verdict.suitability)
        assertEquals(null, verdict.score)
        assertFalse(verdict.canGo)
    }

    // -- Wind relative to the coast ---------------------------------------

    @Test
    fun `wind from inland on a north-facing beach reads as offshore`() {
        // The beach looks north, so offshore wind arrives from the south.
        assertEquals(WindShore.OFFSHORE, windShore(windDirectionDeg = 180.0, coastFacingDeg = 0.0))
        assertEquals(WindShore.ONSHORE, windShore(windDirectionDeg = 0.0, coastFacingDeg = 0.0))
        assertEquals(WindShore.CROSS, windShore(windDirectionDeg = 90.0, coastFacingDeg = 0.0))
    }

    @Test
    fun `coast orientation is ignored when it has not been configured`() {
        assertEquals(WindShore.UNKNOWN, windShore(windDirectionDeg = 180.0, coastFacingDeg = null))
    }

    @Test
    fun `offshore wind scores better than onshore for surf`() {
        val base = RuleInput(waveHeightM = 1.4, wavePeriodS = 12.0, windKmh = 12.0)
        val offshore = evaluate(
            SportType.SURF,
            base.copy(windDirectionDeg = 180.0, coastFacingDeg = 0.0),
            settings
        )
        val onshore = evaluate(
            SportType.SURF,
            base.copy(windDirectionDeg = 0.0, coastFacingDeg = 0.0),
            settings
        )

        assertTrue(offshore.score!! > onshore.score!!)
    }

    // -- Surf and windsurf -------------------------------------------------

    @Test
    fun `flat sea blocks surf`() {
        val verdict = evaluate(SportType.SURF, RuleInput(waveHeightM = 0.2, windKmh = 5.0), settings)

        assertEquals(Suitability.NO_GO, verdict.suitability)
        assertTrue(verdict.blockers.any { it.contains("Sin ola") })
    }

    @Test
    fun `too little wind blocks windsurf just as too much does`() {
        val calm = evaluate(SportType.WINDSURF, RuleInput(windKmh = 8.0, gustKmh = 12.0), settings)
        val storm = evaluate(SportType.WINDSURF, RuleInput(windKmh = 80.0, gustKmh = 95.0), settings)

        assertEquals(Suitability.NO_GO, calm.suitability)
        assertEquals(Suitability.NO_GO, storm.suitability)
    }

    @Test
    fun `gusty wind is blocked by the gust spread limit`() {
        val gusty = RuleInput(windKmh = 30.0, gustKmh = 65.0)
        val verdict = evaluate(SportType.WINDSURF, gusty, settings)

        assertEquals(Suitability.NO_GO, verdict.suitability)
        assertTrue(verdict.blockers.any { it.contains("racheado") })
    }

    @Test
    fun `a textbook windsurf day scores well and is a go`() {
        val verdict = evaluate(
            SportType.WINDSURF,
            RuleInput(windKmh = 33.0, gustKmh = 38.0, airTempC = 20.0, waveHeightM = 0.8),
            settings
        )

        assertTrue(verdict.canGo)
        assertTrue(verdict.score!! >= 70)
    }

    // -- Verdict plumbing --------------------------------------------------

    @Test
    fun `spot conditions fall back to today when there is no current snapshot`() {
        val conditions = SpotConditions(
            spot = skiSpot(),
            now = null,
            days = listOf(
                DailyConditions(
                    date = "2026-01-10",
                    temperatureMaxC = -4.0,
                    snowDepthCm = 95.0,
                    snowfallCm = 15.0,
                    windSpeedKmh = 12.0,
                    rainMm = 0.0
                )
            )
        )

        val verdict = conditions.nowVerdict(settings)
        assertTrue(verdict.canGo)
    }

    @Test
    fun `today's rain total is used instead of the instantaneous rate`() {
        // Nothing is falling this minute, but the day brings 20 mm — well past the 8 mm limit.
        val conditions = SpotConditions(
            spot = skiSpot(),
            now = NowConditions(
                temperatureC = -3.0, windSpeedKmh = 10.0, snowDepthCm = 90.0,
                rainMm = 0.0, precipitationMm = 0.0, snowfallCm = 0.0
            ),
            days = listOf(
                DailyConditions(
                    date = "2026-01-10", temperatureMaxC = -3.0, windSpeedKmh = 10.0,
                    snowDepthCm = 90.0, rainMm = 20.0, precipitationMm = 20.0, snowfallCm = 0.0
                )
            )
        )

        val verdict = conditions.nowVerdict(settings)
        assertEquals(Suitability.NO_GO, verdict.suitability)
        assertTrue(verdict.blockers.any { it.contains("Lluvia") })
    }

    @Test
    fun `an hour inherits its day's fresh snow rather than the hourly rate`() {
        val hour = HourlyConditions(
            "2026-01-10T10:00", temperatureC = -5.0, windSpeedKmh = 10.0,
            snowDepthCm = 90.0, snowfallCm = 0.0, precipitationMm = 0.0, visibilityM = 20_000.0
        )
        val day = DailyConditions(date = "2026-01-10", snowfallCm = 25.0, rainMm = 0.0)

        val withDay = SpotConditions(spot = skiSpot(), hours = listOf(hour), days = listOf(day))
        val without = SpotConditions(spot = skiSpot(), hours = listOf(hour))

        assertTrue(
            withDay.hourlyVerdict(hour, settings).score!! >
                without.hourlyVerdict(hour, settings).score!!
        )
    }

    @Test
    fun `the best window is the longest unbroken run of go-able hours`() {
        val spot = surfSpot()
        // Two runs: 09-10 (2 h) and 13-15 (3 h). The longer one wins.
        val goable = HourlyConditions("", waveHeightM = 1.4, wavePeriodS = 12.0, windSpeedKmh = 8.0)
        val flat = HourlyConditions("", waveHeightM = 0.1, wavePeriodS = 4.0, windSpeedKmh = 8.0)
        val hours = listOf(
            flat.copy(time = "2026-01-10T08:00"),
            goable.copy(time = "2026-01-10T09:00"),
            goable.copy(time = "2026-01-10T10:00"),
            flat.copy(time = "2026-01-10T11:00"),
            flat.copy(time = "2026-01-10T12:00"),
            goable.copy(time = "2026-01-10T13:00"),
            goable.copy(time = "2026-01-10T14:00"),
            goable.copy(time = "2026-01-10T15:00")
        )

        val conditions = SpotConditions(spot = spot, hours = hours)
        assertEquals("13:00" to "15:00", conditions.bestWindow("2026-01-10", settings))
    }

    @Test
    fun `a day with no go-able hour has no best window`() {
        val conditions = SpotConditions(
            spot = surfSpot(),
            hours = listOf(HourlyConditions("2026-01-10T09:00", waveHeightM = 0.1, windSpeedKmh = 8.0))
        )

        assertEquals(null, conditions.bestWindow("2026-01-10", settings))
    }
}
