package world.gregs.voidps.network.decode

import io.ktor.utils.io.core.*
import world.gregs.voidps.engine.entity.character.player.Player
import world.gregs.voidps.network.Decoder
import world.gregs.voidps.network.Handler
import world.gregs.voidps.network.readBooleanAdd

class NPCOption4Decoder(handler: Handler? = null) : Decoder(3, handler) {

    override fun decode(player: Player, packet: ByteReadPacket) {
        handler?.npcOption(
            player = player,
            npcIndex = packet.readShortLittleEndian().toInt(),
            run = packet.readBooleanAdd(),
            option = 4
        )
    }

}