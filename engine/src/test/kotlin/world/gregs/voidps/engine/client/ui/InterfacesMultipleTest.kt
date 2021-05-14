package world.gregs.voidps.engine.client.ui

import io.mockk.every
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import world.gregs.voidps.cache.definition.data.InterfaceDefinition
import world.gregs.voidps.engine.client.ui.Interfaces.Companion.ROOT_ID
import world.gregs.voidps.engine.client.ui.Interfaces.Companion.ROOT_INDEX
import world.gregs.voidps.engine.client.ui.event.InterfaceClosed
import world.gregs.voidps.engine.client.ui.event.InterfaceOpened
import world.gregs.voidps.network.encode.closeInterface
import world.gregs.voidps.network.encode.openInterface
import world.gregs.voidps.network.encode.updateInterface

internal class InterfacesMultipleTest : InterfaceTest() {

    private val zero = "zero"
    private val one = "one"
    private val two = "two"

    @BeforeEach
    override fun setup() {
        super.setup()
        every { definitions.get(zero) } returns InterfaceDefinition(id = 0, extras = mapOf("parent_fixed" to one, "index_fixed" to ROOT_INDEX))
        every { definitions.getId(zero) } returns 0
        every { definitions.get(one) } returns InterfaceDefinition(id = 1, extras = mapOf("parent_fixed" to two, "index_fixed" to ROOT_INDEX))
        every { definitions.getId(one) } returns 1
        every { definitions.get(two) } returns InterfaceDefinition(id = 2, extras = mapOf("parent_fixed" to ROOT_ID, "index_fixed" to ROOT_INDEX))
        every { definitions.getId(two) } returns 2
    }

    @Test
    fun `Can't open child if parent isn't open`() {
        assertFalse(interfaces.open(one))

        verify(exactly = 0) {
            client.openInterface(any(), any(), any(), any())
            events.emit(InterfaceOpened(1, one))
        }
    }

    @Test
    fun `Can open parents and children`() {
        assertTrue(interfaces.open(two))
        assertTrue(interfaces.open(one))
        assertTrue(interfaces.open(zero))

        verifyOrder {
            client.updateInterface(2, 0)
            events.emit(InterfaceOpened(2, two))
            client.openInterface(false, 2, 0, 1)
            events.emit(InterfaceOpened(1, one))
            client.openInterface(false, 1, 0, 0)
            events.emit(InterfaceOpened(0, zero))
        }
    }

    @Test
    fun `Remove doesn't close children`() {
        interfaces.open(two)
        interfaces.open(one)
        interfaces.open(zero)

        assertTrue(interfaces.remove(two))

        assertFalse(interfaces.contains(two))
        assertTrue(interfaces.contains(one))
        assertTrue(interfaces.contains(zero))

        verifyOrder {
            client.closeInterface(0, 0)
            events.emit(InterfaceClosed(2, two))
        }
        verify(exactly = 0) {
            client.closeInterface(2, 0)
            events.emit(InterfaceClosed(1, one))
            client.closeInterface(1, 0)
            events.emit(InterfaceClosed(0, zero))
        }
    }

    @Test
    fun `Close children doesn't close parent`() {
        interfaces.open(two)
        interfaces.open(one)
        interfaces.open(zero)

        assertTrue(interfaces.closeChildren(two))

        assertTrue(interfaces.contains(two))
        assertFalse(interfaces.contains(one))
        assertFalse(interfaces.contains(zero))
        verifyOrder {
            client.closeInterface(2, 0)
            events.emit(InterfaceClosed(1, one))
            client.closeInterface(1, 0)
            events.emit(InterfaceClosed(0, zero))
        }
        verify(exactly = 0) {
            client.closeInterface(0, 0)
            events.emit(InterfaceClosed(2, two))
        }
    }

    @Test
    fun `Close closes children and parent`() {
        interfaces.open(two)
        interfaces.open(one)
        interfaces.open(zero)

        assertTrue(interfaces.close(two))

        assertFalse(interfaces.contains(two))
        assertFalse(interfaces.contains(one))
        assertFalse(interfaces.contains(zero))
        verifyOrder {
            client.closeInterface(0, 0)
            events.emit(InterfaceClosed(2, two))
            client.closeInterface(2, 0)
            events.emit(InterfaceClosed(1, one))
            client.closeInterface(1, 0)
            events.emit(InterfaceClosed(0, zero))
        }
    }
}
