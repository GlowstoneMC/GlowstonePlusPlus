package net.glowstone.io.entity;

import java.util.function.Function;
import net.glowstone.entity.projectile.GlowArrow;
import net.glowstone.util.nbt.CompoundTag;
import org.bukkit.Location;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.jetbrains.annotations.NonNls;

public class ArrowStore<T extends GlowArrow> extends ProjectileStore<T> {

    public static final String LIFE = "life";
    public static final String DAMAGE = "damage";
    public static final String CRITICAL = "crit";
    public static final String PICKUP = "pickup";

    public ArrowStore(Class<T> clazz, @NonNls String id, Function<Location, T> constructor) {
        super(clazz, id, constructor);
    }

    @Override
    public void save(T entity, CompoundTag tag) {
        super.save(entity, tag);
        tag.putBool(CRITICAL, entity.isCritical());
        tag.putShort(LIFE, entity.getLife());
        tag.putDouble(DAMAGE, entity.getDamage());

        tag.putByte(PICKUP, getPickup(entity));
    }

    @Override
    public void load(T entity, CompoundTag tag) {
        super.load(entity, tag);
        tag.readBoolean(CRITICAL, entity::setCritical);
        tag.readShort(LIFE, entity::setLife);
        tag.readDouble(DAMAGE, entity::setDamage);

        tag.readByte(PICKUP, v -> setPickup(entity, v));
    }

    private byte getPickup(T entity) {
        switch (entity.getPickupStatus()) {
            case CREATIVE_ONLY:
                return 2;
            case ALLOWED:
                return 1;
            case DISALLOWED:
            default:
                return 0;
        }
    }

    private void setPickup(T entity, byte pickup) {
        switch (pickup) {
            case 2:
                entity.setPickupStatus(PickupStatus.CREATIVE_ONLY);
                break;
            case 1:
                entity.setPickupStatus(PickupStatus.ALLOWED);
                break;
            case 0:
            default:
                entity.setPickupStatus(PickupStatus.DISALLOWED);
        }
    }
}
