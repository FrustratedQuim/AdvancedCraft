package ratger.kotlinSimple

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.ceil

object CommandManager : CommandExecutor, TabCompleter {
    private val mm = MiniMessage.miniMessage()
    private const val CRAFTS_PER_PAGE = 10
    private const val ADMIN_PERMISSION = "advancedcraft.admin"
    private const val WARN_NO_PERMISSION = "<#FF1500>У вас недостаточно прав!"
    private const val WARN_HELP_CRAFTS = "<#FF1500>Правильное использование: /acraft crafts <page>"
    private const val WARN_EMPTY_CRAFTS = "<#FF1500>Пока-что пусто.."

    fun registerCommands(plugin: JavaPlugin) {
        plugin.getCommand("acraft")?.let {
            it.setExecutor(this)
            it.tabCompleter = this
        }
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(mm.deserialize(WARN_NO_PERMISSION))
            return true
        }

        if (args.isEmpty() || args[0] != "crafts") {
            sender.sendMessage(mm.deserialize(WARN_HELP_CRAFTS))
            return true
        }

        val page = args.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        displayCrafts(sender, page)
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!sender.hasPermission(ADMIN_PERMISSION)) return emptyList()

        return when (args.size) {
            1 -> listOf("crafts")
            2 -> {
                if (args[0] == "crafts") {
                    val totalPages = SQLManager.getTotalPages(CRAFTS_PER_PAGE)
                    (1..totalPages).map { it.toString() }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    // Пусть будет отдельными сообщениями, т.к. многие юзают показ времени перед сообщением - при одном целом будет выглядеть страшно
    private fun displayCrafts(sender: CommandSender, page: Int) {
        val crafts = SQLManager.getCrafts()
        if (crafts.isEmpty()) {
            sender.sendMessage(mm.deserialize(WARN_EMPTY_CRAFTS))
            return
        }

        val totalPages = ceil(crafts.size.toDouble() / CRAFTS_PER_PAGE).toInt()
        val currentPage = page.coerceAtMost(totalPages)
        val startIndex = (currentPage - 1) * CRAFTS_PER_PAGE
        val endIndex = minOf(startIndex + CRAFTS_PER_PAGE, crafts.size)

        sender.sendMessage(mm.deserialize("<#FF5700>----- <#FF6B00>Последние специальные крафты</#FF6B00> -----"))

        for (i in startIndex until endIndex) {
            val craft = crafts[i]
            val actualName = when (craft.variant) {
                "Molten" -> "Пылающий"
                "Ancient" -> "Древний"
                "Awakened" -> "Пробуждённый"
                "Sacred" -> "Верховный"
                else -> craft.variant
            }
            val line = " <#FF5700>[<#FFE4B3>${i+1}</#FFE4B3>]</#FF5700> <#FF8F26>$actualName<#FFE4B3> →<click:run_command:'/tppos ${craft.x} ${craft.y} ${craft.z} 0 0 ${craft.world}'><#FFBD66> ${craft.x}/${craft.y}/${craft.z} <#FF6B00>(<#FFE4B3>${craft.world}</#FFE4B3>)</click>"
            sender.sendMessage(mm.deserialize(line))
        }

        sender.sendMessage(mm.deserialize("<#FF5700>-----"))

        val pageNav = buildPageNavigation(currentPage, totalPages)
        sender.sendMessage(mm.deserialize(pageNav))
    }

    private fun buildPageNavigation(currentPage: Int, totalPages: Int): String {
        val pages = mutableListOf<Int>()
        for (i in (currentPage - 2)..(currentPage + 3)) {
            if (i in 1..totalPages) {
                pages.add(i)
            }
        }

        val pageLinks = pages.joinToString(" | ") { page ->
            if (page == currentPage) {
                "<click:run_command:'/acraft crafts $page'><hover:show_text:'/acraft crafts $page'><u><#FFE4B3>$page</#FFE4B3></u></hover></click>"
            } else {
                "<click:run_command:'/acraft crafts $page'><hover:show_text:'/acraft crafts $page'><#FFE4B3>$page</#FFE4B3></hover></click>"
            }
        }
        return "<#FF8F26>Страница <#FFE4B3>$currentPage/$totalPages ▶ <#FF6B00>($pageLinks)"
    }
}
