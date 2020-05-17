import kotlinx.coroutines.runBlocking
import rs.dusk.engine.event.then
import rs.dusk.engine.event.where
import rs.dusk.engine.model.entity.Direction
import rs.dusk.engine.model.entity.factory.NPCFactory
import rs.dusk.engine.model.entity.index.npc.NPC
import rs.dusk.engine.model.entity.index.npc.NPCMoveType
import rs.dusk.engine.model.entity.index.npc.NPCs
import rs.dusk.engine.model.entity.index.player.command.Command
import rs.dusk.engine.model.entity.index.update.visual.*
import rs.dusk.engine.model.entity.index.update.visual.npc.*
import rs.dusk.engine.model.world.Tile
import rs.dusk.utility.inject

val factory: NPCFactory by inject()
val npcs: NPCs by inject()

Command where { prefix == "npc" } then {
    println("Npc command")
    runBlocking {
        val radius = 22
        (-radius..radius).forEach { x ->
            (-radius..radius).forEach { y ->
                factory.spawn(1, player.tile.x + x, player.tile.y + y, player.tile.plane, Direction.NORTH)
            }
        }
    }
}

Command where { prefix == "npckill" } then {
    npcs.indexed.forEachIndexed { index, npc ->
        npcs.indexed[index] = null
        if (npc != null) {
            npcs.remove(npc.tile, npc)
            npcs.remove(npc.tile.chunk, npc)
        }
    }
}

Command where { prefix == "npcs" } then {
    println("NPCs: ${npcs.indexed.filterNotNull().size}")
}

Command where { prefix == "npcname" } then {
    val npc = npcs[player.tile.add(y = 1)]!!.first()!!
    npc.name = "Bob"
}

Command where { prefix == "npctfm" } then {
    val npc = npcs[player.tile.add(y = 1)]!!.first()!!
    npc.transform = 50
}

Command where { prefix == "npcturn" } then {
    val npc = npcs[player.tile.add(y = 1)]!!.first()!!
    val parts = content.split(" ")
    npc.turn(parts[0].toInt(), parts[1].toInt())
}

Command where { prefix == "npcmodel" } then {
    val npc = npcs[player.tile.add(y = 1)]!!.first()!!
    npc.setModelChange(intArrayOf(217, 246, 292, 326, 170, 177, 274, 185))
}

Command where { prefix == "npclvl" } then {
    val npc = npcs[player.tile.add(y = 1)]!!.first()!!
    npc.combatLevel = 100
}

Command where { prefix == "npcanim" } then {
    val npc = npcs[player.tile.add(y = 1)]!!.first()!!
    npc.setAnimation(content.toInt())// 863
}

Command where { prefix == "npcoverlay" } then {
    val npc = npcs[player.tile.add(y = 1)]!!.first()!!
    npc.setColourOverlay(-2108002746, 10, 100)
}

Command where { prefix == "npcchat" } then {
    val npc = npcs[player.tile.add(y = 1)]!!.first()!!
    npc.forceChat = "Testing"
}

Command where { prefix == "npcgfx" } then {
    val npc = npcs[player.tile.add(y = 1)]!!.first()!!
    val id = content.toInt()
    npc.setGraphic(id)// 93
}

Command where { prefix == "npchit" } then {
    val npc = npcs[player.tile.add(y = 1)]!!.first()!!
    npc.addHit(Hit(10, Hit.Mark.Healed, 255))
}

Command where { prefix == "npctime" } then {
    val npc = npcs[player.tile.add(y = 1)]!!.first()!!
    npc.setTimeBar(true, 0, 60, 1)
}

Command where { prefix == "npcwatch" } then {
    val npc = npcs[player.tile.add(y = 1)]!!.first()!!
    npc.watch(player)
}

Command where { prefix == "npcwalk" } then {
    val npc = npcs[player.tile.add(y = 1)]!!.first()!!
    val direction = Direction.NORTH
    npc.movement.walkStep = direction
    npc.movement.delta = direction.delta
    move(npc, player.tile.add(direction.delta))
}

Command where { prefix == "npccrawl" } then {
    val npc = npcs[player.tile.add(y = 1)]!!.first()!!
    val direction = Direction.NORTH
    npc.movementType = NPCMoveType.Crawl
    npc.movement.walkStep = direction
    npc.movement.delta = direction.delta
    move(npc, player.tile.add(direction.delta))
}

Command where { prefix == "npcrun" } then {
    val npc = npcs[player.tile.add(y = 1)]!!.first()!!
    val walk = Direction.NORTH
    val run = Direction.NORTH
    npc.movement.walkStep = walk
    npc.movement.runStep = run
    npc.movement.delta = walk.delta.add(run.delta)
    move(npc, npc.tile.add(npc.movement.delta))
}

Command where { prefix == "npctele" } then {
    val npc = npcs[player.tile.add(y = 1)]!!.first()!!
    val deltaX = 0
    val deltaY = 3
    npc.movement.delta = Tile(deltaX, deltaY, 0)
    move(npc, npc.tile.add(deltaX, deltaY, 0))
}

fun move(npc: NPC, tile: Tile) {
    npc.movement.lastTile = npc.tile
    npcs.remove(npc.tile, npc)
    npcs.remove(npc.tile.chunk, npc)
    npc.tile = tile
    npcs[npc.tile] = npc
    npcs[npc.tile.chunk] = npc
}