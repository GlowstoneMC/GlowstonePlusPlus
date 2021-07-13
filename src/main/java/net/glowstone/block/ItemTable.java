package net.glowstone.block;

import com.destroystokyo.paper.MaterialTags;
import com.google.common.collect.Sets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.glowstone.GlowServer;
import net.glowstone.block.blocktype.BlockAnvil;
import net.glowstone.block.blocktype.BlockBeacon;
import net.glowstone.block.blocktype.BlockBed;
import net.glowstone.block.blocktype.BlockBrewingStand;
import net.glowstone.block.blocktype.BlockButton;
import net.glowstone.block.blocktype.BlockCactus;
import net.glowstone.block.blocktype.BlockCarpet;
import net.glowstone.block.blocktype.BlockCarrot;
import net.glowstone.block.blocktype.BlockCauldron;
import net.glowstone.block.blocktype.BlockChest;
import net.glowstone.block.blocktype.BlockChorusFlower;
import net.glowstone.block.blocktype.BlockChorusPlant;
import net.glowstone.block.blocktype.BlockCocoa;
import net.glowstone.block.blocktype.BlockConcretePowder;
import net.glowstone.block.blocktype.BlockCrops;
import net.glowstone.block.blocktype.BlockDaylightDetector;
import net.glowstone.block.blocktype.BlockDeadBush;
import net.glowstone.block.blocktype.BlockDirectDrops;
import net.glowstone.block.blocktype.BlockDirt;
import net.glowstone.block.blocktype.BlockDispenser;
import net.glowstone.block.blocktype.BlockDoor;
import net.glowstone.block.blocktype.BlockDoublePlant;
import net.glowstone.block.blocktype.BlockDropless;
import net.glowstone.block.blocktype.BlockDropper;
import net.glowstone.block.blocktype.BlockEnchantmentTable;
import net.glowstone.block.blocktype.BlockEndRod;
import net.glowstone.block.blocktype.BlockEnderChest;
import net.glowstone.block.blocktype.BlockEnderPortalFrame;
import net.glowstone.block.blocktype.BlockFalling;
import net.glowstone.block.blocktype.BlockFarmland;
import net.glowstone.block.blocktype.BlockFence;
import net.glowstone.block.blocktype.BlockFenceGate;
import net.glowstone.block.blocktype.BlockFire;
import net.glowstone.block.blocktype.BlockFlowerPot;
import net.glowstone.block.blocktype.BlockFurnace;
import net.glowstone.block.blocktype.BlockGrass;
import net.glowstone.block.blocktype.BlockGrassPath;
import net.glowstone.block.blocktype.BlockGravel;
import net.glowstone.block.blocktype.BlockHay;
import net.glowstone.block.blocktype.BlockHopper;
import net.glowstone.block.blocktype.BlockHugeMushroom;
import net.glowstone.block.blocktype.BlockIce;
import net.glowstone.block.blocktype.BlockIronTrapDoor;
import net.glowstone.block.blocktype.BlockJukebox;
import net.glowstone.block.blocktype.BlockLadder;
import net.glowstone.block.blocktype.BlockLamp;
import net.glowstone.block.blocktype.BlockLava;
import net.glowstone.block.blocktype.BlockLeaves;
import net.glowstone.block.blocktype.BlockLever;
import net.glowstone.block.blocktype.BlockLog;
import net.glowstone.block.blocktype.BlockMagma;
import net.glowstone.block.blocktype.BlockMelon;
import net.glowstone.block.blocktype.BlockMobSpawner;
import net.glowstone.block.blocktype.BlockMonsterEgg;
import net.glowstone.block.blocktype.BlockMushroom;
import net.glowstone.block.blocktype.BlockMycel;
import net.glowstone.block.blocktype.BlockNeedsAttached;
import net.glowstone.block.blocktype.BlockNetherWart;
import net.glowstone.block.blocktype.BlockNote;
import net.glowstone.block.blocktype.BlockObserver;
import net.glowstone.block.blocktype.BlockOre;
import net.glowstone.block.blocktype.BlockPiston;
import net.glowstone.block.blocktype.BlockPotato;
import net.glowstone.block.blocktype.BlockPumpkin;
import net.glowstone.block.blocktype.BlockPumpkinBase;
import net.glowstone.block.blocktype.BlockPurpurPillar;
import net.glowstone.block.blocktype.BlockQuartz;
import net.glowstone.block.blocktype.BlockRails;
import net.glowstone.block.blocktype.BlockRandomDrops;
import net.glowstone.block.blocktype.BlockRedstone;
import net.glowstone.block.blocktype.BlockRedstoneComparator;
import net.glowstone.block.blocktype.BlockRedstoneOre;
import net.glowstone.block.blocktype.BlockRedstoneRepeater;
import net.glowstone.block.blocktype.BlockRedstoneTorch;
import net.glowstone.block.blocktype.BlockSapling;
import net.glowstone.block.blocktype.BlockSign;
import net.glowstone.block.blocktype.BlockSkull;
import net.glowstone.block.blocktype.BlockSlab;
import net.glowstone.block.blocktype.BlockSnow;
import net.glowstone.block.blocktype.BlockSnowBlock;
import net.glowstone.block.blocktype.BlockSponge;
import net.glowstone.block.blocktype.BlockStairs;
import net.glowstone.block.blocktype.BlockStem;
import net.glowstone.block.blocktype.BlockStone;
import net.glowstone.block.blocktype.BlockSugarCane;
import net.glowstone.block.blocktype.BlockTallGrass;
import net.glowstone.block.blocktype.BlockTnt;
import net.glowstone.block.blocktype.BlockTorch;
import net.glowstone.block.blocktype.BlockType;
import net.glowstone.block.blocktype.BlockVine;
import net.glowstone.block.blocktype.BlockWater;
import net.glowstone.block.blocktype.BlockWeb;
import net.glowstone.block.blocktype.BlockWoodenTrapDoor;
import net.glowstone.block.blocktype.BlockWorkbench;
import net.glowstone.block.data.BlockDataManager;
import net.glowstone.block.itemtype.ItemArmorStand;
import net.glowstone.block.itemtype.ItemBanner;
import net.glowstone.block.itemtype.ItemBoat;
import net.glowstone.block.itemtype.ItemBow;
import net.glowstone.block.itemtype.ItemBucket;
import net.glowstone.block.itemtype.ItemChorusFruit;
import net.glowstone.block.itemtype.ItemDye;
import net.glowstone.block.itemtype.ItemEgg;
import net.glowstone.block.itemtype.ItemEndCrystal;
import net.glowstone.block.itemtype.ItemEnderPearl;
import net.glowstone.block.itemtype.ItemExperienceBottle;
import net.glowstone.block.itemtype.ItemFilledBucket;
import net.glowstone.block.itemtype.ItemFirework;
import net.glowstone.block.itemtype.ItemFishCooked;
import net.glowstone.block.itemtype.ItemFishRaw;
import net.glowstone.block.itemtype.ItemFishingRod;
import net.glowstone.block.itemtype.ItemFlintAndSteel;
import net.glowstone.block.itemtype.ItemFood;
import net.glowstone.block.itemtype.ItemFoodSeeds;
import net.glowstone.block.itemtype.ItemGoldenApple;
import net.glowstone.block.itemtype.ItemHoe;
import net.glowstone.block.itemtype.ItemItemFrame;
import net.glowstone.block.itemtype.ItemKnowledgeBook;
import net.glowstone.block.itemtype.ItemMilk;
import net.glowstone.block.itemtype.ItemMinecart;
import net.glowstone.block.itemtype.ItemPainting;
import net.glowstone.block.itemtype.ItemPlaceAs;
import net.glowstone.block.itemtype.ItemPoisonousPotato;
import net.glowstone.block.itemtype.ItemRawChicken;
import net.glowstone.block.itemtype.ItemRottenFlesh;
import net.glowstone.block.itemtype.ItemSeeds;
import net.glowstone.block.itemtype.ItemShovel;
import net.glowstone.block.itemtype.ItemSign;
import net.glowstone.block.itemtype.ItemSnowball;
import net.glowstone.block.itemtype.ItemSoup;
import net.glowstone.block.itemtype.ItemSpawn;
import net.glowstone.block.itemtype.ItemSpiderEye;
import net.glowstone.block.itemtype.ItemType;
import net.glowstone.block.itemtype.ItemWrittenBook;
import net.glowstone.entity.objects.GlowMinecart;
import net.glowstone.inventory.ToolType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.TreeSpecies;

/**
 * The lookup table for block and item types.
 */
public final class ItemTable {

    private static final ItemTable INSTANCE = new ItemTable();

    static {
        INSTANCE.registerBuiltins();
    }

    private final EnumMap<Material, ItemType> materialToType = new EnumMap<>(Material.class);
    private final Map<NamespacedKey, ItemType> extraTypes = new HashMap<>();
    private int nextBlockId;
    private int nextItemId;

    ////////////////////////////////////////////////////////////////////////////
    // Data

    private ItemTable() {
    }

    public static ItemTable instance() {
        return INSTANCE;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Registration

    private void registerBuiltins() {
        // TODO: Some new materials in 1.13 may be missing, although most are covered as part of
        // MaterialUtil sets.

        // Blocks:
        reg(Material.FLOWER_POT, new BlockFlowerPot());
        reg(Material.JUKEBOX, new BlockJukebox());
        reg(Material.NOTE_BLOCK, new BlockNote());
        reg(Material.SPAWNER, new BlockMobSpawner());
        regAll(MaterialTags.INFESTED_BLOCKS, new BlockMonsterEgg());
        reg(Material.DRAGON_EGG, new BlockFalling(Material.DRAGON_EGG));
        regAll(Tag.SIGNS, BlockSign::new, Sound.BLOCK_WOOD_BREAK);
        regAll(Tag.WALL_SIGNS, BlockSign::new, Sound.BLOCK_WOOD_BREAK);
        reg(Material.CRAFTING_TABLE, new BlockWorkbench(), Sound.BLOCK_WOOD_BREAK);
        reg(Material.ENDER_CHEST, new BlockEnderChest());
        reg(Material.CHEST, new BlockChest(), Sound.BLOCK_WOOD_BREAK);
        reg(Material.TRAPPED_CHEST, new BlockChest(true), Sound.BLOCK_WOOD_BREAK);
        reg(Material.DISPENSER, new BlockDispenser());
        reg(Material.DROPPER, new BlockDropper());
        reg(Material.BOOKSHELF, new BlockDirectDrops(Material.BOOK, 3), Sound.BLOCK_WOOD_BREAK);
        reg(Material.CLAY, new BlockDirectDrops(Material.CLAY_BALL, 4), Sound.BLOCK_GRAVEL_BREAK);
        regAll(MaterialTags.STAINED_TERRACOTTA, new BlockDirectDrops(ToolType.PICKAXE));
        regAll(MaterialTags.GLAZED_TERRACOTTA, new BlockDirectDrops(ToolType.PICKAXE));
        regAll(Tag.WOODEN_DOORS, BlockDoor::new, Sound.BLOCK_WOOD_BREAK);
        reg(Material.IRON_DOOR, new BlockDoor(Material.IRON_DOOR));
        reg(Material.FARMLAND, new BlockFarmland(), Sound.BLOCK_GRAVEL_BREAK);
        reg(Material.GLASS, new BlockDropless());
        reg(Material.GLASS_PANE, new BlockDropless());
        regAll(MaterialTags.STAINED_GLASS, new BlockDropless());
        regAll(MaterialTags.STAINED_GLASS_PANES, new BlockDropless());
        reg(Material.GLOWSTONE, new BlockRandomDrops(Material.GLOWSTONE_DUST, 2, 4));
        reg(Material.MYCELIUM, new BlockMycel(), Sound.BLOCK_GRAVEL_BREAK);
        reg(Material.GRASS, new BlockGrass(), Sound.BLOCK_GRASS_BREAK);
        reg(Material.DIRT, new BlockDirt(), Sound.BLOCK_GRAVEL_BREAK);
        reg(Material.GRAVEL, new BlockGravel(), Sound.BLOCK_GRAVEL_BREAK);
        reg(Material.SAND, new BlockFalling(Material.SAND), Sound.BLOCK_SAND_BREAK);
        reg(Material.ANVIL, new BlockAnvil());
        reg(Material.ICE, new BlockIce());
        reg(Material.PACKED_ICE, new BlockDropless());
        reg(Material.SNOW, new BlockSnow(), Sound.BLOCK_SNOW_BREAK);
        reg(Material.SNOW_BLOCK, new BlockSnowBlock(), Sound.BLOCK_SNOW_BREAK);
        reg(Material.PRISMARINE, new BlockDirectDrops(ToolType.PICKAXE));
        reg(Material.RED_SANDSTONE, new BlockDirectDrops(ToolType.PICKAXE));
        reg(Material.SANDSTONE, new BlockDirectDrops(ToolType.PICKAXE));
        reg(Material.NETHER_BRICK, new BlockDirectDrops(ToolType.PICKAXE));
        reg(Material.NETHER_BRICK_FENCE,
            new BlockFence(Material.NETHER_BRICK_FENCE, ToolType.PICKAXE));
        regAll(Tag.WOODEN_FENCES, BlockFence::new);
        reg(Material.NETHERRACK, new BlockDirectDrops(ToolType.PICKAXE));
        reg(Material.IRON_BARS, new BlockDirectDrops(ToolType.PICKAXE)); // TODO: Convert
        reg(Material.BRICK, new BlockDirectDrops(ToolType.PICKAXE));
        reg(Material.LEGACY_SMOOTH_BRICK, new BlockDirectDrops(ToolType.PICKAXE)); // TODO: Convert
        reg(Material.END_STONE, new BlockDirectDrops(ToolType.PICKAXE));
        reg(Material.COBBLESTONE, new BlockDirectDrops(ToolType.PICKAXE));
        reg(Material.COBBLESTONE_WALL, new BlockDirectDrops(ToolType.PICKAXE));
        reg(Material.MOSSY_COBBLESTONE, new BlockDirectDrops(ToolType.PICKAXE));
        reg(Material.STONE, new BlockStone());
        reg(Material.OBSIDIAN, new BlockDirectDrops(ToolType.DIAMOND_PICKAXE));
        reg(Material.COAL_ORE, new BlockOre(Material.COAL, ToolType.PICKAXE));
        reg(Material.COAL_BLOCK, new BlockDirectDrops(ToolType.PICKAXE));
        reg(Material.IRON_ORE, new BlockDirectDrops(ToolType.STONE_PICKAXE));
        reg(Material.IRON_BLOCK, new BlockDirectDrops(ToolType.STONE_PICKAXE));
        reg(Material.GOLD_ORE, new BlockDirectDrops(ToolType.IRON_PICKAXE));
        reg(Material.GOLD_BLOCK, new BlockDirectDrops(ToolType.IRON_PICKAXE));
        reg(Material.DIAMOND_ORE, new BlockOre(Material.DIAMOND, ToolType.IRON_PICKAXE));
        reg(Material.DIAMOND_BLOCK, new BlockDirectDrops(ToolType.IRON_PICKAXE));
        reg(Material.EMERALD_ORE, new BlockOre(Material.EMERALD, ToolType.IRON_PICKAXE));
        reg(Material.EMERALD_BLOCK, new BlockDirectDrops(ToolType.PICKAXE));
        reg(Material.LAPIS_ORE,
            new BlockOre(Material.LAPIS_LAZULI, ToolType.STONE_PICKAXE, 4, 4, 8));
        reg(Material.LAPIS_BLOCK, new BlockDirectDrops(ToolType.STONE_PICKAXE));
        reg(Material.NETHER_QUARTZ_ORE, new BlockOre(Material.QUARTZ, ToolType.PICKAXE));
        reg(Material.REDSTONE_ORE, new BlockRedstoneOre());
        reg(Material.REDSTONE_BLOCK, new BlockDirectDrops(ToolType.PICKAXE));
        reg(Material.CARROT, new BlockCarrot(), Sound.BLOCK_GRASS_BREAK);
        reg(Material.COCOA, new BlockCocoa(), Sound.BLOCK_WOOD_BREAK);
        reg(Material.DEAD_BUSH, new BlockDeadBush(), Sound.BLOCK_GRASS_BREAK);
        reg(Material.TALL_GRASS, new BlockTallGrass(), Sound.BLOCK_GRASS_BREAK);
        reg(Material.RED_MUSHROOM_BLOCK, new BlockHugeMushroom(true), Sound.BLOCK_WOOD_BREAK);
        reg(Material.BROWN_MUSHROOM_BLOCK, new BlockHugeMushroom(false), Sound.BLOCK_WOOD_BREAK);
        regAll(Tag.LEAVES, new BlockLeaves(), Sound.BLOCK_GRASS_BREAK);
        reg(Material.MELON, new BlockMelon(), Sound.BLOCK_WOOD_BREAK);
        reg(Material.MELON_STEM, new BlockStem(Material.MELON_STEM), Sound.BLOCK_GRASS_BREAK);
        reg(Material.NETHER_WART, new BlockNetherWart(), Sound.BLOCK_GRASS_BREAK);
        reg(Material.POTATO, new BlockPotato(), Sound.BLOCK_GRASS_BREAK);
        reg(Material.PUMPKIN_STEM, new BlockStem(Material.PUMPKIN_STEM), Sound.BLOCK_GRASS_BREAK);
        reg(Material.WHEAT, new BlockCrops(), Sound.BLOCK_GRASS_BREAK);
        reg(Material.CAKE, new BlockDropless(), Sound.BLOCK_WOOL_BREAK);
        reg(Material.COBWEB, new BlockWeb());
        reg(Material.FIRE, new BlockFire());
        reg(Material.END_PORTAL_FRAME, new BlockEnderPortalFrame());
        regAll(MaterialTags.WOODEN_GATES, new BlockFenceGate());
        regAll(Tag.TRAPDOORS, new BlockWoodenTrapDoor(), Sound.BLOCK_WOOD_BREAK);
        reg(Material.IRON_TRAPDOOR, new BlockIronTrapDoor());
        reg(Material.FURNACE, new BlockFurnace());
        reg(Material.LEVER, new BlockLever());
        reg(Material.HOPPER, new BlockHopper());
        reg(Material.PISTON, new BlockPiston(false));
        reg(Material.STICKY_PISTON, new BlockPiston(true));
        regAll(Tag.WOODEN_STAIRS, new BlockStairs(), Sound.BLOCK_WOOD_BREAK);
        regAll(Sets.difference(Tag.STAIRS.getValues(), Tag.WOODEN_STAIRS.getValues()), new BlockStairs());
        regAll(Tag.WOODEN_SLABS, new BlockSlab(), Sound.BLOCK_WOOD_BREAK);
        regAll(Sets.difference(Tag.SLABS.getValues(), Tag.WOODEN_SLABS.getValues()), new BlockSlab());
        reg(Material.HAY_BLOCK, new BlockHay());
        reg(Material.QUARTZ_BLOCK, new BlockQuartz());
        regAll(Tag.LOGS, new BlockLog(), Sound.BLOCK_WOOD_BREAK);
        reg(Material.LADDER, new BlockLadder(), Sound.BLOCK_WOOD_BREAK);
        reg(Material.VINE, new BlockVine());
        reg(Material.STONE_BUTTON, new BlockButton(Material.STONE_BUTTON));
        regAll(Tag.WOODEN_BUTTONS, BlockButton::new, Sound.BLOCK_WOOD_BREAK);
        regAll(MaterialTags.BEDS, new BlockBed());
        regAll(MaterialTags.SKULLS, new BlockSkull());
        reg(Material.TORCH, new BlockTorch());
        reg(Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
            new BlockDirectDrops(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, ToolType.PICKAXE));
        reg(Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
            new BlockDirectDrops(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, ToolType.PICKAXE));
        reg(Material.STONE_PRESSURE_PLATE,
            new BlockDirectDrops(Material.STONE_PRESSURE_PLATE, ToolType.PICKAXE));
        reg(Material.DAYLIGHT_DETECTOR, new BlockDaylightDetector());
        regAll(Tag.FLOWERS, new BlockNeedsAttached());
        reg(Material.BROWN_MUSHROOM, new BlockMushroom(Material.BROWN_MUSHROOM));
        reg(Material.RED_MUSHROOM, new BlockMushroom(Material.RED_MUSHROOM));
        reg(Material.SUGAR_CANE, new BlockSugarCane(), Sound.BLOCK_GRASS_BREAK);
        regAll(Tag.SAPLINGS, new BlockSapling());
        reg(Material.RAIL, new BlockRails());
        reg(Material.ACTIVATOR_RAIL, new BlockRails());
        reg(Material.DETECTOR_RAIL, new BlockRails());
        reg(Material.POWERED_RAIL, new BlockRails());
        regAll(Tag.CARPETS, new BlockCarpet(), Sound.BLOCK_WOOL_BREAK);
        reg(Material.ENCHANTING_TABLE, new BlockEnchantmentTable());
        reg(Material.BREWING_STAND, new BlockBrewingStand());
        reg(Material.CACTUS, new BlockCactus());
        reg(Material.WATER, new BlockWater());
        reg(Material.LAVA, new BlockLava());
        reg(Material.CAULDRON, new BlockCauldron());
        reg(Material.SPONGE, new BlockSponge());
        reg(Material.TNT, new BlockTnt());
        reg(Material.LEGACY_DOUBLE_PLANT, new BlockDoublePlant()); // TODO: Convert
        reg(Material.PUMPKIN, new BlockPumpkin());
        reg(Material.JACK_O_LANTERN, new BlockPumpkinBase(Material.JACK_O_LANTERN));
        reg(Material.SEA_LANTERN, new BlockRandomDrops(Material.PRISMARINE_CRYSTALS, 2, 3));
        reg(Material.REDSTONE_LAMP, new BlockLamp());
        reg(Material.REDSTONE_WIRE, new BlockRedstone());
        reg(Material.REDSTONE_TORCH, new BlockRedstoneTorch());
        reg(Material.REPEATER, new BlockRedstoneRepeater());
        reg(Material.MAGMA_BLOCK, new BlockMagma());
        reg(Material.NETHER_WART_BLOCK, new BlockDirectDrops(Material.NETHER_WART_BLOCK, ToolType
            .AXE));
        reg(Material.RED_NETHER_BRICKS, new BlockDirectDrops(Material.RED_NETHER_BRICKS, ToolType
            .PICKAXE));
        reg(Material.BONE_BLOCK, new BlockDirectDrops(Material.BONE_BLOCK, ToolType.PICKAXE));
        reg(Material.OBSERVER, new BlockObserver());
        reg(Material.COMPARATOR, new BlockRedstoneComparator());
        reg(Material.BEACON, new BlockBeacon());
        reg(Material.PURPUR_PILLAR, new BlockPurpurPillar());
        reg(Material.PURPUR_BLOCK, new BlockDirectDrops(Material.PURPUR_BLOCK, ToolType.PICKAXE));
        reg(Material.END_ROD, new BlockEndRod());
        regAll(MaterialTags.CONCRETES, BlockDirectDrops::new);
        regAll(MaterialTags.CONCRETE_POWDER, new BlockConcretePowder());
        regAll(MaterialTags.GLAZED_TERRACOTTA, BlockDirectDrops::new);
        reg(Material.CHORUS_FLOWER, new BlockChorusFlower());
        reg(Material.CHORUS_PLANT, new BlockChorusPlant());
        reg(Material.GRASS_PATH, new BlockGrassPath(), Sound.BLOCK_GRASS_BREAK);

        // Non-block and ItemPlaceAs items:
        reg(Material.FLINT_AND_STEEL, new ItemFlintAndSteel());
        regAll(Tag.SIGNS, new ItemSign());
        reg(Material.REDSTONE, new ItemPlaceAs(Material.REDSTONE_WIRE));
        reg(Material.BREWING_STAND, new ItemPlaceAs(Material.BREWING_STAND));
        reg(Material.CAULDRON, new ItemPlaceAs(Material.CAULDRON));
        reg(Material.FLOWER_POT, new ItemPlaceAs(Material.FLOWER_POT));
        regAll(MaterialTags.SKULLS, ItemPlaceAs::new);
        reg(Material.BUCKET, new ItemBucket());
        reg(Material.WATER_BUCKET, new ItemFilledBucket(Material.WATER));
        reg(Material.LAVA_BUCKET, new ItemFilledBucket(Material.LAVA));
        reg(Material.WOODEN_HOE, new ItemHoe());
        reg(Material.STONE_HOE, new ItemHoe());
        reg(Material.IRON_HOE, new ItemHoe());
        reg(Material.GOLDEN_HOE, new ItemHoe());
        reg(Material.DIAMOND_HOE, new ItemHoe());
        reg(Material.WOODEN_SHOVEL, new ItemShovel());
        reg(Material.STONE_SHOVEL, new ItemShovel());
        reg(Material.IRON_SHOVEL, new ItemShovel());
        reg(Material.GOLDEN_SHOVEL, new ItemShovel());
        reg(Material.DIAMOND_SHOVEL, new ItemShovel());
        regAll(MaterialTags.SPAWN_EGGS, new ItemSpawn());
        reg(Material.WHEAT_SEEDS, new ItemSeeds(Material.WHEAT, Material.FARMLAND));
        reg(Material.MELON_SEEDS, new ItemSeeds(Material.MELON_STEM, Material.FARMLAND));
        reg(Material.PUMPKIN_SEEDS, new ItemSeeds(Material.PUMPKIN_STEM, Material.FARMLAND));
        reg(Material.NETHER_WART, new ItemSeeds(Material.NETHER_WART, Material.SOUL_SAND));
        reg(Material.CARROT, new ItemFoodSeeds(Material.CARROT, Material.FARMLAND, 3, 3.6f));
        reg(Material.POTATO, new ItemFoodSeeds(Material.POTATO, Material.FARMLAND, 1, 0.6f));
        reg(Material.INK_SAC, new ItemDye());
        regAll(Tag.BANNERS, new ItemBanner());
        reg(Material.IRON_DOOR, new ItemPlaceAs(Material.IRON_DOOR));
        reg(Material.WRITTEN_BOOK, new ItemWrittenBook());
        reg(Material.ITEM_FRAME, new ItemItemFrame());
        reg(Material.APPLE, new ItemFood(4, 2.4f));
        reg(Material.BAKED_POTATO, new ItemFood(5, 6f));
        reg(Material.BREAD, new ItemFood(5, 6f));
        reg(Material.COOKED_CHICKEN, new ItemFood(6, 7.2f));
        reg(Material.COOKED_COD, new ItemFishCooked());
        reg(Material.COOKED_SALMON, new ItemFishCooked());
        reg(Material.COOKED_MUTTON, new ItemFood(6, 9.6f));
        reg(Material.COOKED_BEEF, new ItemFood(8, 12.8f));
        reg(Material.COOKED_RABBIT, new ItemFood(5, 6f));
        reg(Material.COOKIE, new ItemFood(2, 0.4f));
        reg(Material.GOLDEN_APPLE, new ItemGoldenApple());
        reg(Material.GOLDEN_CARROT, new ItemFood(6, 14.4f));
        reg(Material.COOKED_PORKCHOP, new ItemFood(8, 12.8f));
        reg(Material.MELON, new ItemFood(2, 1.2f));
        reg(Material.BEETROOT, new ItemFood(1, 1.2f));
        reg(Material.BEETROOT_SOUP, new ItemSoup(6, 7.2f));
        reg(Material.MUSHROOM_STEW, new ItemSoup(6, 7.2f));
        reg(Material.POISONOUS_POTATO, new ItemPoisonousPotato());
        reg(Material.PUMPKIN_PIE, new ItemFood(8, 4.8f));
        reg(Material.RABBIT_STEW, new ItemSoup(10, 12f));
        reg(Material.BEEF, new ItemFood(3, 1.8f));
        reg(Material.CHICKEN, new ItemRawChicken());
        reg(Material.COD, new ItemFishRaw());
        reg(Material.SALMON, new ItemFishRaw());
        reg(Material.MUTTON, new ItemFood(2, 1.2f));
        reg(Material.PORKCHOP, new ItemFood(3, 1.8f));
        reg(Material.RABBIT, new ItemFood(3, 1.8f));
        reg(Material.ROTTEN_FLESH, new ItemRottenFlesh());
        reg(Material.SPIDER_EYE, new ItemSpiderEye());
        reg(Material.CHORUS_FRUIT, new ItemChorusFruit());
        reg(Material.ARMOR_STAND, new ItemArmorStand());
        reg(Material.MILK_BUCKET, new ItemMilk());
        reg(Material.MINECART, new ItemMinecart(GlowMinecart.MinecartType.RIDEABLE));
        reg(Material.COMMAND_BLOCK_MINECART, new ItemMinecart(GlowMinecart.MinecartType.COMMAND));
        reg(Material.TNT_MINECART, new ItemMinecart(GlowMinecart.MinecartType.TNT));
        reg(Material.HOPPER_MINECART, new ItemMinecart(GlowMinecart.MinecartType.HOPPER));
        reg(Material.FURNACE_MINECART, new ItemMinecart(GlowMinecart.MinecartType.FURNACE));
        reg(Material.CHEST_MINECART, new ItemMinecart(GlowMinecart.MinecartType.CHEST));
        reg(Material.SNOWBALL, new ItemSnowball());
        reg(Material.EGG, new ItemEgg());
        reg(Material.BOW, new ItemBow());
        reg(Material.EXPERIENCE_BOTTLE, new ItemExperienceBottle());
        reg(Material.END_CRYSTAL, new ItemEndCrystal());
        reg(Material.OAK_BOAT, new ItemBoat(TreeSpecies.GENERIC));
        reg(Material.SPRUCE_BOAT, new ItemBoat(TreeSpecies.REDWOOD));
        reg(Material.BIRCH_BOAT, new ItemBoat(TreeSpecies.BIRCH));
        reg(Material.JUNGLE_BOAT, new ItemBoat(TreeSpecies.JUNGLE));
        reg(Material.ACACIA_BOAT, new ItemBoat(TreeSpecies.ACACIA));
        reg(Material.DARK_OAK_BOAT, new ItemBoat(TreeSpecies.DARK_OAK));
        reg(Material.PAINTING, new ItemPainting());
        reg(Material.FIREWORK_ROCKET, new ItemFirework());
        reg(Material.ENDER_PEARL, new ItemEnderPearl());
        reg(Material.KNOWLEDGE_BOOK, new ItemKnowledgeBook());
        reg(Material.FISHING_ROD, new ItemFishingRod());
    }

    private void reg(Material material, ItemType type) {
        if (material.isBlock() != type instanceof BlockType) {
            throw new IllegalArgumentException(
                "Cannot mismatch item and block: " + material + ", " + type);
        }

        if (materialToType.containsKey(material)) {
            throw new IllegalArgumentException(
                "Cannot use " + type + " for " + material + ", is already " + materialToType
                    .get(material));
        }

        materialToType.put(material, type);
        type.setMaterial(material);

        if (material.isBlock()) {
            nextBlockId = Math.max(nextBlockId, material.getId() + 1);
            if (type.getClass() != BlockType.class) {
                ((BlockType) type).setPlaceSound(Sound.BLOCK_STONE_BREAK);
            }
        } else {
            nextItemId = Math.max(nextItemId, material.getId() + 1);
        }
    }

    private void reg(Material material, ItemType type, Sound sound) {
        if (material.isBlock() != type instanceof BlockType) {
            throw new IllegalArgumentException(
                "Cannot mismatch item and block: " + material + ", " + type);
        }

        if (materialToType.containsKey(material)) {
            throw new IllegalArgumentException(
                "Cannot use " + type + " for " + material + ", is already " + materialToType
                    .get(material));
        }

        materialToType.put(material, type);
        type.setMaterial(material);

        if (material.isBlock()) {
            nextBlockId = Math.max(nextBlockId, material.getId() + 1);
            ((BlockType) type).setPlaceSound(sound);
        } else {
            nextItemId = Math.max(nextItemId, material.getId() + 1);
        }
    }

    private void regAll(Tag<Material> tag, ItemType type) {
        regAll(tag.getValues(), type);
    }

    private void regAll(Iterable<Material> materials, ItemType type) {
        for (Material material : materials) {
            reg(material, type);
        }
    }

    private void regAll(Tag<Material> tag, ItemType type, Sound sound) {
        regAll(tag.getValues(), type, sound);
    }

    private void regAll(Iterable<Material> materials, ItemType type, Sound sound) {
        for (Material material : materials) {
            reg(material, type, sound);
        }
    }

    private void regAll(Tag<Material> tag,
            Function<? super Material, ? extends ItemType> itemTypeFunction) {
        regAll(tag.getValues(), itemTypeFunction);
    }

    private void regAll(Iterable<Material> items,
                        Function<? super Material, ? extends ItemType> itemTypeFunction) {
        for (Material item : items) {
            reg(item, itemTypeFunction.apply(item));
        }
    }

    private void regAll(Tag<Material> tag,
            Function<? super Material, ? extends ItemType> itemTypeFunction, Sound sound) {
        regAll(tag.getValues(), itemTypeFunction, sound);
    }

    private void regAll(Iterable<Material> items,
                        Function<? super Material, ? extends ItemType> itemTypeFunction,
                        Sound sound) {
        for (Material item : items) {
            reg(item, itemTypeFunction.apply(item), sound);
        }
    }

    /**
     * Register a new, non-Vanilla ItemType. It will be assigned an ID automatically.
     *
     * @param key  the namespaced key of the ItemType
     * @param type the ItemType to register.
     * @return if the registration was successful
     */
    public boolean register(NamespacedKey key, ItemType type) {
        return extraTypes.putIfAbsent(key, type) == null;
    }

    private ItemType createDefault(Material material) {
        if (material == null || material == Material.AIR) {
            return null;
        }

        ItemType result;
        if (material.isBlock()) {
            result = new BlockType();
        } else {
            result = new ItemType();
        }
        reg(material, result);
        return result;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Type access

    @Deprecated
    public ItemType getItem(int id) {
        BlockDataManager blockDataManager = ((GlowServer) Bukkit.getServer()).getBlockDataManager();
        return getItem(blockDataManager.convertToBlockData(id).getMaterial());
    }

    /**
     * Returns the {@link ItemType} for a {@link Material}, or null if not a block.
     *
     * @param mat a {@link Material}
     * @return {@code mat} as an {@link ItemType}
     */
    public ItemType getItem(Material mat) {
        ItemType type = materialToType.get(mat);
        if (type == null) {
            type = createDefault(mat);
        }
        return type;
    }

    @Deprecated
    public BlockType getBlock(int id) {
        BlockDataManager blockDataManager = ((GlowServer) Bukkit.getServer()).getBlockDataManager();
        return getBlock(blockDataManager.convertToBlockData(id).getMaterial());
    }

    /**
     * Returns the {@link BlockType} for a {@link Material}, or null if not a block.
     *
     * @param mat a {@link Material}
     * @return {@code mat} as a {@link BlockType}, or null if {@code mat} isn't a block
     */
    public BlockType getBlock(Material mat) {
        ItemType itemType = getItem(mat);
        if (itemType instanceof BlockType) {
            return (BlockType) itemType;
        }
        return null;
    }

}
