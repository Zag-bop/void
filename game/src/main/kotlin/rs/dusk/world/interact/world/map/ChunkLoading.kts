import rs.dusk.engine.entity.character.move.Moved
import rs.dusk.engine.entity.character.player.Player
import rs.dusk.engine.entity.character.player.PlayerRegistered
import rs.dusk.engine.entity.character.player.PlayerUnregistered
import rs.dusk.engine.event.then
import rs.dusk.engine.event.where
import rs.dusk.engine.map.Tile
import rs.dusk.engine.map.area.area
import rs.dusk.engine.map.chunk.Chunk
import rs.dusk.engine.map.chunk.ChunkBatcher
import rs.dusk.utility.inject


/**
 * Keeps track of local chunks for batched updates
 */
val batcher: ChunkBatcher by inject()

PlayerRegistered then {
    load(player)
}

PlayerUnregistered then {
    forEachChunk(player, player.tile) { chunk ->
        batcher.unsubscribe(player, chunk)
    }
}

Moved where { entity is Player && from.chunk != to.chunk } then {
    val player = entity as Player
    forEachChunk(player, from) { chunk ->
        batcher.unsubscribe(player, chunk)
    }
    load(player)
}

fun load(player: Player) {
    forEachChunk(player, player.tile) { chunk ->
        if(batcher.subscribe(player, chunk)) {
            batcher.sendInitial(player, chunk)
        }
    }
}

fun forEachChunk(player: Player, tile: Tile, block: (Chunk) -> Unit) {
    val view = tile.chunk.area(player.viewport.tileSize shr 5)
    for (chunk in view) {
        block(chunk)
    }
}