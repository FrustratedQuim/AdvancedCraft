package ratger.kotlinSimple

import org.bukkit.*
import org.bukkit.block.Chest
import org.bukkit.entity.Item
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.Bukkit
import java.util.*

object CraftingManager {
    private val ITEM_MATERIAL = Material.TURTLE_SCUTE
    private val CMD_LIST = setOf(1001, 1002, 1003, 1004)
    private val blockedBlocks: MutableSet<Location> = mutableSetOf()
    private val blockedItems: MutableSet<UUID> = mutableSetOf()
    private val blockedInWorlds: MutableSet<World> = mutableSetOf()
    private val blockedPositions: MutableSet<Triple<Int, Int, Int>> = mutableSetOf()

    private data class ActiveCraft(
        val task: BukkitRunnable,
        val variant: Int,
        val center: Location,
        val blockedLocs: List<Location>,
        val itemUUID: UUID,
        val remainingItems: ItemStack?,
        var isCompleted: Boolean = false
    )

    private val activeCrafts: MutableList<ActiveCraft> = mutableListOf()

    // Удаление блока с вызовом партиклов разрушения, без ванильного дропа
    private fun extraBreak(location: Location) {
        val block = location.block
        val material = block.type
        if (material != Material.AIR) {
            location.world?.spawnParticle(Particle.BLOCK, location.add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.0, material.createBlockData())
            block.type = Material.AIR
        }
    }

    fun onItemBurn(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity !is Item) return

        val cause = event.cause
        if (cause != EntityDamageEvent.DamageCause.FIRE && cause != EntityDamageEvent.DamageCause.FIRE_TICK) {
            return
        }

        val itemStack = entity.itemStack
        if (itemStack.type != ITEM_MATERIAL) return

        val meta: ItemMeta = itemStack.itemMeta ?: return
        if (!meta.hasCustomModelData()) return

        val cmd = meta.customModelData
        if (cmd !in CMD_LIST) return

        event.isCancelled = true
        val itemUUID = entity.uniqueId
        blockedItems.add(itemUUID)
        entity.remove()

        val centerLoc = entity.location.block.location
        val world = centerLoc.world ?: return
        val fixedCenter = findFireBlock(world, centerLoc) ?: centerLoc
        val blockCenter = Triple(fixedCenter.blockX, fixedCenter.blockY, fixedCenter.blockZ)

        val nearbyPositions = mutableSetOf<Triple<Int, Int, Int>>()
        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    nearbyPositions.add(Triple(blockCenter.first + dx, blockCenter.second + dy, blockCenter.third + dz))
                }
            }
        }

        val found = nearbyPositions.any { blockedPositions.contains(it) }
        val usedItems = ItemStack(ITEM_MATERIAL, itemStack.amount).apply {
            itemMeta = meta.clone()
        }

        if (found) {
            extraBreak(fixedCenter)
            blockedItems.remove(itemUUID)
            world.dropItemNaturally(fixedCenter, itemStack)
            return
        }

        extraBreak(fixedCenter)
        scanStructure(cmd, fixedCenter, itemUUID, usedItems)
    }

    private fun findFireBlock(world: World, center: Location): Location? {
        val x = center.blockX
        val y = center.blockY
        val z = center.blockZ
        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    val block = world.getBlockAt(x + dx, y + dy, z + dz)
                    if (block.type == Material.FIRE || block.type == Material.SOUL_FIRE) {
                        return Location(
                            world,
                            (x + dx).toDouble(),
                            (y + dy).toDouble(),
                            (z + dz).toDouble()
                        )
                    }
                }
            }
        }
        return null
    }

    private fun scanStructure(
        variant: Int,
        center: Location,
        itemUUID: UUID,
        usedItems: ItemStack?
    ) {
        val world = center.world ?: return
        val struct = StructureDefinitions.getStructure(variant) ?: return

        val tempLocations = mutableListOf<Location>()
        for ((offset, correctMaterial) in struct) {
            val block = world.getBlockAt(
                center.blockX + offset.blockX,
                center.blockY + offset.blockY,
                center.blockZ + offset.blockZ
            )

            if (block.type != correctMaterial) {
                tempLocations.clear()
                blockedItems.remove(itemUUID)
                usedItems?.let { world.dropItemNaturally(center, it) }
                return
            }
            tempLocations.add(block.location)
        }

        val blockCenter = Triple(center.blockX, center.blockY, center.blockZ)
        blockedPositions.add(blockCenter)

        blockedBlocks.addAll(tempLocations)
        blockedInWorlds.add(world)

        val remainingItems = if (usedItems!!.amount > 1) {
            ItemStack(ITEM_MATERIAL, usedItems.amount - 1).apply {
                itemMeta = usedItems.itemMeta.clone()
            }
        } else null

        SQLManager.logCraft(variant, center)
        startCrafting(variant, center, tempLocations, itemUUID, remainingItems)
    }

    private fun startCrafting(
        variant: Int,
        center: Location,
        blockedLocs: List<Location>,
        itemUUID: UUID,
        remainingItems: ItemStack?
    ) {
        val world = center.world ?: return
        val plugin = Bukkit.getPluginManager().getPlugin("AdvancedCraft")!!
        val steps = getCraftingSteps(variant, world, center, blockedLocs, itemUUID, remainingItems)

        val task = object : BukkitRunnable() {
            var ticks = 0L
            override fun run() {
                steps.firstOrNull { it.first == ticks }?.second?.invoke()
                ticks++
            }
        }

        activeCrafts.add(ActiveCraft(task, variant, center, blockedLocs, itemUUID, remainingItems))
        task.runTaskTimer(plugin, 0L, 1L)
    }

    private fun getCraftingSteps(
        variant: Int,
        world: World,
        center: Location,
        blockedLocs: List<Location>,
        itemUUID: UUID,
        remainingItems: ItemStack?
    ): List<Pair<Long, () -> Unit>> {
        fun msToTicks(ms: Long): Long = ms / 50

        return when (variant) {
            1001 -> listOf(
                0L to {},
                msToTicks(2100) to { world.spawnParticle(Particle.SMOKE, center.x, center.y + 0.2, center.z, 50, 0.6, 0.1, 0.6, 0.02) },
                msToTicks(2850) to { world.playSound(center, Sound.ENTITY_BREEZE_DEATH, 1.0f, 1.0f) },
                msToTicks(2900) to { breakArea(world, center, 1, -1) },
                msToTicks(3700) to { world.spawnParticle(Particle.FLAME, center.x, center.y + 2.2, center.z, 100, 0.0, 0.0, 0.0, 0.15) },
                msToTicks(3850) to { world.playSound(center, Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.0f) },
                msToTicks(3900) to { dropResultItem(world, center, variant, remainingItems, blockedLocs, itemUUID, 2.0) }
            )
            1002 -> listOf(
                0L to {},
                msToTicks(2000) to {
                    world.playSound(center, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.0f, 1.0f)
                    world.spawnParticle(Particle.FLAME, center.x, center.y + 3.5, center.z, 10, 0.1, 0.1, 0.1, 0.02)
                },
                msToTicks(2150) to { world.spawnParticle(Particle.FLAME, center.x, center.y + 2.5, center.z, 20, 0.3, 0.1, 0.3, 0.02) },
                msToTicks(2300) to { world.spawnParticle(Particle.FLAME, center.x, center.y + 1.5, center.z, 30, 0.4, 0.1, 0.4, 0.02) },
                msToTicks(2450) to { world.spawnParticle(Particle.FLAME, center.x, center.y + 0.5, center.z, 30, 0.6, 0.1, 0.6, 0.02) },
                msToTicks(2600) to { world.spawnParticle(Particle.FLAME, center.x, center.y - 0.5, center.z, 40, 0.75, 0.1, 0.75, 0.02) },
                msToTicks(2700) to { breakArea(world, center, 1, -1) },
                msToTicks(2750) to { world.spawnParticle(Particle.FLAME, center.x, center.y - 1.5, center.z, 50, 0.9, 0.1, 0.9, 0.02) },
                msToTicks(2850) to { breakArea(world, center, 1, -2) },
                msToTicks(2900) to { dropResultItem(world, center, variant, remainingItems, blockedLocs, itemUUID, -1.5) }
            )
            1003 -> listOf(
                0L to {},
                msToTicks(2500) to { strikeAndBreak(world, center, -1.5, -1.0, -1.5, -2, -1, -2) },
                msToTicks(3150) to { strikeAndBreak(world, center, -1.5, -1.0, 1.5, -2, -1, 2) },
                msToTicks(3800) to { strikeAndBreak(world, center, 1.5, -1.0, 1.5, 2, -1, 2) },
                msToTicks(4450) to { strikeAndBreak(world, center, 1.5, -1.0, -1.5, 2, -1, -2) },
                msToTicks(5350) to { world.playSound(center, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f) },
                msToTicks(5400) to { breakArea(world, center, 1, -1) },
                msToTicks(6700) to {
                    world.playSound(center, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 1.0f)
                    world.spawnParticle(Particle.SONIC_BOOM, center.x, center.y + 2.0, center.z, 1, 0.0, 0.0, 0.0, 0.0)
                },
                msToTicks(7200) to { dropResultItem(world, center, variant, remainingItems, blockedLocs, itemUUID, 1.3) }
            )
            1004 -> listOf(
                0L to {},
                msToTicks(2500) to { world.spawnParticle(Particle.SONIC_BOOM, center.x + 3.0, center.y + 0.5, center.z, 1, 0.0, 0.0, 0.0, 0.0) },
                msToTicks(3000) to {
                    val cx = center.blockX
                    val cy = center.blockY
                    val cz = center.blockZ
                    extraBreak(world.getBlockAt(cx + 3, cy, cz).location)
                    extraBreak(world.getBlockAt(cx + 3, cy + 1, cz).location)
                    world.playSound(center, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1.0f, 1.0f)
                    world.spawnParticle(Particle.SONIC_BOOM, center.x - 3.0, center.y + 0.5, center.z, 1, 0.0, 0.0, 0.0, 0.0)
                },
                msToTicks(3500) to {
                    val cx = center.blockX
                    val cy = center.blockY
                    val cz = center.blockZ
                    extraBreak(world.getBlockAt(cx - 3, cy, cz).location)
                    extraBreak(world.getBlockAt(cx - 3, cy + 1, cz).location)
                    world.playSound(center, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1.0f, 1.0f)
                    world.spawnParticle(Particle.SONIC_BOOM, center.x, center.y + 0.5, center.z + 3.0, 1, 0.0, 0.0, 0.0, 0.0)
                },
                msToTicks(4000) to {
                    val cx = center.blockX
                    val cy = center.blockY
                    val cz = center.blockZ
                    extraBreak(world.getBlockAt(cx, cy, cz + 3).location)
                    extraBreak(world.getBlockAt(cx, cy + 1, cz + 3).location)
                    world.playSound(center, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1.0f, 1.0f)
                    world.spawnParticle(Particle.SONIC_BOOM, center.x, center.y + 0.5, center.z - 3.0, 1, 0.0, 0.0, 0.0, 0.0)
                },
                msToTicks(4500) to {
                    val cx = center.blockX
                    val cy = center.blockY
                    val cz = center.blockZ
                    extraBreak(world.getBlockAt(cx, cy, cz - 3).location)
                    extraBreak(world.getBlockAt(cx, cy + 1, cz - 3).location)
                    world.playSound(center, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1.0f, 1.0f)
                },
                msToTicks(5000) to { world.playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 1.0f) },
                msToTicks(6500) to {
                    world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f)
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, center.clone().add(0.0, 1.0, 0.0), 5, 0.0, 0.0, 0.0, 0.0)
                },
                msToTicks(6600) to { extraBreak(center.clone().add(0.0, -1.0, 0.0)) },
                msToTicks(6650) to { dropResultItem(world, center, variant, remainingItems, blockedLocs, itemUUID, 1.0) }
            )
            else -> listOf(0L to { cleanupAfterCraft(world, blockedLocs, itemUUID, center) })
        }
    }

    private fun breakArea(world: World, center: Location, offset: Int, yOffset: Int) {
        val cx = center.blockX
        val cy = center.blockY + yOffset
        val cz = center.blockZ
        for (dx in -offset..offset) {
            for (dz in -offset..offset) {
                extraBreak(world.getBlockAt(cx + dx, cy, cz + dz).location)
            }
        }
    }

    private fun strikeAndBreak(
        world: World, center: Location,
        strikeX: Double, strikeY: Double, strikeZ: Double,
        blockX: Int, blockY: Int, blockZ: Int
    ) {
        world.strikeLightningEffect(center.clone().add(strikeX, strikeY, strikeZ))
        extraBreak(world.getBlockAt(
            center.blockX + blockX,
            center.blockY + blockY,
            center.blockZ + blockZ
        ).location)
    }

    private fun dropResultItem(
        world: World,
        center: Location,
        variant: Int,
        remainingItems: ItemStack?,
        blockedLocs: List<Location>,
        itemUUID: UUID,
        yOffset: Double
    ) {
        val resultItem = ItemStack(ITEM_MATERIAL, 1).apply {
            itemMeta = itemMeta?.apply { setCustomModelData(variant + 1000) }
        }

        world.dropItemNaturally(center.clone().add(0.0, 0.0 + yOffset, 0.0), resultItem)
        remainingItems?.let { world.dropItemNaturally(center.clone().add(0.0, 0.0 + yOffset, 0.0), it) }
        cleanupAfterCraft(world, blockedLocs, itemUUID, center)

        activeCrafts.find {
            it.itemUUID == itemUUID && it.center == center
        }?.isCompleted = true
    }

    fun forceShutdown() {
        val uniqueCrafts = activeCrafts.distinctBy { it.itemUUID to it.center }
        activeCrafts.clear()

        for (craft in uniqueCrafts) {
            if (craft.isCompleted) continue
            val world = craft.center.world ?: continue
            craft.task.cancel()

            when (craft.variant) {
                1001 -> { breakArea(world, craft.center, 1, 1) }
                1002 -> {
                    breakArea(world, craft.center, 1, 1)
                    breakArea(world, craft.center, 1, 2)
                }
                1003 -> {
                    breakArea(world, craft.center, 1, 1)
                    val cx = craft.center.blockX
                    val cy = craft.center.blockY - 1
                    val cz = craft.center.blockZ
                    world.getBlockAt(cx + 2, cy, cz + 2).type = Material.AIR
                    world.getBlockAt(cx - 2, cy, cz + 2).type = Material.AIR
                    world.getBlockAt(cx + 2, cy, cz - 2).type = Material.AIR
                    world.getBlockAt(cx - 2, cy, cz - 2).type = Material.AIR
                }
                1004 -> {
                    val cx = craft.center.blockX
                    val cy = craft.center.blockY
                    val cz = craft.center.blockZ
                    world.getBlockAt(cx, cy - 1, cz).type = Material.AIR
                    world.getBlockAt(cx + 3, cy, cz).type = Material.AIR
                    world.getBlockAt(cx + 3, cy + 1, cz).type = Material.AIR
                    world.getBlockAt(cx - 3, cy, cz).type = Material.AIR
                    world.getBlockAt(cx - 3, cy + 1, cz).type = Material.AIR
                    world.getBlockAt(cx, cy, cz + 3).type = Material.AIR
                    world.getBlockAt(cx, cy + 1, cz + 3).type = Material.AIR
                    world.getBlockAt(cx, cy, cz - 3).type = Material.AIR
                    world.getBlockAt(cx, cy + 1, cz - 3).type = Material.AIR
                }
            }

            val chestLocation = craft.center.block
            chestLocation.type = Material.CHEST

            val chestInventory = (chestLocation.state as? Chest)?.inventory ?: continue
            chestInventory.clear()

            val resultItem = ItemStack(ITEM_MATERIAL, 1).apply {
                itemMeta = itemMeta?.apply { setCustomModelData(craft.variant + 1000) }
            }

            chestInventory.addItem(resultItem)
            craft.remainingItems?.let { remaining ->
                chestInventory.addItem(remaining).values.forEach { leftover ->
                    world.dropItemNaturally(craft.center, leftover)
                }
            }

            SQLManager.logCraft(craft.variant, craft.center)
            cleanupAfterCraft(world, craft.blockedLocs, craft.itemUUID, craft.center)
        }
    }

    private fun cleanupAfterCraft(
        world: World,
        blockedLocs: List<Location>,
        itemUUID: UUID,
        center: Location
    ) {
        blockedBlocks.removeAll(blockedLocs.toSet())
        blockedItems.remove(itemUUID)
        blockedPositions.remove(Triple(center.blockX, center.blockY, center.blockZ))

        if (blockedBlocks.none { it.world == world }) {
            blockedInWorlds.remove(world)
        }

        activeCrafts.removeAll { it.itemUUID == itemUUID }
    }

    fun isBlockBlocked(location: Location): Boolean = blockedBlocks.contains(location)
    fun isItemBlocked(itemUUID: UUID): Boolean = blockedItems.contains(itemUUID)
    fun isWorldBlocked(world: World): Boolean = blockedInWorlds.contains(world)
}