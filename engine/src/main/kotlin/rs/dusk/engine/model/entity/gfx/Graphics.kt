package rs.dusk.engine.model.entity.gfx

import rs.dusk.engine.model.entity.list.BatchList
import rs.dusk.engine.model.world.ChunkPlane

/**
 * @author Greg Hibberd <greg@greghibberd.com>
 * @since July 04, 2020
 */
class Graphics(override val chunks: MutableMap<ChunkPlane, MutableSet<AreaGraphic>> = mutableMapOf()) :
    BatchList<AreaGraphic>