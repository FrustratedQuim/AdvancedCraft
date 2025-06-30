package ratger.kotlinSimple

import org.bukkit.Material
import org.bukkit.util.Vector

object StructureDefinitions {
    // Пылающий слиток (Пылающая кирка)
    private val MOLTEN_STRUCTURE: List<Pair<Vector, Material>> = listOf(
        Pair(Vector(0.0, -1.0, 0.0), Material.MAGMA_BLOCK),
        Pair(Vector(1.0, -1.0, 0.0), Material.OBSIDIAN),
        Pair(Vector(1.0, -1.0, 1.0), Material.OBSIDIAN),
        Pair(Vector(1.0, -1.0, -1.0), Material.OBSIDIAN),
        Pair(Vector(0.0, -1.0, 1.0), Material.OBSIDIAN),
        Pair(Vector(0.0, -1.0, -1.0), Material.OBSIDIAN),
        Pair(Vector(-1.0, -1.0, 1.0), Material.OBSIDIAN),
        Pair(Vector(-1.0, -1.0, -1.0), Material.OBSIDIAN)
    )

    // Слиток рибдита (Древняя кирка)
    private val ANCIENT_STRUCTURE: List<Pair<Vector, Material>> = listOf(
        Pair(Vector(0.0, -1.0, 0.0), Material.ANCIENT_DEBRIS),
        Pair(Vector(1.0, -1.0, 0.0), Material.ANCIENT_DEBRIS),
        Pair(Vector(-1.0, -1.0, 0.0), Material.ANCIENT_DEBRIS),
        Pair(Vector(0.0, -1.0, 1.0), Material.ANCIENT_DEBRIS),
        Pair(Vector(0.0, -1.0, -1.0), Material.ANCIENT_DEBRIS),

        Pair(Vector(0.0, -2.0, 0.0), Material.GOLD_BLOCK),
        Pair(Vector(1.0, -2.0, 0.0), Material.GOLD_BLOCK),
        Pair(Vector(1.0, -2.0, 1.0), Material.GOLD_BLOCK),
        Pair(Vector(1.0, -2.0, -1.0), Material.GOLD_BLOCK),
        Pair(Vector(0.0, -2.0, 1.0), Material.GOLD_BLOCK),
        Pair(Vector(0.0, -2.0, -1.0), Material.GOLD_BLOCK),
        Pair(Vector(-1.0, -2.0, 1.0), Material.GOLD_BLOCK),
        Pair(Vector(-1.0, -2.0, -1.0), Material.GOLD_BLOCK)
    )

    // Слиток деккарита (Пробуждённая кирка)
    private val AWAKENED_STRUCTURE: List<Pair<Vector, Material>> = buildList {
        add(Pair(Vector(0.0, -1.0, 0.0), Material.DIAMOND_BLOCK))
        add(Pair(Vector(1.0, -1.0, 0.0), Material.DIAMOND_BLOCK))
        add(Pair(Vector(1.0, -1.0, 1.0), Material.DIAMOND_BLOCK))
        add(Pair(Vector(1.0, -1.0, -1.0), Material.DIAMOND_BLOCK))
        add(Pair(Vector(0.0, -1.0, 1.0), Material.DIAMOND_BLOCK))
        add(Pair(Vector(0.0, -1.0, -1.0), Material.DIAMOND_BLOCK))
        add(Pair(Vector(-1.0, -1.0, 1.0), Material.DIAMOND_BLOCK))
        add(Pair(Vector(-1.0, -1.0, -1.0), Material.DIAMOND_BLOCK))

        add(Pair(Vector(2.0, -1.0, 2.0), Material.LIGHTNING_ROD))
        add(Pair(Vector(-2.0, -1.0, 2.0), Material.LIGHTNING_ROD))
        add(Pair(Vector(2.0, -1.0, -2.0), Material.LIGHTNING_ROD))
        add(Pair(Vector(-2.0, -1.0, -2.0), Material.LIGHTNING_ROD))

        // 4x4 квадрат эндерняка
        for (dx in -2..2) {
            for (dz in -2..2) {
                add(Pair(Vector(dx.toDouble(), -2.0, dz.toDouble()), Material.END_STONE))
            }
        }
    }

    // Слиток ауралка (Верховная кирка)
    private val SACRED_STRUCTURE: List<Pair<Vector, Material>> = listOf(
        Pair(Vector(0.0, -1.0, 0.0), Material.LODESTONE),

        Pair(Vector(3.0, -1.0, 0.0), Material.OBSIDIAN),
        Pair(Vector(3.0, 0.0, 0.0), Material.AMETHYST_BLOCK),
        Pair(Vector(3.0, 1.0, 0.0), Material.AMETHYST_BLOCK),
        Pair(Vector(3.0, 2.0, 0.0), Material.OBSIDIAN),

        Pair(Vector(-3.0, -1.0, 0.0), Material.OBSIDIAN),
        Pair(Vector(-3.0, 0.0, 0.0), Material.AMETHYST_BLOCK),
        Pair(Vector(-3.0, 1.0, 0.0), Material.AMETHYST_BLOCK),
        Pair(Vector(-3.0, 2.0, 0.0), Material.OBSIDIAN),

        Pair(Vector(0.0, -1.0, 3.0), Material.OBSIDIAN),
        Pair(Vector(0.0, 0.0, 3.0), Material.AMETHYST_BLOCK),
        Pair(Vector(0.0, 1.0, 3.0), Material.AMETHYST_BLOCK),
        Pair(Vector(0.0, 2.0, 3.0), Material.OBSIDIAN),

        Pair(Vector(0.0, -1.0, -3.0), Material.OBSIDIAN),
        Pair(Vector(0.0, 0.0, -3.0), Material.AMETHYST_BLOCK),
        Pair(Vector(0.0, 1.0, -3.0), Material.AMETHYST_BLOCK),
        Pair(Vector(0.0, 2.0, -3.0), Material.OBSIDIAN)
    )

    private val structures: Map<Int, List<Pair<Vector, Material>>> = mapOf(
        1001 to MOLTEN_STRUCTURE,
        1002 to ANCIENT_STRUCTURE,
        1003 to AWAKENED_STRUCTURE,
        1004 to SACRED_STRUCTURE
    )

    fun getStructure(variant: Int): List<Pair<Vector, Material>>? = structures[variant]
}