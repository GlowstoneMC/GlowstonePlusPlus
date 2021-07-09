package net.glowstone.entity.monster;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.glowstone.entity.meta.MetadataIndex;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GlowZombieVillager extends GlowZombie implements ZombieVillager {

    /**
     * The {@link UUID} of the player who converted this Zombie Villager.
     */
    @Getter
    @Setter
    @Nullable
    private UUID conversionPlayerId;
    /**
     * The conversion time of this Zombie Villager, in ticks.
     *
     * @return the conversion time of this Zombie Villager, in ticks
     */
    @Getter
    private int conversionTime;

    /**
     * Creates a zombie villager that is a farmer.
     *
     * @param loc the initial location
     */
    public GlowZombieVillager(Location loc) {
        super(loc, EntityType.ZOMBIE_VILLAGER);
        setBoundingBox(0.6, 1.95);
        setConversionTime(-1);
        setVillagerProfession(Villager.Profession.FARMER);
    }

    @Override
    public Villager.Profession getVillagerProfession() {
        int profession = metadata.containsKey(MetadataIndex.ZOMBIE_VILLAGER_PROFESSION)
            ? metadata.getInt(MetadataIndex.ZOMBIE_VILLAGER_PROFESSION) : 0;
        return Villager.Profession.values()[profession];
    }

    @Override
    public void setVillagerProfession(Villager.Profession profession) {
        checkNotNull(profession);
        metadata.set(MetadataIndex.ZOMBIE_VILLAGER_PROFESSION, profession.ordinal());
    }

    @Override
    public Villager.@NotNull Type getVillagerType() {
        return Villager.Type.PLAINS;
    }

    @Override
    public void setVillagerType(@NotNull Villager.Type type) {
        // TODO: 1.16
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    protected Sound getHurtSound() {
        return Sound.ENTITY_ZOMBIE_VILLAGER_HURT;
    }

    @Override
    protected Sound getDeathSound() {
        return Sound.ENTITY_ZOMBIE_VILLAGER_DEATH;
    }

    @Override
    protected Sound getAmbientSound() {
        return Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT;
    }

    /**
     * Sets the conversion time of this Zombie Villager.
     *
     * @param conversionTime the conversion time of this villager, in ticks
     */
    public void setConversionTime(int conversionTime) {
        this.conversionTime = Math.max(-1, conversionTime);
        metadata.set(MetadataIndex.ZOMBIE_VILLAGER_IS_CONVERTING, this.conversionTime != -1);
    }

    @Override
    public @Nullable OfflinePlayer getConversionPlayer() {
        if (conversionPlayerId == null) {
            return null;
        }
        return Bukkit.getOfflinePlayer(conversionPlayerId);
    }

    @Override
    public void setConversionPlayer(@Nullable OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null) {
            conversionPlayerId = null;
        } else {
            conversionPlayerId = offlinePlayer.getUniqueId();
        }
    }
}
