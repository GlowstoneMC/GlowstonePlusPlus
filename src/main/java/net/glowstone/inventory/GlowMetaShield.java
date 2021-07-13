package net.glowstone.inventory;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.glowstone.block.blocktype.BlockBanner;
import net.glowstone.util.nbt.CompoundTag;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class GlowMetaShield extends GlowMetaBanner {

    /**
     * Creates an instance by copying from the given {@link ItemMeta}. If that item is another
     * {@link BannerMeta}, the banner is copied; otherwise, the new shield has no banner.
     * @param meta the {@link ItemMeta} to copy
     */
    public GlowMetaShield(ItemMeta meta) {
        super(meta);
    }

    @Override
    void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);
        if (baseColor != null) {
            tag.putInt("Base", baseColor.getWoolData());
        }
        tag.putCompoundList("Patterns", BlockBanner.toNbt(patterns));
    }

    @Override
    void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        tag.readCompoundList("Patterns", patterns -> this.patterns = BlockBanner.fromNbt(patterns)
        );
        tag.readInt("Base", color -> this.baseColor = DyeColor.getByWoolData((byte) color));
    }

    @Override
    public @NotNull ItemMeta clone() {
        return new GlowMetaShield(this);
    }

    @Override
    public boolean isApplicable(Material material) {
        return material == Material.SHIELD;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> result = super.serialize();
        result.put("meta-type", "SHIELD");
        List<Map<String, String>> patternsList = new ArrayList<>();
        for (Pattern pattern : patterns) {
            patternsList.add(
                ImmutableMap.of(pattern.getPattern().toString(), pattern.getColor().toString()));
        }
        result.put("pattern", patternsList);
        if (baseColor != null) {
            result.put("baseColor", baseColor);
        }
        return result;
    }
}
