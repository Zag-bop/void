package rs.dusk.engine.entity.model.visual.visuals.player

import rs.dusk.engine.entity.model.Player
import rs.dusk.engine.entity.model.visual.Visual

/**
 * @author Greg Hibberd <greg@greghibberd.com>
 * @since April 25, 2020
 */
data class TemporaryMoveType(var type: Int = MovementType.NONE) : Visual

const val TEMPORARY_MOVE_TYPE_MASK = 0x1

fun Player.flagTemporaryMoveType() = visuals.flag(TEMPORARY_MOVE_TYPE_MASK)

fun Player.getTemporaryMoveType() = visuals.getOrPut(TEMPORARY_MOVE_TYPE_MASK) { TemporaryMoveType() }

var Player.temporaryMoveType: Int
    get() = getTemporaryMoveType().type
    set(value) {
        getTemporaryMoveType().type = value
        flagTemporaryMoveType()
    }