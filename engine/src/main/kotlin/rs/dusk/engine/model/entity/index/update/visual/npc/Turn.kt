package rs.dusk.engine.model.entity.index.update.visual.npc

import rs.dusk.engine.model.entity.index.npc.NPC
import rs.dusk.engine.model.entity.index.update.Visual

/**
 * @author Greg Hibberd <greg@greghibberd.com>
 * @since April 25, 2020
 */
data class Turn(
    var x: Int = 0,
    var y: Int = 0,
    var directionX: Int = 0,
    var directionY: Int = 0
) : Visual

const val TURN_MASK = 0x8

fun NPC.getTurn() = visuals.getOrPut(TURN_MASK) { Turn() }

fun NPC.flagTurn() = visuals.flag(TURN_MASK)

fun NPC.turn(deltaX: Int = 0, deltaY: Int = -1) {
    val turn = getTurn()
    turn.x = tile.x
    turn.y = tile.y
    turn.directionX = deltaX
    turn.directionY = deltaY
    flagTurn()
}