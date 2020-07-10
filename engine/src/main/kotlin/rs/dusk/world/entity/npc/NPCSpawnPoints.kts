package rs.dusk.world.entity.npc

import rs.dusk.engine.data.file.FileLoader
import rs.dusk.engine.event.EventBus
import rs.dusk.engine.event.then
import rs.dusk.engine.model.engine.Startup
import rs.dusk.engine.model.entity.Direction
import rs.dusk.engine.model.world.Region
import rs.dusk.engine.model.world.Tile
import rs.dusk.engine.model.world.map.MapLoaded
import rs.dusk.utility.getProperty
import rs.dusk.utility.inject

val files: FileLoader by inject()
val bus: EventBus by inject()

data class NPCSpawnPoint(val id: Int, val tile: Tile, val direction: Direction = Direction.NONE)

val spawns: MutableMap<Region, MutableList<NPCSpawnPoint>> = mutableMapOf()

Startup then {
    val path: String = getProperty("npcsPath")
    val points: Array<NPCSpawnPoint> = files.load(path)
    points.forEach { spawn ->
        val list = spawns.getOrPut(spawn.tile.region) { mutableListOf() }
        list.add(spawn)
    }
}

MapLoaded then {
    val spawns = spawns[region] ?: return@then
    spawns.forEach { spawn ->
        val npc = bus.emit(NPCSpawn(spawn.id, spawn.tile, spawn.direction))
        // TODO respawn on death
    }
}