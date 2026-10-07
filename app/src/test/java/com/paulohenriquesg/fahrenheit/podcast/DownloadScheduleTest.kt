package com.paulohenriquesg.fahrenheit.podcast

import org.junit.Assert.assertEquals
import org.junit.Test

/** The server keeps a cron schedule; the panel offers three plain choices (#182). */
class DownloadScheduleTest {

    @Test
    fun `the web app's hourly and daily schedules read as their choices`() {
        assertEquals(ScheduleChoice.Hourly, DownloadSchedule.choiceOf("0 * * * *"))
        assertEquals(ScheduleChoice.Hourly, DownloadSchedule.choiceOf("15 * * * *"))
        assertEquals(ScheduleChoice.Daily, DownloadSchedule.choiceOf("0 0 * * *"))
        assertEquals(ScheduleChoice.Daily, DownloadSchedule.choiceOf("30 4 * * *"))
    }

    @Test
    fun `one day a week reads as every week`() {
        assertEquals(ScheduleChoice.Weekly, DownloadSchedule.choiceOf("0 0 * * 0"))
        assertEquals(ScheduleChoice.Weekly, DownloadSchedule.choiceOf("15 3 * * 5"))
    }

    @Test
    fun `anything else is custom`() {
        listOf(
            "*/30 * * * *", "0 */6 * * *", "0 0 * * 1,3", "0 0 1 * *", "0 0 * * *  extra", "nonsense", "", null
        ).forEach { cron ->
            assertEquals(cron.toString(), ScheduleChoice.Custom, DownloadSchedule.choiceOf(cron))
        }
    }

    @Test
    fun `each choice is sent as a cron the web app writes too`() {
        assertEquals("0 * * * *", DownloadSchedule.cronOf(ScheduleChoice.Hourly))
        assertEquals("0 0 * * *", DownloadSchedule.cronOf(ScheduleChoice.Daily))
        assertEquals("0 0 * * 0", DownloadSchedule.cronOf(ScheduleChoice.Weekly))
    }

    @Test
    fun `what is sent reads back as the choice`() {
        listOf(ScheduleChoice.Hourly, ScheduleChoice.Daily, ScheduleChoice.Weekly).forEach {
            assertEquals(it, DownloadSchedule.choiceOf(DownloadSchedule.cronOf(it)))
        }
    }
}
