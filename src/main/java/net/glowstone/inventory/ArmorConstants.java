package net.glowstone.inventory;

import lombok.Getter;
import net.glowstone.util.InventoryUtil;
import org.bukkit.inventory.ItemStack;

public enum ArmorConstants {

    LEATHER_HELMET(1), LEATHER_CHESTPLATE(3), LEATHER_LEGGINGS(2), LEATHER_BOOTS(1),
    GOLDEN_HELMET(2), GOLDEN_CHESTPLATE(5), GOLDEN_LEGGINGS(3), GOLDEN_BOOTS(1),
    CHAINMAIL_HELMET(2), CHAINMAIL_CHESTPLATE(5), CHAINMAIL_LEGGINGS(4), CHAINMAIL_BOOTS(1),
    IRON_HELMET(2), IRON_CHESTPLATE(6), IRON_LEGGINGS(5), IRON_BOOTS(2),
    DIAMOND_HELMET(3, 2), DIAMOND_CHESTPLATE(8, 2), DIAMOND_LEGGINGS(6, 2), DIAMOND_BOOTS(3, 2);

    @Getter
    private final int defense;

    @Getter
    private final int toughness;

    ArmorConstants(int defense, int toughness) {
        this.defense = defense;
        this.toughness = toughness;
    }

    ArmorConstants(int defense) {
        this(defense, 0);
    }

    /**
     * Calculate total defense of a suit of armor.
     *
     * @param armor all armor items worn
     * @return the total defense
     */
    public static int getDefense(ItemStack[] armor) {
        int defense = 0;
        for (ItemStack stack : armor) {
            if (InventoryUtil.isEmpty(stack)) {
                continue;
            }
            try {
                ArmorConstants constant = valueOf(stack.getType().name());
                defense += constant.getDefense();
            } catch (IllegalArgumentException ignored) {
                // Ignored
            }
        }
        return defense;
    }

    /**
     * Calculate total toughness of a suit of armor.
     *
     * @param armor all armor items worn
     * @return the total toughness
     */
    public static int getToughness(ItemStack[] armor) {
        int toughness = 0;
        for (ItemStack stack : armor) {
            if (InventoryUtil.isEmpty(stack)) {
                continue;
            }
            try {
                ArmorConstants constant = valueOf(stack.getType().name());
                toughness += constant.getToughness();
            } catch (IllegalArgumentException ignored) {
                // Ignored
            }
        }
        return toughness;
    }
}
