package org.redrune.network.codec.update.decode.impl

import org.redrune.core.network.model.packet.DataType
import org.redrune.core.network.model.packet.PacketMetaData
import org.redrune.core.network.model.packet.access.PacketReader
import org.redrune.network.codec.update.decode.UpdateMessageDecoder
import org.redrune.network.codec.update.decode.message.UpdateRequestMessage
import org.redrune.tools.constants.FileServerOpcodes.FILE_REQUEST
import org.redrune.tools.constants.FileServerOpcodes.PRIORITY_FILE_REQUEST

/**
 * @author Tyluur <contact@kiaira.tech>
 * @since February 18, 2020
 */
@PacketMetaData(opcodes = [FILE_REQUEST, PRIORITY_FILE_REQUEST], length = 3)
class UpdateRequestMessageDecoder : UpdateMessageDecoder<UpdateRequestMessage>() {

    override fun decode(packet: PacketReader): UpdateRequestMessage {
        val hash = packet.readUnsigned(DataType.MEDIUM)
        val indexId = (hash shr 16).toInt()
        val archiveId = (hash and 0xffff).toInt()
        return UpdateRequestMessage(indexId, archiveId, packet.opcode == PRIORITY_FILE_REQUEST)
    }

}