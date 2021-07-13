package net.glowstone.block.entity.state;

import com.destroystokyo.paper.MaterialTags;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import net.glowstone.block.GlowBlock;
import net.glowstone.block.entity.DispenserEntity;
import net.glowstone.dispenser.ArmorDispenseBehavior;
import net.glowstone.dispenser.BucketDispenseBehavior;
import net.glowstone.dispenser.DefaultDispenseBehavior;
import net.glowstone.dispenser.DispenseBehavior;
import net.glowstone.dispenser.DispenseBehaviorRegistry;
import net.glowstone.dispenser.EmptyBucketDispenseBehavior;
import net.glowstone.dispenser.FlintAndSteelDispenseBehavior;
import net.glowstone.dispenser.ProjectileDispenseBehavior;
import net.glowstone.dispenser.TntDispenseBehavior;
import net.glowstone.entity.projectile.GlowArrow;
import net.glowstone.entity.projectile.GlowEgg;
import net.glowstone.entity.projectile.GlowFireball;
import net.glowstone.entity.projectile.GlowLingeringPotion;
import net.glowstone.entity.projectile.GlowSnowball;
import net.glowstone.entity.projectile.GlowSpectralArrow;
import net.glowstone.entity.projectile.GlowSplashPotion;
import net.glowstone.entity.projectile.GlowThrownExpBottle;
import net.glowstone.entity.projectile.GlowTippedArrow;
import net.glowstone.util.InventoryUtil;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SplashPotion;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.util.Vector;

public class GlowDispenser extends GlowContainer implements Dispenser, BlockProjectileSource {

    @Getter
    private static final DispenseBehaviorRegistry dispenseBehaviorRegistry =
        new DispenseBehaviorRegistry();

    public GlowDispenser(GlowBlock block) {
        super(block);
    }

    /**
     * Registers all vanilla dispense behaviors.
     */
    public static void register() {
        // register all dispense behaviors
        DefaultDispenseBehavior bucketDispenseBehavior = new BucketDispenseBehavior();
        DispenseBehaviorRegistry registry = getDispenseBehaviorRegistry();
        registry.putBehavior(Material.WATER_BUCKET, bucketDispenseBehavior);
        registry.putBehavior(Material.LAVA_BUCKET, bucketDispenseBehavior);
        registry.putBehavior(Material.BUCKET, new EmptyBucketDispenseBehavior());
        registry.putBehavior(Material.FLINT_AND_STEEL, new FlintAndSteelDispenseBehavior());
        registry.putBehavior(Material.TNT, new TntDispenseBehavior());
        ArmorDispenseBehavior armorDispenseBehavior = new ArmorDispenseBehavior();
        registry.putBehavior(Material.LEATHER_BOOTS, armorDispenseBehavior);
        registry.putBehavior(Material.LEATHER_LEGGINGS, armorDispenseBehavior);
        registry.putBehavior(Material.LEATHER_CHESTPLATE, armorDispenseBehavior);
        registry.putBehavior(Material.LEATHER_HELMET, armorDispenseBehavior);
        registry.putBehavior(Material.GOLDEN_BOOTS, armorDispenseBehavior);
        registry.putBehavior(Material.GOLDEN_LEGGINGS, armorDispenseBehavior);
        registry.putBehavior(Material.GOLDEN_CHESTPLATE, armorDispenseBehavior);
        registry.putBehavior(Material.GOLDEN_HELMET, armorDispenseBehavior);
        registry.putBehavior(Material.IRON_BOOTS, armorDispenseBehavior);
        registry.putBehavior(Material.IRON_LEGGINGS, armorDispenseBehavior);
        registry.putBehavior(Material.IRON_CHESTPLATE, armorDispenseBehavior);
        registry.putBehavior(Material.IRON_HELMET, armorDispenseBehavior);
        registry.putBehavior(Material.CHAINMAIL_BOOTS, armorDispenseBehavior);
        registry.putBehavior(Material.CHAINMAIL_LEGGINGS, armorDispenseBehavior);
        registry.putBehavior(Material.CHAINMAIL_CHESTPLATE, armorDispenseBehavior);
        registry.putBehavior(Material.CHAINMAIL_HELMET, armorDispenseBehavior);
        registry.putBehavior(Material.DIAMOND_BOOTS, armorDispenseBehavior);
        registry.putBehavior(Material.DIAMOND_LEGGINGS, armorDispenseBehavior);
        registry.putBehavior(Material.DIAMOND_CHESTPLATE, armorDispenseBehavior);
        registry.putBehavior(Material.DIAMOND_HELMET, armorDispenseBehavior);
        for (Material headType : MaterialTags.SKULLS.getValues()) {
            registry.putBehavior(headType, armorDispenseBehavior);
        }
        registry.putBehavior(Material.PUMPKIN, armorDispenseBehavior);

        registry.putBehavior(Material.EGG, new ProjectileDispenseBehavior(GlowEgg::new));
        registry.putBehavior(Material.SNOWBALL, new ProjectileDispenseBehavior(GlowSnowball::new));
        registry.putBehavior(Material.ARROW, new ProjectileDispenseBehavior(GlowArrow::new));
        registry.putBehavior(Material.EXPERIENCE_BOTTLE,
            new ProjectileDispenseBehavior(GlowThrownExpBottle::new));
        registry.putBehavior(Material.SPECTRAL_ARROW,
            new ProjectileDispenseBehavior(GlowSpectralArrow::new));
        registry.putBehavior(Material.TIPPED_ARROW,
            new ProjectileDispenseBehavior(((location, itemStack) -> {
                GlowTippedArrow tippedArrow = new GlowTippedArrow(location);
                tippedArrow.copyFrom((PotionMeta) itemStack.getItemMeta());
                return tippedArrow;
            })));
        registry.putBehavior(Material.FIRE_CHARGE, new ProjectileDispenseBehavior(location -> {
            Fireball fireball = new GlowFireball(location);
            fireball.setYield(0);
            fireball.setIsIncendiary(true);
            return fireball;
        }));
        registry.putBehavior(Material.SPLASH_POTION,
            new ProjectileDispenseBehavior((location, itemStack) -> {
                SplashPotion potion = new GlowSplashPotion(location);
                potion.setItem(itemStack);
                return potion;
            }));
        registry.putBehavior(Material.LINGERING_POTION,
            new ProjectileDispenseBehavior((location, itemStack) -> {
                SplashPotion potion = new GlowLingeringPotion(location);
                potion.setItem(itemStack);
                return potion;
            }));
        // TODO: Firework rockets
    }

    private DispenserEntity getBlockEntity() {
        return (DispenserEntity) getBlock().getBlockEntity();
    }

    @Override
    public BlockProjectileSource getBlockProjectileSource() {
        return this;
    }

    @Override
    public boolean dispense() {
        GlowBlock block = getBlock();

        int dispenseSlot = getDispenseSlot();
        if (dispenseSlot < 0) {
            block.getWorld().playEffect(block.getLocation(), Effect.CLICK1, 0);
            return false;
        }

        ItemStack origItems = getInventory().getItem(dispenseSlot);

        DispenseBehavior behavior = getDispenseBehavior(origItems.getType());
        ItemStack result = behavior.dispense(block, origItems);
        getInventory().setItem(dispenseSlot, result);
        return true;
    }

    /**
     * Returns the dispense behavior that will dispense this type of item.
     *
     * @param itemType the item type to dispense
     * @return the dispense behavior
     */
    protected DispenseBehavior getDispenseBehavior(Material itemType) {
        return getDispenseBehaviorRegistry().getBehavior(itemType);
    }

    public int getDispenseSlot() {
        return InventoryUtil.getRandomSlot(ThreadLocalRandom.current(), getInventory(), true);
    }

    /**
     * Puts as much as possible of an {@link ItemStack} in the dispenser, and returns the rest.
     *
     * @param toPlace the item stack
     * @return the portion of the item stack that didn't fit in the dispenser, or null if it all
     * fit
     */
    public ItemStack placeInDispenser(ItemStack toPlace) {
        Inventory inv = getInventory();
        Map<Integer, ItemStack> map = inv.addItem(toPlace);
        return map.isEmpty() ? null : map.get(0);
    }

    @Override
    public Inventory getInventory() {
        return getBlockEntity().getInventory();
    }

    @Override
    public boolean update(boolean force, boolean applyPhysics) {
        ItemStack[] contents = getInventory().getContents();

        boolean result = super.update(force, applyPhysics);

        if (result) {
            getBlockEntity().setContents(contents);
            getBlockEntity().updateInRange();
        }

        return result;
    }

    @Override
    public <T extends Projectile> T launchProjectile(Class<? extends T> projectile) {
        return launchProjectile(projectile, null);
    }

    @Override
    public <T extends Projectile> T launchProjectile(Class<? extends T> projectile,
                                                     Vector velocity) {
        // todo: projectile launching
        return null;
    }

}
