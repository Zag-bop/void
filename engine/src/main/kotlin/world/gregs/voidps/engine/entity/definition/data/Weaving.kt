package world.gregs.voidps.engine.entity.definition.data

data class Weaving(
    val amount: Int = 1,
    val to: String = "",
    val level: Int = 1,
    val xp: Double = 0.0
) {

    companion object {

        @Suppress("UNCHECKED_CAST")
        operator fun invoke(map: Map<String, Any>) = Weaving(
            amount = map["amount"] as? Int ?: EMPTY.amount,
            to = map["to"] as? String ?: "",
            level = map["level"] as? Int ?: EMPTY.level,
            xp = map["xp"] as? Double ?: EMPTY.xp,
        )

        val EMPTY = Weaving()
    }
}