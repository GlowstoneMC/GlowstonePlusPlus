package net.glowstone.entity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.destroystokyo.paper.ClientOption;
import com.destroystokyo.paper.MaterialTags;
import com.destroystokyo.paper.Title;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.flowpowered.network.Message;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import net.glowstone.EventFactory;
import net.glowstone.GlowOfflinePlayer;
import net.glowstone.GlowWorld;
import net.glowstone.GlowWorldBorder;
import net.glowstone.block.GlowBlock;
import net.glowstone.block.ItemTable;
import net.glowstone.block.blocktype.BlockBed;
import net.glowstone.block.entity.SignEntity;
import net.glowstone.block.itemtype.ItemFood;
import net.glowstone.block.itemtype.ItemType;
import net.glowstone.chunk.ChunkManager.ChunkLock;
import net.glowstone.chunk.GlowChunk;
import net.glowstone.chunk.GlowChunk.Key;
import net.glowstone.constants.GameRules;
import net.glowstone.constants.GlowBlockEntity;
import net.glowstone.constants.GlowEffect;
import net.glowstone.constants.GlowParticle;
import net.glowstone.constants.GlowSound;
import net.glowstone.entity.meta.ClientSettings;
import net.glowstone.entity.meta.MetadataIndex;
import net.glowstone.entity.meta.MetadataIndex.StatusFlags;
import net.glowstone.entity.meta.MetadataMap;
import net.glowstone.entity.meta.profile.GlowPlayerProfile;
import net.glowstone.entity.meta.profile.ProfileCache;
import net.glowstone.entity.monster.GlowBoss;
import net.glowstone.entity.objects.GlowItem;
import net.glowstone.entity.passive.GlowFishingHook;
import net.glowstone.inventory.GlowInventory;
import net.glowstone.inventory.GlowInventoryView;
import net.glowstone.inventory.InventoryMonitor;
import net.glowstone.inventory.ToolType;
import net.glowstone.inventory.crafting.PlayerRecipeMonitor;
import net.glowstone.io.PlayerDataService.PlayerReader;
import net.glowstone.map.GlowMapCanvas;
import net.glowstone.net.GlowSession;
import net.glowstone.net.message.play.entity.DestroyEntitiesMessage;
import net.glowstone.net.message.play.entity.EntityAnimationMessage;
import net.glowstone.net.message.play.entity.EntityMetadataMessage;
import net.glowstone.net.message.play.entity.EntityVelocityMessage;
import net.glowstone.net.message.play.entity.SetPassengerMessage;
import net.glowstone.net.message.play.game.BlockBreakAnimationMessage;
import net.glowstone.net.message.play.game.BlockChangeMessage;
import net.glowstone.net.message.play.game.ChatMessage;
import net.glowstone.net.message.play.game.ExperienceMessage;
import net.glowstone.net.message.play.game.HealthMessage;
import net.glowstone.net.message.play.game.JoinGameMessage;
import net.glowstone.net.message.play.game.MapDataMessage;
import net.glowstone.net.message.play.game.MultiBlockChangeMessage;
import net.glowstone.net.message.play.game.NamedSoundEffectMessage;
import net.glowstone.net.message.play.game.PlayEffectMessage;
import net.glowstone.net.message.play.game.PlayParticleMessage;
import net.glowstone.net.message.play.game.PluginMessage;
import net.glowstone.net.message.play.game.PositionRotationMessage;
import net.glowstone.net.message.play.game.RespawnMessage;
import net.glowstone.net.message.play.game.SignEditorMessage;
import net.glowstone.net.message.play.game.SpawnPositionMessage;
import net.glowstone.net.message.play.game.StateChangeMessage;
import net.glowstone.net.message.play.game.StateChangeMessage.Reason;
import net.glowstone.net.message.play.game.StopSoundMessage;
import net.glowstone.net.message.play.game.TimeMessage;
import net.glowstone.net.message.play.game.TitleMessage;
import net.glowstone.net.message.play.game.TitleMessage.Action;
import net.glowstone.net.message.play.game.UnloadChunkMessage;
import net.glowstone.net.message.play.game.UpdateBlockEntityMessage;
import net.glowstone.net.message.play.game.UpdateSignMessage;
import net.glowstone.net.message.play.game.UserListHeaderFooterMessage;
import net.glowstone.net.message.play.game.UserListItemMessage;
import net.glowstone.net.message.play.game.UserListItemMessage.Entry;
import net.glowstone.net.message.play.inv.CloseWindowMessage;
import net.glowstone.net.message.play.inv.OpenWindowMessage;
import net.glowstone.net.message.play.inv.SetWindowContentsMessage;
import net.glowstone.net.message.play.inv.SetWindowSlotMessage;
import net.glowstone.net.message.play.inv.WindowPropertyMessage;
import net.glowstone.net.message.play.player.ResourcePackSendMessage;
import net.glowstone.scoreboard.GlowScoreboard;
import net.glowstone.scoreboard.GlowTeam;
import net.glowstone.util.Convert;
import net.glowstone.util.EntityUtils;
import net.glowstone.util.InventoryUtil;
import net.glowstone.util.Position;
import net.glowstone.util.StatisticMap;
import net.glowstone.util.TextMessage;
import net.glowstone.util.TickUtil;
import net.glowstone.util.nbt.CompoundTag;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.Effect.Type;
import org.bukkit.EntityAnimation;
import org.bukkit.GameMode;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Note;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Statistic;
import org.bukkit.Tag;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldType;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerUnregisterChannelEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.InventoryView.Property;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.map.MapView;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;


/**
 * Represents an in-game player.
 *
 * @author Graham Edgecombe
 */
@DelegateDeserialization(GlowOfflinePlayer.class)
public class GlowPlayer extends GlowHumanEntity implements Player {

    /**
     * Max distance of a fishing hook.
     */
    public static final int HOOK_MAX_DISTANCE = 32;

    /**
     * The network session attached to this player.
     *
     * @return The GlowSession of the player.
     */
    @Getter
    private final GlowSession session;

    /**
     * The entities that the client knows about. Guarded by {@link #worldLock}.
     */
    private final Set<GlowEntity> knownEntities = new HashSet<>();

    /**
     * The entities that are hidden from the client.
     */
    private final Set<UUID> hiddenEntities = new HashSet<>();

    /**
     * The chunks that the client knows about.
     */
    private final Set<Key> knownChunks = new HashSet<>();

    /**
     * A queue of BlockChangeMessages to be sent.
     */
    private final Queue<BlockChangeMessage> blockChanges = new ConcurrentLinkedDeque<>();

    /**
     * A queue of messages that should be sent after block changes are processed.
     *
     * <p>Used for sign updates and other situations where the block must be sent first.
     */
    private final List<Message> afterBlockChanges = new LinkedList<>();

    /**
     * The set of plugin channels this player is listening on.
     */
    private final Set<String> listeningChannels = new HashSet<>();

    /**
     * The player's statistics, and related data.
     */
    private final StatisticMap stats = new StatisticMap();

    /**
     * Whether the player has played before (will be false on first join).
     */
    private final boolean hasPlayedBefore;

    /**
     * The time the player first played, or 0 if unknown.
     */
    @Getter
    private final long firstPlayed;

    /**
     * The time the player last played, or 0 if unknown.
     */
    @Getter
    private final long lastPlayed;
    @Getter
    private final PlayerRecipeMonitor recipeMonitor;
    /**
     * Current casted fishing hook.
     */
    private final AtomicReference<GlowFishingHook> currentFishingHook = new AtomicReference<>(null);
    public Location teleportedTo = null;
    @Setter
    public boolean affectsSpawning = true;
    /**
     * The time the player joined, in milliseconds, to be saved as last played time.
     *
     * @return The player's join time.
     */
    @Getter
    private long joinTime;
    /**
     * The settings sent by the client.
     */
    private ClientSettings settings = ClientSettings.DEFAULT;
    /**
     * The lock used to prevent chunks from unloading near the player.
     */
    private ChunkLock chunkLock;
    /**
     * The tracker for changes to the currently open inventory.
     */
    private InventoryMonitor invMonitor;
    /**
     * The display name of this player, for chat purposes.
     */
    private String displayName;
    /**
     * The name a player has in the player list.
     */
    private String playerListName;
    /**
     * Cumulative amount of experience points the player has collected.
     */
    @Getter
    private int totalExperience;
    /**
     * The current level (or skill point amount) of the player.
     */
    @Getter
    private int level;
    /**
     * The progress made to the next level, from 0 to 1.
     */
    @Getter
    private float exp;
    /**
     * The human entity's current food level.
     */
    @Getter
    private int foodLevel = 20;
    /**
     * The player's current exhaustion level.
     */
    @Getter
    @Setter
    private float exhaustion;
    /**
     * The player's current saturation level.
     */
    @Getter
    private float saturation;
    /**
     * Whether to perform special scaling of the player's health.
     */
    @Getter
    private boolean healthScaled;
    /**
     * The scale at which to display the player's health.
     */
    @Getter
    private double healthScale = 20;
    /**
     * If this player has seen the end credits.
     */
    @Getter
    @Setter
    private boolean seenCredits;
    /**
     * Recipes this player has unlocked.
     */
    private Collection<Recipe> recipes = new HashSet<>();
    /**
     * This player's current time offset.
     */
    private long timeOffset;
    /**
     * Whether the time offset is relative.
     */
    @Getter
    private boolean playerTimeRelative = true;
    /**
     * The player-specific weather, or null for normal weather.
     */
    private WeatherType playerWeather;
    /**
     * The player's compass target.
     */
    @Getter
    private Location compassTarget;
    /**
     * Whether this player's sleeping state is ignored when changing time.
     */
    private boolean sleepingIgnored;
    /**
     * The bed in which the player currently lies.
     */
    private GlowBlock bed;
    /**
     * The bed spawn location of a player.
     */
    private Location bedSpawn;
    /**
     * Whether to use the bed spawn even if there is no bed block.
     *
     * @return Whether the player is forced to spawn at their bed.
     */
    @Getter
    private boolean bedSpawnForced;
    // TODO: 1.16 what does this do?
    @Getter
    @Setter
    private float hurtDirection;
    /**
     * The location of the sign the player is currently editing, or null.
     */
    private Location signLocation;
    /**
     * Whether the player is permitted to fly.
     */
    private boolean canFly;
    /**
     * Whether the player is currently flying.
     */
    @Getter
    private boolean flying;
    /**
     * The player's base flight speed.
     */
    @Getter
    private float flySpeed = 0.1f;
    /**
     * The player's base walking speed.
     */
    @Getter
    private float walkSpeed = 0.2f;
    /**
     * The scoreboard the player is currently subscribed to.
     */
    private GlowScoreboard scoreboard;
    /**
     * The player's current title, if any.
     */
    private Title.Builder currentTitle = new Title.Builder();
    /**
     * The one block the player is currently digging.
     */
    @Getter
    private GlowBlock digging;
    /**
     * The number of ticks elapsed since the player started digging.
     */
    private long diggingTicks = 0;
    /**
     * The total number of ticks needed to dig the current block.
     */
    private long totalDiggingTicks = Long.MAX_VALUE;
    /**
     * The one itemstack the player is currently usage and associated time.
     */
    @Getter
    @Setter
    private ItemStack usageItem;
    @Getter
    private int usageTime;
    @Getter
    private int startingUsageTime;
    private Entity spectating;
    private HashMap<Advancement, AdvancementProgress> advancements;
    private String resourcePackHash;
    private PlayerResourcePackStatusEvent.Status resourcePackStatus;
    private List<Conversation> conversations = new ArrayList<>();
    private Set<BossBar> bossBars = ConcurrentHashMap.newKeySet();
    /**
     * The player's previous chunk x coordinate.
     */
    private int prevCentralX;
    /**
     * The player's previous chunk x coordinate.
     */
    private int prevCentralZ;
    /**
     * If this is the player's first time getting blocks streamed.
     */
    private boolean firstStream = true;
    /**
     * If we should force block streaming regardless of chunk difference.
     */
    private boolean forceStream = false;
    private final Player.Spigot spigot = new Player.Spigot() {
        @Deprecated
        public void playEffect(Location location, Effect effect, int id, int data, float offsetX,
                               float offsetY, float offsetZ, float speed, int particleCount,
                               int radius) {
            if (effect.getType() == Type.VISUAL && particleCount > 0) {
                MaterialData material = new MaterialData(
                    server.getBlockDataManager().convertToBlockData(id).getMaterial(),
                    (byte) data
                );
                showParticle(location, effect, material, offsetX, offsetY, offsetZ, speed,
                    particleCount);
            } else {
                GlowPlayer.this.playEffect(location, effect, data);
            }
        }

        @Override
        public @NotNull InetSocketAddress getRawAddress() {
            return session.getAddress();
        }

        @Override
        public void respawn() {
            GlowPlayer.this.respawn();
        }

        @Override
        public boolean getCollidesWithEntities() {
            return isCollidable();
        }

        @Override
        public void setCollidesWithEntities(boolean collides) {
            setCollidable(collides);
        }

        @Override
        public @NotNull Set<Player> getHiddenPlayers() {
            return hiddenEntities.stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        }

        @Override
        public void sendMessage(@NotNull ChatMessageType position, BaseComponent... components) {
            GlowPlayer.this.sendMessage(position, components);
        }

        @Override
        public void sendMessage(@NotNull ChatMessageType position,
                                @NotNull BaseComponent component) {
            GlowPlayer.this.sendMessage(position, component);
        }

        @Override
        public void sendMessage(BaseComponent... components) {
            GlowPlayer.this.sendMessage(components);
        }

        @Override
        public void sendMessage(@NotNull BaseComponent component) {
            GlowPlayer.this.sendMessage(component);
        }
    };
    /**
     * The player's ender pearl cooldown game tick counter.
     * 1 second, or 20 game ticks by default.
     * The player can use ender pearl again if equals 0.
     */
    @Getter
    @Setter
    private int enderPearlCooldown = 0;

    @Getter
    @Setter
    @Nullable
    private String playerListHeader;
    @Getter
    @Setter
    @Nullable
    private String playerListFooter;

    /**
     * Creates a new player and adds it to the world.
     *
     * @param session The player's session.
     * @param profile The player's profile with name and UUID information.
     * @param reader  The PlayerReader to be used to initialize the player.
     */
    public GlowPlayer(GlowSession session, GlowPlayerProfile profile, PlayerReader reader) {
        super(initLocation(session, reader), profile);
        setBoundingBox(0.6, 1.8);
        this.session = session;

        chunkLock = world.newChunkLock(getName());

        // read data from player reader
        hasPlayedBefore = reader.hasPlayedBefore();
        if (hasPlayedBefore) {
            firstPlayed = reader.getFirstPlayed();
            lastPlayed = reader.getLastPlayed();
            bedSpawn = reader.getBedSpawnLocation();
        } else {
            firstPlayed = 0;
            lastPlayed = 0;
        }

        //creates InventoryMonitor to avoid NullPointerException
        invMonitor = new InventoryMonitor(getOpenInventory());
        server.getPlayerStatisticIoService().readStatistics(this);
        recipeMonitor = new PlayerRecipeMonitor(this);

        updateBossBars();
    }

    /**
     * Read the location from a PlayerReader for entity initialization.
     *
     * <p>Will fall back to a reasonable default rather than returning null.
     *
     * @param session The player's session.
     * @param reader  The PlayerReader to get the location from.
     * @return The location to spawn the player.
     */
    private static Location initLocation(GlowSession session, PlayerReader reader) {
        if (reader.hasPlayedBefore()) {
            Location loc = reader.getLocation();
            if (loc != null) {
                return loc;
            }
        }

        return findSafeSpawnLocation(session.getServer().getWorlds().get(0).getSpawnLocation());
    }

    ////////////////////////////////////////////////////////////////////////////
    // Internals

    /**
     * Find a a Location obove or below the specified Location, which is on ground.
     *
     * <p>The returned Location will be at the center of the block, X and Y wise.
     *
     * @param spawn The Location a safe spawn position should be found at.
     * @return The location to spawn the player at.
     */
    private static Location findSafeSpawnLocation(Location spawn) {
        World world = spawn.getWorld();
        int blockX = spawn.getBlockX();
        int blockY = spawn.getBlockY();
        int blockZ = spawn.getBlockZ();

        int highestY = world.getHighestBlockYAt(blockX, blockZ);

        int y = blockY;
        boolean wasPreviousSafe = false;
        for (; y <= highestY; y++) {
            Material type = world.getBlockAt(blockX, y, blockZ).getType();
            boolean safe = Material.AIR.equals(type);

            if (wasPreviousSafe && safe) {
                y--;
                break;
            }
            wasPreviousSafe = safe;
        }

        return new Location(world, blockX + 0.5, y, blockZ + 0.5);
    }

    /**
     * Returns the current fishing hook.
     *
     * @return the current fishing hook, or null if not fishing
     */
    public GlowFishingHook getCurrentFishingHook() {
        return currentFishingHook.get();
    }

    /**
     * Removes the current fishing hook, if any, and sets a new one.
     *
     * @param fishingHook the new fishing hook, or null to stop fishing
     */
    public void setCurrentFishingHook(GlowFishingHook fishingHook) {
        GlowFishingHook oldHook = currentFishingHook.getAndSet(fishingHook);
        if (oldHook != null && !(oldHook.equals(fishingHook)) && !oldHook.isDead()) {
            oldHook.remove();
        }
    }

    /**
     * Loads the player's state and sends the messages that are necessary on login.
     *
     * @param session the player's session
     * @param reader  the source of the player's saved state
     */
    public void join(GlowSession session, PlayerReader reader) {
        String type = world.getWorldType().getName().toLowerCase();

        reader.readData(this);
        reader.close();

        int gameMode = getGameMode().getValue();
        setGameModeDefaults();

        session.send(new JoinGameMessage(
            getEntityId(),
            world.isHardcore(),
            gameMode,
            -1, // TODO: determine previous gamemode
            server.getWorlds().stream().map(World::getName).toArray(String[]::new),
            world.getName(),
            world.getSeedHash(),
            server.getMaxPlayers(),
            world.getViewDistance(),
            world.getGameRuleMap().getBoolean(GameRules.REDUCED_DEBUG_INFO),
            !world.getGameRuleMap().getBoolean(GameRules.DO_IMMEDIATE_RESPAWN),
            false, // TODO: Debug worlds
            world.getWorldType() == WorldType.FLAT
        ));

        // send server brand and supported plugin channels
        Message pluginMessage = PluginMessage.fromString("minecraft:brand", server.getName());
        if (pluginMessage != null) {
            session.send(pluginMessage);
        }
        sendSupportedChannels();
        joinTime = System.currentTimeMillis();

        // Add player to list of online players
        getServer().setPlayerOnline(this, true);

        // save data back out
        saveData();

        streamBlocks(); // stream the initial set of blocks
        sendWeather();
        sendRainDensity();
        sendSkyDarkness();
        getServer().sendPlayerAbilities(this);

        // send initial location
        session.send(new PositionRotationMessage(location));

        session.send(((GlowWorldBorder) world.getWorldBorder()).createMessage());
        sendTime();
        setCompassTarget(world.getSpawnLocation()); // set our compass target

        scoreboard = server.getScoreboardManager().getMainScoreboard();
        scoreboard.subscribe(this);

        invMonitor = new InventoryMonitor(getOpenInventory());
        updateInventory(); // send inventory contents
        session.send(recipeMonitor.createInitMessage());

        if (!server.getResourcePackUrl().isEmpty()) {
            setResourcePack(server.getResourcePackUrl(), server.getResourcePackHash());
        }
    }

    @Override
    public String toString() {
        return "GlowPlayer{name=" + getName() + "}";
    }

    @Override
    public void damage(double amount) {
        damage(amount, DamageCause.CUSTOM);
    }

    @Override
    public void damage(double amount, Entity cause) {
        super.damage(amount, cause);
        sendHealth();
    }

    @Override
    public void damage(double amount, Entity source, @NotNull DamageCause cause) {
        boolean pvpAllowed = server.isPvpEnabled() && world.getPVP();
        if (!pvpAllowed) {
            if (source instanceof Player) {
                return;
            }
            if (cause == DamageCause.PROJECTILE && source instanceof Projectile) {
                Projectile projectile = (Projectile) source;
                if (projectile.getShooter() instanceof Player) {
                    return;
                }
            }
        }
        super.damage(amount, source, cause);
    }

    @Override
    public void damage(double amount, DamageCause cause) {
        // todo: better idea
        double old = getHealth();
        super.damage(amount, cause);
        if (old != getHealth()) {
            addExhaustion(0.1f);
            sendHealth();
            incrementStatistic(Statistic.DAMAGE_TAKEN, (int) Math.round(amount));
        }
    }

    @Override
    public boolean canTakeDamage(DamageCause damageCause) {
        return damageCause == DamageCause.FALL ? !getAllowFlight() && super
            .canTakeDamage(damageCause) : super.canTakeDamage(damageCause);
    }

    /**
     * Kicks this player.
     */
    @Override
    public void remove() {
        knownChunks.clear();
        chunkLock.clear();
        saveData();
        getInventory().removeViewer(this);
        getInventory().getCraftingInventory().removeViewer(this);
        permissions.clearPermissions();
        getServer().setPlayerOnline(this, false);
        getWorld().getRawPlayers().remove(this);

        if (scoreboard != null) {
            scoreboard.unsubscribe(this);
            scoreboard = null;
        }
        clearBossBars();
        super.remove();
    }

    /**
     * Handle player disconnection.
     *
     * @param async if true, the player's data is saved asynchronously
     */
    public void remove(boolean async) {
        knownChunks.clear();
        chunkLock.clear();
        saveData(async);
        getInventory().removeViewer(this);
        getInventory().getCraftingInventory().removeViewer(this);
        permissions.clearPermissions();
        getServer().setPlayerOnline(this, false);

        if (scoreboard != null) {
            scoreboard.unsubscribe(this);
            scoreboard = null;
        }
        clearBossBars();
        super.remove();
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    public void pulse() {
        super.pulse();
        incrementStatistic(Statistic.TIME_SINCE_DEATH);

        if (usageItem != null) {
            if (usageItem.equals(getItemInHand())) { //todo: implement offhand
                if (--usageTime == 0) {
                    ItemType item = ItemTable.instance().getItem(usageItem.getType());
                    if (item instanceof ItemFood) {
                        ((ItemFood) item).eat(this, usageItem);
                    }
                }
            } else {
                usageItem = null;
                usageTime = 0;
            }
        }

        if (digging != null) {
            pulseDigging();
        }

        if (exhaustion > 4.0f) {
            exhaustion -= 4.0f;

            if (saturation > 0f) {
                saturation = Math.max(saturation - 1f, 0f);
                sendHealth();
            } else if (world.getDifficulty() != Difficulty.PEACEFUL) {
                FoodLevelChangeEvent event = EventFactory.getInstance()
                    .callEvent(new FoodLevelChangeEvent(this, Math.max(foodLevel - 1, 0)));
                if (!event.isCancelled()) {
                    foodLevel = event.getFoodLevel();
                }
                sendHealth();
            }
        }

        if (getHealth() < getMaxHealth() && !isDead()) {
            if (foodLevel >= 18 && ticksLived % 80 == 0
                || world.getDifficulty() == Difficulty.PEACEFUL) {
                EntityUtils.heal(this, 1, EntityRegainHealthEvent.RegainReason.SATIATED);
                exhaustion = Math.min(exhaustion + 3.0f, 40.0f);

                saturation -= 3;
            }
        }

        // Process food level and starvation based on difficulty.
        switch (world.getDifficulty()) {
            case PEACEFUL: {
                if (foodLevel < 20 && ticksLived % 20 == 0) {
                    foodLevel++;
                }
                break;
            }
            case EASY: {
                if (foodLevel == 0 && getHealth() > 10 && ticksLived % 80 == 0) {
                    damage(1, DamageCause.STARVATION);
                }
                break;
            }
            case NORMAL: {
                if (foodLevel == 0 && getHealth() > 1 && ticksLived % 80 == 0) {
                    damage(1, DamageCause.STARVATION);
                }
                break;
            }
            case HARD: {
                if (foodLevel == 0 && ticksLived % 80 == 0) {
                    damage(1, DamageCause.STARVATION);
                }
                break;
            }
            default: {
                // Do nothing when there are other game difficulties.
            }
        }

        // process ender pearl cooldown, decrease by 1 every game tick.
        if (enderPearlCooldown > 0) {
            enderPearlCooldown--;
        }

        // stream world
        streamBlocks();
        processBlockChanges();

        // add to playtime (despite inaccurate name, this counts ticks rather than minutes)
        incrementStatistic(Statistic.PLAY_ONE_MINUTE);
        if (isSneaking()) {
            incrementStatistic(Statistic.SNEAK_TIME);
        }

        // update inventory
        for (InventoryMonitor.Entry entry : invMonitor.getChanges()) {
            sendItemChange(entry.slot, entry.item);
        }

        // send changed metadata
        List<MetadataMap.Entry> changes = metadata.getChanges();
        if (!changes.isEmpty()) {
            session.send(new EntityMetadataMessage(getEntityId(), changes));
        }

        // Entity IDs are only unique per world, so we can't spawn or teleport between worlds while
        // updating them.
        worldLock.writeLock().lock();
        try {
            // update or remove entities
            List<GlowEntity> destroyEntities = new LinkedList<>();
            for (Iterator<GlowEntity> it = knownEntities.iterator(); it.hasNext(); ) {
                GlowEntity entity = it.next();
                if (!isWithinDistance(entity) || entity.isRemoved()) {
                    destroyEntities.add(entity);
                } else {
                    entity.createUpdateMessage(session).forEach(session::send);
                }
            }
            if (!destroyEntities.isEmpty()) {
                List<Integer> destroyIds = new ArrayList<>(destroyEntities.size());
                for (GlowEntity entity : destroyEntities) {
                    knownEntities.remove(entity);
                    destroyIds.add(entity.getEntityId());
                }
                session.send(new DestroyEntitiesMessage(destroyIds));
            }
            // add entities
            knownChunks.forEach(key ->
                world.getChunkAt(key.getX(), key.getZ()).getRawEntities().stream()
                    .filter(entity -> this != entity
                        && isWithinDistance(entity)
                        && !entity.isDead()
                        && !knownEntities.contains(entity)
                        && !hiddenEntities.contains(entity.getUniqueId()))
                    .forEach((entity) -> Bukkit.getScheduler()
                        .runTaskAsynchronously(null, () -> {
                            worldLock.readLock().lock();
                            try {
                                knownEntities.add(entity);
                            } finally {
                                worldLock.readLock().unlock();
                            }
                            entity.createSpawnMessage().forEach(session::send);
                            entity.createAfterSpawnMessage(session)
                                .forEach(session::send);
                        })));
        } finally {
            worldLock.writeLock().unlock();
        }

        if (passengerChanged) {
            session.send(new SetPassengerMessage(getEntityId(), getPassengers().stream()
                .mapToInt(Entity::getEntityId).toArray()));
        }
        getAttributeManager().sendMessages(session);

        GlowFishingHook hook = currentFishingHook.get();
        if (hook != null) {
            // The line will disappear if the player wanders more than 32 blocks away from the
            // bobber, or if the player stops holding a fishing rod.
            if (getInventory().getItemInMainHand().getType() != Material.FISHING_ROD
                && getInventory().getItemInOffHand().getType() != Material.FISHING_ROD) {
                setCurrentFishingHook(null);
            }

            if (hook.location.distanceSquared(location) > HOOK_MAX_DISTANCE * HOOK_MAX_DISTANCE) {
                setCurrentFishingHook(null);
            }
        }
    }

    @Override
    protected void pulsePhysics() {
        // trust the client with physics
        // just update the bounding box
        updateBoundingBox();
    }

    @Override
    protected void jump() {
        // don't make the client jump, please
    }

    /**
     * Process and send pending BlockChangeMessages.
     */
    private void processBlockChanges() {
        for (Key key : knownChunks) {
            GlowChunk chunk = world.getChunk(key);
            if (chunk == null) {
                return;
            }
            List<BlockChangeMessage> messages = chunk.getBlockChanges();
            int size = messages.size();
            if (size == 1) {
                session.send(messages.get(0));
            } else if (size > 1) {
                session.send(new MultiBlockChangeMessage(key.getX(), key.getZ(), messages));
            }
        }
        processPersonalBlockChanges();
        // now send post-block-change messages
        List<Message> postMessages = new ArrayList<>(afterBlockChanges);
        afterBlockChanges.clear();
        postMessages.forEach(session::send);
    }

    /**
     * Process and send packets sent specifically to us.
     */
    private void processPersonalBlockChanges() {
        if (blockChanges.isEmpty()) {
            return;
        }
        // separate messages by chunk
        // inner map is used to only send one entry for same coordinates
        Map<Key, Map<BlockVector, BlockChangeMessage>> chunks = new HashMap<>();
        BlockChangeMessage message;
        while ((message = blockChanges.poll()) != null) {
            Key key = GlowChunk.Key.of(message.getX() >> 4, message.getZ() >> 4);
            if (canSeeChunk(key)) {
                Map<BlockVector, BlockChangeMessage> map = chunks
                    .computeIfAbsent(key, k -> new HashMap<>());
                map.put(new BlockVector(message.getX(), message.getY(), message
                    .getZ()), message);
            }
        }
        // send away
        for (Map.Entry<Key, Map<BlockVector, BlockChangeMessage>> entry : chunks.entrySet()) {
            Key key = entry.getKey();
            List<BlockChangeMessage> value = new ArrayList<>(entry.getValue().values());

            if (value.size() == 1) {
                session.send(value.get(0));
            } else if (value.size() > 1) {
                session.send(new MultiBlockChangeMessage(key.getX(), key.getZ(), value));
            }
        }
    }

    /**
     * Streams chunks to the player's client.
     */
    private void streamBlocks() {
        Set<Key> previousChunks = null;
        ArrayList<Key> newChunks = new ArrayList<>();

        int centralX = location.getBlockX() >> 4;
        int centralZ = location.getBlockZ() >> 4;
        int radius = Math.min(server.getViewDistance(), 1 + settings.getViewDistance());

        if (firstStream) {
            firstStream = false;
            for (int x = centralX - radius; x <= centralX + radius; x++) {
                for (int z = centralZ - radius; z <= centralZ + radius; z++) {
                    newChunks.add(GlowChunk.Key.of(x, z));
                }
            }
        } else if (Math.abs(centralX - prevCentralX) > radius
            || Math.abs(centralZ - prevCentralZ) > radius) {
            knownChunks.clear();
            for (int x = centralX - radius; x <= centralX + radius; x++) {
                for (int z = centralZ - radius; z <= centralZ + radius; z++) {
                    newChunks.add(GlowChunk.Key.of(x, z));
                }
            }
        } else if (forceStream || prevCentralX != centralX || prevCentralZ != centralZ) {
            previousChunks = new HashSet<>(knownChunks);
            for (int x = centralX - radius; x <= centralX + radius; x++) {
                for (int z = centralZ - radius; z <= centralZ + radius; z++) {
                    Key key = GlowChunk.Key.of(x, z);
                    if (knownChunks.contains(key)) {
                        previousChunks.remove(key);
                    } else {
                        newChunks.add(key);
                    }
                }
            }
        } else {
            return; // early end if there's no changes
        }

        prevCentralX = centralX;
        prevCentralZ = centralZ;

        // sort chunks by distance from player - closer chunks sent first
        newChunks.sort((a, b) -> {
            double dx = 16 * a.getX() + 8 - location.getX();
            double dz = 16 * a.getZ() + 8 - location.getZ();
            double da = dx * dx + dz * dz;
            dx = 16 * b.getX() + 8 - location.getX();
            dz = 16 * b.getZ() + 8 - location.getZ();
            double db = dx * dx + dz * dz;
            return Double.compare(da, db);
        });

        // populate then send chunks to the player
        // done in two steps so that all the new chunks are finalized before any of them are sent
        // this prevents sending a chunk then immediately sending block changes in it because
        // one of its neighbors has populated

        // first step: force population then acquire lock on each chunk
        newChunks.forEach(newChunk -> {
            world.getChunkManager().forcePopulation(newChunk.getX(), newChunk.getZ());
            knownChunks.add(newChunk);
            chunkLock.acquire(newChunk);
        });

        boolean skylight = world.getEnvironment() == Environment.NORMAL;
        ByteBufAllocator alloc = session.getChannel() == null ? null : session.getChannel().alloc();

        newChunks.stream().map(key -> world.getChunk(key).toMessage(skylight, true, alloc))
                .forEach(session::send);

        // send visible block entity data
        newChunks.stream().flatMap(key -> world.getChunkAt(key.getX(),
            key.getZ()).getRawBlockEntities().stream())
            .forEach(entity -> entity.update(this));

        // and remove old chunks
        if (previousChunks != null) {
            previousChunks.forEach(key -> {
                session.send(new UnloadChunkMessage(key.getX(), key.getZ()));
                knownChunks.remove(key);
                chunkLock.release(key);
            });
            previousChunks.clear();
        }
    }

    /**
     * Spawn the player at the given location after they have already joined.
     *
     * <p>Used for changing worlds and respawning after death.
     *
     * @param location The location to place the player.
     */
    private void spawnAt(Location location) {
        GlowWorld oldWorld;
        // switch worlds
        worldLock.writeLock().lock();
        try {
            oldWorld = world;
            world.getEntityManager().unregister(this);
            world = (GlowWorld) location.getWorld();
            world.getEntityManager().register(this);
            updateBossBars();
        } finally {
            worldLock.writeLock().unlock();
        }

        // switch chunk set
        // no need to send chunk unload messages - respawn unloads all chunks
        knownChunks.clear();
        chunkLock.clear();
        chunkLock = world.newChunkLock(getName());

        // spawn into world
        session.send(new RespawnMessage(
            world.getName(),
            world.getSeedHash(),
            getGameMode().getValue(),
            -1,
            false,
            world.getWorldType() == WorldType.FLAT,
            oldWorld.getEnvironment() != world.getEnvironment()
        ));

        setRawLocation(location, false); // take us to spawn position
        session.send(new PositionRotationMessage(location));
        teleportedTo = location.clone();
        setCompassTarget(world.getSpawnLocation()); // set our compass target

        streamBlocks(); // stream blocks

        sendWeather();
        sendRainDensity();
        sendSkyDarkness();
        sendTime();
        updateInventory();

        // fire world change if needed
        if (oldWorld != world) {
            session.send(((GlowWorldBorder) world.getWorldBorder()).createMessage());
            EventFactory.getInstance().callEvent(new PlayerChangedWorldEvent(this, oldWorld));
        }
    }

    /**
     * Remove all boss bars, then add back the ones whose world we're in.
     */
    private void updateBossBars() {
        clearBossBars();
        worldLock.readLock().lock();
        try {
            for (GlowBoss boss : world.getEntitiesByClass(GlowBoss.class)) {
                boss.addBarToPlayer(this);
            }
        } finally {
            worldLock.readLock().unlock();
        }
    }

    private void clearBossBars() {
        for (BossBar bar : bossBars) {
            removeBossBar(bar);
        }
    }

    /**
     * Respawn the player after they have died.
     */
    public void respawn() {
        if (!isDead()) {
            return;
        }

        // restore health
        setHealth(getMaxHealth());
        setFoodLevel(20);

        // reset fire ticks
        setFireTicks(0);

        worldLock.writeLock().lock();
        try {
            // determine spawn destination
            boolean spawnAtBed = true;
            Location dest = getBedSpawnLocation();
            if (dest == null) {
                dest = world.getSpawnLocation();
                spawnAtBed = false;
                if (bedSpawn != null) {
                    setBedSpawnLocation(null);
                    sendMessage("Your home bed was missing or obstructed");
                }
            }

            if (!spawnAtBed) {
                dest = findSafeSpawnLocation(dest);
            }

            // fire event and perform spawn
            PlayerRespawnEvent event = new PlayerRespawnEvent(this, dest, spawnAtBed);
            EventFactory.getInstance().callEvent(event);
            if (event.getRespawnLocation().getWorld().equals(getWorld()) && !knownEntities
                .isEmpty()) {
                // we need to manually reset all known entities if the player respawns in the
                // same world
                List<Integer> entityIds = new ArrayList<>(knownEntities.size());
                entityIds.addAll(knownEntities.stream().map(GlowEntity::getEntityId)
                    .collect(Collectors.toList()));
                session.send(new DestroyEntitiesMessage(entityIds));
                knownEntities.clear();
            }
            active = true;
            deathTicks = 0;
            setStatistic(Statistic.TIME_SINCE_DEATH, 0);
            spawnAt(event.getRespawnLocation());
        } finally {
            worldLock.writeLock().unlock();
        }
        // just in case any items are left in their inventory after they respawn
        updateInventory();
    }

    /**
     * Checks whether the player can see the given chunk.
     *
     * @param chunk The chunk to check.
     * @return If the chunk is known to the player's client.
     */
    public boolean canSeeChunk(Key chunk) {
        return knownChunks.contains(chunk);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Basic stuff

    /**
     * Checks whether the player can see the given entity.
     *
     * @param entity The entity to check.
     * @return If the entity is known to the player's client.
     */
    public boolean canSeeEntity(GlowEntity entity) {
        worldLock.readLock().lock();
        try {
            return knownEntities.contains(entity);
        } finally {
            worldLock.readLock().unlock();
        }
    }

    /**
     * Open the sign editor interface at the specified location.
     *
     * @param loc The location to open the editor at
     */
    public void openSignEditor(Location loc) {
        signLocation = loc.clone();
        signLocation.setX(loc.getBlockX());
        signLocation.setY(loc.getBlockY());
        signLocation.setZ(loc.getBlockZ());
        signLocation.setYaw(0);
        signLocation.setPitch(0);

        // Client closes inventory when sign editor is opened
        if (!GlowInventoryView.isDefault(getOpenInventory())) {
            closeInventory();
        }

        session.send(new SignEditorMessage(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    /**
     * Check that the specified location matches that of the last opened sign editor, and if so,
     * clears the last opened sign editor.
     *
     * @param loc The location to check
     * @return Whether the location matched.
     */
    public boolean checkSignLocation(Location loc) {
        if (loc.equals(signLocation)) {
            signLocation = null;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get a UserListItemMessage entry representing adding this player.
     *
     * @return The entry (action ADD_PLAYER) with this player's information.
     */
    public Entry getUserListEntry() {
        TextMessage displayName = null;
        if (playerListName != null && !playerListName.isEmpty()) {
            displayName = new TextMessage(playerListName);
        }
        return UserListItemMessage.add(getProfile(), getGameMode().getValue(), 0, displayName);
    }

    /**
     * Send a UserListItemMessage to every player that can see this player.
     *
     * @param updateMessage The message to send.
     */
    private void updateUserListEntries(UserListItemMessage updateMessage) {
        server.getRawOnlinePlayers().stream().filter(player -> player.canSee(this))
            .forEach(player -> player.getSession().send(updateMessage));
    }

    @Override
    public void setVelocity(Vector velocity) {
        PlayerVelocityEvent event = EventFactory.getInstance()
            .callEvent(new PlayerVelocityEvent(this, velocity));
        if (!event.isCancelled()) {
            velocity = event.getVelocity();
            super.setVelocity(velocity);
            session.send(new EntityVelocityMessage(getEntityId(), velocity));
        }
    }

    @Override
    public @NotNull BoundingBox getBoundingBox() {
        return null;
    }

    @Override
    public void setRotation(float yaw, float pitch) {

    }

    /**
     * Set this player's client settings.
     *
     * @param settings The settings to set.
     */
    public void setSettings(ClientSettings settings) {
        String newLocale = settings.getLocale();
        if (!newLocale.equalsIgnoreCase(this.settings.getLocale())) {
            EventFactory.getInstance().callEvent(new PlayerLocaleChangeEvent(this, newLocale));
        }
        if (settings.getMainHand() != getMainHand().ordinal()) {
            EventFactory.getInstance().callEvent(new PlayerChangedMainHandEvent(this,
                settings.getMainHand() == 0 ? MainHand.LEFT : MainHand.RIGHT));
        }
        forceStream = settings.getViewDistance() != this.settings.getViewDistance()
            && settings.getViewDistance() + 1 <= server.getViewDistance();
        this.settings = settings;
        metadata.set(MetadataIndex.PLAYER_SKIN_PARTS, settings.getSkinFlags());
        metadata.set(MetadataIndex.PLAYER_MAIN_HAND, settings.getMainHand());
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("name", getName()); // NON-NLS
        return ret;
    }

    @Override
    public EntityType getType() {
        return EntityType.PLAYER;
    }

    @Override
    public InetSocketAddress getAddress() {
        return session.getAddress();
    }

    @Override
    public int getProtocolVersion() {
        return session.getVersion();
    }

    @Nullable
    @Override
    public InetSocketAddress getVirtualHost() {
        return session.getVirtualHost();
    }

    ////////////////////////////////////////////////////////////////////////////
    // HumanEntity overrides

    @Override
    public boolean isOnline() {
        return session.isActive() && session.isOnline();
    }

    @Override
    public boolean isBanned() {
        return server.getBanList(BanList.Type.NAME).isBanned(getName());
    }

    @Override
    public boolean isWhitelisted() {
        return server.getWhitelist().containsProfile(
            new GlowPlayerProfile(getName(), getUniqueId(), true));
    }

    @Override
    public void setWhitelisted(boolean value) {
        if (value) {
            server.getWhitelist().add(this);
        } else {
            server.getWhitelist().remove(new GlowPlayerProfile(getName(), getUniqueId(), true));
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Editable properties

    @Override
    protected boolean hasDefaultLandingBehavior() {
        return false;
    }

    @Override
    public Player getPlayer() {
        return this;
    }

    @Override
    public boolean hasPlayedBefore() {
        return hasPlayedBefore;
    }

    @Override
    public boolean isOp() {
        return getServer().getOpsList().containsUuid(getUniqueId());
    }

    @Override
    public void setOp(boolean value) {
        if (value) {
            getServer().getOpsList().add(this);
        } else {
            getServer().getOpsList().remove(new GlowPlayerProfile(getName(), getUniqueId(), true));
        }
        permissions.recalculatePermissions();
    }

    @Override
    public List<Message> createSpawnMessage() {
        List<Message> result = super.createSpawnMessage();
        return result;
    }

    @Override
    public @NotNull Component displayName() {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public void displayName(@org.jetbrains.annotations.Nullable Component component) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public String getDisplayName() {
        if (displayName != null) {
            return displayName;
        }
        if (scoreboard != null) {
            GlowTeam team = (GlowTeam) scoreboard.getEntryTeam(getName());
            if (team != null) {
                return team.getPlayerDisplayName(getName());
            }
        }
        return getName();
    }

    @Override
    public void setDisplayName(String name) {
        displayName = name;
    }

    @Override
    public void playerListName(@org.jetbrains.annotations.Nullable Component component) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @org.jetbrains.annotations.Nullable Component playerListName() {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @org.jetbrains.annotations.Nullable Component playerListHeader() {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @org.jetbrains.annotations.Nullable Component playerListFooter() {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public String getPlayerListName() {
        return playerListName == null || playerListName.isEmpty() ? getName() : playerListName;
    }

    @Override
    public void setPlayerListName(String name) {
        // update state
        playerListName = name;

        // send update message
        TextMessage displayName = null;
        if (playerListName != null && !playerListName.isEmpty()) {
            displayName = new TextMessage(playerListName);
        }
        updateUserListEntries(UserListItemMessage.displayNameOne(getUniqueId(), displayName));
    }

    @Override
    public void setCompassTarget(Location loc) {
        compassTarget = loc;
        session.send(new SpawnPositionMessage(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    @Override
    public Location getBedSpawnLocation() {
        if (bedSpawn == null) {
            return null;
        }

        // Find head of bed
        GlowBlock block = (GlowBlock) bedSpawn.getBlock();
        GlowBlock head = BlockBed.getHead(block);
        GlowBlock foot = BlockBed.getFoot(block);
        if (head != null) {
            // If there is a bed, try to find an empty spot next to the bed
            if (MaterialTags.BEDS.isTagged(head.getType())) {
                Block spawn = BlockBed.getExitLocation(head, foot);
                return spawn == null ? null : spawn.getLocation().add(0.5, 0.1, 0.5);
            }
            if (bedSpawnForced) {
                Material bottom = head.getType();
                Material top = head.getRelative(BlockFace.UP).getType();
                // Do not check floor when forcing spawn
                if (BlockBed.isValidSpawn(bottom) && BlockBed.isValidSpawn(top)) {
                    return bedSpawn.clone().add(0.5, 0.1, 0.5); // No blocks are blocking the spawn
                }
            }
        }
        return null;
    }

    @Override
    public void setBedSpawnLocation(Location bedSpawn) {
        setBedSpawnLocation(bedSpawn, false);
    }

    @Override
    public void setBedSpawnLocation(Location location, boolean force) {
        bedSpawn = location;
        bedSpawnForced = force;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Entity status

    @Override
    public long getLastLogin() {
        return 0;
    }

    @Override
    public long getLastSeen() {
        return 0;
    }

    @Override
    public boolean sleep(@NotNull Location location, boolean force) {
        return false;
    }

    @Override
    public void wakeup(boolean setSpawnLocation) {

    }

    @Override
    public @NotNull Location getBedLocation() {
        return null;
    }

    @Override
    public boolean isSleepingIgnored() {
        return sleepingIgnored;
    }

    @Override
    public void setSleepingIgnored(boolean isSleeping) {
        sleepingIgnored = isSleeping;
    }

    @Override
    public void setGameMode(GameMode mode) {
        if (getGameMode() != mode) {
            PlayerGameModeChangeEvent event = new PlayerGameModeChangeEvent(this, mode);
            if (EventFactory.getInstance().callEvent(event).isCancelled()) {
                return;
            }

            super.setGameMode(mode);
            super.setFallDistance(0);
            updateUserListEntries(UserListItemMessage.gameModeOne(getUniqueId(), mode.getValue()));
            session.send(new StateChangeMessage(Reason.GAMEMODE, mode.getValue()));
        }
        setGameModeDefaults();
    }

    @Override
    public ItemStack getActiveItem() {
        return usageItem;
    }

    public void setUsageTime(int usageTime) {
        startingUsageTime = usageTime;
        this.usageTime = usageTime;
    }

    @Override
    public int getItemUseRemainingTime() {
        return usageTime;
    }

    @Override
    public int getHandRaisedTime() {
        return startingUsageTime - usageTime;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Player capabilities

    @Override
    public boolean isHandRaised() {
        return usageTime != 0;
    }

    private void setGameModeDefaults() {
        GameMode mode = getGameMode();
        setAllowFlight(mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR);
        metadata.setBit(MetadataIndex.STATUS, StatusFlags.INVISIBLE, mode == GameMode.SPECTATOR);
    }

    @Override
    public boolean isSneaking() {
        return metadata.getBit(MetadataIndex.STATUS, StatusFlags.SNEAKING);
    }

    @Override
    public void setSneaking(boolean sneak) {
        if (EventFactory.getInstance()
            .callEvent(new PlayerToggleSneakEvent(this, sneak)).isCancelled()) {
            return;
        }

        metadata.setBit(MetadataIndex.STATUS, StatusFlags.SNEAKING, sneak);
    }

    @Override
    public boolean isSprinting() {
        return metadata.getBit(MetadataIndex.STATUS, StatusFlags.SPRINTING);
    }

    @Override
    public void setSprinting(boolean sprinting) {
        if (EventFactory.getInstance()
            .callEvent(new PlayerToggleSprintEvent(this, sprinting)).isCancelled()) {
            return;
        }

        metadata.setBit(MetadataIndex.STATUS, StatusFlags.SPRINTING, sprinting);
    }

    @Override
    public double getEyeHeight() {
        return getEyeHeight(false);
    }

    @Override
    public double getEyeHeight(boolean ignoreSneaking) {
        // Height of player's eyes above feet. Matches CraftBukkit.
        if (ignoreSneaking || !isSneaking()) {
            return 1.62;
        } else {
            return 1.54;
        }
    }

    @Override
    public boolean getAllowFlight() {
        return canFly;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Experience and levelling

    @Override
    public void setAllowFlight(boolean flight) {
        canFly = flight;
        if (!canFly) {
            flying = false;
        }
        getServer().sendPlayerAbilities(this);
    }

    @Override
    public void setFlying(boolean value) {
        flying = value && canFly;
        getServer().sendPlayerAbilities(this);
    }

    @Override
    public void setFlySpeed(float value) throws IllegalArgumentException {
        flySpeed = value;
        getServer().sendPlayerAbilities(this);
    }

    @Override
    public void setWalkSpeed(float value) throws IllegalArgumentException {
        walkSpeed = value;
        getServer().sendPlayerAbilities(this);
    }

    @Override
    public void setLevel(int level) {
        int newLevel = Math.max(level, 0);

        if (newLevel != this.level) {
            EventFactory.getInstance().callEvent(
                new PlayerLevelChangeEvent(this, this.level, newLevel));
            this.level = newLevel;
            sendExperience();
        }
    }

    @Override
    public void setTotalExperience(int exp) {
        totalExperience = Math.max(exp, 0);
        sendExperience();
    }

    @Override
    public void sendExperienceChange(float v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void sendExperienceChange(float v, int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void giveExp(int xp) {
        PlayerExpChangeEvent event = EventFactory.getInstance()
            .callEvent(new PlayerExpChangeEvent(this, xp));
        xp = event.getAmount();
        totalExperience += xp;

        // gradually award levels based on xp points
        float value = 1.0f / getExpToLevel();
        for (int i = 0; i < xp; ++i) {
            exp += value;
            if (exp >= 1) {
                exp -= 1;
                setLevel(level + 1);
                value = 1.0f / getExpToLevel(level);
            }
        }
        sendExperience();
    }

    @Override
    public void giveExp(int xp, boolean applyMending) {
        // todo: implement applyMending
        giveExp(xp);
    }

    @Override
    public int applyMending(int amount) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void giveExpLevels(int amount) {
        setLevel(getLevel() + amount);
    }

    @Override
    public void setExp(float percentToLevel) {
        exp = Math.min(Math.max(percentToLevel, 0), 1);
        sendExperience();
    }

    @Override
    public int getExpToLevel() {
        return getExpToLevel(level);
    }

    private int getExpToLevel(int level) {
        if (level >= 30) {
            return 62 + (level - 30) * 7;
        } else if (level >= 15) {
            return 17 + (level - 15) * 3;
        } else {
            return 17;
        }
    }

    @Override
    public float getAttackCooldown() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void sendExperience() {
        session.send(new ExperienceMessage(getExp(), getLevel(), getTotalExperience()));
    }

    @Override
    public boolean discoverRecipe(@NotNull NamespacedKey recipe) {
        return false;
    }

    @Override
    public int discoverRecipes(@NotNull Collection<NamespacedKey> recipes) {
        return 0;
    }

    @Override
    public boolean undiscoverRecipe(@NotNull NamespacedKey recipe) {
        return false;
    }

    @Override
    public int undiscoverRecipes(@NotNull Collection<NamespacedKey> recipes) {
        return 0;
    }

    @Override
    public boolean hasDiscoveredRecipe(@NotNull NamespacedKey namespacedKey) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public @NotNull Set<NamespacedKey> getDiscoveredRecipes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    ////////////////////////////////////////////////////////////////////////////
    // Health and food handling

    @Override
    public boolean dropItem(boolean b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Recipes this player has unlocked.
     *
     * @return An immutable list of unlocked recipes.
     */
    public Collection<Recipe> getUnlockedRecipes() {
        return ImmutableList.copyOf(recipes);
    }

    /**
     * Teach the player a new recipe.
     *
     * @param recipe The recipe to be added to learnt recipes
     * @param notify If the player should be notified of the recipes learnt
     * @return If this recipe was not learned already.
     */
    public boolean learnRecipe(Recipe recipe, boolean notify) {
        return recipe != null && recipes.add(recipe);
    }

    /**
     * Remove a recipe from the player's known recipes.
     *
     * @param recipe The recipe to be removed from learnt recipes
     * @return If this recipe was learned before it was removed.
     */
    public boolean unlearnRecipe(Recipe recipe) {
        return recipes.remove(recipe);
    }

    /**
     * Checks to see if the player knows this recipe.
     *
     * @param recipe The recipe to check
     * @return If the player knows the recipe
     */
    public boolean knowsRecipe(Recipe recipe) {
        return recipes.contains(recipe);
    }

    @Override
    public void setHealth(double health) {
        super.setHealth(health);
        sendHealth();
    }

    @Override
    public void setMaxHealth(double health) {
        super.setMaxHealth(health);
        sendHealth();
    }

    @Override
    public void setHealthScaled(boolean scale) {
        healthScaled = scale;
        sendHealth();
    }

    @Override
    public void setHealthScale(double scale) throws IllegalArgumentException {
        healthScaled = true;
        healthScale = scale;
        sendHealth();
    }

    @Override
    public Entity getSpectatorTarget() {
        return spectating;
    }

    @Override
    public void setSpectatorTarget(Entity entity) {
        teleport(entity.getLocation(), PlayerTeleportEvent.TeleportCause.SPECTATE);
        spectating = entity;
    }

    /**
     * Updates the hunger bar and hunger saturation.
     *
     * @param food       the amount of food (in half-icons on the hunger bar)
     * @param saturation the amount of food saturation (in half-icons of food it will save)
     */
    public void setFoodLevelAndSaturation(int food, float saturation) {
        this.foodLevel = Math.max(Math.min(food, 20), 0);
        this.saturation = Math.min(this.saturation + food * saturation * 2.0F, this.foodLevel);
        sendHealth();
    }

    @Override
    public void setFoodLevel(int food) {
        this.foodLevel = Math.min(food, 20);
        sendHealth();
    }

    @Override
    public int getSaturatedRegenRate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setSaturatedRegenRate(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getUnsaturatedRegenRate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setUnsaturatedRegenRate(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getStarvationRate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setStarvationRate(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private boolean shouldCalculateExhaustion() {
        return getGameMode() == GameMode.SURVIVAL | getGameMode() == GameMode.ADVENTURE;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Actions

    /**
     * Increases the exhaustion counter, but applies the maximum.
     *
     * @param exhaustion the amount of exhaustion to add
     */
    // todo: effects
    // todo: swim
    // todo: jump
    // todo: food poisoning
    // todo: jump and sprint
    public void addExhaustion(float exhaustion) {
        if (shouldCalculateExhaustion()) {
            this.exhaustion = Math.min(this.exhaustion + exhaustion, 40f);
        }
    }

    /**
     * Add the exhaustion for sprinting from the given location to the current location, if this
     * player <em>is</em> sprinting.
     *
     * @param move the previous location
     */
    public void addMoveExhaustion(Location move) {
        if (shouldCalculateExhaustion() && !teleported && isSprinting()) {
            double distanceSquared = location.distanceSquared(move);
            if (distanceSquared > 0) { // update packet and rotation
                double distance = Math.sqrt(distanceSquared);
                addExhaustion((float) (0.1f * distance));
            }
        }
    }

    @Override
    public void setSaturation(float value) {
        saturation = Math.min(value, foodLevel);
        sendHealth();
    }

    private void sendHealth() {
        float finalHealth = (float) (getHealth() / getMaxHealth() * getHealthScale());
        session.send(new HealthMessage(finalHealth, getFoodLevel(), getSaturation()));
    }

    /**
     * Teleport the player.
     *
     * @param location The destination to teleport to.
     * @return Whether the teleport was a success.
     */
    @Override
    public boolean teleport(Location location) {
        return teleport(location, TeleportCause.UNKNOWN);
    }

    @Override
    public boolean teleport(Location location, TeleportCause cause) {
        checkNotNull(location, "location cannot be null"); // NON-NLS
        checkNotNull(location.getWorld(), "location's world cannot be null"); // NON-NLS
        checkNotNull(cause, "cause cannot be null"); // NON-NLS
        if (this.location != null && this.location.getWorld() != null) {
            PlayerTeleportEvent event
                = new PlayerTeleportEvent(this, this.location, location, cause);
            if (EventFactory.getInstance().callEvent(event).isCancelled()) {
                return false;
            }
            location = event.getTo();
            closeInventory();
        }
        worldLock.writeLock().lock();
        try {
            if (location.getWorld() != world) {
                spawnAt(location);
            } else {
                world.getEntityManager().move(this, location);
                //Position.copyLocation(location, this.previousLocation);
                //Position.copyLocation(location, this.location);
                session.send(new PositionRotationMessage(location));
                teleportedTo = location.clone();
            }
        } finally {
            worldLock.writeLock().unlock();
        }

        teleportedTo = location.clone();
        return true;
    }

    /**
     * Finishes the teleport process.
     */
    public void endTeleport() {
        Position.copyLocation(teleportedTo, location);
        teleportedTo = null;
        teleported = true;
    }

    @Override
    protected boolean teleportToSpawn() {
        Location target = getBedSpawnLocation();
        if (target == null) {
            target = server.getWorlds().get(0).getSpawnLocation();
        }

        PlayerPortalEvent event = EventFactory.getInstance()
            .callEvent(new PlayerPortalEvent(this, location.clone(), target, null));
        if (event.isCancelled()) {
            return false;
        }
        target = event.getTo();

        spawnAt(target);
        teleported = true;

        return true;
    }

    @Override
    protected boolean teleportToEnd() {
        if (!server.getAllowEnd()) {
            return false;
        }
        Location target = null;
        for (World world : server.getWorlds()) {
            if (world.getEnvironment() == Environment.THE_END) {
                target = world.getSpawnLocation();
                break;
            }
        }
        if (target == null) {
            return false;
        }

        PlayerPortalEvent event = EventFactory.getInstance()
            .callEvent(new PlayerPortalEvent(this, location.clone(), target, null));
        if (event.isCancelled()) {
            return false;
        }
        target = event.getTo();

        spawnAt(target);
        teleported = true;

        return true;
    }

    /**
     * This player enters the specified bed and is marked as sleeping.
     *
     * @param block the bed
     */
    public void enterBed(GlowBlock block) {
        checkNotNull(block, "Bed block cannot be null");
        Preconditions.checkState(bed == null, "Player already in bed");

        GlowBlock head = BlockBed.getHead(block);
        GlowBlock foot = BlockBed.getFoot(block);
        if (EventFactory.getInstance()
            .callEvent(new PlayerBedEnterEvent(this, head)).isCancelled()) {
            return;
        }

        // Occupy the bed
        BlockBed.setOccupied(head, foot, true);
        bed = head;
        sleeping = true;
        setRawLocation(head.getLocation(), false);

        // TODO: Set bed metadata (LivingEntity)
    }

    /**
     * This player leaves their bed causing them to quit sleeping.
     *
     * @param setSpawn Whether to set the bed spawn of the player
     */
    public void leaveBed(boolean setSpawn) {
        Preconditions.checkState(bed != null, "Player is not in bed");
        GlowBlock head = BlockBed.getHead(bed);
        GlowBlock foot = BlockBed.getFoot(bed);

        // Determine exit location
        Block exitBlock = BlockBed.getExitLocation(head, foot);
        if (exitBlock == null) { // If no empty blocks were found fallback to block above bed
            exitBlock = head.getRelative(BlockFace.UP);
        }

        // Set their spawn (normally omitted if their bed gets destroyed instead of them leaving it)
        if (setSpawn) {
            setBedSpawnLocation(head.getLocation());
        }

        // Empty the bed
        BlockBed.setOccupied(head, foot, false);
        bed = null;
        sleeping = false;

        // And eject the player
        Location exitLocation = exitBlock.getLocation().add(0.5, 0.1, 0.5); // Use center of block
        setRawLocation(exitLocation, false);
        teleported = true;

        // Call event
        EventFactory.getInstance().callEvent(new PlayerBedLeaveEvent(this, head, setSpawn));

        playAnimationToSelf(EntityAnimation.LEAVE_BED);
        playAnimation(EntityAnimation.LEAVE_BED);

        // TODO: Set bed metadata (LivingEntity)
    }

    @Override
    public void sendMessage(@NotNull String message) {
        sendRawMessage(message);
    }

    @Override
    public void sendMessage(String[] messages) {
        for (String line : messages) {
            sendMessage(line);
        }
    }

    @Override
    public void sendMessage(@NotNull BaseComponent component) {
        sendMessage(ChatMessageType.SYSTEM, component);
    }

    @Override
    public void sendMessage(BaseComponent... components) {
        sendMessage(ChatMessageType.SYSTEM, components);
    }

    @Override
    public void sendMessage(ChatMessageType type, BaseComponent... components) {
        session.send(new ChatMessage(TextMessage
            .decode(ComponentSerializer.toString(components)), type));
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String message) {
        if (sender == null) {
            sendMessage(message);
        }

        sendMessages(sender, message);
    }

    @Override
    public void sendMessage(@Nullable UUID sender, @NotNull String[] messages) {
        if (sender == null) {
            sendMessage(messages);
        }

        sendMessages(sender, messages);
    }

    public void sendMessages(UUID sender, String... messages) {
        ProfileCache.getProfile(sender).thenAccept(profile -> {
            String name = profile.getName();
            if (name != null) {
                sendRawMessages(name, messages);
            }
        });
    }

    public void sendRawMessages(ChatMessageType type, String... messages) {
        for (String message : messages) {
            session.send(new ChatMessage(message, type));
        }
    }

    public void sendRawMessages(String senderName, String... messages) {
        sendRawMessages(ChatMessageType.CHAT, String.format("<%1$s> %2$s", senderName, messages));
    }


    @Override
    public void sendRawMessage(@NotNull String message) {
        // old-style formatting to json conversion is in TextMessage
        sendRawMessages(ChatMessageType.SYSTEM, message);
    }

    @Override
    public void sendRawMessage(@Nullable UUID sender, @NotNull String message) {
        if (sender == null) {
            sendRawMessage(message);
        }

        sendMessage(sender, message);
    }

    @Override
    public void sendActionBar(@NotNull String message) {
        // "old" formatting workaround because apparently "new" styling doesn't work as of
        // 01/18/2015
        JSONObject json = new JSONObject();
        json.put("text", message);
        session.send(new ChatMessage(json, ChatMessageType.ACTION_BAR));
    }

    @Override
    public void sendActionBar(char alternateChar, String message) {
        sendActionBar(message); // TODO: don't ignore formatting codes
    }

    @Override
    public void sendActionBar(@NotNull BaseComponent... components) {
        session.send(new ChatMessage(TextMessage
            .decode(ComponentSerializer.toString(components)), ChatMessageType.ACTION_BAR));
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count) {
        spawnParticle(particle, location, count, null);
    }

    @Override
    public void spawnParticle(Particle particle, double x, double y, double z, int count) {
        spawnParticle(particle, x, y, z, count, null);
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, T data) {
        spawnParticle(particle, location, count, 0, 0, 0, 1, data);
    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count,
                                  T data) {
        spawnParticle(particle, x, y, z, count, 0, 0, 0, 1, data);
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count, double offsetX,
                              double offsetY, double offsetZ) {
        spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, 1, null);
    }

    @Override
    public void spawnParticle(Particle particle, double x, double y, double z, int count,
                              double offsetX, double offsetY, double offsetZ) {
        spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, 1, null);
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX,
                                  double offsetY, double offsetZ, T data) {
        spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, 1, data);
    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count,
                                  double offsetX, double offsetY, double offsetZ, T data) {
        spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetY, 1, data);
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count, double offsetX,
                              double offsetY, double offsetZ, double extra) {
        spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, null);
    }

    @Override
    public void spawnParticle(Particle particle, double x, double y, double z, int count,
                              double offsetX, double offsetY, double offsetZ, double extra) {
        spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, extra, null);
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX,
                                  double offsetY, double offsetZ, double extra, T data) {
        double distance = getLocation().distanceSquared(location);
        boolean isLongDistance = GlowParticle.isLongDistance(particle);

        int particleId = GlowParticle.getId(particle);
        Object[] particleData = GlowParticle.getExtData(particle, data);

        if (distance <= 1024.0D || isLongDistance && distance <= 262144.0D) {
            getSession().send(new PlayParticleMessage(particleId, isLongDistance,
                location.getX(), location.getY(), location.getZ(),
                (float) offsetX, (float) offsetY, (float) offsetZ,
                (float) extra, count, particleData));
        }
    }

    @Override
    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count,
                                  double offsetX, double offsetY, double offsetZ, double extra,
                                  T data) {
        spawnParticle(particle, new Location(world, x, y, z), count, offsetX, offsetY, offsetZ,
            extra, data);
    }

    @Override
    public AdvancementProgress getAdvancementProgress(Advancement advancement) {
        return advancements.get(advancement);
    }

    @Override
    public int getClientViewDistance() {
        return 0;
    }

    @Override
    public @NotNull Locale locale() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getPing() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getLocale() {
        return settings.getLocale();
    }

    @Override
    public boolean getAffectsSpawning() {
        return affectsSpawning;
    }

    @Override
    public void updateCommands() {

    }

    @Override
    public void openBook(@NotNull ItemStack itemStack) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getViewDistance() {
        return settings.getViewDistance();
    }

    @Override
    public void setViewDistance(int viewDistance) {
        settings.setViewDistance(viewDistance);
    }

    @Override
    public void kickPlayer(String message) {
        remove();
        session.disconnect(message == null ? "" : message);
    }

    public void kickPlayer(String message, boolean async) {
        remove(async);
        session.disconnect(message == null ? "" : message);
    }


    @Override
    public void kick(@org.jetbrains.annotations.Nullable Component component) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public boolean performCommand(String command) {
        return getServer().dispatchCommand(this, command);
    }

    @Override
    public void chat(String text) {
        chat(text, false);
    }

    /**
     * Says a message (or runs a command).
     *
     * @param text  message sent by the player.
     * @param async whether the message was received asynchronously.
     */
    public void chat(String text, boolean async) {
        if (text.charAt(0) == '/') {
            Runnable task = () -> {
                server.getLogger().info(getName() + " issued command: " + text);
                try {
                    PlayerCommandPreprocessEvent event
                        = new PlayerCommandPreprocessEvent(this, text);
                    if (!EventFactory.getInstance().callEvent(event).isCancelled()) {
                        server.dispatchCommand(this, event.getMessage().substring(1));
                    }
                } catch (Exception ex) {
                    sendMessage(ChatColor.RED
                        + "An internal error occurred while executing your command.");
                    server.getLogger()
                        .log(Level.SEVERE, "Exception while executing command: " + text, ex);
                }
            };

            // if async is true, this task should happen synchronously
            // otherwise, we're sync already, it can happen here
            if (async) {
                server.getScheduler().runTask(null, task);
            } else {
                task.run();
            }
        } else {
            AsyncPlayerChatEvent event = EventFactory.getInstance().onPlayerChat(async, this, text);
            if (event.isCancelled()) {
                return;
            }

            String message = String.format(event.getFormat(), getDisplayName(), event.getMessage());
            getServer().getLogger().info(message);
            for (Player recipient : event.getRecipients()) {
                recipient.sendMessage(message);
            }
        }
    }

    @Override
    public void saveData() {
        saveData(true);
    }

    /**
     * Saves the players current location, health, inventory, motion, and other information into the
     * username.dat file, in the world/player folder.
     *
     * @param async if true, save asynchronously; if false, block until saved
     */
    public void saveData(boolean async) {
        if (async) {
            Bukkit.getScheduler().runTaskAsynchronously(null, () -> {
                server.getPlayerDataService().writeData(GlowPlayer.this);
                server.getPlayerStatisticIoService().writeStatistics(GlowPlayer.this);
            });
        } else {
            server.getPlayerDataService().writeData(this);
            server.getPlayerStatisticIoService().writeStatistics(this);
        }
    }

    @Override
    public void loadData() {
        server.getPlayerDataService().readData(this);
        server.getPlayerStatisticIoService().readStatistics(this);
    }

    @Override
    @Deprecated
    public void setTexturePack(String url) {
        setResourcePack(url);
    }

    @Override
    public void setResourcePack(String url) {
        setResourcePack(url, "");
    }

    @Override
    public void setResourcePack(String url, byte[] hash) {
        checkNotNull(url);
        checkNotNull(hash);
        checkArgument(hash.length == 20, "Resource pack hash is of an invalid length.");
        setResourcePack(url, Convert.fromBytes(hash));
    }

    @Override
    public void setResourcePack(String url, String hash) {
        checkNotNull(url);
        checkNotNull(hash);
        checkArgument(hash.length() == 40, "Resource pack hash is of an invalid length.");
        session.send(new ResourcePackSendMessage(url, hash));
        resourcePackHash = hash;
    }

    @Override
    public PlayerResourcePackStatusEvent.Status getResourcePackStatus() {
        return resourcePackStatus;
    }

    public void setResourcePackStatus(PlayerResourcePackStatusEvent.Status status) {
        resourcePackStatus = status;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Effect and data transmission

    @Override
    public String getResourcePackHash() {
        return resourcePackHash;
    }

    @Override
    public boolean hasResourcePack() {
        return resourcePackStatus == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED;
    }

    @Override
    public PlayerProfile getPlayerProfile() {
        return getProfile();
    }

    @Override
    public void setPlayerProfile(PlayerProfile playerProfile) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public float getCooldownPeriod() {
        return 0;
    }

    @Override
    public float getCooledAttackStrength(float adjustTicks) {
        return 0;
    }

    @Override
    public void resetCooldown() {

    }

    @Override
    public <T> @NotNull T getClientOption(@NotNull ClientOption<T> clientOption) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public @org.jetbrains.annotations.Nullable Firework boostElytra(@NotNull ItemStack itemStack) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void sendOpLevel(byte b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public @NotNull Set<Player> getTrackedPlayers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public @org.jetbrains.annotations.Nullable String getClientBrandName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void playNote(Location loc, Instrument instrument, Note note) {
        Sound sound;
        switch (instrument) {
            case PIANO:
                sound = Sound.BLOCK_NOTE_BLOCK_HARP;
                break;
            case BASS_DRUM:
                sound = Sound.BLOCK_NOTE_BLOCK_BASEDRUM;
                break;
            case SNARE_DRUM:
                sound = Sound.BLOCK_NOTE_BLOCK_SNARE;
                break;
            case STICKS:
                sound = Sound.BLOCK_NOTE_BLOCK_HAT;
                break;
            case BASS_GUITAR:
                sound = Sound.BLOCK_NOTE_BLOCK_BASS;
                break;
            case BELL:
                sound = Sound.BLOCK_NOTE_BLOCK_BELL;
                break;
            default:
                sound = null;
        }
        byte step = note.getId();
        int octave = note.getOctave();
        float pitch = (float) Math.pow(2, octave) / 2f;
        for (int i = 1; i <= step; i++) {
            if (i < 7) {
                pitch += 1f / 3f;
            } else if (step < 18) {
                pitch += 0.05f;
            } else {
                pitch += 0.1f;
            }
        }
        playSound(loc, sound, SoundCategory.MUSIC, 3.0f, pitch);
    }

    @Override
    public void playNote(Location loc, byte instrument, byte note) {
        playNote(loc, Instrument.getByType(instrument), new Note(note));
    }

    @Override
    public void playEffect(Location loc, Effect effect, int data) {
        int id = effect.getId();
        session.send(new PlayEffectMessage(id, loc.getBlockX(), loc.getBlockY(), loc
            .getBlockZ(), data, false));
    }

    @Override
    public <T> void playEffect(Location loc, Effect effect, T data) {
        playEffect(loc, effect, GlowEffect.getDataValue(effect, data));
    }

    @Override
    public void playSound(Location location, Sound sound, float volume, float pitch) {
        playSound(location, sound, GlowSound
            .getSoundCategory(GlowSound.getVanillaId(sound)), volume, pitch);
    }

    @Override
    public void playSound(Location location, String sound, float volume, float pitch) {
        playSound(location, GlowSound.getVanillaSound(sound), volume, pitch);
    }

    @Override
    public void playSound(Location location, String sound, SoundCategory category, float volume,
                          float pitch) {
        if (location == null || sound == null) {
            return;
        }
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        session.send(new NamedSoundEffectMessage(sound, category, x, y, z, volume, pitch));
    }

    @Override
    public void playSound(Location location, Sound sound, SoundCategory category, float volume,
                          float pitch) {
        playSound(location, GlowSound.getVanillaId(sound), category, volume, pitch);
    }

    @Override
    public void stopSound(Sound sound) {
        stopSound(null, sound);
    }

    @Override
    public void stopSound(Sound sound, SoundCategory soundCategory) {
        stopSound(GlowSound.getVanillaId(sound), soundCategory);
    }

    @Override
    public void stopSound(String sound, SoundCategory category) {
        if (sound.equalsIgnoreCase("all")) {
            sound = null;
        }
        session.send(new StopSoundMessage(category, sound));
    }

    public void stopSound(SoundCategory category, Sound sound) {
        stopSound(sound == null ? "" : GlowSound.getVanillaId(sound), category);
    }

    @Override
    public void stopSound(String sound) {
        if (sound == null || sound.equalsIgnoreCase("all")) {
            sound = "";
        }
        stopSound(sound, null);
    }

    @Override
    public Player.Spigot spigot() {
        return spigot;
    }

    @Override
    public CreatureSpawnEvent.@NotNull SpawnReason getEntitySpawnReason() {
        return null;
    }

    /**
     * Sends a {@link PlayParticleMessage} to display the given particle.
     *
     * @param loc      the location
     * @param particle the particle type
     * @param material the item or block data
     * @param offsetX  TODO: document this parameter
     * @param offsetY  TODO: document this parameter
     * @param offsetZ  TODO: document this parameter
     * @param speed    TODO: document this parameter
     * @param amount   the number of particles
     */
    //@Override
    public void showParticle(Location loc, Effect particle, MaterialData material, float offsetX,
                             float offsetY, float offsetZ, float speed, int amount) {
        if (location == null || particle == null || particle.getType() != Type.VISUAL) {
            return;
        }

        int id = GlowParticle.getId(particle);
        boolean longDistance = GlowParticle.isLongDistance(particle);
        float x = (float) loc.getX();
        float y = (float) loc.getY();
        float z = (float) loc.getZ();
        Object[] extData = GlowParticle.getExtData(particle, material);
        session.send(new PlayParticleMessage(id, longDistance, x, y, z, offsetX, offsetY,
            offsetZ, speed, amount, extData));
    }

    @Override
    public void sendBlockChange(Location loc, Material material, byte data) {
        sendBlockChange(loc, material.getId(), data);
    }

    @Override
    public void sendBlockChange(@NotNull Location loc, @NotNull BlockData block) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Deprecated
    public void sendBlockChange(Location loc, int material, byte data) {
        sendBlockChange(new BlockChangeMessage(loc.getBlockX(), loc.getBlockY(), loc
            .getBlockZ(), material, data));
    }

    /**
     * Sends the given {@link BlockChangeMessage} if it's in a chunk this player can see.
     *
     * @param message the message to send
     */
    public void sendBlockChange(BlockChangeMessage message) {
        // only send message if the chunk is within visible range
        Key key = GlowChunk.Key.of(message.getX() >> 4, message.getZ() >> 4);
        if (canSeeChunk(key)) {
            blockChanges.add(message);
        }
    }

    @Deprecated
    public void sendBlockChangeForce(BlockChangeMessage message) {
        blockChanges.add(message);
    }

    @Override
    public void sendBlockDamage(@NotNull Location location, float v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean sendChunkChange(Location loc, int sx, int sy, int sz, byte[] data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void sendSignChange(@NotNull Location location,
                               @org.jetbrains.annotations.Nullable List<Component> list)
        throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void sendSignChange(@NotNull Location location,
                               @org.jetbrains.annotations.Nullable List<Component> list,
                               @NotNull DyeColor dyeColor) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void sendSignChange(Location location, String[] lines) throws IllegalArgumentException {
        checkNotNull(location, "location cannot be null");
        checkNotNull(lines, "lines cannot be null");
        checkArgument(lines.length == 4, "lines.length must equal 4");

        afterBlockChanges.add(UpdateSignMessage
            .fromPlainText(location.getBlockX(), location.getBlockY(), location
                .getBlockZ(), lines));
    }

    @Override
    public void sendSignChange(@NotNull Location location,
                               @org.jetbrains.annotations.Nullable String[] strings,
                               @NotNull DyeColor dyeColor) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Send a sign change, similar to {@link #sendSignChange(Location, String[])}, but using
     * complete TextMessages instead of strings.
     *
     * @param sign     the sign
     * @param location the location of the sign
     * @param lines    the new text on the sign or null to clear it
     * @throws IllegalArgumentException if location is null
     * @throws IllegalArgumentException if lines is non-null and has a length less than 4
     */
    public void sendSignChange(SignEntity sign, Location location,
                               TextMessage[] lines) throws IllegalArgumentException {
        checkNotNull(location, "location cannot be null");
        checkNotNull(lines, "lines cannot be null");
        checkArgument(lines.length == 4, "lines.length must equal 4");

        CompoundTag tag = new CompoundTag();
        sign.saveNbt(tag);
        afterBlockChanges.add(new UpdateBlockEntityMessage(location.getBlockX(), location
            .getBlockY(), location.getBlockZ(), GlowBlockEntity.SIGN.getValue(), tag));
    }

    /**
     * Send a block entity change to the given location.
     *
     * @param location The location of the block entity.
     * @param type     The type of block entity being sent.
     * @param nbt      The NBT structure to send to the client.
     */
    public void sendBlockEntityChange(Location location, GlowBlockEntity type, CompoundTag nbt) {
        checkNotNull(location, "Location cannot be null");
        checkNotNull(type, "Type cannot be null");
        checkNotNull(nbt, "NBT cannot be null");

        afterBlockChanges.add(new UpdateBlockEntityMessage(location.getBlockX(), location
            .getBlockY(), location.getBlockZ(), type.getValue(), nbt));
    }

    @Override
    public void sendMap(MapView map) {
        GlowMapCanvas mapCanvas = GlowMapCanvas.createAndRender(map, this);
        session.send(new MapDataMessage(map.getId(), map.getScale().ordinal(), Collections
            .emptyList(),
            mapCanvas.toSection()));
    }

    @Override
    public void setPlayerListHeaderFooter(@Nullable String header,
                                          @Nullable String footer) {
        setPlayerListHeader(header);
        setPlayerListFooter(footer);
    }

    @Override
    public void setPlayerListHeaderFooter(BaseComponent[] header, BaseComponent[] footer) {
        TextMessage h = TextMessage.decode(ComponentSerializer.toString(header));
        TextMessage f = TextMessage.decode(ComponentSerializer.toString(footer));
        session.send(new UserListHeaderFooterMessage(h, f));
    }

    @Override
    public void setPlayerListHeaderFooter(BaseComponent header, BaseComponent footer) {
        setPlayerListHeaderFooter(new BaseComponent[] {header}, new BaseComponent[] {footer});
    }

    @Override
    public void setTitleTimes(int fadeInTicks, int stayTicks, int fadeOutTicks) {
        currentTitle.fadeIn(fadeInTicks);
        currentTitle.stay(stayTicks);
        currentTitle.fadeOut(fadeOutTicks);
    }

    @Override
    public void setSubtitle(BaseComponent[] subtitle) {
        currentTitle.subtitle(subtitle);
    }

    @Override
    public void setSubtitle(BaseComponent subtitle) {
        currentTitle.subtitle(subtitle);
    }

    @Override
    public void showTitle(BaseComponent[] title) {
        sendTitle(new Title(title));
    }

    @Override
    public void showTitle(BaseComponent title) {
        sendTitle(new Title(title));
    }

    @Override
    public void showTitle(BaseComponent[] title, BaseComponent[] subtitle, int fadeInTicks,
                          int stayTicks, int fadeOutTicks) {
        sendTitle(new Title(title, subtitle, fadeInTicks, stayTicks, fadeOutTicks));
    }

    @Override
    public void showTitle(BaseComponent title, BaseComponent subtitle, int fadeInTicks,
                          int stayTicks, int fadeOutTicks) {
        sendTitle(new Title(title, subtitle, fadeInTicks, stayTicks, fadeOutTicks));
    }

    @Override
    public void sendTitle(Title title) {
        session.sendAll(TitleMessage.fromTitle(title));
    }

    @Override
    public void sendTitle(String title, String subtitle) {
        sendTitle(new Title(title, subtitle));
    }

    @Override
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        sendTitle(new Title(title, subtitle, fadeIn, stay, fadeOut));
    }

    /**
     * Send the player a title base on a {@link Title.Builder}.
     *
     * @param title the {@link Title.Builder} to send the player
     */
    public void sendTitle(Title.Builder title) {
        sendTitle(title.build());
    }

    /**
     * Send the player their current title.
     */
    public void sendTitle() {
        sendTitle(currentTitle);
        currentTitle = new Title.Builder();
    }

    @Override
    public void updateTitle(Title title) {
        Title builtTitle = currentTitle.build();

        if (title.getTitle().length != 0) {
            currentTitle.title(title.getTitle());
        }

        if (title.getSubtitle() != null) {
            currentTitle.subtitle(title.getSubtitle());
        }

        if (builtTitle.getFadeIn() != title.getFadeIn()
            && title.getFadeIn() != Title.DEFAULT_FADE_IN) {
            currentTitle.fadeIn(title.getFadeIn());
        }

        if (builtTitle.getStay() != title.getStay() && title.getStay() != Title.DEFAULT_STAY) {
            currentTitle.stay(title.getStay());
        }

        if (builtTitle.getFadeOut() != title.getFadeOut()
            && title.getFadeOut() != Title.DEFAULT_FADE_OUT) {
            currentTitle.fadeOut(title.getFadeOut());
        }
    }

    /**
     * Update a specific attribute of the player's title.
     *
     * @param action the attribute to update
     * @param value  the value of the attribute
     */
    public void updateTitle(TitleMessage.Action action, Object... value) {
        Preconditions.checkArgument(
            value.length > 0, "Expected at least one argument. Got " + value.length);

        switch (action) {
            case TITLE:
                Preconditions.checkArgument(!(value instanceof String[]
                    || value instanceof BaseComponent[]), "Value is not of the correct type");

                if (value[0] instanceof String) {
                    StringBuilder builder = new StringBuilder();

                    for (int i = 0; i < value.length; i++) {
                        if (i > 0) {
                            builder.append(" ");
                        }
                        builder.append(value[i]);
                    }

                    currentTitle.title(builder.toString());
                } else {
                    BaseComponent[] formattedValue = (BaseComponent[]) value;
                    currentTitle.title(formattedValue);
                }

                break;
            case SUBTITLE:
                Preconditions.checkArgument(!(value instanceof String[]
                    || value instanceof BaseComponent[]), "Value is not of the correct type");

                if (value[0] instanceof String) {
                    StringBuilder builder = new StringBuilder();

                    for (int i = 0; i < value.length; i++) {
                        if (i > 0) {
                            builder.append(" ");
                        }
                        builder.append(value[i]);
                    }

                    currentTitle.subtitle(builder.toString());
                } else {
                    BaseComponent[] formattedValue = (BaseComponent[]) value;
                    currentTitle.subtitle(formattedValue);
                }

                break;
            case TIMES:
                Preconditions
                    .checkArgument(!(value instanceof Integer[]), "Value is not of the "
                        + "correct type");
                Preconditions
                    .checkArgument(value.length == 3, "Expected 3 values. Got " + value.length);

                currentTitle.fadeIn((int) value[0]);
                currentTitle.stay((int) value[1]);
                currentTitle.fadeOut((int) value[2]);

                break;
            default:
                Preconditions
                    .checkArgument(true, "Action is something other than a title, subtitle, "
                        + "or times");
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Statistics

    @Override
    public void hideTitle() {
        currentTitle = new Title.Builder();
        session.send(new TitleMessage(Action.CLEAR));
    }

    @Override
    public int getStatistic(Statistic statistic) throws IllegalArgumentException {
        return stats.get(statistic);
    }

    @Override
    public int getStatistic(Statistic statistic, Material material)
        throws IllegalArgumentException {
        return stats.get(statistic, material);
    }

    @Override
    public int getStatistic(Statistic statistic, EntityType entityType)
        throws IllegalArgumentException {
        return stats.get(statistic, entityType);
    }

    @Override
    public void setStatistic(Statistic statistic, int newValue) throws IllegalArgumentException {
        stats.set(statistic, newValue);
    }

    @Override
    public void setStatistic(Statistic statistic, Material material,
                             int newValue) throws IllegalArgumentException {
        stats.set(statistic, material, newValue);
    }

    @Override
    public void setStatistic(Statistic statistic, EntityType entityType, int newValue) {
        stats.set(statistic, entityType, newValue);
    }

    @Override
    public void incrementStatistic(Statistic statistic) {
        incrementStatistic(statistic, 1);
    }

    @Override
    public void incrementStatistic(Statistic statistic, int amount) {
        int initialAmount = stats.get(statistic);
        PlayerStatisticIncrementEvent event = EventFactory.getInstance().callEvent(
            new PlayerStatisticIncrementEvent(this, statistic, initialAmount,
                initialAmount + amount));

        if (!event.isCancelled()) {
            stats.add(statistic, amount);
        }
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material) {
        incrementStatistic(statistic, material, 1);
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material, int amount) {
        int initialAmount = stats.get(statistic);
        PlayerStatisticIncrementEvent event = EventFactory.getInstance().callEvent(
            new PlayerStatisticIncrementEvent(this, statistic, initialAmount,
                initialAmount + amount, material));

        if (!event.isCancelled()) {
            stats.add(statistic, material, amount);
        }
    }

    @Override
    public void incrementStatistic(Statistic statistic,
                                   EntityType entityType) throws IllegalArgumentException {
        incrementStatistic(statistic, entityType, 1);
    }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType,
                                   int amount) throws IllegalArgumentException {
        int initialAmount = stats.get(statistic);
        PlayerStatisticIncrementEvent event = EventFactory.getInstance().callEvent(
            new PlayerStatisticIncrementEvent(this, statistic, initialAmount,
                initialAmount + amount, entityType));

        if (!event.isCancelled()) {
            stats.add(statistic, entityType, amount);
        }
    }

    @Override
    public void decrementStatistic(Statistic statistic) throws IllegalArgumentException {
        stats.add(statistic, -1);
    }

    @Override
    public void decrementStatistic(Statistic statistic,
                                   int amount) throws IllegalArgumentException {
        stats.add(statistic, -amount);
    }

    @Override
    public void decrementStatistic(Statistic statistic,
                                   Material material) throws IllegalArgumentException {
        stats.add(statistic, material, -1);
    }

    @Override
    public void decrementStatistic(Statistic statistic, Material material,
                                   int amount) throws IllegalArgumentException {
        stats.add(statistic, material, -amount);
    }

    @Override
    public void decrementStatistic(Statistic statistic,
                                   EntityType entityType) throws IllegalArgumentException {
        stats.add(statistic, entityType, -1);
    }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType, int amount) {
        stats.add(statistic, entityType, -amount);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Inventory

    public StatisticMap getStatisticMap() {
        return stats;
    }

    public void sendStats() {
        session.send(stats.toMessage());
    }

    @Override
    public void updateInventory() {
        session.send(new SetWindowContentsMessage(invMonitor.getId(), invMonitor.getContents()));
    }

    /**
     * Sends a {@link SetWindowSlotMessage} to update the contents of an inventory slot.
     *
     * @param slot the slot ID
     * @param item the new contents
     */
    public void sendItemChange(int slot, ItemStack item) {
        if (invMonitor != null) {
            session.send(new SetWindowSlotMessage(invMonitor.getId(), slot, item));
        }
    }

    @Override
    public void setItemOnCursor(ItemStack item) {
        super.setItemOnCursor(item);
        session.send(new SetWindowSlotMessage(-1, -1, item));
    }

    @Override
    public boolean hasCooldown(Material material) {
        return false;
    }

    @Override
    public int getCooldown(Material material) {
        return 0;
    }

    @Override
    public void setCooldown(Material material, int ticks) {

    }

    @Override
    public boolean isDeeplySleeping() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public @org.jetbrains.annotations.Nullable Location getPotentialBedLocation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MainHand getMainHand() {
        return metadata.getByte(MetadataIndex.PLAYER_MAIN_HAND) == 0 ? MainHand.LEFT
            : MainHand.RIGHT;
    }

    @Override
    public boolean setWindowProperty(Property prop, int value) {
        if (!super.setWindowProperty(prop, value)) {
            return false;
        }
        session.send(new WindowPropertyMessage(invMonitor.getId(), prop.getId(), value));
        return true;
    }

    @Override
    public void openInventory(InventoryView view) {
        session.send(new CloseWindowMessage(invMonitor.getId()));

        super.openInventory(view);

        invMonitor = new InventoryMonitor(getOpenInventory());
        int viewId = invMonitor.getId();
        if (viewId != 0) {
            InventoryOpenEvent event = EventFactory.getInstance().callEvent(
                new InventoryOpenEvent(view));
            if (event.isCancelled()) {
                // close the inventory but don't fire the InventoryCloseEvent
                resetInventoryView();
                return;
            }
            String title = view.getTitle();
            boolean defaultTitle = Objects.equals(view.getType().getDefaultTitle(), title);
            if (view.getTopInventory() instanceof PlayerInventory && defaultTitle) {
                title = ((PlayerInventory) view.getTopInventory()).getHolder().getName();
            }
            Message open = new OpenWindowMessage(viewId, invMonitor
                .getType(), title, ((GlowInventory) view.getTopInventory()).getRawSlots());
            session.send(open);
        }

        updateInventory();
    }

    @Override
    public InventoryView openMerchant(Villager villager, boolean b) {
        return null;
    }

    @Override
    public InventoryView openMerchant(Merchant merchant, boolean b) {
        return null;
    }

    @Override
    public @org.jetbrains.annotations.Nullable InventoryView openAnvil(
        @org.jetbrains.annotations.Nullable Location location, boolean b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public @org.jetbrains.annotations.Nullable InventoryView openCartographyTable(
        @org.jetbrains.annotations.Nullable Location location, boolean b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public @org.jetbrains.annotations.Nullable InventoryView openGrindstone(
        @org.jetbrains.annotations.Nullable Location location, boolean b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public @org.jetbrains.annotations.Nullable InventoryView openLoom(
        @org.jetbrains.annotations.Nullable Location location, boolean b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public @org.jetbrains.annotations.Nullable InventoryView openSmithingTable(
        @org.jetbrains.annotations.Nullable Location location, boolean b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    ////////////////////////////////////////////////////////////////////////////
    // Player-specific time and weather

    @Override
    public @org.jetbrains.annotations.Nullable InventoryView openStonecutter(
        @org.jetbrains.annotations.Nullable Location location, boolean b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public GlowItem drop(ItemStack stack) {
        GlowItem dropping = super.drop(stack);
        if (dropping != null) {
            PlayerDropItemEvent event = new PlayerDropItemEvent(this, dropping);
            EventFactory.getInstance().callEvent(event);
            if (event.isCancelled()) {
                dropping.remove();
                dropping = null;
            } else {
                incrementStatistic(Statistic.DROP, stack.getAmount());
            }
        }
        return dropping;
    }

    @Override
    public void setPlayerTime(long time, boolean relative) {
        timeOffset = (time % TickUtil.TICKS_PER_DAY + TickUtil.TICKS_PER_DAY)
            % TickUtil.TICKS_PER_DAY;
        playerTimeRelative = relative;
        sendTime();
    }

    @Override
    public long getPlayerTime() {
        if (playerTimeRelative) {
            // add timeOffset ticks to current time
            return (world.getTime() + timeOffset) % TickUtil.TICKS_PER_DAY;
        } else {
            // return time offset
            return timeOffset;
        }
    }

    @Override
    public long getPlayerTimeOffset() {
        return timeOffset;
    }

    @Override
    public void resetPlayerTime() {
        setPlayerTime(0, true);
    }

    /**
     * Sends a {@link TimeMessage} with the time of day.
     */
    public void sendTime() {
        long time = getPlayerTime();
        if (!playerTimeRelative
            || !world.getGameRuleMap().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)) {
            time *= -1; // negative value indicates fixed time
        }
        session.send(new TimeMessage(world.getFullTime(), time));
    }

    @Override
    public WeatherType getPlayerWeather() {
        return playerWeather;
    }

    @Override
    public void setPlayerWeather(WeatherType type) {
        playerWeather = type;
        sendWeather();
    }

    @Override
    public void resetPlayerWeather() {
        playerWeather = null;
        sendWeather();
        sendRainDensity();
        sendSkyDarkness();
    }

    /**
     * Sends a {@link StateChangeMessage} with the current weather.
     */
    public void sendWeather() {
        boolean stormy = playerWeather == null ? getWorld().hasStorm()
            : playerWeather == WeatherType.DOWNFALL;
        session.send(new StateChangeMessage(stormy ? Reason.START_RAIN : Reason.STOP_RAIN, 0));
    }

    ////////////////////////////////////////////////////////////////////////////
    // Player visibility

    public void sendRainDensity() {
        session.send(new StateChangeMessage(Reason.RAIN_DENSITY, getWorld().getRainDensity()));
    }

    public void sendSkyDarkness() {
        session.send(new StateChangeMessage(Reason.SKY_DARKNESS, getWorld().getSkyDarkness()));
    }

    @Override
    public void hidePlayer(Player player) {
        checkNotNull(player, "player cannot be null");
        if (equals(player) || !player.isOnline() || !session.isActive()) {
            return;
        }
        if (hiddenEntities.contains(player.getUniqueId())) {
            return;
        }

        hiddenEntities.add(player.getUniqueId());
        worldLock.writeLock().lock();
        try {
            if (knownEntities.remove(player)) {
                session.send(new DestroyEntitiesMessage(Collections
                    .singletonList(player.getEntityId())));
            }
        } finally {
            worldLock.writeLock().unlock();
        }
        session.send(UserListItemMessage.removeOne(player.getUniqueId()));
    }

    @Override
    public void hidePlayer(Plugin plugin, Player player) {
        hidePlayer(player); // call old
    }

    @Override
    public void showPlayer(Player player) {
        checkNotNull(player, "player cannot be null");
        if (equals(player) || !player.isOnline() || !session.isActive()) {
            return;
        }
        if (!hiddenEntities.contains(player.getUniqueId())) {
            return;
        }

        hiddenEntities.remove(player.getUniqueId());
        session.send(new UserListItemMessage(UserListItemMessage.Action.ADD_PLAYER, ((GlowPlayer)
            player)
            .getUserListEntry()));
    }

    @Override
    public void showPlayer(Plugin plugin, Player player) {
        showPlayer(player); // call old
    }

    ////////////////////////////////////////////////////////////////////////////
    // Scoreboard

    @Override
    public boolean canSee(Player player) {
        return !hiddenEntities.contains(player.getUniqueId());
    }

    /**
     * Called when a player hidden to this player disconnects. This is necessary so the player is
     * visible again after they reconnected.
     *
     * @param player The disconnected player
     */
    public void stopHidingDisconnectedPlayer(Player player) {
        hiddenEntities.remove(player.getUniqueId());
    }

    ////////////////////////////////////////////////////////////////////////////
    // Conversable

    @Override
    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    @Override
    public void setScoreboard(
        Scoreboard scoreboard) throws IllegalArgumentException, IllegalStateException {
        checkNotNull(scoreboard, "Scoreboard must not be null");
        if (!(scoreboard instanceof GlowScoreboard)) {
            throw new IllegalArgumentException("Scoreboard must be GlowScoreboard");
        }
        if (this.scoreboard == null) {
            throw new IllegalStateException("Player has not loaded or is already offline");
        }
        this.scoreboard.unsubscribe(this);
        this.scoreboard = (GlowScoreboard) scoreboard;
        this.scoreboard.subscribe(this);
    }

    @Override
    public boolean isConversing() {
        return !conversations.isEmpty();
    }

    @Override
    public void acceptConversationInput(String input) {
        conversations.get(0).acceptInput(input);
    }

    @Override
    public boolean beginConversation(Conversation conversation) {
        boolean noQueue = conversations.isEmpty();
        conversations.add(conversation);
        if (noQueue) {
            conversation.begin();
        }
        return noQueue;
    }

    @Override
    public void abandonConversation(Conversation conversation) {
        abandonConversation(conversation, null);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Plugin messages

    @Override
    public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {
        conversations.remove(conversation);
        if (details == null) {
            conversation.abandon();
        } else {
            conversation.abandon(details);
        }
    }

    @Override
    public void sendPluginMessage(Plugin source, String channel, byte[] message) {
        StandardMessenger
            .validatePluginMessage(getServer().getMessenger(), source, channel, message);
        if (listeningChannels.contains(channel)) {
            // only send if player is listening for it
            session.send(new PluginMessage(channel, message));
        }
    }

    @Override
    public Set<String> getListeningPluginChannels() {
        return Collections.unmodifiableSet(listeningChannels);
    }

    /**
     * Add a listening channel to this player.
     *
     * @param channel The channel to add.
     */
    public void addChannel(String channel) {
        checkArgument(listeningChannels.size() < 128, "Cannot add more than 127 channels!");
        if (listeningChannels.add(channel)) {
            EventFactory.getInstance().callEvent(new PlayerRegisterChannelEvent(this, channel));
        }
    }

    /**
     * Remove a listening channel from this player.
     *
     * @param channel The channel to remove.
     */
    public void removeChannel(String channel) {
        if (listeningChannels.remove(channel)) {
            EventFactory.getInstance().callEvent(new PlayerUnregisterChannelEvent(this, channel));
        }
    }

    /**
     * Send the supported plugin channels to the client.
     */
    private void sendSupportedChannels() {
        Set<String> listening = server.getMessenger().getIncomingChannels();

        if (!listening.isEmpty()) {
            // send NUL-separated list of channels we support
            ByteBuf buf = Unpooled.buffer(16 * listening.size());
            for (String channel : listening) {
                buf.writeBytes(channel.getBytes(StandardCharsets.UTF_8));
                buf.writeByte(0);
            }
            session.sendAndRelease(new PluginMessage("REGISTER", buf.array()), buf); // NON-NLS
        }
    }

    /**
     * Updates level after enchanting.
     *
     * @param clicked the enchanting-table slot used: 0 for top, 1 for middle, 2 for bottom
     */
    public void enchanted(int clicked) {
        int newLevel = level - clicked - 1;
        if (newLevel < 0) {
            setExp(0);
            setTotalExperience(0);
        }

        setLevel(newLevel);
        setXpSeed(ThreadLocalRandom.current().nextInt()); //TODO use entity's random instance?
    }

    ////////////////////////////////////////////////////////////////////////////
    // Titles
    public Title getTitle() {
        return currentTitle.build();
    }

    public void clearTitle() {
        session.send(new TitleMessage(Action.CLEAR));
    }

    @Override
    public void setOnGround(boolean onGround) {
        super.setOnGround(onGround);
        int fallDistance = Math.round(getFallDistance());
        this.incrementStatistic(Statistic.FALL_ONE_CM, fallDistance);
    }

    @Override
    public void resetTitle() {
        currentTitle = new Title.Builder();
        session.send(new TitleMessage(Action.RESET));
    }

    /**
     * Starts breaking a block.
     *
     * @param block the block to start breaking
     */
    public void setDigging(GlowBlock block) {
        if (Objects.equals(block, digging)) {
            return;
        }
        if (block == null) {
            totalDiggingTicks = Long.MAX_VALUE;
            // remove the animation
            broadcastBlockBreakAnimation(digging, 10);
        } else {
            double hardness = block.getMaterialValues().getHardness();
            if (hardness >= Float.MAX_VALUE) {
                // This block can't be broken by digging.
                setDigging(null);
                return;
            }
            double breakingTimeMultiplier = 5; // default of 5 when using bare hands
            ItemStack tool = getItemInHand();
            if (tool != null) {
                Material toolType = tool.getType();
                if (block.getType() == Material.COBWEB && ToolType.SWORD.matches(toolType)) {
                    breakingTimeMultiplier = 0.1;
                } else if (Tag.WOOL.isTagged(block.getType())
                        && toolType == Material.SHEARS) {
                    breakingTimeMultiplier = 0.3;
                } else {
                    ToolType effectiveTool = block.getMaterialValues().getTool();
                    if (effectiveTool != null && effectiveTool.matches(toolType)) {
                        double miningMultiplier = ToolType.getMiningMultiplier(toolType);
                        int efficiencyLevel = tool.getEnchantmentLevel(Enchantment.DIG_SPEED);
                        if (efficiencyLevel > 0) {
                            miningMultiplier += efficiencyLevel * efficiencyLevel + 1;
                        }
                        breakingTimeMultiplier = 1.5 / miningMultiplier;
                    } else if (effectiveTool == null
                        || !effectiveTool.matches(Material.DIAMOND_PICKAXE)) {
                        // If the current tool isn't optimal but can still mine the block, the
                        // multiplier is 1.5. Here, we assume for simplicity that this is true of
                        // all non-pickaxe blocks.
                        // FIXME: Does this always match vanilla?
                        breakingTimeMultiplier = 1.5;
                    }
                }
            }
            // TODO: status effects (e.g. Mining Fatigue, Slowness); effect of underwater digging
            totalDiggingTicks = (long)
                (breakingTimeMultiplier * hardness * 20.0 + 0.5); // seconds to ticks, round half-up
            // show other clients the block is beginning to crack
            broadcastBlockBreakAnimation(block, 0);
        }

        diggingTicks = 0;
        digging = block;
    }

    private void sendBlockBreakAnimation(Location loc, int destroyStage) {
        afterBlockChanges
            .add(new BlockBreakAnimationMessage(this.getEntityId(), loc.getBlockX(), loc
                .getBlockY(), loc.getBlockZ(), destroyStage));
    }

    private void broadcastBlockBreakAnimation(GlowBlock block, int destroyStage) {
        GlowChunk.Key key = GlowChunk.Key.of(block.getX() >> 4, block.getZ() >> 4);
        block.getWorld().getRawPlayers().stream()
            .filter(player -> player != this && player.canSeeChunk(key))
            .forEach(player -> player
                .sendBlockBreakAnimation(block.getLocation(), destroyStage));
    }

    private void pulseDigging() {
        if (++diggingTicks <= totalDiggingTicks) {
            // diggingTicks starts at 1 and progresses to totalDiggingTicks, but animation stages
            // are 0 through 9, so subtract 1 from the current tick
            int stage = (int) (10.0 * ((double) (diggingTicks - 1)) / totalDiggingTicks);
            broadcastBlockBreakAnimation(digging, stage);
            return;
        }
        ItemStack tool = getItemInHand();
        short durability = tool.getDurability();
        short maxDurability = tool.getType().getMaxDurability();
        if (!InventoryUtil.isEmpty(tool) && maxDurability != 0 && durability != maxDurability) {
            int baseDamage; // Before applying unbreaking enchantment
            switch (digging.getType()) {
                case GRASS_BLOCK:
                case DIRT:
                case SAND:
                case GRAVEL:
                case MYCELIUM:
                case SOUL_SAND:
                    baseDamage = ToolType.SHOVEL.matches(tool.getType()) ? 1 : 2;
                    break;
                case OAK_LOG:
                case DARK_OAK_LOG:
                case ACACIA_LOG:
                case BIRCH_LOG:
                case JUNGLE_LOG:
                case SPRUCE_LOG:
                case OAK_WOOD:
                case DARK_OAK_WOOD:
                case ACACIA_WOOD:
                case BIRCH_WOOD:
                case JUNGLE_WOOD:
                case SPRUCE_WOOD:
                case CHEST:
                    baseDamage = ToolType.AXE.matches(tool.getType()) ? 1 : 2;
                    break;
                case STONE:
                case COBBLESTONE:
                    baseDamage = ToolType.PICKAXE.matches(tool.getType()) ? 1 : 2;
                    break;
                default:
                    baseDamage = 2;
                    break;
            }
            for (int i = 0; i < baseDamage; i++) {
                tool = InventoryUtil.damageItem(this, tool);
            }
        }
        // Force-update item
        setItemInHand(tool);
        // Break the block
        digging.breakNaturally(tool);
        // Send block status to clients
        int blockX = digging.getX();
        int blockY = digging.getY();
        int blockZ = digging.getZ();
        // Tell the whole chunk
        world.broadcastBlockChangeInRange(
            Key.of(blockX >> 4, blockZ >> 4),
            new BlockChangeMessage(blockX, blockY, blockZ, Material.AIR.getId(), 0)
        );
        setDigging(null);
    }

    /**
     * Returns true if the player is inside a water block.
     *
     * @return True if entity is in water.
     */
    public boolean isInWater() {
        Material mat = getLocation().getBlock().getType();
        return mat == Material.WATER;
    }

    public void playAnimationToSelf(EntityAnimation animation) {
        EntityAnimationMessage message =
            new EntityAnimationMessage(getEntityId(), animation.ordinal());
        getSession().send(message);
    }

    /**
     * Add a boss bar.
     *
     * @param bar the boss bar to add
     */
    public void addBossBar(BossBar bar) {
        bossBars.add(bar);
    }

    /**
     * Remove a boss bar.
     *
     * @param bar the boss bar to remove
     */
    public void removeBossBar(BossBar bar) {
        bossBars.remove(bar);
    }

    /**
     * Returns a collection of the boss bars this player sees.
     *
     * @return the boss bars this player sees
     */
    public Collection<BossBar> getBossBars() {
        return new ArrayList<>(bossBars);
    }

    /**
     * Gets the currently open window ID.
     *
     * @return the currently open window ID, -1 if there is no open window
     */
    public int getOpenWindowId() {
        if (invMonitor == null) {
            return -1;
        }
        return invMonitor.getId();
    }
}
