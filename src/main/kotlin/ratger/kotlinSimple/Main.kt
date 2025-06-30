package ratger.kotlinSimple

import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {

    override fun onEnable() {
        SQLManager.setupSQL()
        server.pluginManager.registerEvents(EventListeners(), this)
        CommandManager.registerCommands(this) // Обработка /acraft
    }

    override fun onDisable() {
        CraftingManager.forceShutdown()
    }
}
