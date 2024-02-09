package world.gregs.voidps.world.interact.entity.player.equip

import com.github.michaelbull.logging.InlineLogger
import world.gregs.voidps.cache.definition.data.ItemDefinition
import world.gregs.voidps.engine.client.ui.closeInterfaces
import world.gregs.voidps.engine.client.ui.event.interfaceRefresh
import world.gregs.voidps.engine.client.ui.interfaceOption
import world.gregs.voidps.engine.client.ui.open
import world.gregs.voidps.engine.entity.character.player.Player
import world.gregs.voidps.engine.inv.sendInventory
import world.gregs.voidps.network.visual.update.player.EquipSlot

interfaceRefresh("worn_equipment") { player: Player ->
    player.sendInventory(id)
}

interfaceOption("Show Equipment Stats", "bonuses", "worn_equipment") {
//    player.setVar("equipment_banking", false)
    player.open("equipment_bonuses")
}

interfaceOption("Show Price-checker", "price", "worn_equipment") {
    player.open("price_checker")
}

interfaceOption("Show Items Kept on Death", "items", "worn_equipment") {
    player.open("items_kept_on_death")
}

val logger = InlineLogger()

interfaceOption(component = "*_slot", id = "worn_equipment") {
    val equipOption = getEquipmentOption(item.def, optionIndex)
    if (equipOption == null) {
        logger.info { "Unhandled equipment option $item - $optionIndex" }
        return@interfaceOption
    }
    val slot = EquipSlot.by(component.removeSuffix("_slot"))
    player.closeInterfaces()
    player.events.emit(InventoryOption(player, id, item, slot.index, equipOption))
}

fun getEquipmentOption(itemDef: ItemDefinition, optionId: Int): String? {
    val equipOption: String? = itemDef.getOrNull<Map<Int, String>>("worn_options")?.get(optionId - 1)
    if (equipOption != null) {
        return equipOption
    }
    return when (optionId) {
        0 -> "Remove"
        9 -> "Examine"
        else -> null
    }
}