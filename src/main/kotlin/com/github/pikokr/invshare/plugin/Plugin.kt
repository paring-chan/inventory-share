package com.github.pikokr.invshare.plugin

import com.google.common.collect.ImmutableList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import net.minecraft.server.v1_16_R3.*
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.WorldSaveEvent
import java.io.File
import kotlin.math.min

class Plugin : JavaPlugin(), Listener {
    private val items: NonNullList<ItemStack>
    private val armor: NonNullList<ItemStack>
    private val extraSlots: NonNullList<ItemStack>
    private val contents: List<NonNullList<ItemStack>>

    init {
        val inv = PlayerInventory(null)
        items = inv.items
        armor = inv.armor
        extraSlots = inv.extraSlots
        contents = ImmutableList.of(items, armor, extraSlots)
    }

    override fun onEnable() {
        server.pluginManager.apply {
            registerEvents(this@Plugin, this@Plugin)
        }
        load()
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        fun Any.setField(name: String, value: Any) {
            val field = javaClass.getDeclaredField(name).apply {
                isAccessible = true
            }

            field.set(this, value)
        }

        val ep = (e.player as CraftPlayer).handle
        val inv = ep.inventory
        inv.setField("items", items)
        inv.setField("armor", armor)
        inv.setField("extraSlots", extraSlots)
        inv.setField("f", contents)
    }

    override fun onDisable() {
        save()
    }

    private fun load() {
        val file = File(dataFolder, "inv.yml").also { if (!it.exists()) return }

        val yml = YamlConfiguration.loadConfiguration(file)
        yml.loadItemStackList("items", items)
        yml.loadItemStackList("armor", armor)
        yml.loadItemStackList("extraSlots", extraSlots)
    }


    @Suppress("UNCHECKED_CAST")
    private fun ConfigurationSection.loadItemStackList(name: String, list: NonNullList<ItemStack>) {
        val map = this.getMapList(name)
        val items = map.map { CraftItemStack.asNMSCopy(CraftItemStack.deserialize(it as Map<String, Any>)) }

        for (i in 0 until min(list.count(), items.count())) {
            list[i] = items[i]
        }
    }

    private fun saveYML() : YamlConfiguration {
        val yaml = YamlConfiguration()

        fun ConfigurationSection.setItemStackList(name: String, list: NonNullList<ItemStack>) {
            set(name, list.map { CraftItemStack.asCraftMirror(it).serialize() })
        }

        yaml.setItemStackList("items", items)
        yaml.setItemStackList("armor", armor)
        yaml.setItemStackList("extraSlots", extraSlots)

        return yaml
    }

    private fun save() {
        val yml = saveYML()
        val df = dataFolder.also { it.mkdirs() }
        val file = File(df, "inv.yml")
        yml.save(file)
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        saveYML()
    }

    @EventHandler
    fun onSave(e: WorldSaveEvent) {
        save()
    }
}