package world.gregs.voidps.world.interact.entity.player.combat.magic.spell

import world.gregs.voidps.engine.entity.character.npc.NPC
import world.gregs.voidps.engine.entity.character.player.Player
import world.gregs.voidps.engine.entity.character.player.chat.ChatType
import world.gregs.voidps.engine.entity.character.update.visual.setAnimation
import world.gregs.voidps.engine.entity.character.update.visual.setGraphic
import world.gregs.voidps.engine.entity.definition.SpellDefinitions
import world.gregs.voidps.engine.entity.hasEffect
import world.gregs.voidps.engine.entity.start
import world.gregs.voidps.engine.event.Priority
import world.gregs.voidps.engine.event.on
import world.gregs.voidps.network.encode.message
import world.gregs.voidps.utility.inject
import world.gregs.voidps.world.interact.entity.combat.CombatSwing
import world.gregs.voidps.world.interact.entity.combat.hit
import world.gregs.voidps.world.interact.entity.combat.spell
import world.gregs.voidps.world.interact.entity.proj.shoot

val definitions: SpellDefinitions by inject()

on<CombatSwing>({ player -> !swung() && player.spell == "teleport_block" }, Priority.LOW) { player: Player ->
    if (target is NPC) {
        delay = -1
        player.message("Nothing interesting happens.")
        return@on
    }
    val spell = player.spell
    player.setAnimation("${spell}_cast")
    player.setGraphic("${spell}_cast")
    player.shoot(name = player.spell, target = target)
    if (player.hit(target) != -1) {
        if (target.hasEffect(spell)) {
            player.message("This player is already effected by this spell.", ChatType.GameFilter)
        } else if (!target.hasEffect(spell)) {
            val protect = target.hasEffect("prayer_deflect_magic") || target.hasEffect("prayer_protect_from_magic")
            val duration: Int = definitions.get(player.spell)["block_ticks"]
            target.start(spell, if (protect) duration / 2 else duration)
        }
    }
    delay = 5
}