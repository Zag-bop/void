package world.gregs.voidps.bot.item

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import world.gregs.voidps.engine.entity.character.contain.inventory
import world.gregs.voidps.engine.entity.character.player.Bot
import world.gregs.voidps.engine.entity.getOrPut
import world.gregs.voidps.engine.entity.item.FloorItem
import world.gregs.voidps.engine.utility.TICKS
import world.gregs.voidps.network.instruct.InteractFloorItem

suspend fun Bot.pickup(floorItem: FloorItem) {
    player.instructions.emit(InteractFloorItem(floorItem.def.id, floorItem.tile.x, floorItem.tile.y, 2))
    if (player.inventory.isFull()) {
        return
    }
    withTimeoutOrNull(TICKS.toMillis(2)) {
        suspendCancellableCoroutine<Unit> { cont ->
            floorItem.getOrPut("bot_jobs") { mutableSetOf<CancellableContinuation<Unit>>() }.add(cont)
        }
    }
}