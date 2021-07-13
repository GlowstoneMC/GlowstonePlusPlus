package net.glowstone.inventory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import net.glowstone.util.nbt.CompoundTag;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class GlowMetaFirework extends GlowMetaItem implements FireworkMeta {

    private List<FireworkEffect> effects = new ArrayList<>();
    @Getter
    private int power;

    /**
     * Creates an instance by copying from the given {@link ItemMeta}. If that item is another
     * {@link FireworkMeta}, its effects and power are copied; otherwise, the new firework has no
     * effects and zero power.
     * @param meta the {@link ItemMeta} to copy
     */
    public GlowMetaFirework(ItemMeta meta) {
        super(meta);
        if (!(meta instanceof FireworkMeta)) {
            return;
        }

        FireworkMeta firework = (FireworkMeta) meta;
        effects.addAll(firework instanceof GlowMetaFirework
                ? ((GlowMetaFirework) firework).effects : firework.getEffects());
        power = firework.getPower();
    }

    @Override
    public boolean isApplicable(Material material) {
        return material == Material.FIREWORK_ROCKET;
    }

    @Override
    public @NotNull GlowMetaFirework clone() {
        return new GlowMetaFirework(this);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> result = super.serialize();
        result.put("meta-type", "FIREWORK");

        result.put("power", power);

        if (hasEffects()) {
            List<Object> effects = this.effects.stream().map(FireworkEffect::serialize)
                .collect(Collectors.toList());
            result.put("effects", effects);
        }

        return result;
    }

    @Override
    void writeNbt(CompoundTag tag) {
        CompoundTag firework = new CompoundTag();
        tag.putCompound("Fireworks", firework);
        firework.putByte("Flight", power);

        List<CompoundTag> explosions = new ArrayList<>();
        if (hasEffects()) {
            explosions.addAll(effects.stream().map(GlowMetaFireworkEffect::toExplosion)
                .collect(Collectors.toList()));
        }
        firework.putCompoundList("Explosions", explosions);
    }

    @Override
    void readNbt(CompoundTag tag) {
        CompoundTag firework = tag.getCompound("Fireworks");
        power = firework.getByte("Flight");

        List<CompoundTag> explosions = firework.getCompoundList("Explosions");
        effects.addAll(
            explosions.stream().map(GlowMetaFireworkEffect::toEffect).collect(Collectors.toList()));
    }

    @Override
    public void addEffect(@NotNull FireworkEffect effect) {
        checkNotNull(effect, "Effect cannot be null.");

        effects.add(effect);
    }

    @Override
    public void addEffects(FireworkEffect... effects) {
        checkNotNull(effects, "Effects cannot be null.");
        for (FireworkEffect effect : effects) {
            checkNotNull(effect, "Null element in effects.");
        }

        this.effects.addAll(Arrays.asList(effects));
    }

    @Override
    public void addEffects(@NotNull Iterable<FireworkEffect> effects) {
        addEffects(Iterables.toArray(effects, FireworkEffect.class));
    }

    @Override
    public @NotNull List<FireworkEffect> getEffects() {
        return ImmutableList.copyOf(effects);
    }

    @Override
    public int getEffectsSize() {
        return effects.size();
    }

    @Override
    public void removeEffect(int index) {
        effects.remove(index);
    }

    @Override
    public void clearEffects() {
        effects.clear();
    }

    @Override
    public boolean hasEffects() {
        return !effects.isEmpty();
    }

    @Override
    public void setPower(int power) {
        checkArgument(power >= 0 && power <= 128, "Power must be 0-128, inclusive");

        this.power = power;
    }
}
