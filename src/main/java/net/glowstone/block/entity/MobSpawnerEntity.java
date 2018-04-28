package net.glowstone.block.entity;

import lombok.Getter;
import lombok.Setter;
import net.glowstone.block.GlowBlock;
import net.glowstone.block.GlowBlockState;
import net.glowstone.block.entity.state.GlowCreatureSpawner;
import net.glowstone.util.nbt.CompoundTag;
import org.bukkit.entity.EntityType;

public class MobSpawnerEntity extends BlockEntity {

    private static final EntityType DEFAULT = EntityType.PIG;

    @Getter
    @Setter
    private EntityType spawning;
    @Getter
    @Setter
    private int delay;

    public MobSpawnerEntity(GlowBlock block) {
        super(block);
        setSaveId("minecraft:mob_spawner");
    }

    @Override
    public void loadNbt(CompoundTag tag) {
        super.loadNbt(tag);
        tag.readString(name -> spawning = EntityType.fromName(name), "EntityId");
        if (spawning == null) {
            spawning = DEFAULT;
        }
        if (!tag.readInt(this::setDelay, "Delay")) {
            delay = 0;
        }
    }

    @Override
    public GlowBlockState getState() {
        return new GlowCreatureSpawner(block);
    }

    @Override
    public void saveNbt(CompoundTag tag) {
        super.saveNbt(tag);
        tag.putString("EntityId", spawning == null ? "" : spawning.getName());
        tag.putInt("Delay", delay);
    }
}
