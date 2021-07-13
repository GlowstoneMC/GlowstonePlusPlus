package net.glowstone.inventory;

import com.destroystokyo.paper.Namespaced;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import net.glowstone.io.nbt.NbtSerialization;
import net.glowstone.util.nbt.CompoundTag;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.tags.CustomItemTagContainer;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of {@link ItemMeta}, created through {@link GlowItemFactory}.
 */
public class GlowMetaItem implements ItemMeta {

    private final SetMultimap<Attribute, AttributeModifier> attributeModifiers = HashMultimap.create();
    private final Set<Namespaced> placeableKeys = new HashSet<>();
    private final Set<Namespaced> destroyableKeys = new HashSet<>();
    private final Set<Material> canPlaceOn = new HashSet<>();
    private final Set<Material> canDestroy = new HashSet<>();
    @Getter
    @Setter
    private String displayName;
    private List<String> lore;
    private Map<Enchantment, Integer> enchants;
    private int hideFlag;
    @Getter
    @Setter
    private boolean unbreakable;

    /**
     * Create a GlowMetaItem, copying from another if possible.
     *
     * @param meta The meta to copy from, or null.
     */
    public GlowMetaItem(ItemMeta meta) {
        if (meta == null) {
            return;
        }

        displayName = meta.getDisplayName();

        if (meta.hasLore()) {
            lore = new ArrayList<>(meta.getLore());
        }
        if (meta.hasEnchants()) {
            enchants = new HashMap<>(meta.getEnchants());
        }
        if (meta instanceof GlowMetaItem) {
            hideFlag = ((GlowMetaItem) meta).hideFlag;
        } else {
            for (ItemFlag flag : meta.getItemFlags()) {
                addItemFlags(flag);
            }
        }
    }

    protected static void serializeEnchants(String name, Map<String, Object> map,
        Map<Enchantment, Integer> enchants) {
        Map<String, Object> enchantList = new HashMap<>();

        for (Entry<Enchantment, Integer> enchantment : enchants.entrySet()) {
            enchantList.put(enchantment.getKey().getName(), enchantment.getValue());
        }

        map.put(name, enchantList);
    }

    protected static void writeNbtEnchants(String name, CompoundTag to,
        Map<Enchantment, Integer> enchants) {
        List<CompoundTag> ench = new ArrayList<>();

        for (Entry<Enchantment, Integer> enchantment : enchants.entrySet()) {
            CompoundTag enchantmentTag = new CompoundTag();

            enchantmentTag.putString("id", enchantment.getKey().getKey().toString());
            enchantmentTag.putShort("lvl", enchantment.getValue());
            ench.add(enchantmentTag);
        }

        to.putCompoundList(name, ench);
    }

    protected static Map<Enchantment, Integer> readNbtEnchants(String name, CompoundTag tag) {
        Map<Enchantment, Integer> result = new HashMap<>(4);
        tag.iterateCompoundList(name, enchantmentTag -> {
            if (enchantmentTag.isString("id") && enchantmentTag.isShort("lvl")) {
                Enchantment enchantment = Enchantment.getByKey(
                        NbtSerialization.namespacedKeyFromString(
                                enchantmentTag.getString("id")));
                result.put(enchantment, (int) enchantmentTag.getShort("lvl"));
            }
        });
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    /**
     * Check whether this ItemMeta can be applied to the given material.
     *
     * @param material The Material.
     * @return True if this ItemMeta is applicable.
     */
    public boolean isApplicable(Material material) {
        return material != Material.AIR;
    }

    @Override
    public Set<Material> getCanDestroy() {
        return new HashSet<>(canDestroy);
    }

    @Override
    public void setCanDestroy(Set<Material> canDestroy) {
        this.canDestroy.clear();
        this.canDestroy.addAll(canDestroy);
    }

    @Override
    public Set<Material> getCanPlaceOn() {
        return new HashSet<>(canPlaceOn);
    }

    @Override
    public void setCanPlaceOn(Set<Material> canPlaceOn) {
        this.canPlaceOn.clear();
        this.canPlaceOn.addAll(canPlaceOn);
    }

    @Override
    public @NotNull Set<Namespaced> getDestroyableKeys() {
        return new HashSet<>(destroyableKeys);
    }

    @Override
    public void setDestroyableKeys(@NotNull Collection<Namespaced> canDestroy) {
        destroyableKeys.clear();
        destroyableKeys.addAll(canDestroy);
    }

    @Override
    public @NotNull Set<Namespaced> getPlaceableKeys() {
        return new HashSet<>(placeableKeys);
    }

    @Override
    public void setPlaceableKeys(@NotNull Collection<Namespaced> canPlaceOn) {
        placeableKeys.clear();
        placeableKeys.addAll(canPlaceOn);
    }

    @Override
    public boolean hasPlaceableKeys() {
        return !placeableKeys.isEmpty();
    }

    @Override
    public boolean hasDestroyableKeys() {
        return !destroyableKeys.isEmpty();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("meta-type", "UNSPECIFIC");
        result.put("unbreakable", isUnbreakable());

        if (hasDisplayName()) {
            result.put("display-name", getDisplayName());
        }
        if (hasLore()) {
            result.put("lore", getLore());
        }

        if (hasEnchants()) {
            serializeEnchants("enchants", result, getEnchants());
        }

        if (hideFlag != 0) {
            Set<String> hideFlags = getItemFlags().stream().map(Enum::name)
                .collect(Collectors.toSet());
            if (hideFlags.isEmpty()) {
                result.put("ItemFlags", hideFlags);
            }
        }
        // TODO: New fields added in 1.13
        return result;
    }

    void writeNbt(CompoundTag tag) {
        CompoundTag displayTags = new CompoundTag();
        if (hasDisplayName()) {
            displayTags.putString("Name", getDisplayName());
        }
        if (hasLore()) {
            displayTags.putStringList("Lore", getLore());
        }

        if (!displayTags.isEmpty()) {
            tag.putCompound("display", displayTags);
        }

        if (hasEnchants()) {
            writeNbtEnchants("Enchantments", tag, enchants);
        }

        if (hideFlag != 0) {
            tag.putInt("HideFlags", hideFlag);
        }
        tag.putBool("Unbreakable", isUnbreakable());
    }

    void readNbt(CompoundTag tag) {
        tag.readCompound("display", display -> {
            display.readString("Name", this::setDisplayName);
            display.readStringList("Lore", this::setLore);
        });

        //TODO currently ignoring level restriction, is that right?
        Map<Enchantment, Integer> tagEnchants = readNbtEnchants("Enchantments", tag);
        if (tagEnchants != null) {
            if (enchants == null) {
                enchants = tagEnchants;
            } else {
                enchants.putAll(tagEnchants);
            }
        }
        tag.readInt("HideFlags", flags -> hideFlag = flags);
        tag.readBoolean("Unbreakable", this::setUnbreakable);
    }

    @Override
    public String toString() {
        Map<String, Object> map = serialize();
        return map.get("meta-type") + "_META:" + map;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Basic properties

    @Override
    public boolean hasDisplayName() {
        return !Strings.isNullOrEmpty(displayName);
    }

    @Override
    public @Nullable Component displayName() {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public void displayName(@Nullable Component component) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @NotNull BaseComponent[] getDisplayNameComponent() {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public void setDisplayNameComponent(@Nullable BaseComponent[] baseComponents) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    // TODO: support localization

    @Override
    public boolean hasLocalizedName() {
        return hasDisplayName();
    }

    @Override
    public @NotNull String getLocalizedName() {
        return getDisplayName();
    }

    @Override
    public void setLocalizedName(String name) {
        displayName = name;
    }

    @Override
    public boolean hasLore() {
        return lore != null && !lore.isEmpty();
    }

    @Override
    public @Nullable List<Component> lore() {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public void lore(@Nullable List<Component> list) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public List<String> getLore() {
        // TODO: Defensive copy
        return lore;
    }

    @Override
    public @Nullable List<BaseComponent[]> getLoreComponents() {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public void setLore(List<String> lore) {
        // todo: fancy validation things
        // TODO: Defensive copy
        this.lore = lore;
    }

    @Override
    public void setLoreComponents(@Nullable List<BaseComponent[]> list) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public boolean hasCustomModelData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getCustomModelData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCustomModelData(@Nullable Integer integer) {
        throw new UnsupportedOperationException();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Enchants

    @Override
    public boolean hasEnchants() {
        return enchants != null && !enchants.isEmpty();
    }

    @Override
    public boolean hasEnchant(@NotNull Enchantment ench) {
        return hasEnchants() && enchants.containsKey(ench);
    }

    @Override
    public int getEnchantLevel(@NotNull Enchantment ench) {
        return hasEnchant(ench) ? enchants.get(ench) : 0;
    }

    @Override
    public @NotNull Map<Enchantment, Integer> getEnchants() {
        return hasEnchants() ? Collections.unmodifiableMap(enchants) : Collections.emptyMap();
    }

    @Override
    public boolean addEnchant(@NotNull Enchantment ench, int level, boolean ignoreLevelRestriction) {
        if (enchants == null) {
            enchants = new HashMap<>(4);
        }

        if (ignoreLevelRestriction || level >= ench.getStartLevel() && level <= ench
            .getMaxLevel()) {
            Integer old = enchants.put(ench, level);
            return old == null || old != level;
        }
        return false;
    }

    @Override
    public boolean removeEnchant(@NotNull Enchantment ench) {
        return hasEnchants() && enchants.remove(ench) != null;
    }

    @Override
    public boolean hasConflictingEnchant(@NotNull Enchantment ench) {
        if (!hasEnchants()) {
            return false;
        }

        for (Enchantment e : enchants.keySet()) {
            if (e.conflictsWith(ench)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public @NotNull ItemMeta clone() {
        return new GlowMetaItem(this);
    }

    @Override
    public void addItemFlags(ItemFlag... itemFlags) {
        for (ItemFlag itemFlag : itemFlags) {
            hideFlag |= getBitModifier(itemFlag);
        }
    }

    @Override
    public void removeItemFlags(ItemFlag... itemFlags) {
        for (ItemFlag itemFlag : itemFlags) {
            hideFlag &= ~getBitModifier(itemFlag);
        }
    }

    @Override
    public @NotNull Set<ItemFlag> getItemFlags() {
        Set<ItemFlag> currentFlags = EnumSet.noneOf(ItemFlag.class);
        ItemFlag[] values;
        for (int length = (values = ItemFlag.values()).length, i = 0; i < length; ++i) {
            ItemFlag f = values[i];
            if (hasItemFlag(f)) {
                currentFlags.add(f);
            }
        }
        return currentFlags;
    }

    @Override
    public boolean hasItemFlag(@NotNull ItemFlag itemFlag) {
        int bitModifier = getBitModifier(itemFlag);
        return (hideFlag & bitModifier) == bitModifier;
    }

    @Override
    public boolean hasAttributeModifiers() {
        return !attributeModifiers.isEmpty();
    }

    @Override
    public @Nullable Multimap<Attribute, AttributeModifier> getAttributeModifiers() {
        return HashMultimap.create(attributeModifiers);
    }

    @Override
    public @NotNull Multimap<Attribute, AttributeModifier> getAttributeModifiers(@NotNull EquipmentSlot slot) {
        return Multimaps.filterValues(getAttributeModifiers(), modifier -> slot == modifier.getSlot());
    }

    @Override
    public @Nullable Collection<AttributeModifier> getAttributeModifiers(
            @NotNull Attribute attribute) {
        return null;
    }

    @Override
    public boolean addAttributeModifier(@NotNull Attribute attribute,
            @NotNull AttributeModifier modifier) {
        return false;
    }

    @Override
    public void setAttributeModifiers(
            @Nullable Multimap<Attribute, AttributeModifier> attributeModifiers) {

    }

    @Override
    public boolean removeAttributeModifier(@NotNull Attribute attribute) {
        return false;
    }

    @Override
    public boolean removeAttributeModifier(@NotNull EquipmentSlot slot) {
        return false;
    }

    @Override
    public boolean removeAttributeModifier(@NotNull Attribute attribute,
            @NotNull AttributeModifier modifier) {
        return false;
    }

    @Override
    public @NotNull CustomItemTagContainer getCustomTagContainer() {
        return null;
    }

    @Override
    public void setVersion(int version) {
        throw new UnsupportedOperationException();
    }

    private byte getBitModifier(ItemFlag hideFlag) {
        return (byte) (1 << hideFlag.ordinal());
    }

    @Override
    public @NotNull PersistentDataContainer getPersistentDataContainer() {
        throw new UnsupportedOperationException();
    }
}
