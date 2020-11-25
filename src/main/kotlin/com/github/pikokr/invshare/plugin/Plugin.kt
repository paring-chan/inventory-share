package com.github.pikokr.invshare.plugin

import com.google.common.collect.ImmutableList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import net.minecraft.server.v1_16_R3.*

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
    }
}