package net.glowstone.inventory;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import net.glowstone.util.nbt.CompoundTag;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.jetbrains.annotations.NotNull;

public class GlowMetaSpawn extends GlowMetaItem implements SpawnEggMeta {

    @Getter
    @Setter
    private EntityType spawnedType;
    @Getter
    @Setter
    private CompoundTag entityTag;

    /**
     * Creates an instance by copying from the given {@link ItemMeta}. If that item is another
     * {@link SpawnEggMeta}, the creature type is copied; if it's a {@link GlowMetaSpawn}, any
     * custom NBT for the spawned entity is also copied.
     * @param meta the {@link ItemMeta} to copy
     */
    public GlowMetaSpawn(ItemMeta meta) {
        super(meta);

        if (meta instanceof SpawnEggMeta) {
            this.spawnedType = ((SpawnEggMeta) meta).getSpawnedType();
            if (meta instanceof GlowMetaSpawn) {
                entityTag = ((GlowMetaSpawn) meta).entityTag;
            }
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> result = super.serialize();
        result.put("meta-type", "MONSTER_EGG");
        if (hasSpawnedType()) {
            result.put("entity-id", "minecraft:" + getSpawnedType().getName());
        }
        return result;
    }

    @Override
    void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);
        CompoundTag entityTag = getEntityTag() != null ? getEntityTag() : new CompoundTag();
        if (hasSpawnedType()) {
            entityTag.putString("id", "minecraft:" + getSpawnedType().getName());
        }
        tag.putCompound("EntityTag", entityTag);
    }

    @Override
    void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        tag.readCompound("EntityTag", entityTag -> {
            this.entityTag = entityTag;
            entityTag.readString("id", id -> {
                if (id.startsWith("minecraft:")) {
                    id = id.substring("minecraft:".length());
                }
                spawnedType = EntityType.fromName(id);
            });
        });
    }

    @Override
    public boolean isApplicable(Material material) {
        return material.getKey().getKey().contains("spawn_egg");
    }

    public boolean hasSpawnedType() {
        return this.spawnedType != null;
    }

    @Override
    public @NotNull GlowMetaSpawn clone() {
        return new GlowMetaSpawn(this);
    }
}
