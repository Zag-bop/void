package world.gregs.voidps.world.activity.quest

import world.gregs.voidps.engine.client.Minimap
import world.gregs.voidps.engine.client.clearMinimap
import world.gregs.voidps.engine.client.minimap
import world.gregs.voidps.engine.client.ui.close
import world.gregs.voidps.engine.client.ui.open
import world.gregs.voidps.engine.entity.character.player.PlayerContext
import world.gregs.voidps.engine.get
import world.gregs.voidps.engine.map.chunk.DynamicChunks
import world.gregs.voidps.engine.map.instance.Instances
import world.gregs.voidps.engine.map.region.Region

private val tabs = listOf(
    "combat_styles",
    "task_system",
    "stats",
    "quest_journals",
    "inventory",
    "worn_equipment",
    "prayer_list",
    "modern_spellbook",
    "emotes",
    "notes"
)

fun PlayerContext.startCutscene(region: Region): Region {
    val instance = Instances.small()
    get<DynamicChunks>().copy(region, instance)
    hideTabs()
    return instance
}

fun PlayerContext.hideTabs() {
    tabs.forEach {
        player.close(it)
    }
    player.minimap(Minimap.HideMap)
}

fun PlayerContext.stopCutscene(instance: Region) {
    Instances.free(instance)
    get<DynamicChunks>().clear(instance)
    player.open("fade_in")
    showTabs()
}

fun PlayerContext.showTabs() {
    tabs.forEach {
        player.open(it)
    }
    player.clearMinimap()
}