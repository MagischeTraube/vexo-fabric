package xyz.vexo.features.impl.misc.recipe

import com.google.common.collect.HashMultimap
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ResolvableProfile
import xyz.vexo.Vexo
import xyz.vexo.utils.ApiUtils
import java.io.File
import java.util.UUID
import kotlinx.coroutines.launch
import xyz.vexo.utils.IInitializable
import net.minecraft.world.item.component.DyedItemColor

object RecipeRepository : IInitializable{

    private const val RECIPES_API = "${Vexo.VEXO_API}/recipes"
    private val CACHE_FILE = File(Vexo.configDir, "recipes_cache.json")


    data class RemoteRecipe(
        val id: String,
        val name: String,
        val type: String,
        val material: String?,
        val itemModel: String?,
        val textureValue: String?,
        val textureSignature: String?,
        val dyedColor: Int?
    )

    private var cache: List<RemoteRecipe>? = null
    private var cachedVersion: Int = -1

    override fun init() {
        Vexo.scope.launch {
            syncRecipes()
        }
    }

    private suspend fun syncRecipes() {
        try {
            val remote = ApiUtils.fetchJsonWithRetry(RECIPES_API)

            if (remote == null) {
                loadFromDisk()
                return
            }

            val remoteVersion =
                remote.getAsJsonObject("data")
                    ?.get("version")
                    ?.asInt
                    ?: -1


            if (!CACHE_FILE.exists()) {
                saveToDisk(remote)
                cache = parseResponse(remote)
                cachedVersion = remoteVersion
                return
            }


            val localJson = Vexo.gson.fromJson(
                CACHE_FILE.readText(),
                JsonObject::class.java
            )

            val localVersion =
                localJson.getAsJsonObject("data")
                    ?.get("version")
                    ?.asInt
                    ?: -1


            when {
                remoteVersion > localVersion -> {
                    saveToDisk(remote)
                    cache = parseResponse(remote)
                    cachedVersion = remoteVersion
                }

                remoteVersion == localVersion -> {
                    loadFromDisk()
                }

                else -> {
                    loadFromDisk()
                }
            }

        } catch (e: Exception) {
            loadFromDisk()
        }
    }

    fun getRecipes(): List<RemoteRecipe> {
        return cache ?: emptyList()
    }

    private fun parseResponse(json: JsonObject): List<RemoteRecipe> {
        val data = json.getAsJsonObject("data") ?: return emptyList()
        val arr = data.getAsJsonArray("recipes") ?: JsonArray()

        return arr.mapNotNull { el ->
            try {
                val o = el.asJsonObject
                RemoteRecipe(
                    id = o.get("id").asString,
                    name = o.get("name").asString,
                    type = o.get("type")?.takeUnless { it.isJsonNull }?.asString ?: "item",
                    material = o.get("material")?.takeUnless { it.isJsonNull }?.asString,
                    itemModel = o.get("item_model")?.takeUnless { it.isJsonNull }?.asString,
                    textureValue = o.get("texture_value")?.takeUnless { it.isJsonNull }?.asString,
                    textureSignature = o.get("texture_signature")?.takeUnless { it.isJsonNull }?.asString,
                    dyedColor = o.get("dyed_color")?.takeUnless { it.isJsonNull }?.asInt
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun saveToDisk(json: JsonObject) {
        try {
            CACHE_FILE.writeText(json.toString())
        } catch (e: Exception) { }
    }

    private fun loadFromDisk(): List<RemoteRecipe>? {
        return try {
            if (!CACHE_FILE.exists()) return null

            val json = Vexo.gson.fromJson(
                CACHE_FILE.readText(),
                JsonObject::class.java
            )

            cachedVersion =
                json.getAsJsonObject("data")
                    ?.get("version")
                    ?.asInt
                    ?: cachedVersion

            val recipes = parseResponse(json)

            cache = recipes

            recipes

        } catch (e: Exception) {
            null
        }
    }

    fun buildItemStack(r: RemoteRecipe): ItemStack {
        val stack = when (r.type) {
            "player_head" -> buildPlayerHead(r)
            else -> buildSimpleItem(r)
        }

        if (r.name.isNotBlank()) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(r.name))
        }

        return stack
    }

    private fun buildSimpleItem(r: RemoteRecipe): ItemStack {
        val stack = ItemStack(resolveItem(r.material))

        applySkyblockData(stack, r.id)

        r.dyedColor?.let {
            stack.set(
                DataComponents.DYED_COLOR,
                DyedItemColor(it)
            )
        }

        r.itemModel?.let {
            val parts = it.split(":", limit = 2)
            val namespace = if (parts.size == 2) parts[0] else "minecraft"
            val path = if (parts.size == 2) parts[1] else parts[0]

            stack.set(
                DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath(namespace, path)
            )
        }

        return stack
    }

    private fun buildPlayerHead(r: RemoteRecipe): ItemStack {
        val stack = ItemStack(Items.PLAYER_HEAD)

        applySkyblockData(stack, r.id)

        if (r.textureValue != null) {
            val propertyMap = HashMultimap.create<String, Property>().apply {
                put("textures", Property("textures", r.textureValue, r.textureSignature))
            }
            val profile = GameProfile(UUID.randomUUID(), "", PropertyMap(propertyMap))
            stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile))
        }

        return stack
    }

    private fun applySkyblockData(stack: ItemStack, id: String) {
        val tag = CompoundTag().apply {
            putString("id", id)
            val pbv = CompoundTag().apply {
                putString("hypixel_skyblock:id", id)
            }
            put("PublicBukkitValues", pbv)
        }

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
    }

    private fun resolveItem(material: String?): Item {
        val loc = Identifier.fromNamespaceAndPath(
            "minecraft",
            (material ?: "barrier").lowercase()
        )
        return BuiltInRegistries.ITEM.getOptional(loc).orElse(null) ?: Items.BARRIER
    }
}