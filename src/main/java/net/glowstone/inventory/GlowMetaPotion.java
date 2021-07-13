package net.glowstone.inventory;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import net.glowstone.util.nbt.CompoundTag;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

public class GlowMetaPotion extends GlowMetaItem implements PotionMeta {

    @Getter
    @Setter
    PotionData basePotionData;
    List<PotionEffect> effects = new ArrayList<>();
    @Getter
    @Setter
    Color color = null;

    /**
     * Creates an instance by copying from the given {@link ItemMeta}. If that item is another
     * {@link PotionMeta}, its color, {@link PotionData} and {@link PotionEffect}s are copied;
     * otherwise, the new potion has no effects.
     * @param meta the {@link ItemMeta} to copy
     */
    public GlowMetaPotion(ItemMeta meta) {
        super(meta);
        if (!(meta instanceof PotionMeta)) {
            return;
        }
        copyFrom((PotionMeta) meta);
    }

    /**
     * Copies potion data from another instance
     *
     * @param source The object to copy from
     */
    public void copyFrom(PotionMeta source) {
        setBasePotionData(source.getBasePotionData());
        setColor(source.getColor());
        for (PotionEffect effect : source.getCustomEffects()) {
            addCustomEffect(effect, true);
        }
    }

    /**
     * Reads a {@link PotionEffect} from an NBT compound tag.
     *
     * @param tag a potion effect NBT compound tag
     * @return {@code tag} as a {@link PotionEffect}
     */
    public static PotionEffect fromNbt(CompoundTag tag) {
        PotionEffectType type = PotionEffectType.getById(tag.getByte("Id"));
        int duration = tag.getInt("Duration");
        int amplifier = tag.getByte("Amplifier");
        boolean ambient = tag.getBoolean("Ambient", false);
        boolean particles = tag.getBoolean("ShowParticles", true);

        return new PotionEffect(type, duration, amplifier, ambient, particles);
    }

    /**
     * Converts a {@link PotionEffect} to an NBT compound tag.
     *
     * @param effect the potion effect
     * @return {@code effect} as an NBT compound tag
     */
    public static CompoundTag toNbt(PotionEffect effect) {
        CompoundTag tag = new CompoundTag();

        tag.putByte("Id", effect.getType().getId());
        tag.putInt("Duration", effect.getDuration());
        tag.putByte("Amplifier", effect.getAmplifier());
        tag.putBool("Ambient", effect.isAmbient());
        tag.putBool("ShowParticles", effect.hasParticles());

        return tag;
    }

    @Override
    public boolean isApplicable(Material material) {
        return material == Material.POTION || material == Material.SPLASH_POTION
            || material == Material.TIPPED_ARROW || material == Material.LINGERING_POTION;
    }

    @Override
    public @NotNull GlowMetaPotion clone() {
        return new GlowMetaPotion(this);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> result = super.serialize();
        result.put("meta-type", "POTION");

        if (hasCustomEffects()) {
            result.put("custom-effects", getCustomEffects());
        }

        return result;
    }

    @Override
    void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);

        if (hasCustomEffects()) {
            List<CompoundTag> customEffects = effects.stream().map(GlowMetaPotion::toNbt)
                .collect(Collectors.toList());
            tag.putCompoundList("CustomEffects", customEffects);
        }
        tag.putString("Potion", dataToString(basePotionData));
        if (this.color != null) {
            tag.putInt("CustomPotionColor", this.color.asRGB());
        }
    }

    @Override
    void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        tag.iterateCompoundList("CustomEffects", effect -> addCustomEffect(fromNbt(effect), true)
        );
        tag.readString("Potion", potion -> setBasePotionData(dataFromString(potion)));
        tag.readInt("CustomPotionColor", color -> this.color = Color.fromRGB(color));
    }

    @Override
    public boolean hasCustomEffects() {
        return !effects.isEmpty();
    }

    @Override
    public @NotNull List<PotionEffect> getCustomEffects() {
        return ImmutableList.copyOf(effects);
    }

    @Override
    public boolean addCustomEffect(@NotNull PotionEffect effect, boolean overwrite) {
        checkNotNull(effect, "PotionEffect cannot be null.");

        for (PotionEffect eff : effects) {
            if (eff.getType() == effect.getType() && !overwrite) {
                return false;
            }
        }

        effects.add(effect);
        return true;
    }

    @Override
    public boolean removeCustomEffect(@NotNull PotionEffectType type) {
        Iterator<PotionEffect> it = effects.iterator();

        while (it.hasNext()) {
            PotionEffect effect = it.next();
            if (effect.getType() == type) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasCustomEffect(@NotNull PotionEffectType type) {
        for (PotionEffect effect : effects) {
            if (effect.getType() == type) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean setMainEffect(@NotNull PotionEffectType type) {
        PotionEffect main = null;
        for (PotionEffect effect : effects) {
            if (effect.getType() == type) {
                if (effects.indexOf(effect) == 0) {
                    return false;
                }
                main = effect;
                effects.remove(effect);
                break;
            }
        }
        if (main == null) {
            return false;
        }

        effects.add(0, main);
        return true;
    }

    @Override
    public boolean clearCustomEffects() {
        if (effects.isEmpty()) {
            return false;
        }
        effects.clear();
        return true;
    }

    @Override
    public boolean hasColor() {
        return color != null;
    }

    /**
     * Converts a PotionData to a Potion ID string.
     *
     * @param basePotionData the PotionData to convert
     * @return the Potion ID string
     */
    public static String dataToString(PotionData basePotionData) {
        String name = "minecraft:";
        if (basePotionData.isExtended()) {
            name += "long_";
        } else if (basePotionData.isUpgraded()) {
            name += "strong_";
        }
        return name + PotionTypeTable.toName(basePotionData.getType());
    }

    /**
     * Converts a Potion ID string to the PotionData of this item meta.
     *
     * @param string the Potion ID string
     * @return the resultant PotionData
     */
    private PotionData dataFromString(String string) {
        PotionType type;
        boolean extended = false;
        boolean upgraded = false;
        if (string.startsWith("minecraft:")) {
            string = string.replace("minecraft:", "");
        }
        if (string.startsWith("long_")) {
            string = string.replace("long_", "");
            extended = true;
        } else if (string.startsWith("strong_")) {
            string = string.replace("strong_", "");
            upgraded = true;
        }
        type = PotionTypeTable.fromName(string);
        return new PotionData(type, extended, upgraded);
    }

    /**
     * Conversion for Bukkit Potion names to Vanilla Potion names.
     */
    enum PotionTypeTable {
        EMPTY(PotionType.UNCRAFTABLE, "empty"),
        LEAPING(PotionType.JUMP, "leaping"),
        SWIFTNESS(PotionType.SPEED, "swiftness"),
        HEALING(PotionType.INSTANT_HEAL, "healing"),
        HARMING(PotionType.INSTANT_DAMAGE, "harming"),
        REGENERATION(PotionType.REGEN, "regeneration");

        PotionType type;
        String name;

        PotionTypeTable(PotionType type, String name) {
            this.type = type;
            this.name = name;
        }

        /**
         * Converts a Vanilla Potion ID to an equivalent Bukkit PotionType.
         *
         * @param name the Vanilla Potion ID
         * @return the PotionType equivalent
         */
        static PotionType fromName(String name) {
            for (PotionTypeTable table : values()) {
                if (name.equalsIgnoreCase(table.name)) {
                    return table.type;
                }
            }
            return PotionType.valueOf(name.toUpperCase());
        }

        /**
         * Converts a Bukkit PotionType to an equivalent Vanilla Potion ID.
         *
         * @param type the Bukkit PotionType
         * @return the Vanilla Potion ID equivalent
         */
        static String toName(PotionType type) {
            for (PotionTypeTable table : values()) {
                if (type == table.type) {
                    return table.name;
                }
            }
            return type.name().toLowerCase();
        }
    }
}
