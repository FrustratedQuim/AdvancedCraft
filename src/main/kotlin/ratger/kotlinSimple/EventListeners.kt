package ratger.kotlinSimple

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import net.kyori.adventure.text.Component

class EventListeners : Listener {

    @EventHandler(priority = EventPriority.LOW)
    fun onEntityDamage(event: EntityDamageEvent) {
        CraftingManager.onItemBurn(event)
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val blockLoc = event.block.location
        if (CraftingManager.isWorldBlocked(blockLoc.world) && CraftingManager.isBlockBlocked(blockLoc)) {
            event.isCancelled = true
        }

        // Удалить при финальном билде, существует для теста
        if (event.block.type == Material.BEDROCK) {
            val player = event.player
            val cmdList = listOf(1001, 1002, 1003, 1004)
            for (cmd in cmdList) {
                val item = ItemStack(Material.TURTLE_SCUTE, 1)
                val meta: ItemMeta? = item.itemMeta
                if (meta != null) {
                    meta.displayName(Component.text("Предмет №$cmd"))
                    meta.setCustomModelData(cmd)
                    item.itemMeta = meta
                }
                player.inventory.addItem(item)
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        for (block in event.blocks) {
            val loc = block.location
            if (CraftingManager.isWorldBlocked(loc.world) && CraftingManager.isBlockBlocked(loc)) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        for (block in event.blocks) {
            val loc = block.location
            if (CraftingManager.isWorldBlocked(loc.world) && CraftingManager.isBlockBlocked(loc)) {
                event.isCancelled = true
                return
            }
        }
    }

    // Добавлено на всякий случай, чтобы не забирали воронками/вагонетками и прочим дерьмом с быстрым подбором
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onItemPickup(event: EntityPickupItemEvent) {
        val itemEntity = event.item
        val uuid = itemEntity.uniqueId
        if (CraftingManager.isWorldBlocked(itemEntity.world) && CraftingManager.isItemBlocked(uuid)) {
            event.isCancelled = true
        }
    }
}