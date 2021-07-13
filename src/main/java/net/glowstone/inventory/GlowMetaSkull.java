package net.glowstone.inventory;

import static com.google.common.base.Preconditions.checkArgument;

import com.destroystokyo.paper.profile.PlayerProfile;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import net.glowstone.GlowOfflinePlayer;
import net.glowstone.GlowServer;
import net.glowstone.ServerProvider;
import net.glowstone.entity.GlowPlayer;
import net.glowstone.entity.meta.profile.GlowPlayerProfile;
import net.glowstone.util.nbt.CompoundTag;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

public class GlowMetaSkull extends GlowMetaItem implements SkullMeta {

    private static final GlowPlayerProfile UNKNOWN_PLAYER = new GlowPlayerProfile("MHF_Steve",
            new UUID(0xc06f89064c8a4911L, 0x9c29ea1dbd1aab82L), true);

    final AtomicReference<GlowPlayerProfile> owner = new AtomicReference<>();

    /**
     * Creates an instance by copying from the given {@link ItemMeta}. If that item is another
     * {@link SkullMeta} with an owner, attempts to copy the owning player.
     * @param meta the {@link ItemMeta} to copy
     */
    public GlowMetaSkull(ItemMeta meta) {
        super(meta);
        if (!(meta instanceof SkullMeta)) {
            return;
        }
        SkullMeta skull = (SkullMeta) meta;
        if (skull.hasOwner()) {
            if (skull instanceof GlowMetaSkull) {
                owner.set(((GlowMetaSkull) skull).owner.get());
            } else {
                if (!setOwningPlayerInternal(skull.getOwningPlayer())) {
                    owner.set(UNKNOWN_PLAYER);
                    // necessary to preserve the return value of hasOwner()
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Internal stuff

    /**
     * Deserializes an instance as specified in {@link
     * org.bukkit.configuration.serialization.ConfigurationSerializable}.
     *
     * @param data a serialized instance
     * @return the instance as a GlowMetaSkull
     */
    public static GlowMetaSkull deserialize(Map<String, Object> data) {
        GlowMetaSkull result = new GlowMetaSkull(null);
        if (data.containsKey("owner")) {
            result.owner.set((GlowPlayerProfile) data.get("owner"));
        }
        return result;
    }

    @Override
    public @NotNull SkullMeta clone() {
        return new GlowMetaSkull(this);
    }

    @Override
    public boolean isApplicable(Material material) {
        return material.getKey().getKey().contains("skull") || material.getKey().getKey().contains("head");
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> result = super.serialize();
        result.put("meta-type", "SKULL");
        if (hasOwner()) {
            result.put("owner", owner);
        }
        return result;
    }

    @Override
    void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);
        if (hasOwner()) {
            tag.putCompound("SkullOwner", owner.get().toNbt());
        }
    }

    @Override
    void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        if (tag.containsKey("SkullOwner")) {
            if (!tag.readString("SkullOwner",
                name -> owner.set(GlowPlayerProfile.getProfile(name).join()))) {
                tag.readCompound("SkullOwner",
                    skullOwner -> owner.set(GlowPlayerProfile.fromNbt(skullOwner).join())
                );
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties

    @Override
    public String getOwner() {
        return hasOwner() ? owner.get().getName() : null;
    }

    @Override
    public boolean hasOwner() {
        return owner.get() != null;
    }

    @Override
    public boolean setOwner(String name) {
        GlowPlayerProfile owner = GlowPlayerProfile.getProfile(name).join();
        if (owner == null) {
            return false;
        }
        this.owner.set(owner);
        return true;
    }

    @Override
    public void setPlayerProfile(PlayerProfile profile) {
        if (profile == null) {
            this.owner.set(UNKNOWN_PLAYER);
            return;
        }
        checkArgument(profile instanceof GlowPlayerProfile);
        this.owner.set((GlowPlayerProfile) profile);
    }

    @Override
    public PlayerProfile getPlayerProfile() {
        return this.owner.get();
    }

    @Override
    public OfflinePlayer getOwningPlayer() {
        return ((GlowServer) ServerProvider.getServer()).getOfflinePlayer(owner.get());
    }

    /**
     * {@inheritDoc}
     *
     * <p>When this returns false, it may still succeed asynchronously.
     */
    @Override
    public boolean setOwningPlayer(OfflinePlayer owningPlayer) {
        if (hasOwner()) {
            return false;
        }
        return setOwningPlayerInternal(owningPlayer);
    }

    private boolean setOwningPlayerInternal(OfflinePlayer owningPlayer) {
        if (owningPlayer instanceof GlowOfflinePlayer) {
            GlowOfflinePlayer impl = (GlowOfflinePlayer) owningPlayer;
            this.owner.set(impl.getProfile());
            return true;
        } else if (owningPlayer instanceof GlowPlayer) {
            this.owner.set(((GlowPlayer) owningPlayer).getProfile());
            return true;
        } else {
            CompletableFuture<GlowPlayerProfile> profileFuture = GlowPlayerProfile
                    .getProfile(owningPlayer.getName());
            GlowPlayerProfile profile = profileFuture.getNow(null);
            if (profile != null) {
                this.owner.set(profile);
                return true;
            } else {
                profileFuture.thenAcceptAsync(this.owner::set);
                return false;
            }
        }
    }
}
