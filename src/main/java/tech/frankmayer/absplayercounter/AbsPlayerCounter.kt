package tech.frankmayer.absplayercounter

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getPluginManager
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.lang.Exception
import java.nio.charset.StandardCharsets
import java.util.logging.Level

class AbsPlayerCounter : JavaPlugin(), Listener, CommandExecutor, TabCompleter {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configPath = "plugins/AbsPlayerCounter/"
    private val configFilePath = configPath + "config.json"
    private val configFile = File(configFilePath)
    private var config: Config? = null
    private val signs = arrayOf(Material.ACACIA_SIGN, Material.ACACIA_WALL_SIGN, Material.BIRCH_SIGN, Material.BIRCH_WALL_SIGN, Material.CRIMSON_SIGN, Material.CRIMSON_WALL_SIGN, Material.DARK_OAK_SIGN, Material.DARK_OAK_WALL_SIGN, Material.JUNGLE_SIGN, Material.JUNGLE_WALL_SIGN, Material.OAK_SIGN, Material.OAK_WALL_SIGN, Material.SPRUCE_SIGN, Material.SPRUCE_WALL_SIGN, Material.WARPED_SIGN, Material.WARPED_WALL_SIGN)

    override fun onEnable() {
        try {
            val p = File(configPath)
            if (!p.exists()) {
                p.mkdirs()
            }

            if (configFile.exists() && !configFile.isDirectory) {
                val json = configFile.readText(Charsets.UTF_8)
                val type = object : TypeToken<Config>() {}.type
                config = gson.fromJson(json, type)
            }
        } catch (e: Exception) {
            Bukkit.getLogger().log(Level.WARNING, "Invalid Config!")
        }

        getPluginManager().registerEvents(this, this)
        super.onEnable()
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String>? {
        return if (sender is Player && command.name == "makecountersign") {
            val lookAt = sender.getTargetBlock(setOf(Material.AIR, Material.CAVE_AIR, Material.VOID_AIR, Material.TALL_GRASS, Material.GRASS, Material.BARRIER), 64).location
            when (args.count()) {
                1 -> mutableListOf("${lookAt.blockX}")
                2 -> mutableListOf("${lookAt.blockY}")
                3 -> mutableListOf("${lookAt.blockZ}")
                else -> null
            }
        } else {
            null
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (command.name) {
            "makecountersign" -> {
                if (args.count() != 3 || sender !is Player) {
                    return false
                }
                val x = args[0].toIntOrNull() ?: return false
                val y = args[1].toIntOrNull() ?: return false
                val z = args[2].toIntOrNull() ?: return false

                val world = sender.location.world ?: return false

                if (config == null) {
                    config = Config(world.name, x, y, z, "${ChatColor.MAGIC}==========", "${ChatColor.RESET}Du bist der", "${ChatColor.RESET}%dte Spieler", "${ChatColor.MAGIC}==========")
                } else {
                    config!!.world = world.name
                    config!!.locX = x
                    config!!.locY = y
                    config!!.locZ = z
                }

                configFile.writeText(gson.toJson(config), StandardCharsets.UTF_8)
                updateSign()
                return true
            }
        }

        return false
    }

    private fun getBlockFromConfig(): Block? {
        if (config != null) {
            val world = Bukkit.getWorld(config!!.world) ?: return null
            val block = world.getBlockAt(config!!.locX, config!!.locY, config!!.locZ)
            if (signs.contains(block.type)) {
                return block
            }
        }
        return null
    }

    private fun updateSign() {
        val block = getBlockFromConfig() ?: return
        val num = Bukkit.getOfflinePlayers().count()
        val sign = block.state as Sign
        sign.setLine(0, config!!.line1.format(num))
        sign.setLine(1, config!!.line2.format(num))
        sign.setLine(2, config!!.line3.format(num))
        sign.setLine(3, config!!.line4.format(num))
        sign.update()
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        updateSign()
    }
}
