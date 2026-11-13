package com.startune.music.presentation.recap

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.startune.music.R
import com.startune.music.data.stats.StatsTimeRange
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.IsoFields

/** The month name in the user's locale, e.g. "November". */
@Composable
fun recapMonthName(endDate: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return endDate.month.getDisplayName(TextStyle.FULL, locale)
}

/** The big hero text in the intro blob: the year, the month name, the week, Today, All time. */
@Composable
fun recapHeroLabel(range: StatsTimeRange, year: Int, endDate: LocalDate): String = when (range) {
    StatsTimeRange.YEAR -> year.toString()
    StatsTimeRange.MONTH -> recapMonthName(endDate)
    StatsTimeRange.WEEK -> stringResource(
        R.string.recap_intro_hero_week,
        endDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    )
    StatsTimeRange.DAY -> stringResource(R.string.recap_intro_hero_day)
    StatsTimeRange.ALL -> stringResource(R.string.recap_intro_hero_all)
}

/** The period noun for the not-enough subtitle: "2026", "November", "week", "day", "all-time". */
@Composable
fun recapPeriodNoun(range: StatsTimeRange, year: Int, endDate: LocalDate): String = when (range) {
    StatsTimeRange.YEAR -> year.toString()
    StatsTimeRange.MONTH -> recapMonthName(endDate)
    StatsTimeRange.WEEK -> stringResource(R.string.recap_period_week)
    StatsTimeRange.DAY -> stringResource(R.string.recap_period_day)
    StatsTimeRange.ALL -> stringResource(R.string.recap_period_all)
}

@StringRes
fun recapIntroTitleRes(range: StatsTimeRange, isSoFar: Boolean): Int = when (range) {
    StatsTimeRange.YEAR -> if (isSoFar) R.string.recap_intro_title_so_far else R.string.recap_intro_title
    StatsTimeRange.MONTH -> if (isSoFar) R.string.recap_intro_title_month_so_far else R.string.recap_intro_title_month
    StatsTimeRange.WEEK -> if (isSoFar) R.string.recap_intro_title_week_so_far else R.string.recap_intro_title_week
    StatsTimeRange.DAY -> R.string.recap_intro_title_day
    StatsTimeRange.ALL -> R.string.recap_intro_title_all
}

@StringRes
fun recapIntroSubtitleRes(range: StatsTimeRange): Int = when (range) {
    StatsTimeRange.YEAR -> R.string.recap_intro_subtitle
    StatsTimeRange.MONTH -> R.string.recap_intro_subtitle_month
    StatsTimeRange.WEEK -> R.string.recap_intro_subtitle_week
    StatsTimeRange.DAY -> R.string.recap_intro_subtitle_day
    StatsTimeRange.ALL -> R.string.recap_intro_subtitle_all
}

/** The minutes kicker, formatted with the period. */
@Composable
fun recapMinutesKicker(range: StatsTimeRange, year: Int, endDate: LocalDate): String = when (range) {
    StatsTimeRange.YEAR -> stringResource(R.string.recap_minutes_kicker, year)
    StatsTimeRange.MONTH -> stringResource(R.string.recap_minutes_kicker_month, recapMonthName(endDate))
    StatsTimeRange.WEEK -> stringResource(R.string.recap_minutes_kicker_week)
    StatsTimeRange.DAY -> stringResource(R.string.recap_minutes_kicker_day)
    StatsTimeRange.ALL -> stringResource(R.string.recap_minutes_kicker_all)
}

@StringRes
fun recapSummaryKickerRes(range: StatsTimeRange): Int = when (range) {
    StatsTimeRange.YEAR -> R.string.recap_summary_kicker
    StatsTimeRange.MONTH -> R.string.recap_summary_kicker_month
    StatsTimeRange.WEEK -> R.string.recap_summary_kicker_week
    StatsTimeRange.DAY -> R.string.recap_summary_kicker_day
    StatsTimeRange.ALL -> R.string.recap_summary_kicker_all
}

/** The summary title, formatted with the period. */
@Composable
fun recapSummaryTitle(range: StatsTimeRange, year: Int, endDate: LocalDate, isSoFar: Boolean): String = when (range) {
    StatsTimeRange.YEAR -> stringResource(
        if (isSoFar) R.string.recap_summary_title_so_far else R.string.recap_summary_title,
        year
    )
    StatsTimeRange.MONTH -> stringResource(
        if (isSoFar) R.string.recap_summary_title_month_so_far else R.string.recap_summary_title_month,
        recapMonthName(endDate)
    )
    StatsTimeRange.WEEK -> stringResource(
        if (isSoFar) R.string.recap_summary_title_week_so_far else R.string.recap_summary_title_week
    )
    StatsTimeRange.DAY -> stringResource(R.string.recap_summary_title_day)
    StatsTimeRange.ALL -> stringResource(R.string.recap_summary_title_all)
}

@StringRes
fun recapDedicationFirstSongRes(range: StatsTimeRange): Int = when (range) {
    StatsTimeRange.YEAR -> R.string.recap_dedication_first_song
    StatsTimeRange.MONTH -> R.string.recap_dedication_first_song_month
    StatsTimeRange.WEEK -> R.string.recap_dedication_first_song_week
    // Unreachable: the Dedication slide is skipped for a single day.
    StatsTimeRange.DAY -> R.string.recap_dedication_first_song
    StatsTimeRange.ALL -> R.string.recap_dedication_first_song_all
}

/** The playback queue name for the top songs. */
@Composable
fun recapQueueName(range: StatsTimeRange, year: Int, endDate: LocalDate): String = when (range) {
    StatsTimeRange.YEAR -> stringResource(R.string.recap_top_songs_queue_name, year)
    StatsTimeRange.MONTH -> stringResource(R.string.recap_top_songs_queue_name_month, recapMonthName(endDate))
    StatsTimeRange.WEEK -> stringResource(R.string.recap_top_songs_queue_name_week)
    StatsTimeRange.DAY -> stringResource(R.string.recap_top_songs_queue_name_day)
    StatsTimeRange.ALL -> stringResource(R.string.recap_top_songs_queue_name_all)
}

/** The text shared with the summary image. */
@Composable
fun recapShareText(range: StatsTimeRange, year: Int, endDate: LocalDate): String = when (range) {
    StatsTimeRange.YEAR -> stringResource(R.string.recap_share_text, year)
    StatsTimeRange.MONTH -> stringResource(R.string.recap_share_text_month, recapMonthName(endDate))
    StatsTimeRange.WEEK -> stringResource(R.string.recap_share_text_week)
    StatsTimeRange.DAY -> stringResource(R.string.recap_share_text_day)
    StatsTimeRange.ALL -> stringResource(R.string.recap_share_text_all)
}

/** The shared image's file stem, so each range overwrites only its own previous image. */
fun recapShareFileStem(range: StatsTimeRange, year: Int): String = when (range) {
    StatsTimeRange.YEAR -> "recap_$year"
    else -> "recap_${range.name.lowercase()}_$year"
}

@StringRes
private fun recapCardTitleRes(range: StatsTimeRange, isSoFar: Boolean): Int = when (range) {
    StatsTimeRange.YEAR -> if (isSoFar) R.string.recap_card_title_so_far else R.string.recap_card_title
    StatsTimeRange.MONTH -> if (isSoFar) R.string.recap_card_title_month_so_far else R.string.recap_card_title_month
    StatsTimeRange.WEEK -> if (isSoFar) R.string.recap_card_title_week_so_far else R.string.recap_card_title_week
    StatsTimeRange.DAY -> if (isSoFar) R.string.recap_card_title_day_so_far else R.string.recap_card_title_day
    StatsTimeRange.ALL -> if (isSoFar) R.string.recap_card_title_all_so_far else R.string.recap_card_title_all
}

@StringRes
private fun recapCardSubtitleRes(range: StatsTimeRange, isSoFar: Boolean): Int = when (range) {
    StatsTimeRange.YEAR -> if (isSoFar) R.string.recap_card_subtitle_so_far else R.string.recap_card_subtitle
    StatsTimeRange.MONTH -> if (isSoFar) R.string.recap_card_subtitle_month_so_far else R.string.recap_card_subtitle_month
    StatsTimeRange.WEEK -> if (isSoFar) R.string.recap_card_subtitle_week_so_far else R.string.recap_card_subtitle_week
    StatsTimeRange.DAY -> if (isSoFar) R.string.recap_card_subtitle_day_so_far else R.string.recap_card_subtitle_day
    StatsTimeRange.ALL -> if (isSoFar) R.string.recap_card_subtitle_all_so_far else R.string.recap_card_subtitle_all
}

/** The Stats card title for [entry], formatted with the year or the month name. */
@Composable
fun recapCardTitle(entry: RecapEntry): String = when (entry.range) {
    StatsTimeRange.YEAR -> stringResource(recapCardTitleRes(entry.range, entry.isSoFar), entry.year)
    StatsTimeRange.MONTH -> stringResource(recapCardTitleRes(entry.range, entry.isSoFar), recapMonthName(entry.endDate))
    else -> stringResource(recapCardTitleRes(entry.range, entry.isSoFar))
}

/** The Stats card subtitle for [entry]. */
@Composable
fun recapCardSubtitle(entry: RecapEntry): String =
    stringResource(recapCardSubtitleRes(entry.range, entry.isSoFar))
