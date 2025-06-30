package ratger.kotlinSimple

import org.bukkit.Bukkit
import org.bukkit.Location
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.LocalDate
import java.util.logging.Logger
import kotlin.math.ceil

object  SQLManager {
    private val logger: Logger = Bukkit.getPluginManager().getPlugin("AdvancedCraft")!!.logger
    private lateinit var dbFile: File
    private const val DB_NAME = "PlayerCrafts.db"

    fun setupSQL() {
        try {
            Class.forName("org.sqlite.JDBC")
            val plugin = Bukkit.getPluginManager().getPlugin("AdvancedCraft")

            val folder = plugin?.dataFolder ?: File("plugins/AdvancedCraft").also { it.mkdirs() }
            if (!folder.exists()) folder.mkdirs()

            dbFile = File(folder, DB_NAME)
            if (!dbFile.exists()) dbFile.createNewFile()

            getConnection().use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS PlayerCrafts (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            variant TEXT NOT NULL,
                            x INTEGER NOT NULL,
                            y INTEGER NOT NULL,
                            z INTEGER NOT NULL,
                            world TEXT NOT NULL,
                            date TEXT NOT NULL
                        )
                    """.trimIndent())
                }
            }
            logger.info("Подключение к $DB_NAME успешно.")
        } catch (e: ClassNotFoundException) {
            logger.severe("Драйвер SQLite не найден: ${e.message}")
        } catch (e: SQLException) {
            logger.severe("Ошибка инициализации SQLite: ${e.message}")
        }
    }

    private fun getConnection(): Connection {
        return DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
    }

    // Логирование данных об активации алтарей (Крафты)
    fun logCraft(variant: Int, center: Location) {
        val actualName = when (variant) {
            1001 -> "Molten"
            1002 -> "Ancient"
            1003 -> "Awakened"
            1004 -> "Sacred"
            else -> "Unknown"
        }

        val x = center.blockX
        val y = center.blockY
        val z = center.blockZ
        val world = center.world?.name ?: "Unknown"
        val date = LocalDate.now().let {
            "${it.dayOfMonth}.${it.monthValue}.${it.year}"
        }

        try {
            getConnection().use { connection ->
                connection.prepareStatement("""
                    INSERT INTO PlayerCrafts (variant, x, y, z, world, date)
                    VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent()).use { statement ->
                    statement.setString(1, actualName)
                    statement.setInt(2, x)
                    statement.setInt(3, y)
                    statement.setInt(4, z)
                    statement.setString(5, world)
                    statement.setString(6, date)
                    statement.executeUpdate()
                }
            }
            logger.info("Крафт сохранён: $actualName, $x, $y, $z, $world, $date")
        } catch (e: SQLException) {
            logger.severe("Ошибка сохранения в SQLite: ${e.message}")
        }
    }

    data class CraftInfo(
        val variant: String,
        val x: Int, val y: Int, val z: Int,
        val world:  String,
        val date: String
    )

    // Получение всех записей об крафтах в алтарях
    fun getCrafts(): List<CraftInfo> {
        val crafts = mutableListOf<CraftInfo>()
        try {
            getConnection().use { connection ->
                connection.createStatement().use { statement ->
                    val resultSet = statement.executeQuery(
                        "SELECT variant, x, y, z, world, date FROM PlayerCrafts ORDER BY id DESC"
                    )
                    while (resultSet.next()) {
                        crafts.add(
                            CraftInfo(
                                variant = resultSet.getString("variant"),
                                x = resultSet.getInt("x"),
                                y = resultSet.getInt("y"),
                                z = resultSet.getInt("z"),
                                world = resultSet.getString("world"),
                                date = resultSet.getString("date")
                            )
                        )
                    }
                }
            }
        } catch (e: SQLException) {
            logger.severe("Ошибка получения крафтов из SQLite: ${e.message}")
        }
        return crafts
    }

    // Кол-во доступных страниц для табуляции
    fun getTotalPages(craftsPerPage: Int): Int {
        try {
            getConnection().use { connection ->
                connection.createStatement().use { statement ->
                    val resultSet = statement.executeQuery("SELECT COUNT(*) FROM PlayerCrafts")
                    val count = if (resultSet.next()) resultSet.getInt(1) else 0
                    return ceil(count.toDouble() / craftsPerPage).toInt()
                }
            }
        } catch (e: SQLException) {
            logger.severe("Ошибка при получении количества страниц из SQLite: ${e.message}")
            return 1
        }
    }

}