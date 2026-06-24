package com.nimboweather.forecast.data.nowcast

import com.nimboweather.forecast.data.NowcastState

/**
 * Pure decision for the rain-nowcast push: should we notify, and what do we persist to
 * avoid re-firing for the same rain episode? Frame-work free and unit tested; the worker
 * supplies the clock (hourOfDay) + persisted [lastEventKey] and performs the actual post.
 *
 * Rules (see `docs/precip-nowcast-spec.md`):
 * - Notify only on a dry→rain transition (`RainStarting`) — the retention bet.
 * - One notification per episode: dedup via [eventKey]; reset to null once it's no longer
 *   "starting" so the next episode can fire again.
 * - Quiet hours 22:00–07:00: suppress, but DON'T advance the key, so an imminent event can
 *   still fire once quiet hours end.
 */
object NowcastAlerts {

    data class Notification(val title: String, val body: String)

    /** [notification] is null when nothing should be posted; [eventKey] is what to persist. */
    data class Result(val notification: Notification?, val eventKey: String?)

    private const val STARTING_KEY = "start"

    fun decide(state: NowcastState, lastEventKey: String?, hourOfDay: Int): Result {
        val currentKey = if (state is NowcastState.RainStarting) STARTING_KEY else null

        // Quiet hours: suppress without advancing the key (preserve a pending event).
        if (isQuietHour(hourOfDay)) return Result(notification = null, eventKey = lastEventKey)

        // Not an alertable "starting" state, or already notified this episode → just sync the key.
        if (currentKey == null || currentKey == lastEventKey) {
            return Result(notification = null, eventKey = currentKey)
        }

        val minutes = (state as NowcastState.RainStarting).minutesUntilStart
        return Result(
            notification = Notification(
                title = "Rain expected soon",
                body = bodyFor(minutes)
            ),
            eventKey = currentKey
        )
    }

    private fun bodyFor(minutes: Int): String {
        val whenText = if (minutes <= 15) "within the next 15 min" else "in about $minutes min"
        return "Rain likely near you $whenText — grab an umbrella."
    }

    /** 22:00–06:59 local is quiet (07:00 onward is allowed). */
    private fun isQuietHour(hourOfDay: Int): Boolean = hourOfDay >= 22 || hourOfDay < 7
}
