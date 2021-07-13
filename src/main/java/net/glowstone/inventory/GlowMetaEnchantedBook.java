package net.glowstone.inventory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.glowstone.util.nbt.CompoundTag;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class GlowMetaEnchantedBook extends GlowMetaItem implements EnchantmentStorageMeta {

    private Map<Enchantment, Integer> storedEnchants;

    /**
     * Creates an instance by copying from the given {@link ItemMeta}. If that item is another
     * {@link EnchantmentStorageMeta}, its enchantments are copied; otherwise, the new book has no
     * enchantments.
     * @param meta the {@link ItemMeta} to copy
     */
    public GlowMetaEnchantedBook(ItemMeta meta) {
        super(meta);

        if (!(meta instanceof EnchantmentStorageMeta)) {
            return;
        }

        EnchantmentStorageMeta book = (EnchantmentStorageMeta) meta;
        if (book.hasStoredEnchants()) {
            storedEnchants = new HashMap<>(book instanceof GlowMetaEnchantedBook
                    ? ((GlowMetaEnchantedBook) book).storedEnchants : book.getStoredEnchants());
        }
    }

    @Override
    public boolean isApplicable(Material material) {
        return material == Material.ENCHANTED_BOOK;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();

        map.put("meta-type", "ENCHANTED");

        if (hasStoredEnchants()) {
            serializeEnchants("stored-enchants", map, storedEnchants);
        }

        return map;
    }

    @Override
    void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);

        writeNbtEnchants("StoredEnchantments", tag, storedEnchants);
    }

    @Override
    void readNbt(CompoundTag tag) {
        super.readNbt(tag);

        //TODO currently ignoring level restriction, is that right?
        Map<Enchantment, Integer> enchants = readNbtEnchants("StoredEnchantments", tag);
        if (enchants != null) {
            if (storedEnchants == null) {
                storedEnchants = enchants;
            } else {
                storedEnchants.putAll(enchants);
            }
        }
    }

    @Override
    public boolean hasStoredEnchants() {
        return storedEnchants != null && !storedEnchants.isEmpty();
    }

    @Override
    public boolean hasStoredEnchant(@NotNull Enchantment ench) {
        return hasStoredEnchants() && storedEnchants.containsKey(ench);
    }

    @Override
    public int getStoredEnchantLevel(@NotNull Enchantment ench) {
        return hasStoredEnchant(ench) ? storedEnchants.get(ench) : 0;
    }

    @Override
    public @NotNull Map<Enchantment, Integer> getStoredEnchants() {
        return hasStoredEnchants() ? Collections.unmodifiableMap(storedEnchants)
                : Collections.emptyMap();
    }

    @Override
    public boolean addStoredEnchant(@NotNull Enchantment ench, int level, boolean ignoreLevelRestriction) {
        if (storedEnchants == null) {
            storedEnchants = new HashMap<>(4);
        }

        if (ignoreLevelRestriction || level >= ench.getStartLevel() && level <= ench
                .getMaxLevel()) {
            Integer old = storedEnchants.put(ench, level);
            return old == null || old != level;
        }
        return false;
    }

    @Override
    public boolean removeStoredEnchant(@NotNull Enchantment ench) throws IllegalArgumentException {
        return hasStoredEnchants() && storedEnchants.remove(ench) != null;
    }

    @Override
    public boolean hasConflictingStoredEnchant(@NotNull Enchantment ench) {
        if (!hasStoredEnchants()) {
            return false;
        }

        for (Enchantment e : storedEnchants.keySet()) {
            if (e.conflictsWith(ench)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public @NotNull GlowMetaEnchantedBook clone() {
        return new GlowMetaEnchantedBook(this);
    }
}
