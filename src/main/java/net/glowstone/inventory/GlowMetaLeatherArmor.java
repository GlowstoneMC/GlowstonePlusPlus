package net.glowstone.inventory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import lombok.Getter;
import net.glowstone.util.nbt.CompoundTag;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;

public class GlowMetaLeatherArmor extends GlowMetaItem implements LeatherArmorMeta {

    private static final Color DEFAULT_LEATHER_COLOR
            = GlowItemFactory.instance().getDefaultLeatherColor();
    @Getter
    private Color color = GlowItemFactory.instance().getDefaultLeatherColor();

    /**
     * Creates an instance by copying from the given {@link ItemMeta}. If that item is another
     * {@link LeatherArmorMeta}, its color is copied; otherwise, the new item is undyed.
     * @param meta the {@link ItemMeta} to copy
     */
    public GlowMetaLeatherArmor(ItemMeta meta) {
        super(meta);
        if (meta instanceof LeatherArmorMeta) {
            color = ((LeatherArmorMeta) meta).getColor();
        }
    }

    @Override
    public void setColor(Color color) {
        checkNotNull(color, "Armor color cannot be null");
        this.color = color;
    }

    @Override
    public boolean isApplicable(Material material) {
        switch (material) {
            case LEATHER_HELMET:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_BOOTS:
                return true;
            default:
                return false;
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> result = super.serialize();
        result.put("meta-type", "LEATHER_ARMOR");
        if (hasColor()) {
            result.put("color", color.serialize());
        }
        return result;
    }

    @Override
    void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);

        if (hasColor()) {
            CompoundTag display = new CompoundTag();
            display.putInt("color", color.asRGB());
            tag.putCompound("display", display);
        }
    }

    @Override
    void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        tag.readCompound("display", display ->
                display.readInt("color", rgb -> color = Color.fromRGB(rgb))
        );
    }

    @Override
    public @NotNull LeatherArmorMeta clone() {
        return new GlowMetaLeatherArmor(this);
    }

    private boolean hasColor() {
        return !color.equals(DEFAULT_LEATHER_COLOR);
    }
}
