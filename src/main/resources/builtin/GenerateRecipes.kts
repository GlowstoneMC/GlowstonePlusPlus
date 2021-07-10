/**
 * This script generates all 1.13 vanilla recipes except banner, potion and firework recipes.
 * This file is under MIT License.
 * THE FILE AUTHOR: KisaragiEffective@github.com
 */
import java.io.File

data class Smelt(val result: String, val exp: Double)

fun void(@Suppress("UNUSED_PARAMETER") value: Any?) {
    /* dispose the value: do nothing */
}

fun die(message: String? = null): Nothing {
    throw Exception("Called die: ${message ?: "unknown"}")
}

fun <T> missingMapKey(key: T): Nothing {
    die("map key `$key` is missing")
}

fun smelting(fileName: String, ingredient: String, result: String = fileName, exp: Double, tickToCook: Int = 200) {
    with(File(dest, "$fileName.json")) {
        createNewFile()
        with(printWriter()) {
            println("""
{
    "type": "smelting",
    "ingredient": {
        "item": "minecraft:$ingredient"
    },
    "result": "minecraft:$result",
    "experience": $exp,
    "cookingtime": $tickToCook
}
            """.trimMargin())
            flush()
        }
    }
}

fun crafting(fileName: String, json: String) {
    with(File(dest, "$fileName.json")) {
        createNewFile()
        with(printWriter()) {
            println(json.trimMargin())
            flush()
        }
    }
}

fun shapedCraft(fileName: String, pattern1: String, pattern2: String? = null, pattern3: String? = null,
                subst: Map</* Template char -> actual material */ String, String>, result: String = fileName, amount: Int = 1) {
    val tab = "    "
    val dqf = {es: String -> """"$es""""}
    val pat = tab + tab + dqf(pattern1) + if (pattern2 != null) {
        ",\n"
    } else {
        ""
    } + if (pattern2 != null) {
        tab + tab + dqf(pattern2)
    } else {
        ""
    } + if (pattern3 != null) {
        ",\n"
    } else {
        ""
    } + if (pattern3 != null) {
        tab + tab + dqf(pattern3)
    } else {
        ""
    }

    val mapComputed = subst.map {
        tab + tab + dqf(it.key) + ": {\n" +
        tab + tab + tab + dqf("item") + ": " + dqf("minecraft:${it.value}") + '\n' +
        tab + tab + "}"
    }.joinToString(",\n")

    crafting(fileName, """
{
    "type": "crafting_shaped",
    "pattern": [
$pat
    ],
    "key": {
$mapComputed
    },
    "result": {
        "item": "minecraft:$result",
        "count": $amount
    }
}
""")
}

fun shapelessCraft(fileName: String, vararg ingredients: String, result: String = fileName, amount: Int = 1) {
    val t = "        " // sp * 8
    val computed =
        ingredients.joinToString(",\n") { """
        |$t{
        |$t    "item": "minecraft:$it"
        |$t}
        """.trimMargin()
        }

    crafting(fileName, """
{
    "type": "minecraft:crafting_shapeless",
    "ingredients": [
$computed
    ],
    "result": {
        "item": "minecraft:$result",
        "count": $amount
    }
}
""")
}

fun block2x2(fileName: String, ingredient: String, result: String = fileName, amount: Int = 1) {
    shapedCraft(fileName, "##", "##", subst = mapOf("#" to ingredient), result = result, amount = amount)
}

fun block3x3(fileName: String, ingredient: String, result: String = fileName, amount: Int = 1) {
    shapedCraft(fileName, "###", "###", "###", mapOf("#" to ingredient), result, amount)
}

val dest = File("""C:\Users\Obsidian550D\Documents\intellij\glowstone\src\main\resources\builtin\datapack\data\minecraft\recipes\""")
val planks = "planks"

// Mapping
val wood = setOf("oak", "spruce", "birch", "jungle", "acacia", "dark_oak")
val color = setOf(
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray",
        "cyan", "purple", "blue", "brown", "green", "red", "black"
)
val ore = setOf("coal", "iron_ingot", "golden_ingot", "lapis_lazuli", "redstone_dust", "diamond", "emerald")
/**
 * ingredient id -> result id
 */
val oreMap = mapOf("coal" to "coal_block", "iron_ingot" to "iron_block", "golden_ingot" to "gold_block", "lapis_lazuli" to "lapis_block", "redstone_dust" to "redstone_block", "diamond" to "diamond_block", "emerald" to "emerald_block")
val toolRank = mapOf(
        planks to "wooden", "stone" to "stone", "iron_ingot" to "iron", "golden_ingot" to "golden",
        "diamond" to "diamond"
)
val tools = setOf("axe", "pickaxe", "shovel", "sword", "hoe")
val oreSmelting = mapOf(
    "coal_ore" to Smelt("coal", 0.1),
    "iron_ore" to Smelt("iron_ingot", 0.7),
    "gold_ore" to Smelt("golden_ingot", 1.0),
    "lapis_ore" to Smelt("lapis_lazuli", 0.2),
    "redstone_ore" to Smelt("redstone_dust", 0.7),
    "diamond_ore" to Smelt("diamond", 1.0),
    "emerald_ore" to Smelt("emerald", 1.0)
)
// https://minecraft.gamepedia.com/Smelting
// TODO: Foods cooking
val furnaceMap = mapOf(
        "cactus" to Smelt("green_dye", 1.0)
) +
        wood.map { Pair("${it}_log", Smelt("charcoal", 0.15)) }.toMap() +
        color.map { Pair("${it}_terracotta", Smelt("${it}_glazed_terracotta_", 0.1)) } +
        oreSmelting
val dye = mapOf(
    "white" to "bone_meal", "orange" to "orange_dye", "magenta" to "magenta_dye", "light_blue" to "light_blue_dye", "yellow" to "yellow_dye",
    "lime" to "lime_dye", "pink" to "pink_dye", "gray" to "gray_dye", "light_gray" to "light_gray_dye", "cyan" to "cyan_dye",
    "purple" to "purple_dye", "blue" to "lapis_lazuli", "brown" to "cocoa_beans", "green" to "green_dye", "red" to "red_dye", "black" to "ink_sac"
)

/** name -> actual material */
val armorIngredients = mapOf("leather" to "leather", "fire" to "chainmail", "iron_ingot" to "iron", "golden_ingot" to "gold", "diamond" to "diamond")
val armors = setOf("helmet", "chestplate", "leggings", "boots")

// force update
dest.walk().forEach { it.delete() }
void(dest.mkdirs())

furnaceMap.forEach {
    smelting(it.value.result, it.key, exp = it.value.exp)
}

for (kind in wood) {
    // planks
    shapedCraft("${kind}_planks", "#", subst = mapOf("#" to "${kind}_log"), amount = 4)

    // slabs
    shapedCraft("${kind}_slab", "###", subst = mapOf("#" to "${kind}_planks"), amount = 6)

    // stairs - (mirrored recipes will be auto generated)
    shapedCraft("${kind}_stairs", "#", "##", "###", mapOf("#" to "${kind}_planks"), amount = 4)

    // pressure plates
    shapedCraft("${kind}_pressure_plates", "##", subst = mapOf("#" to "${kind}_planks"))

    // fences
    shapedCraft("${kind}_fence", "#/#", "#/#", subst = mapOf("#" to "${kind}_planks", "/" to "stick"), amount = 3)

    // fence gates
    shapedCraft("${kind}_fence_gate", "/#/", "/#/", subst = mapOf("#" to "${kind}_planks", "/" to "stick"))

    // doors
    shapedCraft("${kind}_door", "##", "##", "##", mapOf("#" to "${kind}_planks"), amount = 3)

    // buttons
    shapedCraft("${kind}_button", "#", subst = mapOf("#" to "${kind}_planks"))

    // boats
    shapedCraft("${kind}_boat", "# #", "###", subst = mapOf("#" to "${kind}_planks"))
}
println("Generic wood recipes completed")

for (kind in oreMap) {
    // blocks -> ingredients
    shapelessCraft("${kind.value}_unblock", kind.value, result = kind.key, amount = 9)

    // ingredients -> blocks
    block3x3(kind.value, kind.key, amount = 9)
}
println("Generic ore recipes completed")

// generic colored things
for (c in color) {
    val cur = dye[c] ?: error("Expected key $c in dye mapping.")
    shapelessCraft("${c}_wool", "white_wool", cur)
    shapedCraft("${c}_terracotta", "###", "#d#", "###", mapOf("#" to "terracotta", "d" to cur), amount = 8)
    shapedCraft("${c}_stained_glass", "###", "#d#", "###", mapOf("#" to "glass", "d" to cur), amount = 8)
    shapedCraft("${c}_concrete_poweder", "sgs", "gdg", "sgs", mapOf("s" to "sand", "g" to "gravel", "d" to cur))
    shapedCraft("${c}_carpet", "##", subst = mapOf("#" to "${c}_wool"), amount = 3)
    shapedCraft("${c}_stained_glass_pane", "###", "###", "###", mapOf("#" to "${c}_stained_glass"), amount = 16)
    // (re)taint recipes
    // FIXME: Numbers of file will be O(n^2); where n is `color` size
    for (c2 in color) {
        shapelessCraft("${c2}_shulker_box_from_${c}", "shulker_box", cur, result = "${c2}_shulker_box")
    }
    shapedCraft("${c}_bed", "###", "///", subst = mapOf("#" to "${c}_wool", "/" to planks))
    shapelessCraft("${c}_bed", "white_bed", cur)

    shapedCraft("${c}_banner", "###", "###" , " / ", mapOf("#" to "${c}_wool", "/" to "stick"))
}
println("Colored thing recipes completed")

// dyes
run {
    // If not found -> key is missing -> "die"
    fun dyeValue(key: String) = dye[key] ?: missingMapKey(key)

    // red
    run {
        val cur = dyeValue("red")
        shapelessCraft("${cur}_1", "poppy", result = cur)
        shapelessCraft("${cur}_2", "red_tulip", result = cur)
        shapelessCraft("${cur}_3", "rose_bush", result = cur, amount = 2)
        shapelessCraft("${cur}_4", "beetroot", result = cur)
    }

    // orange
    run {
        val cur = dyeValue("orange")
        shapelessCraft("${cur}_1", "orange_tulip", result = cur)
        shapelessCraft("${cur}_2", dyeValue("red"), dyeValue("yellow"), result = cur, amount = 2)
    }

    // yellow
    run {
        val cur = dyeValue("yellow")
        shapelessCraft("${cur}_1", "dandelion", result = cur)
        shapelessCraft("${cur}_2", "sunflower", result = cur, amount = 2)
    }

    // lime
    run {
        val cur = dyeValue("lime")
        smelting("${cur}_1", "sea_pickle", cur, 0.2)
        shapelessCraft("${cur}_2", dyeValue("green"), dyeValue("white"), result = cur, amount = 2)
    }

    // green
    run {
        val cur = dyeValue("green")
        smelting("${cur}_1", "cacatus", result = cur, exp = 0.2)
    }

    // cyan
    run {
        val cur = dyeValue("cyan")
        shapelessCraft("${cur}_1", dyeValue("green"), dyeValue("blue"), result = cur, amount = 2)
    }

    // blue: skipped -> it's just Lapis lazuli

    // light blue
    run {
        val cur = dyeValue("light_blue")
        shapelessCraft("${cur}_1", "blue_orchid", cur)
        shapelessCraft("${cur}_2", dyeValue("blue"), dyeValue("white"), result = cur, amount = 2)
    }

    // purple
    run {
        val cur = dyeValue("purple")
        shapelessCraft("${cur}_1", dyeValue("blue"), dyeValue("red"), result = cur, amount = 2)
    }

    // magenta
    run {
        val cur = dyeValue("magenta")
        shapelessCraft("${cur}_1", "allium", cur)
        shapelessCraft("${cur}_2", "lilac", cur, amount = 2)
        shapelessCraft("${cur}_3", dyeValue("purple"), dyeValue("pink"), result = cur, amount = 2)
        shapelessCraft("${cur}_4", dyeValue("purple"), dyeValue("red"), dyeValue("white"), result = cur, amount = 3)
        shapelessCraft("${cur}_5", dyeValue("red"), dyeValue("blue"), dyeValue("pink"), result = cur, amount = 3)
        shapelessCraft("${cur}_6", dyeValue("red"), dyeValue("blue"), dyeValue("red"), dyeValue("white"), result = cur, amount = 4)
    }

    // pink
    run {
        val cur = dyeValue("pink")
        shapelessCraft("${cur}_1", "pink_tulip", result = cur)
        shapelessCraft("${cur}_2", "peony", result = cur, amount = 2)
        shapelessCraft("${cur}_3", dyeValue("white"), dyeValue("red"), result = cur, amount = 2)
    }

    // white: skipped -> it's just bone-meal

    // light gray
    run {
        val cur = dyeValue("light_gray")
        shapelessCraft("${cur}_1", "azure_bluet", result = cur)
        shapelessCraft("${cur}_2", "oxeye_daisy", result = cur)
        shapelessCraft("${cur}_3", dyeValue("gray"), dyeValue("white"), result = cur, amount = 2)
        shapelessCraft("${cur}_4", dyeValue("black"), dyeValue("white"), dyeValue("white"), result = cur, amount = 3)
    }

    // gray
    run {
        val cur = dyeValue("gray")
        shapelessCraft("${cur}_1", dyeValue("white"), dyeValue("black"), result = cur, amount = 2)
    }

    // black: skipped -> it's just ink_sac

    // brown: skipped -> it's just cocoa_beans
}
println("Dye recipes completed")

// tools
for (k in toolRank) {
    for (l in tools) {
        val r1 = when (l) {
            "axe" -> "##"
            "pickaxe" -> "###"
            "sword" -> "#"
            "shovel" -> "#"
            "hoe" -> "##"
            else -> error("Unknown $l")
        }

        val r2 = when (l) {
            "axe" -> "#/"
            "pickaxe" -> " / "
            "sword" -> "/"
            "shovel" -> "/"
            "hoe" -> " /"
            else -> error("Unknown $l")
        }

        val r3 = when (l) {
            "axe" -> " /"
            "pickaxe" -> " / "
            "sword" -> "/"
            "shovel" -> "/"
            "hoe" -> " /"
            else -> error("Unknown $l")
        }
        shapedCraft(k.value + l, r1, r2, r3, mapOf("#" to k.key, "/" to "stick"))
    }
}
println("Tool recipes completed")

// armors
for (k in armorIngredients) {
    for (l in armors) {
        val r1 = when (l) {
            "helmet" -> "###"
            "chestplate" -> "# #"
            "leggings" -> "###"
            "boots" -> "# #"
            else -> error("Unknown $l")
        }

        val r2 = when (l) {
            "helmet" -> "# #"
            "chestplate" -> "###"
            "leggings" -> "# #"
            "boots" -> "# #"
            else -> error("Unknown $l")
        }

        val r3 = when (l) {
            "helmet" -> null
            "chestplate" -> "###"
            "leggings" -> "# #"
            "boots" -> null
            else -> error("Unknown $l")
        }
        shapedCraft(k.value + l, r1, r2, r3, mapOf("#" to k.key))
    }
}
println("Armor recipe completed")

// other recipes
// base
shapedCraft("stick", "#", "#", subst = mapOf("#" to planks))
shapedCraft("torch", "c", "/", subst = mapOf("c" to "coal", "/" to "stick"), amount = 4)
block2x2("crafting_table", planks)
shapedCraft("furnace", "###", "# #", "###", mapOf("#" to "cobblestone"))
shapedCraft("chest", "###", "# #", "###", mapOf("#" to planks))

//block
run {
    block2x2("glowstone", "glowstone_dust")
    block2x2("white_wool", "string")
    shapedCraft("tnt", "sgs", "gsg", "sgs", mapOf("s" to "sand", "g" to "gun_powder"))
    val slab = mapOf("stone" to "smooth_stone_slab", "cobblestone" to "cobble_stone_slab")
    val stairs = mapOf("quartz" to "quartz_stairs", "cobblestone" to "cobblestone_stairs", "stone_bricks" to "stone_brick_stairs", "bricks" to "brick_stairs", "sandstone" to "sandstone_stairs", "red_sandstone" to "red_sandstone_stairs")
    for (s in slab) {
        shapedCraft(s.value, "###", subst = mapOf("#" to s.key), amount = 6)
    }

    for (s in stairs) {
        shapedCraft(s.value, "#", "##", "###", mapOf("#" to s.key), amount = 4)
    }

    shapedCraft("snow_block", "##", "##", subst = mapOf("#" to "snowball"))
    block3x3("packed_ice", "ice")
    block3x3("blue_ice", "packed_ice")
    block2x2("clay", "clay_ball")
    block2x2("stone_bricks", "stone")
    shapedCraft("stone_bricks", "##", "##", subst = mapOf("#" to "stone"))
    shapedCraft("mossy_stone_bricks", "#~", subst = mapOf("#" to "stone_brick", "~" to "vine"))
    shapedCraft("chiseled_stone_bricks", "_", "_", subst = mapOf("_" to "stone_brick_slab", "~" to "vine"))

    for (s in setOf("sandstone", "red_sandstone")) {
        shapedCraft("chiseled_$s", "_", "_", subst = mapOf("_" to "${s}_slab"))
        block2x2("cut_$s", s, amount = 4)
        block2x2("cut_$s", "chiseled_$s", amount = 4)
    }

    shapedCraft("bookshelf", "www", "bbb", "www", mapOf("w" to planks, "b" to "book"))
    shapedCraft("jack_o_lantern", "p", "^", subst = mapOf("p" to "carved_pumpkin", "^" to "torch"))
    block3x3("melon_block", "melon_slice")
    block2x2("quartz_block", "quartz")
    shapedCraft("hay_unblock", "h", subst = mapOf("h" to "hay_block"), result = "wheat", amount = 9)
    shapedCraft("chiseled_quartz_block", "_", "_", subst = mapOf("_" to "quartz_slab"))
    shapedCraft("pillar_quartz_block", "_", "_", subst = mapOf("_" to "quartz"))
    block3x3("hay_block", "wheat")
    block3x3("slime_block", "slime_ball")
    shapedCraft("granite", "d", "q", subst = mapOf("d" to "diorite", "q" to "quartz"))
    shapedCraft("andesite", "c", "d", subst = mapOf("d" to "diorite", "c" to "cobblestone"))
    shapedCraft("diorite", "cq", "qc", subst = mapOf("q" to "quartz", "c" to "cobblestone"))
    for (s in setOf("granite", "andesite", "diorite")) {
        block2x2("polished_$s", s, amount = 4)
    }
    shapedCraft("coarse_dirt", "dg", "gd", subst = mapOf("d" to "dirt", "g" to "gravel"))
    shapedCraft("granite", "cv", subst = mapOf("c" to "cobblestone", "v" to "vine"))
    block2x2("prismarine", "prismarine_shard")
    block3x3("prismarine_block", "prismarine_shard")
    shapedCraft("dark_prismarine", "###", "#d#", "###", mapOf("#" to "prismarine_shard", "d" to "ink_sac"))
    shapedCraft("sea_lantern", "#o#", "ooo", "#o#", mapOf("#" to "prismarine_shard", "o" to "prismarine_crystals"))
    block2x2("end_stone_bricks", "end_stone", amount = 4)
    block2x2("prismarine", "prismarine_shard", amount = 4)
    block2x2("magma_block", "magma_cream")
    block3x3("nether_wart_block", "nether_wart")
    shapedCraft("red_nether_bricks", "wb", "bw", subst = mapOf("w" to "nether_wart", "b" to "nether_brick"))
}
println("Block recipes completed")

// transport
run {
    shapedCraft("minecart", "i i", "iii", subst = mapOf("i" to "iron_ingot"))
    shapedCraft("furnace_minecart", "m", "f", subst = mapOf("m" to "minecart", "f" to "furnace"))
    shapedCraft("chest_minecart", "m", "c", subst = mapOf("m" to "minecart", "c" to "chest"))
    shapedCraft("hopper_minecart", "m", "h", subst = mapOf("m" to "minecart", "h" to "hopper"))
    shapedCraft("tnt_minecart", "m", "t", subst = mapOf("m" to "minecart", "t" to "tnt"))
    shapedCraft("rail", "i i", "i/i", "i i", mapOf("i" to "iron_ingot", "/" to "stick"), amount = 16)
    shapedCraft("powered_rail", "g g", "g/g", "g*g", mapOf("g" to "golden_ingot", "/" to "stick", "*" to "redstone_dust"), amount = 6)
    shapedCraft("detector_rail", "i i", "i-i", "i*i", mapOf("i" to "golden_ingot", "-" to "stone_pressure_plate", "*" to "redstone_dust"), amount = 6)
    shapedCraft("activator_rail", "i/i", "i^i", "i/i", mapOf("i" to "golden_ingot", "/" to "stick", "^" to "redstone_torch"), amount = 6)
    // boat is done.
}
println("Transport recipes completed")

// machine
run {
    // wooden door is done.
    shapedCraft("iron_door", "ii", "ii", "ii", mapOf("i" to "iron_ingot"), amount = 3)
    // wooden trapdoor is done.
    shapedCraft("iron_trapdoor", "ii", "ii", subst = mapOf("i" to "iron_ingot"))
    shapedCraft("heavy_weighted_pressure_plate", "ii", subst = mapOf("i" to "iron_ingot"))
    shapedCraft("light_weighted_pressure_plate", "gg", subst = mapOf("g" to "golden_ingot"))
    shapedCraft("stone_button", "s", subst = mapOf("s" to "stone"))
    shapedCraft("lever", "/", "s", subst = mapOf("s" to "stone", "/" to "stick"))
    shapedCraft("redstone_torch", "r", "/", subst = mapOf("r" to "redstone", "/" to "stick"))
    shapedCraft("redstone_repeater", "trt", "sss", subst = mapOf("t" to "redstone_torch", "r" to "redstone", "s" to "stone"))
    shapedCraft("repeater", "trt", "sss", subst = mapOf("t" to "redstone_torch", "r" to "redstone", "s" to "stone"))
    shapedCraft("comparator", "t", "tqt", "sss", subst = mapOf("t" to "redstone_torch", "q" to "quartz", "s" to "stone"))
    shapedCraft("jukebox", "ppp", "pdp", "ppp", subst = mapOf("p" to planks, "d" to "diamond"))
    shapedCraft("note_block", "ppp", "prp", "ppp", subst = mapOf("p" to planks, "r" to "redstone"))
    shapedCraft("dropper", "ccc", "c c", "crc", subst = mapOf("c" to "cobblestone", "r" to "redstone"))
    shapedCraft("dispenser", "ccc", "cbc", "crc", subst = mapOf("c" to "cobblestone", "b" to "bow", "r" to "redstone"))
    shapedCraft("piston", "ppp", "cic", "crc", mapOf("p" to planks, "c" to "cobblestone", "i" to "iron_ingot", "r" to "redstone"))
    shapedCraft("sticky_piston", "s", "p", subst = mapOf("p" to "piston", "s" to "slime_ball"))
    shapedCraft("redstone_lamp", " r ", "rgr", " r ", subst = mapOf("r" to "redstone_dust", "g" to "glowstone"))
    shapedCraft("trapwire_hook", "i", "/", "p", mapOf("i" to "iron_ingot", "/" to "stick", "p" to planks))
    shapelessCraft("trapped_chest", "chest", "trapwire_hook")
    shapedCraft("hopper", "i i", "ici", " i ", mapOf("i" to "iron_ingot", "c" to "chest"))
    shapedCraft("daylight_detector", "ggg", "qqq", "sss", mapOf("g" to "glass", "q" to "quartz", "s" to "oak_slab"))
    shapedCraft("observer", "ccc", "rrq", "ccc", mapOf("c" to "cobblestone", "q" to "quartz", "r" to "redstone_dust"))
}
println("Redstone recipes completed")

// food
run {
    shapedCraft("bowl", "p p", " p ", subst = mapOf("p" to planks), amount = 4)
    shapedCraft("cookie", "wcw", subst = mapOf("w" to "wheat", "c" to "cocoa_beans"), amount = 8)
    shapedCraft("cookie", "wcw", subst = mapOf("w" to "wheat", "c" to "cocoa_beans"), amount = 8)
    shapedCraft("mushroom_stew", "r", "b", "o", subst = mapOf("r" to "red_mushroom", "b" to "brown_mushroom", "o" to "bowl"))
    shapedCraft("bread", "www", subst = mapOf("w" to "wheat"), amount = 3)
    shapedCraft("golden_apple", "ggg", "gag", "ggg", mapOf("g" to "golden_ingot", "a" to "apple"))
    shapedCraft("sugar", "ggg", "gag", "ggg", mapOf("g" to "golden_ingot", "a" to "apple"))
}
println("Food recipes completed")

// items
run {
    shapelessCraft("book", "leather", "paper", "paper", "paper")
}
println("Item recipes completed")
println("All recipes completed. Quit.")

// EOF
