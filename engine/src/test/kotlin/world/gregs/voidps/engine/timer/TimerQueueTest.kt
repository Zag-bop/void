package world.gregs.voidps.engine.timer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import world.gregs.voidps.engine.GameLoop

internal class TimerQueueTest : TimersTest() {

    @BeforeEach
    override fun setup() {
        super.setup()
        timers = TimerQueue(events)
    }

    @Test
    fun `Multiple timers run at once`() {
        timers.start("1")
        timers.start("2")
        repeat(3) {
            timers.run()
            GameLoop.tick++
        }
        assertTrue(timers.contains("1"))
        assertTrue(timers.contains("2"))
        assertEquals(TimerStart("1"), emitted.pop())
        assertEquals(TimerStart("2"), emitted.pop())
        repeat(3) {
            assertEquals(TimerTick("1"), emitted.pop())
            assertEquals(TimerTick("2"), emitted.pop())
        }
        assertTrue(emitted.isEmpty())
    }
}