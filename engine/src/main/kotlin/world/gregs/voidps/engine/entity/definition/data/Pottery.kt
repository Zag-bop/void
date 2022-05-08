package world.gregs.voidps.engine.entity.definition.data

data class Pottery(
    val list: Map<String, Ceramic> = emptyMap()
) {

    data class Ceramic(
        val level: Int = 1,
        val xp: Double = 0.0
    ) {
        companion object {

            @Suppress("UNCHECKED_CAST")
            operator fun invoke(map: Map<String, Any>) = Ceramic(
                level = map["level"] as? Int ?: EMPTY.level,
                xp = map["xp"] as? Double ?: EMPTY.xp,
            )

            val EMPTY = Ceramic()
        }
    }

    companion object {

        @Suppress("UNCHECKED_CAST")
        operator fun invoke(map: Map<String, Any>) = Pottery(map.mapValues { Ceramic(it.value as Map<String, Any>) })

        val EMPTY = Pottery()
    }
}