package com.github.pikokr.invshare.plugin

import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class Plugin : JavaPlugin(), Listener {
    override fun onEnable() {
        server.pluginManager.apply {
            registerEvents(this@Plugin, this@Plugin)
        }
    }
}