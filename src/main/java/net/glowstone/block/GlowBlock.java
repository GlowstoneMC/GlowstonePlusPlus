package net.glowstone.block;

import com.destroystokyo.paper.block.BlockSoundGroup;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import net.glowstone.GlowServer;
import net.glowstone.GlowWorld;
import net.glowstone.ServerProvider;
import net.glowstone.block.MaterialValueManager.ValueCollection;
import net.glowstone.block.blocktype.BlockRedstone;
import net.glowstone.block.blocktype.BlockRedstoneTorch;
import net.glowstone.block.blocktype.BlockType;
import net.glowstone.block.data.BlockDataManager;
import net.glowstone.block.data.states.StatefulBlockData;
import net.glowstone.block.entity.BlockEntity;
import net.glowstone.chunk.GlowChunk;
import net.glowstone.net.message.play.game.BlockChangeMessage;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Button;
import org.bukkit.material.Lever;
import org.bukkit.material.RedstoneTorch;
import org.bukkit.metadata.MetadataStore;
import org.bukkit.metadata.MetadataStoreBase;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a single block in a world.
 */
public class GlowBlock implements Block {

    /**
     * The BlockFaces of a single-layer 3x3 area.
     */
    private static final BlockFace[] LAYER = new BlockFace[] {
        BlockFace.NORTH_WEST, BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SELF,
        BlockFace.WEST, BlockFace.SOUTH_WEST, BlockFace.SOUTH, BlockFace.SOUTH_EAST};

    /**
     * The BlockFaces of all directly adjacent.
     */
    private static final BlockFace[] ADJACENT = new BlockFace[] {
        BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP,
        BlockFace.DOWN};
    /**
     * The metadata store for blocks.
     */
    private static final MetadataStore<Block> metadata = new BlockMetadataStore();
    private static final Map<GlowBlock, List<Long>> counterMap = new HashMap<>();
    @Getter
    private final int x;
    @Getter
    private final int y;
    @Getter
    private final int z;
    @Getter
    private final GlowWorld world;

    /**
     * Creates an object to refer to a block.
     *
     * @param chunk the chunk
     * @param x     the X coordinate
     * @param y     the Y coordinate
     * @param z     the Z coordinate
     */
    public GlowBlock(GlowChunk chunk, int x, int y, int z) {
        world = chunk.getWorld();
        this.x = x;
        this.y = Math.min(256, Math.max(y, 0));
        this.z = z;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Basics
    @Override
    public @NotNull GlowChunk getChunk() {
        return (GlowChunk) world.getChunkAt(this);
    }

    @Override
    public @NotNull Location getLocation() {
        return new Location(getWorld(), x, y, z);
    }

    @Override
    public Location getLocation(Location loc) {
        if (loc == null) {
            return null;
        }
        loc.setWorld(getWorld());
        loc.setX(x);
        loc.setY(y);
        loc.setZ(z);
        return loc;
    }

    public BlockEntity getBlockEntity() {
        return getChunk().getEntity(x & 0xf, y, z & 0xf);
    }

    @Override
    public @NotNull GlowBlockState getState() {
        BlockEntity entity = getBlockEntity();
        if (entity != null) {
            GlowBlockState state = entity.getState();
            if (state != null) {
                return state;
            }
        }
        return new GlowBlockState(this);
    }

    @Override
    public @NotNull GlowBlockState getState(boolean useSnapshot) {
        return getState(); // TODO: disable use of snapshot
    }

    @Override
    public @NotNull Biome getBiome() {
        return getWorld().getBiome(x, z);
    }

    @Override
    public void setBiome(@NotNull Biome bio) {
        getWorld().setBiome(x, z, bio);
    }

    @Override
    public double getTemperature() {
        return getWorld().getTemperature(x, z);
    }

    @Override
    public double getHumidity() {
        return getWorld().getHumidity(x, z);
    }

    ////////////////////////////////////////////////////////////////////////////
    // getFace & getRelative
    @Override
    public BlockFace getFace(@NotNull Block block) {
        for (BlockFace face : BlockFace.values()) {
            if (x + face.getModX() == block.getX() && y + face.getModY() == block.getY()
                && z + face.getModZ() == block.getZ()) {
                return face;
            }
        }
        return null;
    }

    @Override
    public @NotNull GlowBlock getRelative(int modX, int modY, int modZ) {
        return getWorld().getBlockAt(x + modX, y + modY, z + modZ);
    }

    @Override
    public @NotNull GlowBlock getRelative(BlockFace face) {
        return getRelative(face.getModX(), face.getModY(), face.getModZ());
    }

    @Override
    public @NotNull GlowBlock getRelative(BlockFace face, int distance) {
        return getRelative(
            face.getModX() * distance,
            face.getModY() * distance,
            face.getModZ() * distance
        );
    }

    ////////////////////////////////////////////////////////////////////////////
    // Type and typeid getters/setters
    @Override
    public @NotNull Material getType() {
        return world.getBlockTypeAt(x, y, z);
    }

    @Override
    public void setType(Material type) {
        setTypeId(type.getId());
    }

    /**
     * Set the Material type of a block and optionally apply physics.
     */
    @Override
    public void setType(Material type, boolean applyPhysics) {
        setTypeId(type.getId(), applyPhysics);
    }

    /**
     * Set the Material type of a block with data and optionally apply physics.
     *
     * @param type         The type to set the block to.
     * @param data         The raw data to set the block to.
     * @param applyPhysics notify this block and surrounding blocks to update physics
     */
    public void setType(Material type, byte data, boolean applyPhysics) {
        setTypeIdAndData(type.getId(), data, applyPhysics);
    }

    @Deprecated
    public boolean setTypeId(int type) {
        return setTypeId(type, true);
    }

    @Deprecated
    public boolean setTypeId(int type, boolean applyPhysics) {
        return setTypeIdAndData(type, (byte) 0, applyPhysics);
    }

    @Deprecated
    public boolean setTypeIdAndData(int type, byte data, boolean applyPhysics) {
        Material oldTypeId = getType();
        byte oldData = getData();

        GlowChunk chunk = (GlowChunk) world.getChunkAt(this);
        Material material =
            ((GlowServer) Bukkit.getServer()).getBlockDataManager().convertToBlockData(type)
                .getMaterial();
        chunk.setType(x & 0xf, z & 0xf, y, material);
        chunk.setMetaData(x & 0xf, z & 0xf, y, data);

        // TODO: fix so this hack isn't needed!
        /*if (oldTypeId == Material.DOUBLE_PLANT
                && getRelative(BlockFace.UP).getType() == Material.DOUBLE_PLANT) {
            world.getChunkAtAsync(this, c -> ((GlowChunk) c).setType(x & 0xf, z & 0xf, y + 1, 0));
            GlowChunk.Key key = GlowChunk.Key.of(x >> 4, z >> 4);
            BlockChangeMessage bcmsg = new BlockChangeMessage(x, y + 1, z, 0, 0);
            world.broadcastBlockChangeInRange(key, bcmsg);
        }*/

        if (applyPhysics) {
            applyPhysics(oldTypeId, material, oldData, data);
        }

        GlowChunk.Key key = GlowChunk.Key.of(x >> 4, z >> 4);
        BlockChangeMessage bcmsg = new BlockChangeMessage(x, y, z, type, data);
        world.broadcastBlockChangeInRange(key, bcmsg);

        return true;
    }

    @Override
    public boolean isEmpty() {
        return MaterialUtil.AIR_VARIANTS.contains(getType());
    }

    @Override
    public boolean isLiquid() {
        Material mat = getType();
        return mat == Material.WATER || mat == Material.LAVA;
    }

    @Override
    public boolean isBuildable() {
        // TODO: This may not be the right implementation
        return this.isSolid();
    }

    /**
     * Get block material's flammable ability. (ability to have fire spread to it)
     *
     * @return if this block is flammable
     */
    public boolean isFlammable() {
        return getMaterialValues().getFlameResistance() >= 0;
    }

    /**
     * Get block material's burn ability. (ability to have fire consume it)
     *
     * @return if this block is burnable
     */
    public boolean isBurnable() {
        return getMaterialValues().getFireResistance() >= 0;
    }

    @Override
    public boolean isReplaceable() {
        // TODO: This may not be the right implementation
        return !this.isSolid();
    }

    @Override
    public boolean isSolid() {
        return getType().isSolid();
    }

    public ValueCollection getMaterialValues() {
        return ((GlowServer) ServerProvider.getServer())
            .getMaterialValueManager().getValues(getType());
    }

    ////////////////////////////////////////////////////////////////////////////
    // Data and light getters/setters
    @Override
    public byte getData() {
        return (byte) ((GlowChunk) world.getChunkAt(this)).getMetaData(x & 0xf, z & 0xf, y);
    }

    public void setData(byte data) {
        setData(data, true);
    }

    public void setData(byte data, boolean applyPhysics) {
        byte oldData = getData();
        ((GlowChunk) world.getChunkAt(this)).setMetaData(x & 0xf, z & 0xf, y, data);
        if (applyPhysics) {
            applyPhysics(getType(), getType(), oldData, data);
        }

        GlowChunk.Key key = GlowChunk.Key.of(x >> 4, z >> 4);
        BlockDataManager blockDataManager = ((GlowServer) Bukkit.getServer()).getBlockDataManager();
        StatefulBlockData blockData = blockDataManager.createBlockData(getType());
        BlockChangeMessage bcmsg = new BlockChangeMessage(x, y, z,
            blockDataManager.convertToBlockId(blockData), data);
        world.broadcastBlockChangeInRange(key, bcmsg);
    }

    @Override
    public void setBlockData(@NotNull BlockData data, boolean applyPhysics) {
        // TODO
    }

    @Override
    public void setBlockData(@NotNull BlockData data) {
        // TODO
    }

    @Override
    public @NotNull BlockData getBlockData() {
        return null; // TODO
    }

    @Override
    public byte getLightLevel() {
        return (byte) Math.max(getLightFromSky(), getLightFromBlocks());
    }

    @Override
    public byte getLightFromSky() {
        return ((GlowChunk) world.getChunkAt(this)).getSkyLight(x & 0xf, z & 0xf, y);
    }

    @Override
    public byte getLightFromBlocks() {
        return ((GlowChunk) world.getChunkAt(this)).getBlockLight(x & 0xf, z & 0xf, y);
    }

    @Override
    public boolean isValidTool(@NotNull ItemStack itemStack) {
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Redstone
    @Override
    public boolean isBlockPowered() {
        // Strong powered?

        if (getType() == Material.REDSTONE_BLOCK) {
            return true;
        }

        if (getType() == Material.LEVER && ((Lever) getState().getData()).isPowered()) {
            return true;
        }

        if (MaterialUtil.BUTTONS.contains(getType())
            && ((Button) getState().getData()).isPowered()) {
            return true;
        }

        // Now checking for power attached, only solid blocks transmit this..
        if (!getType().isSolid()) {
            return false;
        }

        for (BlockFace face : ADJACENT) {
            GlowBlock target = getRelative(face);
            switch (target.getType()) {
                case LEVER:
                    Lever lever = (Lever) target.getState().getData();
                    if (lever.isPowered() && lever.getAttachedFace() == target.getFace(this)) {
                        return true;
                    }
                    break;
                case STONE_BUTTON:
                case OAK_BUTTON:
                case DARK_OAK_BUTTON:
                case ACACIA_BUTTON:
                case BIRCH_BUTTON:
                case JUNGLE_BUTTON:
                case SPRUCE_BUTTON:
                    Button button = (Button) target.getState().getData();
                    if (button.isPowered() && button.getAttachedFace() == target.getFace(this)) {
                        return true;
                    }
                    break;
                case REPEATER:
                    if (((Directional) target.getBlockData()).getFacing() == target.getFace(this)) {
                        return true;
                    }
                    break;
                case REDSTONE_TORCH:
                    if (face == BlockFace.DOWN
                        && ((RedstoneTorch) target.getState().getData()).isPowered()) {
                        return true;
                    }
                    break;
                case REDSTONE_WIRE:
                    if (target.getData() > 0
                        && BlockRedstone.calculateConnections(target)
                        .contains(target.getFace(this))) {
                        return true;
                    }
                    break;
                default:
                    // do nothing
            }
        }

        return false;
    }

    @Override
    public boolean isBlockIndirectlyPowered() {
        // Is a nearby block directly powered?
        for (BlockFace face : ADJACENT) {
            GlowBlock block = getRelative(face);
            if (block.isBlockPowered()) {
                return true;
            }

            switch (block.getType()) {
                case REDSTONE_TORCH:
                    if (((Lightable) block.getBlockData()).isLit()
                        &&
                        face != BlockRedstoneTorch.getAttachedBlockFace(block).getOppositeFace()) {
                        return true;
                    }
                    break;
                case REDSTONE_WIRE:
                    if (block.getData() > 0 && BlockRedstone.calculateConnections(block)
                        .contains(block.getFace(this))) {
                        return true;
                    }
                    break;
                default:
                    // do nothing
            }
        }
        return false;
    }

    @Override
    public boolean isBlockFacePowered(BlockFace face) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isBlockFaceIndirectlyPowered(BlockFace face) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getBlockPower(BlockFace face) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getBlockPower() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public @NotNull PistonMoveReaction getPistonMoveReaction() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String toString() {
        return "GlowBlock{chunk=" + getChunk() + ",x=" + x + ",y=" + y + ",z=" + z + ",type="
            + getType() + ",data=" + getData() + "}";
    }

    ////////////////////////////////////////////////////////////////////////////
    // Drops and breaking

    /**
     * Break the block naturally, randomly dropping only some of the drops.
     *
     * @param yield The approximate portion of the drops to actually drop.
     * @return true if the block was destroyed
     */
    public boolean breakNaturally(float yield) {
        return breakNaturally(yield, ItemTable.instance().getBlock(getType()).getMinedDrops(this));
    }

    /**
     * Breaks this block and drops items.
     *
     * @param yield the probability to drop each item
     * @param drops the items to potentially drop
     * @return true if broken; false if this block is already air
     */
    public boolean breakNaturally(float yield, Collection<ItemStack> drops) {
        if (getType() == Material.AIR) {
            return false;
        }

        Location location = getLocation();
        drops.stream().filter(stack -> ThreadLocalRandom.current().nextFloat() < yield)
            .forEach(stack -> getWorld().dropItemNaturally(location, stack));

        setType(Material.AIR);
        return true;
    }

    @Override
    public boolean breakNaturally() {
        return breakNaturally(1.0f);
    }

    @Override
    public boolean breakNaturally(ItemStack tool) {
        Collection<ItemStack> drops = getDrops(tool);
        if (!drops.isEmpty()) {
            return breakNaturally(1.0f, drops);
        } else {
            return setTypeId(Material.AIR.getId());
        }
    }

    @Override
    public boolean breakNaturally(@NotNull ItemStack tool, boolean triggerEffect) {
        // TODO: triggerEffect
        return breakNaturally(tool);
    }

    @Override
    public boolean applyBoneMeal(@NotNull BlockFace blockFace) {
        return false;
    }

    @Override
    public Collection<ItemStack> getDrops() {
        return Objects.requireNonNull(ItemTable.instance().getBlock(getType())).getMinedDrops(this);
    }

    @Override
    public Collection<ItemStack> getDrops(ItemStack tool) {
        return Objects.requireNonNull(ItemTable.instance().getBlock(getType()))
            .getDrops(this, tool);
    }

    @Override
    public @NotNull Collection<ItemStack> getDrops(@NotNull ItemStack tool,
                                                   @Nullable Entity entity) {
        // TODO: find usage for entity
        return getDrops(tool);
    }

    @Override
    public boolean isPreferredTool(@NotNull ItemStack itemStack) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isPassable() {
        return isEmpty(); // TODO
    }

    @Override
    public @Nullable RayTraceResult rayTrace(@NotNull Location start, @NotNull Vector direction,
                                             double maxDistance,
                                             @NotNull FluidCollisionMode fluidCollisionMode) {
        return null; // TODO
    }

    @Override
    public @NotNull BoundingBox getBoundingBox() {
        if (isEmpty()) {
            return new BoundingBox(x, y, z, x, y, z); // 0x0x0 bounding box
        }
        return BoundingBox.of(this); // TODO: Some blocks don't fill their space
    }

    @Override
    public @NotNull BlockSoundGroup getSoundGroup() {
        return null;
    }

    @NotNull
    @Override
    public String getTranslationKey() {
        return null;
    }

    @NotNull
    @Override
    public float getDestroySpeed(@NotNull ItemStack tool) {
        // TODO: not implemented
        return 1.0f;
    }

    @Override
    public @NotNull float getDestroySpeed(@NotNull ItemStack itemStack, boolean considerEnchants) {
        // TODO: not implemented
        return 1.0f;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Metadata
    @Override
    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
        metadata.setMetadata(this, metadataKey, newMetadataValue);
    }

    @Override
    public List<MetadataValue> getMetadata(String metadataKey) {
        return metadata.getMetadata(this, metadataKey);
    }

    @Override
    public boolean hasMetadata(String metadataKey) {
        return metadata.hasMetadata(this, metadataKey);
    }

    @Override
    public void removeMetadata(@NotNull String metadataKey, @NotNull Plugin owningPlugin) {
        metadata.removeMetadata(this, metadataKey, owningPlugin);
    }

    /////////////////////////////////////////////////////////////////////////////
    // Physics

    /**
     * Notify this block and its surrounding blocks that this block has changed type and data.
     *
     * @param oldType the old block type
     * @param newType the new block type
     * @param oldData the old data
     * @param newData the new data
     */
    public void applyPhysics(Material oldType, Material newType, byte oldData, byte newData) {
        if (!world.isInitialized()) {
            // World is still loading.
            return;
        }
        // notify the surrounding blocks that this block has changed
        ItemTable itemTable = ItemTable.instance();

        for (int y = -1; y <= 1; y++) {
            for (BlockFace face : LAYER) {
                if (y == 0 && face == BlockFace.SELF) {
                    continue;
                }

                GlowBlock notify = getRelative(face.getModX(), face.getModY() + y, face.getModZ());

                BlockFace blockFace;
                if (y == 0) {
                    blockFace = face.getOppositeFace();
                } else if (y == -1 && face == BlockFace.SELF) {
                    blockFace = BlockFace.UP;
                } else if (y == 1 && face == BlockFace.SELF) {
                    blockFace = BlockFace.DOWN;
                } else {
                    blockFace = null;
                }

                BlockType notifyType = itemTable.getBlock(notify.getType());
                if (notifyType != null) {
                    notifyType
                        .onNearBlockChanged(notify, blockFace, this, oldType, oldData,
                            newType, newData);
                }
            }
        }

        BlockType type = itemTable.getBlock(oldType);
        if (type != null) {
            type.onBlockChanged(this, oldType, oldData, newType, newData);
        }
    }

    /**
     * Increments the count of recent state changes. Used to implement redstone-torch burnout.
     *
     * @param timeout the number of game ticks before this state change is no longer considered
     *                recent
     */
    public void count(int timeout) {
        GlowBlock target = this;
        List<Long> gameTicks = new ArrayList<>();
        for (GlowBlock block : counterMap.keySet()) {
            if (block.getLocation().equals(getLocation())) {
                gameTicks = counterMap.get(block);
                target = block;
                break;
            }
        }

        long time = getWorld().getFullTime();
        gameTicks.add(time + timeout);

        counterMap.put(target, gameTicks);
    }

    /**
     * Returns the number of recent state changes, as defined by {@link #count(int)}. Used to
     * implement redstone-torch burnout.
     *
     * @return the number of recent state changes
     */
    public int getCounter() {
        GlowBlock target = this;
        List<Long> gameTicks = new ArrayList<>();
        for (GlowBlock block : counterMap.keySet()) {
            if (block.getLocation().equals(getLocation())) {
                gameTicks = counterMap.get(block);
                target = block;
                break;
            }
        }

        long time = getWorld().getFullTime();

        gameTicks.removeIf(rate -> rate < time);

        counterMap.put(target, gameTicks);
        return gameTicks.size();
    }

    @Override
    public int hashCode() {
        return y << 24 ^ x ^ z ^ getWorld().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GlowBlock other = (GlowBlock) obj;
        return x == other.x && y == other.y && z == other.z && getWorld().equals(other.getWorld());
    }

    /**
     * The metadata store class for blocks.
     */
    private static final class BlockMetadataStore extends MetadataStoreBase<Block> implements
        MetadataStore<Block> {

        @Override
        protected @NotNull String disambiguate(Block subject, @NotNull String metadataKey) {
            return subject.getWorld() + "," + subject.getX() + "," + subject.getY() + "," + subject
                .getZ() + ":" + metadataKey;
        }
    }
}

