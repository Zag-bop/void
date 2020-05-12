package rs.dusk.engine.client.update

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.koin.test.mock.declareMock
import rs.dusk.core.io.write.Writer
import rs.dusk.engine.EngineTasks
import rs.dusk.engine.client.Sessions
import rs.dusk.engine.client.clientSessionModule
import rs.dusk.engine.entity.list.MAX_PLAYERS
import rs.dusk.engine.entity.list.entityListModule
import rs.dusk.engine.entity.list.player.Players
import rs.dusk.engine.model.entity.index.Changes.Companion.ADJACENT_REGION
import rs.dusk.engine.model.entity.index.Changes.Companion.GLOBAL_REGION
import rs.dusk.engine.model.entity.index.Changes.Companion.HEIGHT
import rs.dusk.engine.model.entity.index.Changes.Companion.RUN
import rs.dusk.engine.model.entity.index.Changes.Companion.TELE
import rs.dusk.engine.model.entity.index.Changes.Companion.UPDATE
import rs.dusk.engine.model.entity.index.Changes.Companion.WALK
import rs.dusk.engine.model.entity.index.player.Player
import rs.dusk.engine.model.world.RegionPlane
import rs.dusk.engine.model.world.Tile
import rs.dusk.engine.script.KoinMock
import rs.dusk.engine.view.TrackingSet
import rs.dusk.engine.view.Viewport
import rs.dusk.network.rs.codec.game.encode.message.PlayerUpdateMessage
import rs.dusk.utility.func.toInt

/**
 * @author Greg Hibberd <greg></greg>@greghibberd.com>
 * @since May 02, 2020
 */
internal class PlayerUpdateTaskTest : KoinMock() {

    lateinit var task: PlayerUpdateTask
    override val modules = listOf(entityListModule, clientSessionModule)

    @BeforeEach
    fun setup() {
        task = spyk(PlayerUpdateTask(EngineTasks()))
    }

    @Test
    fun `Called for each player with sessions`() {
        // Given
        val player = mockk<Player>(relaxed = true)
        declareMock<Players> {
            every { forEach(any()) } answers {
                val block = arg<(Player) -> Unit>(0)
                block.invoke(player)
            }
            every { getAtIndex(any()).hint(Player::class) } returns null
        }
        declareMock<Sessions> {
            every { contains(player) } returns true
            every { send(player, any(), any<PlayerUpdateMessage>()) } just Runs
        }
        // When
        task.run()
        // Then
        coVerify {
            task.update(player)
        }
    }

    @Test
    fun `Player without session not called`() {
        // Given
        val player = mockk<Player>(relaxed = true)
        declareMock<Players> {
            every { forEach(any()) } answers {
                val block = arg<(Player) -> Unit>(0)
                block.invoke(player)
            }
            every {
                hint(Player::class)
                getAtIndex(any())
            } returns null
        }
        declareMock<Sessions> {
            every { contains(player) } returns false
        }
        // When
        task.run()
        // Then
        coVerify(exactly = 0) {
            task.update(player)
        }
    }

    @Test
    fun `Locals processed before global, active before idle`() {
        // Given
        val player = mockk<Player>(relaxed = true)
        val viewport = mockk<Viewport>(relaxed = true)
        val entities = mockk<TrackingSet<Player>>(relaxed = true)
        every { player.viewport } returns viewport
        every { viewport.players } returns entities
        // When
        runBlocking {
            task.update(player).await()
        }
        // Then
        verifyOrder {
            task.processLocals(any(), any(), entities, viewport, true)
            task.processLocals(any(), any(), entities, viewport, false)
            task.processGlobals(any(), any(), entities, viewport, true)
            task.processGlobals(any(), any(), entities, viewport, false)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `Local idle player ignored on update`(active: Boolean) {
        // Given
        val activePlayer = mockk<Player>(relaxed = true)
        val idlePlayer = mockk<Player>(relaxed = true)
        val entities = mockk<TrackingSet<Player>>(relaxed = true)
        val viewport = mockk<Viewport>(relaxed = true)
        val idleIndex = 1
        val activeIndex = 2

        every { entities.current } returns setOf(idlePlayer, activePlayer)
        every { viewport.isIdle(idleIndex) } returns true
        every { viewport.isIdle(activeIndex) } returns false
        every { activePlayer.index } returns activeIndex
        every { idlePlayer.index } returns idleIndex
        every { idlePlayer.changes.localUpdate } returns -1
        every { activePlayer.changes.localUpdate } returns -1
        // When
        task.processLocals(mockk(relaxed = true), mockk(relaxed = true), entities, viewport, active)
        // Then
        verify(exactly = (!active).toInt()) { viewport.setIdle(idleIndex) }
        verify(exactly = active.toInt()) { viewport.setIdle(activeIndex) }
    }

    @Test
    fun `Local player removed`() {
        // Given
        val player = mockk<Player>(relaxed = true)
        val viewport = mockk<Viewport>(relaxed = true)
        val entities = mockk<TrackingSet<Player>>(relaxed = true)
        val sync: Writer = mockk(relaxed = true)

        every { entities.remove.contains(player) } returns true
        every { entities.current } returns setOf(player)
        every { entities.lastSeen[player] } returns null
        // When
        task.processLocals(sync, mockk(relaxed = true), entities, viewport, true)
        // Then
        verify(exactly = 0) { viewport.setIdle(any()) }
        verifyOrder {
            sync.startBitAccess()
            sync.writeBits(1, true)
            sync.writeBits(1, false)// Even when update isn't null (relaxed)
            sync.writeBits(2, 0)
            task.encodeRegion(sync, entities, player)
            sync.finishBitAccess()
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [WALK, RUN])
    fun `Local player move on foot`(type: Int) {
        // Given
        val player = mockk<Player>(relaxed = true)
        val viewport = mockk<Viewport>(relaxed = true)
        val entities = mockk<TrackingSet<Player>>(relaxed = true)
        val sync: Writer = mockk(relaxed = true)
        val value = 1

        every { player.changes.localValue } returns value
        every { player.changes.localUpdate } returns type
        every { entities.current } returns setOf(player)
        // When
        task.processLocals(sync, mockk(relaxed = true), entities, viewport, true)
        // Then
        verify(exactly = 0) { viewport.setIdle(any()) }
        verifyOrder {
            sync.writeBits(1, true)
            sync.writeBits(1, true)
            sync.writeBits(2, type)
            sync.writeBits(type + 2, value)
        }
    }

    @Test
    fun `Local player teleport`() {
        // Given
        val player = mockk<Player>(relaxed = true)
        val viewport = mockk<Viewport>(relaxed = true)
        val entities = mockk<TrackingSet<Player>>(relaxed = true)
        val sync: Writer = mockk(relaxed = true)
        val value = 1

        every { player.changes.localValue } returns value
        every { player.changes.localUpdate } returns TELE
        every { entities.current } returns setOf(player)
        // When
        task.processLocals(sync, mockk(relaxed = true), entities, viewport, true)
        // Then
        verify(exactly = 0) { viewport.setIdle(any()) }
        verifyOrder {
            sync.writeBits(1, true)
            sync.writeBits(1, true)
            sync.writeBits(2, TELE)
            sync.writeBits(1, false)
            sync.writeBits(12, value)
        }
    }

    @Test
    fun `Local player visual update`() {
        // Given
        val player = mockk<Player>(relaxed = true)
        val viewport = mockk<Viewport>(relaxed = true)
        val entities = mockk<TrackingSet<Player>>(relaxed = true)
        val sync: Writer = mockk(relaxed = true)
        val updates: Writer = mockk(relaxed = true)

        every { player.changes.localValue } returns -1
        every { player.changes.localUpdate } returns UPDATE
        every { entities.current } returns setOf(player)
        // When
        task.processLocals(sync, updates, entities, viewport, true)
        // Then
        verify(exactly = 0) { viewport.setIdle(any()) }
        verifyOrder {
            sync.writeBits(1, true)
            sync.writeBits(1, true)
            sync.writeBits(2, UPDATE)
            updates.writeBytes(player.visuals.update!!)
        }
    }

    @Test
    fun `Local player no visual update`() {
        // Given
        val player = mockk<Player>(relaxed = true)
        val viewport = mockk<Viewport>(relaxed = true)
        val entities = mockk<TrackingSet<Player>>(relaxed = true)
        val sync: Writer = mockk(relaxed = true)
        val updates: Writer = mockk(relaxed = true)

        every { player.changes.localValue } returns -1
        every { player.changes.localUpdate } returns UPDATE
        every { player.visuals.update } returns null
        every { entities.current } returns setOf(player)
        // When
        task.processLocals(sync, updates, entities, viewport, true)
        // Then
        verify(exactly = 0) {
            viewport.setIdle(any())
            updates.writeBytes(any<ByteArray>())
        }
        verifyOrder {
            sync.writeBits(1, true)
            sync.writeBits(1, false)
            sync.writeBits(2, UPDATE)
        }
    }

    @Test
    fun `Local player skip`() {
        // Given
        val player = mockk<Player>(relaxed = true)
        val skipPlayer = mockk<Player>(relaxed = true)
        val viewport = mockk<Viewport>(relaxed = true)
        val entities = mockk<TrackingSet<Player>>(relaxed = true)
        val sync: Writer = mockk(relaxed = true)
        val updates: Writer = mockk(relaxed = true)
        val index = 1

        every { skipPlayer.index } returns index
        every { skipPlayer.changes.localUpdate } returns -1
        every { entities.current } returns linkedSetOf(skipPlayer, player)
        // When
        task.processLocals(sync, updates, entities, viewport, true)
        // Then
        verifyOrder {
            viewport.setIdle(index)
            task.writeSkip(sync, 0)
            sync.writeBits(1, true)
        }
    }

    @Test
    fun `Skip last local player`() {
        // Given
        val player = mockk<Player>(relaxed = true)
        val viewport = mockk<Viewport>(relaxed = true)
        val entities = mockk<TrackingSet<Player>>(relaxed = true)
        val sync: Writer = mockk(relaxed = true)
        val updates: Writer = mockk(relaxed = true)

        every { player.changes.localUpdate } returns -1
        every { entities.current } returns setOf(player)
        // When
        task.processLocals(sync, updates, entities, viewport, true)
        // Then
        verifyOrder {
            viewport.setIdle(any())
            task.writeSkip(sync, 0)
        }
    }

    @Test
    fun `Local player not processed in global`() {
        // Given
        val player = mockk<Player>(relaxed = true)
        val viewport = mockk<Viewport>(relaxed = true)
        val entities = mockk<TrackingSet<Player>>(relaxed = true)
        val index = 1

        declareMock<Players> {
            every {
                hint(Player::class)
                getAtIndex(any())
            } answers {
                if (arg<Int>(0) == index) player else null
            }
        }
        every { player.index } returns index
        every { entities.local.contains(player) } returns true
        // When
        task.processGlobals(mockk(relaxed = true), mockk(relaxed = true), entities, viewport, true)
        // Then
        verify(exactly = 0) {
            entities.add.contains(player)
            viewport.setIdle(index)
        }
    }

    @Test
    fun `Global player add`() {
        // Given
        val player = mockk<Player>(relaxed = true)
        val viewport = mockk<Viewport>(relaxed = true)
        val entities = mockk<TrackingSet<Player>>(relaxed = true)
        val sync: Writer = mockk(relaxed = true)
        val updates: Writer = mockk(relaxed = true)
        val index = 1

        declareMock<Players> {
            every {
                hint(Player::class)
                getAtIndex(any())
            } answers {
                if (arg<Int>(0) == index) player else null
            }
        }
        every { player.tile.x } returns 81
        every { player.tile.y } returns 14
        every { player.index } returns index
        every { entities.add.contains(player) } returns true
        every { entities.lastSeen[any()] } returns null
        // When
        task.processGlobals(sync, updates, entities, viewport, true)
        // Then
        verifyOrder {
            sync.startBitAccess()
            sync.writeBits(1, true)
            sync.writeBits(2, 0)
            sync.writeBits(1, false)// Encode region
            sync.writeBits(6, 17)
            sync.writeBits(6, 14)
            sync.writeBits(1, true)
            updates.writeBytes(any<ByteArray>())
            task.writeSkip(sync, 2045)
            sync.finishBitAccess()
        }
    }

    @Test
    fun `Skip last global player`() {
        // Given
        val player = mockk<Player>(relaxed = true)
        val viewport = mockk<Viewport>(relaxed = true)
        val entities = mockk<TrackingSet<Player>>(relaxed = true)
        val sync: Writer = mockk(relaxed = true)
        val updates: Writer = mockk(relaxed = true)
        declareMock<Players> {
            every {
                hint(Player::class)
                getAtIndex(any())
            } answers {
                if (arg<Int>(0) == MAX_PLAYERS - 2) player else null
            }
        }
        every { entities.add.contains(player) } returns true
        every { entities.lastSeen[any()] } returns null
        // When
        task.processGlobals(sync, updates, entities, viewport, true)
        // Then
        verifyOrder {
            task.writeSkip(sync, 2044)
            task.writeSkip(sync, 0)
        }
    }

    @TestFactory
    fun `Write skip`() = intArrayOf(0, 30, 250, 2040).mapIndexed { index, skip ->
        dynamicTest("Write skip $skip") {
            // Given
            val writer: Writer = mockk(relaxed = true)
            // When
            task.writeSkip(writer, skip)
            // Then
            verifyOrder {
                writer.writeBits(1, 0)
                if (index > 0) {
                    writer.writeBits(2, index)
                }
                val count = when (index) {
                    1 -> 5
                    2 -> 8
                    3 -> 11
                    else -> return@verifyOrder
                }
                writer.writeBits(count, skip)
            }
        }
    }

    @TestFactory
    fun `Encode region`() = intArrayOf(UPDATE, HEIGHT, ADJACENT_REGION, GLOBAL_REGION).mapIndexed { index, updateType ->
        dynamicTest("Encode region change $updateType") {
            // Given
            val writer: Writer = mockk(relaxed = true)
            val value = 10
            val set: TrackingSet<Player> = mockk(relaxed = true)
            val player: Player = mockk(relaxed = true)
            every { player.tile } returns Tile(0)
            every { set.lastSeen[player] } returns null
            every { task.calculateRegionUpdate(any()) } returns updateType
            every { task.calculateRegionValue(any(), any()) } returns value
            // When
            task.encodeRegion(writer, set, player)
            // Then
            verifyOrder {
                writer.writeBits(1, updateType != UPDATE)
                if (updateType != UPDATE) {
                    writer.writeBits(2, updateType)
                    val count = when (index) {
                        1 -> 2
                        2 -> 5
                        3 -> 18
                        else -> return@verifyOrder
                    }
                    writer.writeBits(count, value)
                }
            }
        }
    }

    @TestFactory
    fun `Region update types`() = arrayOf(
        UPDATE to RegionPlane(0, 0, 0),
        HEIGHT to RegionPlane(0, 0, 1),
        ADJACENT_REGION to RegionPlane(1, 1, 0),
        ADJACENT_REGION to RegionPlane(0, 1, 0),
        ADJACENT_REGION to RegionPlane(1, 0, 0),
        ADJACENT_REGION to RegionPlane(-1, -1, 2),
        ADJACENT_REGION to RegionPlane(0, -1, 0),
        ADJACENT_REGION to RegionPlane(-1, 0, 0),
        GLOBAL_REGION to RegionPlane(2, 2, 3),
        GLOBAL_REGION to RegionPlane(0, 2, 0),
        GLOBAL_REGION to RegionPlane(2, 0, 0),
        GLOBAL_REGION to RegionPlane(-2, -2, 0),
        GLOBAL_REGION to RegionPlane(0, -2, 0),
        GLOBAL_REGION to RegionPlane(-2, 0, 0)
    ).map { (expected, delta) ->
        dynamicTest("Region update for movement $delta") {
            // When
            val result = task.calculateRegionUpdate(delta)
            // Then
            assertEquals(expected, result)
        }
    }

    @TestFactory
    fun `Region update values`() = arrayOf(
        Triple(UPDATE, RegionPlane(0, 0, 0), -1),
        Triple(HEIGHT, RegionPlane(0, 0, 1), 1),
        Triple(HEIGHT, RegionPlane(0, 0, -3), -3),
        Triple(ADJACENT_REGION, RegionPlane(-1, 1, 0), 0),
        Triple(ADJACENT_REGION, RegionPlane(0, 1, 1), 9),
        Triple(ADJACENT_REGION, RegionPlane(1, 1, 2), 18),
        Triple(ADJACENT_REGION, RegionPlane(1, 0, 3), 28),
        Triple(ADJACENT_REGION, RegionPlane(1, -1, 0), 7),
        Triple(ADJACENT_REGION, RegionPlane(0, -1, 1), 14),
        Triple(ADJACENT_REGION, RegionPlane(-1, -1, 2), 21),
        Triple(ADJACENT_REGION, RegionPlane(-1, 0, 3), 27),
        Triple(GLOBAL_REGION, RegionPlane(2, 2, 0), 514),
        Triple(GLOBAL_REGION, RegionPlane(-2, -2, 1), 130814),
        Triple(GLOBAL_REGION, RegionPlane(12, -16, 2), 134384)
    ).map { (updateType, delta, expected) ->
        dynamicTest("Region value for movement $delta") {
            // When
            val result = task.calculateRegionValue(updateType, delta)
            // Then
            assertEquals(expected, result)
        }
    }
}