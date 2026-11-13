package com.startune.music.presentation.recap

import androidx.compose.runtime.Immutable
import com.startune.music.data.stats.RecapYearTotal
import com.startune.music.data.stats.StatsTimeRange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.util.concurrent.TimeUnit

/** A recap the app offers to open: the Stats tab's range, and the year for a YEAR recap. */
@Immutable
data class RecapEntry(
    val range: StatsTimeRange,
    /** For YEAR, the year it covers; for the other ranges, the year the period ends in. */
    val year: Int,
    /** The date the recap ends: today for a running period, December 31 for a finished year. */
    val endDate: LocalDate,
    /** True for a recap of a period still running, which reads "so far". */
    val isSoFar: Boolean
)

/** When recaps are offered, and for which periods. Pure, so it is driven by explicit dates. */
internal object RecapSchedule {

    /** Less listening than this makes too thin a story to offer. */
    val MIN_RECAP_LISTENING_MS: Long = TimeUnit.MINUTES.toMillis(30)

    /** A single day rarely reaches thirty minutes, so its bar is lower. */
    private val MIN_DAY_LISTENING_MS: Long = TimeUnit.MINUTES.toMillis(10)

    /** The least listening a recap of [range] needs to be worth opening. */
    fun minListeningMs(range: StatsTimeRange): Long =
        if (range == StatsTimeRange.DAY) MIN_DAY_LISTENING_MS else MIN_RECAP_LISTENING_MS

    /** The seasonal Home card stays up to and including this day of January. */
    private const val HOME_CARD_LAST_JANUARY_DAY = 15

    /**
     * The year the seasonal Home card offers on [today]: the running year through December,
     * then the year that just ended for the first half of January. Null the rest of the year.
     */
    fun homeCardYear(today: LocalDate): Int? = when {
        today.month == Month.DECEMBER -> today.year
        today.month == Month.JANUARY && today.dayOfMonth <= HOME_CARD_LAST_JANUARY_DAY -> today.year - 1
        else -> null
    }

    /** The last day of a recap of [year] made on [today]: today for the running year. */
    fun yearEndDate(year: Int, today: LocalDate): LocalDate =
        if (year == today.year) today else LocalDate.of(year, Month.DECEMBER, 31)

    /**
     * Whether a recap of [range] ending on [endDate] and made on [today] only covers part of
     * its period. YEAR: the running year before December (December already counts as the whole
     * year). MONTH: any day but the last of the month. WEEK: any day but Sunday, the week's
     * last day. DAY and ALL always end mid-period, so they are always "so far".
     */
    fun isSoFar(range: StatsTimeRange, endDate: LocalDate, today: LocalDate): Boolean = when (range) {
        StatsTimeRange.YEAR -> endDate.year == today.year && today.month != Month.DECEMBER
        StatsTimeRange.MONTH -> today.dayOfMonth < today.lengthOfMonth()
        StatsTimeRange.WEEK -> today.dayOfWeek != DayOfWeek.SUNDAY
        StatsTimeRange.DAY, StatsTimeRange.ALL -> true
    }

    /** Whether [year] has enough listening in [totals] for a recap. */
    fun hasEnoughListening(year: Int, totals: List<RecapYearTotal>): Boolean =
        (totals.firstOrNull { it.year == year }?.listenedMs ?: 0L) >= MIN_RECAP_LISTENING_MS

    /**
     * The recap the Stats screen offers for the selected [range] on [today], when [totalMs]
     * of listening in that range is enough for a story. Null when it is too thin. The entry
     * ends today, so its year is [today]'s year.
     */
    fun rangeEntry(range: StatsTimeRange, today: LocalDate, totalMs: Long): RecapEntry? {
        if (totalMs < minListeningMs(range)) return null
        return RecapEntry(range = range, year = today.year, endDate = today, isSoFar = isSoFar(range, today, today))
    }

    /**
     * Recaps the Stats screen offers on [today], most relevant first. Only the running and the
     * previous year qualify: history is kept for two years, so anything older is partial.
     * The year that just ended leads until December, when the running year takes over.
     */
    fun statsEntries(today: LocalDate, totals: List<RecapYearTotal>): List<RecapEntry> {
        val order = if (today.month == Month.DECEMBER) {
            listOf(today.year, today.year - 1)
        } else {
            listOf(today.year - 1, today.year)
        }
        return order
            .filter { hasEnoughListening(it, totals) }
            .map {
                RecapEntry(
                    range = StatsTimeRange.YEAR,
                    year = it,
                    endDate = yearEndDate(it, today),
                    isSoFar = isSoFar(StatsTimeRange.YEAR, yearEndDate(it, today), today)
                )
            }
    }
}
