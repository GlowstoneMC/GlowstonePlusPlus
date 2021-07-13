package net.glowstone.constants;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Floats;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;
import net.glowstone.GlowServer;
import net.glowstone.block.data.states.StatefulBlockData;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Id mappings for particles.
 */
public class GlowParticle {

    private static final Float[] EMPTY_DUST_OPTIONS = new Float[4];

    private static final Function<Object, Object[]> PARTICLE_HANDLER_RETURN_SELF = o -> new Object[]{o};

    private static int count = 0;
    private static final Map<Particle, NamespacedKey> PARTICLES_BY_API = new HashMap<>();
    private static final Map<Effect, NamespacedKey> PARTICLES_BY_EFFECT = new HashMap<>();
    private static final Map<NamespacedKey, GlowParticle> PARTICLES_BY_NAME = new HashMap<>();
    private static final Map<Class<?>, Function<Object, Object[]>> PARTICLE_DATA_HANDLERS = new HashMap<>();

    static {
        // Vanilla particles
        registerParticle("ambient_entity_effect", Particle.SPELL_MOB_AMBIENT);
        registerParticle("angry_villager", Particle.VILLAGER_ANGRY);
        registerParticle(Particle.BARRIER);
        registerParticle("block", Particle.BLOCK_DUST);
        registerParticle("block", Particle.BLOCK_CRACK);
        registerParticle("bubble", Particle.WATER_BUBBLE);
        registerParticle(Particle.CLOUD);
        registerParticle(Particle.CRIT);
        registerParticle(Particle.DAMAGE_INDICATOR);
        registerParticle(Particle.DRAGON_BREATH);
        registerParticle("dripping_lava", Particle.DRIP_LAVA);
        registerParticle("dripping_water", Particle.DRIP_WATER);
        registerParticle("dust", Particle.REDSTONE);
        registerParticle("effect");
        registerParticle("elder_guardian", Particle.MOB_APPEARANCE);
        registerParticle("enchanted_hit");
        registerParticle("enchant", Particle.ENCHANTMENT_TABLE);
        registerParticle("end_rod", Particle.END_ROD);
        registerParticle("entity_effect", Particle.SPELL_MOB);
        registerParticle("explosion_emitter", Particle.EXPLOSION_HUGE);
        registerParticle("explosion", Particle.EXPLOSION_LARGE);
        registerParticle(Particle.FALLING_DUST);
        registerParticle("firework", Particle.FIREWORKS_SPARK);
        registerParticle("fishing", Particle.WATER_WAKE);
        registerParticle(Particle.FLAME);
        registerParticle("happy_villager", Particle.VILLAGER_HAPPY);
        registerParticle(Particle.HEART);
        registerParticle("instant_effect", Particle.SPELL_INSTANT);
        registerParticle("item", Particle.ITEM_CRACK);
        registerParticle("item_slime", Particle.SLIME);
        registerParticle("item_snowball", Particle.SNOWBALL);
        registerParticle("large_smoke", Particle.SMOKE_LARGE);
        registerParticle(Particle.LAVA);
        registerParticle("mycelium", Particle.TOWN_AURA);
        registerParticle(Particle.NOTE);
        registerParticle("poof", Particle.SNOW_SHOVEL);
        registerParticle("poof", Particle.EXPLOSION_NORMAL);
        registerParticle(Particle.PORTAL);
        registerParticle("rain", Particle.WATER_DROP);
        registerParticle("smoke", Particle.SMOKE_NORMAL);
        registerParticle(Particle.SPIT);
        registerParticle(Particle.SQUID_INK);
        registerParticle(Particle.SWEEP_ATTACK);
        registerParticle("totem_of_undying", Particle.TOTEM);
        registerParticle("underwater", Particle.SUSPENDED);
        registerParticle("splash", Particle.WATER_SPLASH);
        registerParticle("witch", Particle.SPELL_WITCH);
        registerParticle(Particle.BUBBLE_POP);
        registerParticle(Particle.BUBBLE_COLUMN_UP);
        registerParticle(Particle.NAUTILUS);
        registerParticle(Particle.DOLPHIN);

        // Particle data handlers
        registerParticleDataHandler(Void.class, o -> ArrayUtils.EMPTY_OBJECT_ARRAY);
        registerParticleDataHandler(Particle.DustOptions.class, o -> {
            if (o.getClass() != Particle.DustOptions.class) {
                return EMPTY_DUST_OPTIONS;
            }
            Particle.DustOptions dust = (Particle.DustOptions) o;
            Color color = dust.getColor();
            float red = color.getRed() / 255f;
            float green = color.getGreen() / 255f;
            float blue = color.getRed() / 255f;
            float size = Floats.constrainToRange(dust.getSize(), 0.01f, 4.0f);
            return new Float[]{red, green, blue, size};
        });
        registerParticleDataHandler(ItemStack.class, PARTICLE_HANDLER_RETURN_SELF);
        registerParticleDataHandler(BlockData.class, o -> {
            if (o.getClass() != BlockData.class) {
                return ArrayUtils.EMPTY_INTEGER_OBJECT_ARRAY;
            }
            int id = ((GlowServer) Bukkit.getServer()).getBlockDataManager().convertToBlockId(
                (StatefulBlockData) o);
            return new Integer[]{id};
        });
    }

    @Getter
    private NamespacedKey name;
    @Getter
    int id;
    @Getter
    private Class<?> dataType;
    @Getter
    private Function<Object, Object[]> dataHandler;
    private boolean longDistance;

    private GlowParticle(NamespacedKey name) {
        this.name = name;
        this.dataType = null;
        this.dataHandler = null;
        this.id = count++;
    }

    private GlowParticle(NamespacedKey name, Class<?> dataType) {
        this(name);
        this.dataType = dataType;
        this.dataHandler = PARTICLE_DATA_HANDLERS.get(this.dataType);
    }

    private GlowParticle(NamespacedKey name, Class<?> dataType, boolean longDistance) {
        this(name, dataType);
        this.longDistance = longDistance;
    }

    public GlowParticle(NamespacedKey name, Particle particle) {
        this(name, particle.getDataType());
        switch (particle) {
            case EXPLOSION_NORMAL:
            case EXPLOSION_LARGE:
            case EXPLOSION_HUGE:
            case MOB_APPEARANCE:
                longDistance = true;
                break;
            default:
                longDistance = false;
                break;
        }
    }

    /**
     * Gets the extra data for this particle.
     *
     * @param o The data parameter, of type {@link GlowParticle#getDataType()}
     * @return The networkable data that represents this extra particle data
     */
    public Object[] getExtData(Object o) {
        return dataHandler.apply(o);
    }

    /**
     * Convert an object to an extData array if possible for a particle.
     *
     * @param particle the {@link Particle} to validate.
     * @param object the Object to convert.
     * @return The extData array for the particle effect.
     */
    public static Object[] getExtData(Particle particle, Object object) {
        return getParticle(particle).getExtData(object);
    }

    /**
     * Convert an object to an extData array if possible for an effect.
     *
     * @param effect the {@link Effect} to validate.
     * @param object the Object to convert.
     * @return The extData array for the particle effect.
     */
    public static Object[] getExtData(Effect effect, Object object) {
        return getParticle(effect).getExtData(object);
    }

    public static GlowParticle getParticle(NamespacedKey name) {
        Preconditions.checkNotNull(name, "Particle does not exist or was null!");
        return PARTICLES_BY_NAME.get(name);
    }

    public static GlowParticle getParticle(Particle particle) {
        return getParticle(PARTICLES_BY_API.get(particle));
    }

    public static GlowParticle getParticle(Effect effect) {
        return getParticle(PARTICLES_BY_EFFECT.get(effect));
    }

    /**
     * Get the particle id for a specified {@link Particle}.
     *
     * @param particle the Particle.
     * @return the particle id.
     */
    public static int getId(@NotNull Particle particle) {
        return getParticle(particle).getId();
    }

    /**
     * Get the particle id for a specified {@link Effect}.
     *
     * @param effect the effect.
     * @return the particle id.
     */
    public static int getId(Effect effect) {
        Preconditions.checkArgument(effect.getType() == Effect.Type.VISUAL, "Effect must be visual to have a particle!");
        return getParticle(effect).getId();
    }

    /**
     * Determine whether a particle type is considered long distance, meaning it has a higher
     * visible range (65536) than normal (256).
     *
     * @return True if the particle is long distance.
     */
    public boolean isLongDistance() {
        return longDistance;
    }

    /**
     * Checks if this particle is long distance
     *
     * @param particle The particle
     * @return If long distance
     * @see GlowParticle#isLongDistance()
     */
    public static boolean isLongDistance(Particle particle) {
        return getParticle(particle).isLongDistance();
    }

    /**
     * Checks if this effect is long distance.
     *
     * @param effect The effect
     * @return If long distance
     * @see GlowParticle#isLongDistance()
     */
    public static boolean isLongDistance(Effect effect) {
        return getParticle(effect).isLongDistance();
    }

    /**
     * Register a new custom particle. An ID will be automatically assigned.
     *
     * @param name The particle's name
     * @return If it successfully registered
     */
    public static boolean registerParticle(NamespacedKey name) {
        if (PARTICLES_BY_NAME.containsKey(name)) {
            return false;
        }
        PARTICLES_BY_NAME.putIfAbsent(name, new GlowParticle(name));
        return true;
    }

    private static void registerParticle(String name) {
        NamespacedKey key = NamespacedKey.minecraft(name);
        PARTICLES_BY_NAME.put(key, new GlowParticle(key));
    }

    private static GlowParticle registerParticle(String name, Particle particle) {
        NamespacedKey key = NamespacedKey.minecraft(name);
        GlowParticle registered = PARTICLES_BY_NAME.computeIfAbsent(key, p -> new GlowParticle(key, particle));
        PARTICLES_BY_API.put(particle, registered.getName());
        return registered;
    }

    private static void registerParticle(Particle particle) {
        String name = particle.name().toLowerCase(Locale.ROOT);
        registerParticle(name, particle);
    }

    private static void registerParticle(String name, Particle particle, Effect effect) {
        PARTICLES_BY_EFFECT.put(effect, registerParticle(name, particle).getName());
    }

    /**
     * Register a new particle data handler for a certain class.
     *
     * @param clazz The class to handle
     * @param handler The function that handles instances of this class
     * @return If there was not already a handler for this class, and the handler was registered successfully
     */
    public static boolean registerParticleDataHandler(Class<?> clazz, Function<Object, Object[]> handler) {
        return PARTICLE_DATA_HANDLERS.putIfAbsent(clazz, handler) == null;
    }
}
