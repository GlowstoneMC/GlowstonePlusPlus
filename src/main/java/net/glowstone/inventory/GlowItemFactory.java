package net.glowstone.inventory;

import java.util.Locale;
import java.util.function.UnaryOperator;
import net.glowstone.util.nbt.CompoundTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.hover.content.Content;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of {@link ItemFactory} responsible for creating ItemMetas.
 */
public final class GlowItemFactory implements ItemFactory {

    private static final GlowItemFactory instance = new GlowItemFactory();
    private static final Color LEATHER_COLOR = Color.fromRGB(0xA06540);

    private GlowItemFactory() {
    }

    /**
     * Get the static GlowItemFactory instance.
     *
     * @return The instance.
     */
    public static GlowItemFactory instance() {
        return instance;
    }

    @Override
    public ItemMeta getItemMeta(@NotNull Material material) {
        return makeMeta(material, null);
    }

    @Override
    public boolean isApplicable(ItemMeta meta, ItemStack stack) throws IllegalArgumentException {
        return isApplicable(meta, stack.getType());
    }

    @Override
    public boolean isApplicable(ItemMeta meta, Material material) throws IllegalArgumentException {
        return meta != null && material != null && toGlowMeta(meta).isApplicable(material);
    }

    @Override
    public boolean equals(ItemMeta meta1, ItemMeta meta2) throws IllegalArgumentException {
        // todo: be nicer about comparisons without involving serialization
        // and the extra new objects for null arguments
        GlowMetaItem glow1;
        GlowMetaItem glow2;
        if (meta1 == null) {
            glow1 = new GlowMetaItem(null);
        } else {
            glow1 = toGlowMeta(meta1);
        }
        if (meta2 == null) {
            glow2 = new GlowMetaItem(null);
        } else {
            glow2 = toGlowMeta(meta2);
        }
        return glow1.serialize().equals(glow2.serialize());
    }

    @Override
    public ItemMeta asMetaFor(@NotNull ItemMeta meta, ItemStack stack) throws IllegalArgumentException {
        return makeMeta(stack.getType(), toGlowMeta(meta));
    }

    @Override
    public ItemMeta asMetaFor(@NotNull ItemMeta meta, @NotNull Material material) throws IllegalArgumentException {
        return makeMeta(material, toGlowMeta(meta));
    }

    @NotNull
    @Override
    public Color getDefaultLeatherColor() {
        return LEATHER_COLOR;
    }

    @Override
    public @NotNull Material updateMaterial(@NotNull ItemMeta meta, @NotNull Material material) throws IllegalArgumentException {
        // TODO: implementation (1.13.2)
        throw new NotImplementedException();
    }

    @Override
    public @NotNull HoverEvent<HoverEvent.ShowItem> asHoverEvent(@NotNull ItemStack itemStack, @NotNull UnaryOperator<HoverEvent.ShowItem> unaryOperator) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @NotNull Component displayName(@NotNull ItemStack itemStack) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @NotNull ItemStack ensureServerConversions(ItemStack itemStack) {
        // TODO: Implementation (1.12.1)
        return itemStack.clone();
    }

    @Override
    public String getI18NDisplayName(ItemStack itemStack) {
        // TODO: Implementation (1.12.1)
        return WordUtils.capitalize(itemStack.getType().name().replaceAll("_", " ").toLowerCase(Locale.ROOT));
    }

    @Override
    public @NotNull Content hoverContentOf(@NotNull ItemStack itemStack) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @NotNull Content hoverContentOf(@NotNull Entity entity) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @NotNull Content hoverContentOf(@NotNull Entity entity, @Nullable String s) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @NotNull Content hoverContentOf(@NotNull Entity entity, @Nullable BaseComponent baseComponent) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @NotNull Content hoverContentOf(@NotNull Entity entity, @NotNull BaseComponent[] baseComponents) {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    /**
     * Writes an {@link ItemMeta} to an NBT tag.
     *
     * @param meta an {@link ItemMeta}
     * @return a compound tag that can become the "tag" subtag of an item NBT tag, or null if
     * {@code meta} matches an item with no "tag" subtag
     */
    public CompoundTag writeNbt(ItemMeta meta) {
        CompoundTag result = new CompoundTag();
        toGlowMeta(meta).writeNbt(result);
        return result.isEmpty() ? null : result;
    }

    /**
     * Reads an {@link ItemMeta} from an NBT tag.
     *
     * @param material the material
     * @param tag      the "tag" subtag of an item NBT tag
     * @return the tag's contents as an {@link ItemMeta}
     */
    public ItemMeta readNbt(Material material, CompoundTag tag) {
        if (tag == null) {
            return null;
        }
        GlowMetaItem meta = makeMeta(material, null);
        if (meta == null) {
            return null;
        }
        meta.readNbt(tag);
        return meta;
    }

    /**
     * Throw a descriptive error if the given ItemMeta does not belong to this factory.
     *
     * @param meta The ItemMeta.
     * @return The GlowMetaItem.
     */
    private GlowMetaItem toGlowMeta(ItemMeta meta) {
        if (meta instanceof GlowMetaItem) {
            return (GlowMetaItem) meta;
        }
        throw new IllegalArgumentException(
                "Item meta " + meta + " was not created by GlowItemFactory");
    }

    /**
     * Get a suitable ItemMeta for the material, reusing the provided meta if non-null and possible.
     */
    private GlowMetaItem makeMeta(Material material, GlowMetaItem meta) {
        // todo: more specific metas
        // TODO: 1.13 will probably be nuked or retooled?
        switch (material) {
            case AIR:
                return null;
            case WRITABLE_BOOK:
            case WRITTEN_BOOK:
                return new GlowMetaBook(meta);
            case ENCHANTED_BOOK:
                return new GlowMetaEnchantedBook(meta);
            case SKELETON_SKULL:
            case WITHER_SKELETON_SKULL:
            case CREEPER_HEAD:
            case DRAGON_HEAD:
            case PLAYER_HEAD:
            case ZOMBIE_HEAD:
                return new GlowMetaSkull(meta);
            case LEGACY_BANNER:
                return new GlowMetaBanner(meta);
            case LEATHER_HELMET:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_BOOTS:
                return new GlowMetaLeatherArmor(meta);
            case FIREWORK_ROCKET:
                return new GlowMetaFirework(meta);
            case FIREWORK_STAR:
                return new GlowMetaFireworkEffect(meta);
            case POTION:
            case SPLASH_POTION:
            case LINGERING_POTION:
            case TIPPED_ARROW:
                return new GlowMetaPotion(meta);
            case LEGACY_MONSTER_EGG:
                return new GlowMetaSpawn(meta);
            case SHIELD:
                return new GlowMetaShield(meta);
            case KNOWLEDGE_BOOK:
                return new GlowMetaKnowledgeBook(meta);
            default:
                return new GlowMetaItem(meta);
        }
    }
}
