package com.hakunakuinama.app.domain.model

import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Shared week helpers so the Dashboard, Budget Tracker and repo all agree on week boundaries. */
object Weeks {

    /**
     * Monday that starts the week containing [date] — Kenyan budgeting weeks run Mon–Sun.
     *
     * Note the deliberate asymmetry: Sunday belongs to the week that started six days
     * earlier, not to the next one. `TemporalAdjusters.previousOrSame` gives that for free
     * and matches how a shopkeeper's "week" runs; a Monday-anchored list that jumped
     * forward on Sunday would leave that day with no shopping list at all.
     */
    fun startOf(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))

    fun current(today: LocalDate = LocalDate.now()): LocalDate = startOf(today)
}
