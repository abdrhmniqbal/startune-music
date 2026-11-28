package com.startune.music.presentation.model

import com.startune.music.data.stats.PlaybackStatsRepository.PlaybackHistoryEntry
import com.startune.music.data.stats.StatsTimeRange
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentlyPlayedRangeFallbackTest {

    // Sunday 2026-11-15, noon: week to date starts Mon 2026-11-09, month
    // starts 2026-11-01, year starts 2026-01-01. Built in the system zone so
    // the boundaries hold wherever the test runs.
    private val nowMillis = LocalDate.of(2026, 11, 15)
        .atTime(12, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    private val dayMs = 24 * 3_600_000L

    private fun entry(daysAgo: Long) =
        PlaybackHistoryEntry(songId = "song-$daysAgo", timestamp = nowMillis - daysAgo * dayMs)

    private fun firstRange(
        history: List<PlaybackHistoryEntry>,
        from: StatsTimeRange = StatsTimeRange.WEEK
    ) = firstRecentlyPlayedRangeWithActivity(history, from, nowMillis)

    @Test
    fun emptyHistoryKeepsTheDefaultRange() {
        assertEquals(StatsTimeRange.WEEK, firstRange(emptyList()))
    }

    @Test
    fun playTodayKeepsWeek() {
        assertEquals(StatsTimeRange.WEEK, firstRange(listOf(entry(0))))
    }

    @Test
    fun nearestRangeWithAPlayWins() {
        assertEquals(StatsTimeRange.WEEK, firstRange(listOf(entry(0), entry(40))))
    }

    @Test
    fun playTenDaysAgoFallsBackToMonth() {
        assertEquals(StatsTimeRange.MONTH, firstRange(listOf(entry(10))))
    }

    @Test
    fun playFortyDaysAgoFallsBackToYear() {
        assertEquals(StatsTimeRange.YEAR, firstRange(listOf(entry(40))))
    }

    @Test
    fun playFourHundredDaysAgoFallsBackToAllTime() {
        assertEquals(StatsTimeRange.ALL, firstRange(listOf(entry(400))))
    }

    @Test
    fun fallbackWalksFromDayThroughWeek() {
        assertEquals(StatsTimeRange.MONTH, firstRange(listOf(entry(10)), StatsTimeRange.DAY))
    }
}
