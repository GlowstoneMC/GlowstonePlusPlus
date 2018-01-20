package net.glowstone.entity.projectile;

import static net.glowstone.net.message.play.entity.SpawnObjectMessage.THROWN_ENDERPEARL;

import com.flowpowered.network.Message;
import java.util.Arrays;
import java.util.List;
import net.glowstone.net.message.play.entity.EntityMetadataMessage;
import net.glowstone.net.message.play.entity.EntityTeleportMessage;
import net.glowstone.net.message.play.entity.EntityVelocityMessage;
import net.glowstone.net.message.play.entity.SpawnObjectMessage;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public class GlowEnderPearl extends GlowProjectile implements EnderPearl {
    private static final double ENDER_PEARL_DAMAGE = 5.0;

    /**
     * Creates a thrown ender pearl with default speed.
     *
     * @param location the position and facing of the thrower
     */
    public GlowEnderPearl(Location location) {
        this(location, 2.0f);
    }

    /**
     * Creates a thrown ender pearl.
     *
     * @param location the position and facing of the thrower
     * @param speed the initial speed
     */
    public GlowEnderPearl(Location location, float speed) {
        super(location);
        setDrag(0.99, false);
        setDrag(0.99, true);
        setHorizontalAirDrag(0.95);
        setGravityAccel(new Vector(0,-0.06,0));
        setVelocity(location.getDirection().multiply(speed));
    }

    @Override
    public void collide(Block block) {
        ProjectileSource source = getShooter();
        if (source instanceof Entity) {
            ((Entity) source).teleport(location, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
            if (source instanceof LivingEntity) {
                ((LivingEntity) source).damage(ENDER_PEARL_DAMAGE,
                        EntityDamageEvent.DamageCause.FALL);
            }
        }
        remove();
    }

    @Override
    public void collide(LivingEntity entity) {
        // No-op.
    }

    @Override
    protected int getObjectId() {
        return THROWN_ENDERPEARL;
    }

    @Override
    public List<Message> createSpawnMessage() {
        return Arrays.asList(
                new SpawnObjectMessage(
                        id, getUniqueId(), THROWN_ENDERPEARL, location),
                new EntityMetadataMessage(id, metadata.getEntryList()),
                // These keep the client from assigning a random velocity
                new EntityTeleportMessage(id, location),
                new EntityVelocityMessage(id, getVelocity())
        );
    }
}
