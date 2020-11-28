package com.github.pikokr.invshare.plugin

import com.google.common.collect.ImmutableList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.WorldSaveEvent
import org.bukkit.inventory.ItemStack
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.AbstractList

class Plugin : JavaPlugin() {
    // 해당 플러그인에 필요한, 서버 버전마다 달라지는 클래스들
    private lateinit var craftItemStackClass: Class<*>
    private lateinit var craftPlayerClass: Class<*>
    private lateinit var entityHumanClass: Class<*>
    private lateinit var itemStackClass: Class<*>
    private lateinit var nonNullListClass: Class<*>
    private lateinit var playerInventoryClass: Class<*>

    // net.minecraft.server.NonNullList extends java.util.AbstractList
    // NonNullList 는 서버 버전마다 달라지는 net.minecraft.server 클래스이므로 부모 클래스 사용
    private lateinit var items: AbstractList<*>
    private lateinit var armor: AbstractList<*>
    private lateinit var extraSlots: AbstractList<*>
    private lateinit var contents: List<AbstractList<*>>

// <init> 은 Bukkit Initialization 이전에 <init>을 호출하면 Bukkit 기능을
// 사용할 수 없기 때문에 JavaPlugin 을 상속하는 클래스에서는 사용하지 않는 것이 좋습니다.
//    init {
//        val inv = PlayerInventory(null)
//        items = inv.items
//        armor = inv.armor
//        extraSlots = inv.extraSlots
//        contents = ImmutableList.of(items, armor, extraSlots)
//    }

    // 리플렉션 필드 가져오기 (필드의 가시성 (public, protected, private 등) 과 상관없이 가져옴
    private fun Class<*>.field(name: String): Field {
        return getDeclaredField(name).apply {
            isAccessible = true
        }
    }

    // 리플렉션 메서드 가져오기 (필드의 가시성 (public, protected, private 등) 과 상관없이 가져옴
    private fun Class<*>.method(name: String, vararg parameterTypes: Class<*>): Method {
        return getDeclaredMethod(name, *parameterTypes).apply {
            isAccessible = true
        }
    }

    override fun onEnable() {
        // `v1_xx_Rx` 형태의 Minecraft 버전 반환
        val version = with("v\\d+_\\d+_R\\d+".toPattern().matcher(Bukkit.getServer()::class.java.`package`.name)) {
            when {
                find() -> group() //
                else -> throw NoSuchElementException("버전을 찾을 수 없습니다.")
            }
        }

        // 버전을 onEnable 밖에 선언하는 것을 방지하기 위해 onEnable 내부에 메소드 정의

        // net.minecraft.server 클래스 가져오기
        fun nms(className: String): Class<*> {
            return Class.forName("net.minecraft.server.$version.$className")
        }

        // org.bukkit.craftbukkit 클래스 가져오기
        fun craftBukkit(className: String): Class<*> {
            return Class.forName("org.bukkit.craftbukkit.$version.$className")
        }

        // 위에서 lateinit 으로 생성해둔 클래스들 정의하기
        craftItemStackClass = craftBukkit("inventory.CraftItemStack")
        craftPlayerClass = craftBukkit("entity.CraftPlayer")
        entityHumanClass = nms("EntityHuman")
        itemStackClass = nms("ItemStack")
        nonNullListClass = nms("NonNullList")
        playerInventoryClass = nms("PlayerInventory")

        // PlayerInventory 클래스의 EntityHuman 타입을 파라미터로 갖는 클래스 생성 -> PlayerInventory(null)
        val inv = playerInventoryClass.getConstructor(entityHumanClass).newInstance(null)

        inv.javaClass.run { // this: java.lang.Class<*> (= Class<PlayerInventory>)
            items = field("items").get(inv) as AbstractList<*>
            armor = field("armor").get(inv) as AbstractList<*>
            extraSlots = field("extraSlots").get(inv) as AbstractList<*>
        }

        contents = ImmutableList.of(items, armor, extraSlots)

        // Listener 클래스 분리
        server.pluginManager.registerEvents(PluginListener(), this)

        load()
    }

    override fun onDisable() {
        save()
    }

    private fun load() {
        @Suppress("UNCHECKED_CAST")
        fun ConfigurationSection.loadItemStackList(name: String, list: AbstractList<*>) {
            val items = getMapList(name).map { args ->
                val itemStack = ItemStack.deserialize(args as Map<String, Any>)

                // net.minecraft.server.ItemStack 객체 가져오기 -> CraftItemStack::asNMSCopy
                craftItemStackClass.method("asNMSCopy", ItemStack::class.java).invoke(null, itemStack)
            } as List<*>

            for (i in 0 until minOf(list.count(), items.count())) {
                nonNullListClass.method("set", Int::class.java, Any::class.java).invoke(list, i, items[i])
            }
        }

        val file = File(dataFolder, "inv.yml").also { if (!it.exists()) return }

        val yml = YamlConfiguration.loadConfiguration(file)
        yml.loadItemStackList("items", items)
        yml.loadItemStackList("armor", armor)
        yml.loadItemStackList("extraSlots", extraSlots)
    }

    private fun save() {
        fun ConfigurationSection.setItemStackList(name: String, list: AbstractList<*>) {
            set(name, list.map { itemStack -> // itemStack: net.minecraft.server.ItemStack
                // org.bukkit.inventory.ItemStack 객체 가져오기 -> CraftItemStack::asCraftMirror
                val craftItemStack = craftItemStackClass.method("asCraftMirror", itemStackClass).invoke(null, itemStack)

                (craftItemStack as ItemStack).serialize()
            })
        }

        val yaml = YamlConfiguration()

        yaml.setItemStackList("items", items)
        yaml.setItemStackList("armor", armor)
        yaml.setItemStackList("extraSlots", extraSlots)

        val dataFolder = dataFolder.also { it.mkdirs() }
        val file = File(dataFolder, "inv.yml")
        yaml.save(file)
    }

    @Suppress("UNUSED_PARAMETER")
    private inner class PluginListener : Listener {
        @EventHandler
        fun onJoin(event: PlayerJoinEvent) {
            // net.minecraft.server.EntityHuman 객체 가져오기 -> CraftPlayer::getHandle
            val entityHuman = craftPlayerClass.method("getHandle").invoke(event.player)

            // net.minecraft.server.PlayerInventory 객체 가져오기 -> EntityHuman::inventory
            val inventory = entityHumanClass.field("inventory").get(entityHuman)

            inventory.javaClass.run { // this: java.lang.Class<*> (= Class<PlayerInventory>)
                field("items").set(inventory, items)
                field("armor").set(inventory, armor)
                field("extraSlots").set(inventory, extraSlots)
                field("f").set(inventory, contents)
            }
        }

        @EventHandler
        fun onPlayerQuit(event: PlayerQuitEvent) {
            save()
        }

        @EventHandler
        fun onSave(event: WorldSaveEvent) {
            save()
        }
    }
}