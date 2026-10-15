package com.example.plantcarecompanion.domain

object LocalPlantCatalog {

    private data class Row(val name: String, val scientific: String, val days: Int, val light: String)

    private val rows = listOf(
        Row("Snake Plant", "Dracaena trifasciata", 14, "Low to bright indirect light"),
        Row("Pothos", "Epipremnum aureum", 7, "Low to bright indirect light"),
        Row("Monstera", "Monstera deliciosa", 7, "Bright indirect light"),
        Row("Peace Lily", "Spathiphyllum wallisii", 7, "Low to medium indirect light"),
        Row("Spider Plant", "Chlorophytum comosum", 7, "Bright indirect light"),
        Row("ZZ Plant", "Zamioculcas zamiifolia", 14, "Low to bright indirect light"),
        Row("Aloe Vera", "Aloe vera", 14, "Bright light, some direct sun"),
        Row("Jade Plant", "Crassula ovata", 14, "Full sun"),
        Row("Rubber Plant", "Ficus elastica", 7, "Bright indirect light"),
        Row("Fiddle Leaf Fig", "Ficus lyrata", 7, "Bright indirect light"),
        Row("Boston Fern", "Nephrolepis exaltata", 3, "Medium indirect light"),
        Row("Heartleaf Philodendron", "Philodendron hederaceum", 7, "Medium indirect light"),
        Row("Cactus", "Cactaceae", 21, "Full sun"),
        Row("Echeveria", "Echeveria elegans", 14, "Full sun"),
        Row("English Ivy", "Hedera helix", 7, "Bright indirect light, part shade"),
        Row("Chinese Evergreen", "Aglaonema commutatum", 10, "Low to medium indirect light"),
        Row("Dragon Tree", "Dracaena marginata", 10, "Medium indirect light"),
        Row("Calathea", "Goeppertia makoyana", 5, "Medium indirect light"),
        Row("Moth Orchid", "Phalaenopsis amabilis", 7, "Bright indirect light"),
        Row("African Violet", "Streptocarpus ionanthus", 5, "Bright indirect light"),
        Row("Money Tree", "Pachira aquatica", 10, "Bright indirect light"),
        Row("Bird of Paradise", "Strelitzia reginae", 7, "Bright light, some direct sun"),
        Row("Christmas Cactus", "Schlumbergera truncata", 10, "Bright indirect light"),
        Row("Peperomia", "Peperomia obtusifolia", 10, "Bright indirect light"),
        Row("Hoya", "Hoya carnosa", 14, "Bright indirect light"),
        Row("Lucky Bamboo", "Dracaena sanderiana", 7, "Low to medium indirect light"),
        Row("Basil", "Ocimum basilicum", 2, "Full sun"),
        Row("Mint", "Mentha spicata", 3, "Full sun to part shade"),
        Row("Rosemary", "Salvia rosmarinus", 7, "Full sun"),
        Row("Lavender", "Lavandula angustifolia", 7, "Full sun"),
    )

    val species: List<Species> = rows.mapIndexed { index, row ->
        Species(
            id = -(index + 1),
            commonName = row.name,
            scientificName = row.scientific,
            wateringIntervalDays = row.days,
            wateringLabel = null,
            lightRequirement = row.light,
            imageUrl = null,
            source = SpeciesSource.BUILT_IN
        )
    }

    fun byId(id: Int): Species? = species.firstOrNull { it.id == id }

    fun search(query: String): List<Species> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return species
        return species.filter {
            it.commonName.lowercase().contains(q) || (it.scientificName?.lowercase()?.contains(q) == true)
        }
    }
}
