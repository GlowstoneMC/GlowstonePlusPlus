package net.glowstone.block.blocktype;

import com.destroystokyo.paper.MaterialTags;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import net.glowstone.GlowWorld;
import net.glowstone.block.GlowBlock;
import net.glowstone.block.GlowBlockState;
import net.glowstone.block.entity.BedEntity;
import net.glowstone.block.entity.BlockEntity;
import net.glowstone.block.entity.state.GlowBed;
import net.glowstone.chunk.GlowChunk;
import net.glowstone.entity.GlowPlayer;
import net.glowstone.i18n.GlowstoneMessages;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.PigZombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Bed;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class BlockBed extends BlockType {

    private static final ImmutableSet<Biome> EXPLOSIVE_IN_BIOMES = ImmutableSet.copyOf(EnumSet.of(
        Biome.NETHER_WASTES, Biome.SOUL_SAND_VALLEY, Biome.CRIMSON_FOREST, Biome.WARPED_FOREST,
        Biome.BASALT_DELTAS,
        Biome.THE_END, Biome.END_BARRENS, Biome.END_MIDLANDS,
        Biome.END_HIGHLANDS, Biome.SMALL_END_ISLANDS));

    /**
     * Helper method for set whether the specified bed blocks are occupied.
     *
     * @param head     head of the bed
     * @param foot     foot of the bed
     * @param occupied if the bed is occupied by a player
     */
    public static void setOccupied(GlowBlock head, GlowBlock foot, boolean occupied) {
        byte headData = head.getData();
        byte footData = foot.getData();
        head.setData((byte) (occupied ? headData | 0x4 : headData & ~0x4));
        foot.setData((byte) (occupied ? footData | 0x4 : footData & ~0x4));
    }

    /**
     * Return whether the specified bed block is occupied.
     *
     * @param block part of the bed
     * @return true if this bed is occupied, false if it is not
     */
    public static boolean isOccupied(GlowBlock block) {
        return (block.getData() & 0x4) == 0x4;
    }

    /**
     * Returns the head of a bed given one of its blocks.
     *
     * @param block part of the bed
     * @return The head of the bed
     */
    public static GlowBlock getHead(GlowBlock block) {
        MaterialData data = block.getState().getData();
        if (!(data instanceof Bed)) {
            return null;
        }
        Bed bed = (Bed) data;
        if (bed.isHeadOfBed()) {
            return block;
        } else {
            return block.getRelative(bed.getFacing());
        }
    }

    /**
     * Returns the foot of a bed given one of its blocks.
     *
     * @param block part of the bed
     * @return The foot of the bed
     */
    public static GlowBlock getFoot(GlowBlock block) {
        MaterialData data = block.getState().getData();
        if (!(data instanceof Bed)) {
            return null;
        }
        Bed bed = (Bed) data;
        if (bed.isHeadOfBed()) {
            return block.getRelative(bed.getFacing().getOppositeFace());
        } else {
            return block;
        }
    }

    /**
     * Returns whether a player can spawn within a block of specified material.
     *
     * @param material the material
     * @return Whether spawning is possible
     */
    public static boolean isValidSpawn(Material material) {
        switch (material) {
            // TODO: Some materials new in 1.13 and some plant types may be missing
            case AIR:
            case OAK_SAPLING:
            case DARK_OAK_SAPLING:
            case BIRCH_SAPLING:
            case ACACIA_SAPLING:
            case SPRUCE_SAPLING:
            case JUNGLE_SAPLING:
            case POWERED_RAIL:
            case DETECTOR_RAIL:
            case TALL_GRASS:
            case DEAD_BUSH:
            case SUNFLOWER:
            case POPPY:
            case BROWN_MUSHROOM:
            case RED_MUSHROOM:
            case TORCH:
            case REDSTONE_WIRE:
            case CARROTS:
            case POTATOES:
            case WHEAT:
            case RAIL:
            case ACTIVATOR_RAIL:
            case LEVER:
            case REDSTONE_TORCH:
            case STONE_BUTTON:
            case SNOW:
            case SUGAR_CANE:
            case COMPARATOR:
            case REPEATER:
            case VINE:
            case TRIPWIRE_HOOK:
            case TRIPWIRE:
            case FLOWER_POT:
            case OAK_BUTTON:
            case DARK_OAK_BUTTON:
            case ACACIA_BUTTON:
            case JUNGLE_BUTTON:
            case SPRUCE_BUTTON:
            case BIRCH_BUTTON:
            case PLAYER_HEAD:
            case WITHER_SKELETON_SKULL:
            case SKELETON_SKULL:
            case ZOMBIE_HEAD:
            case CREEPER_HEAD:
            case DRAGON_HEAD:
            case PLAYER_WALL_HEAD:
            case WITHER_SKELETON_WALL_SKULL:
            case SKELETON_WALL_SKULL:
            case ZOMBIE_WALL_HEAD:
            case CREEPER_WALL_HEAD:
            case DRAGON_WALL_HEAD:
            case HEAVY_WEIGHTED_PRESSURE_PLATE:
            case LIGHT_WEIGHTED_PRESSURE_PLATE:
            case WHITE_CARPET:
            case YELLOW_CARPET:
            case ORANGE_CARPET:
            case LIGHT_BLUE_CARPET:
            case PINK_CARPET:
            case MAGENTA_CARPET:
            case LIME_CARPET:
            case GRAY_CARPET:
            case LIGHT_GRAY_CARPET:
            case PURPLE_CARPET:
            case BLUE_CARPET:
            case GREEN_CARPET:
            case CYAN_CARPET:
            case RED_CARPET:
            case BROWN_CARPET:
            case BLACK_CARPET:
            case LARGE_FERN:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns an 'empty' block next to the bed used to put the player at when they exit a bed or
     * respawn.
     *
     * @param head head of the bed
     * @param foot foot of the bed
     * @return Exit block or {@code null} if all spots are blocked
     */
    public static Block getExitLocation(GlowBlock head, GlowBlock foot) {
        // First check blocks near head
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block b = head.getRelative(x, 0, z);
                boolean floorValid = b.getRelative(BlockFace.DOWN).getType().isSolid();
                boolean bottomValid = isValidSpawn(b.getType());
                boolean topValid = isValidSpawn(b.getRelative(BlockFace.UP).getType());
                if (floorValid && bottomValid && topValid) {
                    return b;
                }
            }
        }

        // Then check the last three blocks near foot
        BlockFace face = head.getFace(foot);
        int modX = face.getModX();
        int modZ = face.getModZ();
        for (int x = modX == 0 ? -1 : modX; x <= (modX == 0 ? 1 : modX); x++) {
            for (int z = modZ == 0 ? -1 : modZ; z <= (modZ == 0 ? 1 : modZ); z++) {
                Block b = foot.getRelative(x, 0, z);
                boolean floorValid = b.getRelative(BlockFace.DOWN).getType().isSolid();
                boolean bottomValid = isValidSpawn(b.getType());
                boolean topValid = isValidSpawn(b.getRelative(BlockFace.UP).getType());
                if (floorValid && bottomValid && topValid) {
                    return b;
                }
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Collection<ItemStack> getDrops(GlowBlock block, ItemStack tool) {
        return Collections.singletonList(new ItemStack(block.getType(), 1,
            (((GlowBed) block.getState()).getColor().getWoolData())));
    }

    @Override
    public boolean canPlaceAt(GlowPlayer player, GlowBlock block, BlockFace against) {
        if (player != null) {
            BlockFace direction = getOppositeBlockFace(player.getLocation(), false)
                .getOppositeFace();
            final GlowBlock otherEnd = block.getRelative(direction);
            return otherEnd.getType() == Material.AIR
                && otherEnd.getRelative(BlockFace.DOWN).getType().isSolid();
        } else {
            return false;
        }
    }

    @Override
    public void onNearBlockChanged(GlowBlock block, BlockFace face, GlowBlock changedBlock,
                                   Material oldType, byte oldData, Material newType, byte newData) {
        if (changedBlock.equals(getHead(block)) || changedBlock.equals(getFoot(block))) {
            if (newType == Material.AIR) {
                block.setType(Material.AIR);
            }
        }
        if (changedBlock.equals(getHead(block).getRelative(BlockFace.DOWN))
            || changedBlock.equals(getFoot(block).getRelative(BlockFace.DOWN))) {
            if (newType == Material.AIR) {
                block.setType(Material.AIR);
            }
        }
    }

    @Override
    public void placeBlock(GlowPlayer player, GlowBlockState state, BlockFace face,
                           ItemStack holding, Vector clickedLoc) {
        BlockFace direction = getOppositeBlockFace(player.getLocation(), false).getOppositeFace();
        super.placeBlock(player, state, face, holding, clickedLoc);

        MaterialData data = state.getData();
        if (data instanceof Bed) {
            ((Bed) data).setFacingDirection(direction);
            ((Bed) data).setHeadOfBed(false);
            state.setData(data);
        } else {
            warnMaterialData(Bed.class, data);
        }
    }

    @Override
    public BlockEntity createBlockEntity(GlowChunk chunk, int cx, int cy, int cz) {
        return new BedEntity(chunk.getBlock(cx, cy, cz));
    }

    @Override
    public void afterPlace(GlowPlayer player, GlowBlock block, ItemStack holding,
                           GlowBlockState oldState) {
        Material type = block.getType();
        if (!MaterialTags.BEDS.isTagged(type)) {
            return;
        }
        GlowBed bed = (GlowBed) block.getState();
        bed.setColor(DyeColor.getByWoolData(holding.getData().getData()));
        bed.update(true);
        BlockFace direction = ((Bed) bed.getData()).getFacing();
        GlowBlock headBlock = block.getRelative(direction);
        headBlock.setType(type);
        GlowBed headBlockState = (GlowBed) headBlock.getState();
        headBlockState.setColor(DyeColor.getByWoolData(holding.getData().getData()));
        MaterialData data = headBlockState.getData();
        ((Bed) data).setHeadOfBed(true);
        ((Bed) data).setFacingDirection(direction);
        headBlockState.setData(data);
        headBlockState.update(true);
    }

    @Override
    public boolean blockInteract(final GlowPlayer player, GlowBlock block, final BlockFace face,
                                 final Vector clickedLoc) {
        GlowWorld world = player.getWorld();
        MaterialData data = block.getState().getData();
        if (!(data instanceof Bed)) {
            warnMaterialData(Bed.class, data);
            return false;
        }
        block = getHead(block);

        // Disallow sleeping in nether and end biomes
        Biome biome = block.getBiome();
        if (EXPLOSIVE_IN_BIOMES.contains(biome)) {
            // Set off an explosion at the bed slightly stronger than TNT
            world.createExplosion(block.getLocation(), 5F, true);
            return true;
        }

        // Sleeping is only possible during the night or a thunderstorm
        // Tick values for day/night time taken from the minecraft wiki
        final long time = world.getTime();
        if ((time < 12541 || time > 23458) && !world.isThundering()) {
            GlowstoneMessages.Bed.DAY.send(player);
            return true;
        }

        if (isOccupied(block)) {
            GlowstoneMessages.Bed.OCCUPIED.send(player);
            return true;
        }

        if (!isWithinDistance(player, block, 3, 2, 3)) {
            return true; // Distance between player and bed is too great, fail silently
        }

        // Check for hostile mobs relative to the block below the head of the bed
        // (Don't use getEntitiesByType etc., because they copy the entire list of entities)
        for (Entity e : world.getEntityManager()) {
            if (e instanceof Creature
                && (e.getType() != EntityType.ZOMBIFIED_PIGLIN || ((PigZombie) e)
                    .isAngry())
                && isWithinDistance(e, block.getRelative(BlockFace.DOWN), 8, 5, 8)) {
                GlowstoneMessages.Bed.MOB.send(player);
                return true;
            }
        }

        player.enterBed(block);
        return true;
    }

    /**
     * Checks whether the entity is within the specified distance from the block.
     *
     * @param entity the entity
     * @param block  the block
     * @param x      maximum distance on x axis
     * @param y      maximum distance on y axis
     * @param z      maximum distance on z axis
     * @return Whether the entity is within distance
     */
    private boolean isWithinDistance(Entity entity, Block block, int x, int y, int z) {
        Location loc = entity.getLocation();
        return Math.abs(loc.getX() - block.getX()) <= x
            && Math.abs(loc.getY() - block.getY()) <= y
            && Math.abs(loc.getZ() - block.getZ()) <= z;
    }
}
