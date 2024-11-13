package io.getimpulse.player

import io.getimpulse.player.core.Formatter
import org.junit.Assert.*
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FormatterUnitTest {

    @Test
    fun empty() {
        assertEquals(Formatter.time(0.seconds), "0:00")
    }

    @Test
    fun seconds() {
        assertEquals(Formatter.time(3.seconds), "0:03")
        assertEquals(Formatter.time(30.seconds), "0:30")
    }

    @Test
    fun minutes() {
        assertEquals(Formatter.time(1.minutes), "1:00")
        assertEquals(Formatter.time(1.minutes + 33.seconds), "1:33")
    }

    @Test
    fun hours() {
        assertEquals(Formatter.time(1.hours), "1:00")
        assertEquals(Formatter.time(1.hours + 1.minutes + 33.seconds), "1:01:33")
        assertEquals(Formatter.time(1.hours + 12.minutes + 33.seconds), "1:12:33")
    }
}