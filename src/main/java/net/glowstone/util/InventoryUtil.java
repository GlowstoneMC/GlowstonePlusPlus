package net.glowstone.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.glowstone.EventFactory;
import net.glowstone.entity.GlowPlayer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class InventoryUtil {

    public static final ItemStack[] NO_ITEMS = new ItemStack[0];
    public static final Collection<ItemStack> NO_ITEMS_COLLECTION = Collections.emptyList();
    public static final ImmutableItemStack EMPTY_STACK = new ImmutableItemStack(Material.AIR, 0);

    /**
     * Checks whether the given ItemStack is empty.
     *
     * @param stack the ItemStack to check
     * @return whether the given ItemStack is empty
     */
    public static boolean isEmpty(@Nullable ItemStack stack) {
        return stack == null || stack.getType() == Material.AIR || stack.getAmount() == 0;
    }

    /**
     * Converts the ItemStack to an empty ItemStack if the given ItemStack is null.
     *
     * @param stack the ItemStack to check and convert, if applicable
     * @return the converted ItemStack if applicable, the original ItemStack otherwise
     */
    @NotNull
    public static ItemStack itemOrEmpty(@Nullable ItemStack stack) {
        return stack == null ? createEmptyStack() : stack;
    }

    /**
     * Creates an empty ItemStack (Material.AIR * 0).
     *
     * @return an empty ItemStack
     */
    public static @NotNull ItemStack createEmptyStack() {
        return EMPTY_STACK;
    }

    /**
     * Get a random slot index in an Inventory.
     *
     * @param random      a Random instance
     * @param inventory   the inventory
     * @param ignoreEmpty whether to skip empty items in the inventory
     * @return the index of a random slot in the inventory, -1 if no possible slot was found
     */
    public static int getRandomSlot(@NotNull Random random,
                                    @NotNull Inventory inventory,
                                    boolean ignoreEmpty) {
        if (!ignoreEmpty) {
            return random.nextInt(inventory.getSize());
        }
        List<Integer> notEmpty = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (!isEmpty(inventory.getItem(i))) {
                notEmpty.add(i);
            }
        }
        if (notEmpty.isEmpty()) {
            return -1;
        }
        return notEmpty.get(random.nextInt(notEmpty.size()));
    }

    /**
     * Inflicts damage to an item. Unbreaking enchantment is applied if present.
     *
     * @param player  the player holding the item, or null if held by a dispenser
     * @param holding the item
     * @return the updated item stack
     */
    public static @NotNull ItemStack damageItem(@Nullable GlowPlayer player, @NotNull ItemStack holding) {
        if (player != null && player.getGameMode() == GameMode.CREATIVE) {
            return holding;
        }

        // Apply unbreaking enchantment.
        // TODO: Armor has a different formula for chance to avoid damage
        int durability = holding.getEnchantmentLevel(Enchantment.DURABILITY);
        if (durability > 0 && ThreadLocalRandom.current().nextDouble() < 1.0 / (durability + 1)) {
            return holding;
        }
        int damage = 1;
        if (player != null) {
            PlayerItemDamageEvent event = EventFactory.getInstance()
                .callEvent(new PlayerItemDamageEvent(player, holding, damage));
            if (event.isCancelled()) {
                return holding;
            }
            damage = event.getDamage();
        }
        holding.setDurability((short) (holding.getDurability() + damage));
        if (holding.getDurability() == holding.getType().getMaxDurability() + 1) {
            if (player != null) {
                EventFactory.getInstance().callEvent(new PlayerItemBreakEvent(player, holding));
            }
            return createEmptyStack();
        }

        return holding;
    }

    /**
     * Consumes 1 copy of the given item, which must be held by the given player, unless that player
     * is in Creative mode.
     *
     * @param player the player holding the item
     * @param item   the item to consume
     */
    public static void consumeHeldItem(@NotNull HumanEntity player, @NotNull ItemStack item) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().clear(player.getInventory().getHeldItemSlot());
        }
    }
}
