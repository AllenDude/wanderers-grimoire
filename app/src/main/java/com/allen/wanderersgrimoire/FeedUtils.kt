package com.allen.wanderersgrimoire

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object FeedUtils {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    /** "TODAY", "YESTERDAY", or a "MMM d" date, for grouping the feed by day. */
    fun dayLabel(timestamp: Long): String {
        val itemCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(itemCal, todayCal) -> "TODAY"
            isSameDay(itemCal, yesterdayCal) -> "YESTERDAY"
            else -> dateFormat.format(itemCal.time).uppercase(Locale.getDefault())
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    fun timeLabel(timestamp: Long): String = timeFormat.format(timestamp)

    /** "reddit.com" from a full URL, stripping the "www." prefix. Falls
     *  back to the raw string if it doesn't parse as a URL at all. */
    fun domainOf(url: String): String {
        return try {
            val host = Uri.parse(url).host ?: return url
            if (host.startsWith("www.")) host.removePrefix("www.") else host
        } catch (_: Exception) {
            url
        }
    }
}
