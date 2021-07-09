package net.glowstone.io.entity;

import java.util.UUID;
import java.util.function.Function;
import net.glowstone.entity.projectile.GlowProjectile;
import net.glowstone.util.nbt.CompoundTag;
import org.bukkit.Location;
import org.jetbrains.annotations.NonNls;

class ProjectileStore<T extends GlowProjectile> extends EntityStore<T> {

    public static final String OWNER = "Owner";
    private final Function<Location, T> constructor;

    /**
     * Creates an instance.
     *
     * @param clazz       the class of projectile this ProjectileStore will store
     * @param id          the entity-type name used in NBT
     * @param constructor {@code clazz}'s constructor taking a Location
     */
    public ProjectileStore(Class<T> clazz, @NonNls String id,
                           Function<Location, T> constructor) {
        super(clazz, id);
        this.constructor = constructor;
    }

    @Override
    public T createEntity(Location location, CompoundTag compound) {
        try {
            return constructor.apply(location);
        } catch (Exception e) {
            e.printStackTrace();
            throw new UnsupportedOperationException("Not implemented yet.");
        }
    }

    @Override
    public void save(T entity, CompoundTag tag) {
        super.save(entity, tag);

        UUID owner = entity.getOwner();
        if (owner != null) {
            tag.putUniqueId(OWNER, owner);
        }

    }

    @Override
    public void load(T entity, CompoundTag tag) {
        super.load(entity, tag);
        // Todo: xTile, yTile, zTile, inTile
        tag.readUniqueId(OWNER, entity::setOwner);
    }
}
