package world.gregs.voidps.engine.entity.character.player.skill.level

import world.gregs.voidps.engine.entity.character.player.skill.Skill
import world.gregs.voidps.engine.event.Event

/**
 * Notification when current skill level has changed.
 * @see [MaxLevelChanged]
 */
data class CurrentLevelChanged(val skill: Skill, val from: Int, val to: Int) : Event