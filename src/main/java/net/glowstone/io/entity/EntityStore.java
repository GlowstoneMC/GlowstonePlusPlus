package net.glowstone.io.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.glowstone.entity.GlowEntity;
import net.glowstone.i18n.ConsoleMessages;
import net.glowstone.io.nbt.NbtSerialization;
import net.glowstone.util.UuidUtils;
import net.glowstone.util.nbt.CompoundTag;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NonNls;

/**
 * The base for entity store classes.
 *
 * @param <T> The type of entity being stored.
 */
@Data
@RequiredArgsConstructor
public abstract class EntityStore<T extends GlowEntity> {

    private static final CompoundTag EMPTY_TAG = new CompoundTag();
    protected final Class<? extends T> type;
    @NonNls
    protected final String entityType;

    public EntityStore(Class<? extends T> type, EntityType entityType) {
        this.type = type;
        this.entityType = entityType.getName();
    }

    /**
     * Create a new entity of this store's type at the given location. The load method will be
     * called separately.
     *
     * @param location The location.
     * @param compound The entity's tag, if extra data is needed.
     * @return The new entity.
     */
    public abstract T createEntity(Location location, CompoundTag compound);

    /**
     * Create a new entity of this store's type at the given location, with all attributes set to
     * their defaults.
     *
     * @param location The location.
     * @return The new entity.
     */
    public T createEntity(Location location) {
        return createEntity(location, EMPTY_TAG);
    }

    // For information on the NBT tags loaded here and elsewhere:
    // http://minecraft.gamepedia.com/Chunk_format#Entity_Format

    // todo: the following tags
    // - bool "Invulnerable"
    // - int "PortalCooldown"

    /**
     * Load data into an existing entity of the appropriate type from the given compound tag.
     *
     * @param entity The target entity.
     * @param tag    The entity's tag.
     */
    public void load(T entity, CompoundTag tag) {
        // id, world, and location are handled by EntityStore
        // base stuff for all entities is here:
        tag.readDoubleList("Motion",
            list -> entity.setVelocity(NbtSerialization.listToVector(list)));
        tag.readFloat("FallDistance", entity::setFallDistance);
        tag.readShort("Fire", entity::setFireTicks);
        tag.readBoolean("OnGround", entity::setOnGround);
        tag.readBooleanNegated("NoGravity", entity::setGravity);
        tag.readBoolean("Silent", entity::setSilent);
        tag.readBoolean("Glowing", entity::setGlowing);
        tag.readBoolean("Invulnerable", entity::setInvulnerable);
        tag.readStringList("Tags", list -> {
            entity.getCustomTags().clear();
            entity.getCustomTags().addAll(list);
        });
        tag.readInt("PortalCooldown", entity::setPortalCooldown);
        // TODO: Refactor using JDK9's Optional.or() once JDK8 support ends
        Optional.ofNullable(
            tag.tryGetUniqueId("UUIDMost", "UUIDLeast")
                .orElseGet(() -> tag.tryGetString("UUID").map(UuidUtils::fromString).orElse(null)))
            .ifPresent(entity::setUniqueId);
        tag.iterateCompoundList("Passengers", entityTag -> {
            Entity passenger = loadPassenger(entity, entityTag);
            if (passenger != null) {
                entity.addPassenger(passenger);
            }
        });
    }

    private Entity loadPassenger(T vehicle, CompoundTag compoundTag) {
        Location location = NbtSerialization.listTagsToLocation(vehicle.getWorld(), compoundTag);

        if (location == null) {
            // We need a location to spawn the entity.
            // since there is no position in the entities nbt,
            // just spawn the passenger at the vehicle.
            // Later on, Entity.addPassenger will make sure of the teleportation
            // to the right coordinates.
            NbtSerialization.locationToListTags(vehicle.getLocation(), compoundTag);
        }

        try {
            // note that creating the entity is sufficient to add it to the world
            return EntityStorage.loadEntity(vehicle.getWorld(), compoundTag);
        } catch (UnknownEntityTypeException e) {
            ConsoleMessages.Warn.Entity.UNKNOWN.log(vehicle, e.getIdOrTag());
        } catch (Exception e) {
            ConsoleMessages.Warn.Entity.LOAD_FAILED.log(e, vehicle);
        }
        return null;
    }

    /**
     * Save information about this entity to the given tag.
     *
     * @param entity The entity to save.
     * @param tag    The target tag.
     */
    public void save(T entity, CompoundTag tag) {
        tag.putString("id", "minecraft:" + entityType);

        // write world info, Pos, Rotation, and Motion
        Location loc = entity.getLocation();
        NbtSerialization.writeWorld(loc.getWorld(), tag);
        NbtSerialization.locationToListTags(loc, tag);
        tag.putDoubleList("Motion", NbtSerialization.vectorToList(entity.getVelocity()));

        tag.putFloat("FallDistance", entity.getFallDistance());
        tag.putShort("Fire", entity.getFireTicks());
        tag.putBool("OnGround", entity.isOnGround());

        tag.putLong("UUIDMost", entity.getUniqueId().getMostSignificantBits());
        tag.putLong("UUIDLeast", entity.getUniqueId().getLeastSignificantBits());

        tag.putBool("NoGravity", !entity.hasGravity());
        tag.putBool("Silent", entity.isSilent());
        tag.putBool("Invulnerable", entity.isInvulnerable());
        tag.putBool("Glowing", entity.isGlowing());
        tag.putInt("PortalCooldown", entity.getPortalCooldown());

        if (!entity.getCustomTags().isEmpty()) {
            tag.putStringList("Tags", entity.getCustomTags());
        }

        // in case Vanilla or CraftBukkit expects non-living entities to have this tag
        tag.putInt("Air", 300);
        savePassengers(entity, tag);
    }

    private void savePassengers(GlowEntity vehicle, CompoundTag tag) {
        List<CompoundTag> passengers = new ArrayList<>();
        for (Entity passenger : vehicle.getPassengers()) {
            if (!(passenger instanceof GlowEntity)) {
                continue;
            }
            GlowEntity glowEntity = (GlowEntity) passenger;
            if (!glowEntity.shouldSave()) {
                continue;
            }
            try {
                CompoundTag compound = new CompoundTag();
                EntityStorage.save(glowEntity, compound);
                passengers.add(compound);
                savePassengers(glowEntity, compound);
            } catch (Exception e) {
                ConsoleMessages.Warn.Entity.SAVE_FAILED_PASSENGER.log(passenger, e);
            }
        }
        if (!passengers.isEmpty()) {
            tag.putCompoundList("Passengers", passengers);
        }
    }
}
