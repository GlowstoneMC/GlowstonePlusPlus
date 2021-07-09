package net.glowstone.entity.projectile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TippedArrow;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

// TODO: stubs
public class GlowTippedArrow extends GlowArrow implements TippedArrow {

    private final Map<PotionEffectType, PotionEffect> customEffects = new ConcurrentHashMap<>();
    @Getter
    @Setter
    private Color color;
    @Getter
    @Setter
    private PotionData basePotionData;

    public GlowTippedArrow(Location location) {
        super(location);
    }

    @Override
    public void collide(LivingEntity entity) {
        super.collide(entity);
        entity.addPotionEffects(customEffects.values());
    }

    @Override
    public boolean hasCustomEffects() {
        return !customEffects.isEmpty();
    }

    @Override
    public @NotNull List<PotionEffect> getCustomEffects() {
        return new ArrayList<>(customEffects.values());
    }

    @Override
    public boolean addCustomEffect(@NotNull PotionEffect potionEffect, boolean overwrite) {
        PotionEffectType type = potionEffect.getType();
        if (overwrite) {
            customEffects.put(type, potionEffect);
            return true;
        } else {
            return customEffects.putIfAbsent(type, potionEffect) == null;
        }
    }

    @Override
    public boolean removeCustomEffect(@NotNull PotionEffectType potionEffectType) {
        return customEffects.remove(potionEffectType) != null;
    }

    @Override
    public boolean hasCustomEffect(PotionEffectType potionEffectType) {
        return customEffects.containsKey(potionEffectType);
    }

    @Override
    public void clearCustomEffects() {
        customEffects.clear();
    }

    public void copyFrom(PotionMeta itemMeta) {
        setBasePotionData(itemMeta.getBasePotionData());
        clearCustomEffects();
        itemMeta.getCustomEffects().forEach(effect -> addCustomEffect(effect, true));
    }
}
