package net.glowstone.inventory;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import net.glowstone.block.blocktype.BlockBanner;
import net.glowstone.util.nbt.CompoundTag;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class GlowMetaBanner extends GlowMetaItem implements BannerMeta {

    protected List<Pattern> patterns = new ArrayList<>();
    @Getter
    @Setter
    protected DyeColor baseColor = null;

    /**
     * Creates an instance by copying from the given {@link ItemMeta}. If that item is another
     * {@link BannerMeta}, its patterns are copied; otherwise, the new banner is blank.
     * @param meta the {@link ItemMeta} to copy
     */
    public GlowMetaBanner(ItemMeta meta) {
        super(meta);
        if (!(meta instanceof BannerMeta)) {
            return;
        }
        BannerMeta banner = (BannerMeta) meta;
        patterns = banner.getPatterns();
        baseColor = banner.getBaseColor();
    }

    @Override
    public @NotNull List<Pattern> getPatterns() {
        return new ArrayList<>(patterns);
    }

    @Override
    public void setPatterns(@NotNull List<Pattern> patterns) {
        checkNotNull(patterns, "Pattern cannot be null!");
        this.patterns.clear();
        this.patterns.addAll(patterns);
    }

    @Override
    public void addPattern(@NotNull Pattern pattern) {
        patterns.add(pattern);
    }

    @Override
    public @NotNull Pattern getPattern(int i) {
        return patterns.get(i);
    }

    @Override
    public @NotNull Pattern removePattern(int i) {
        return patterns.remove(i);
    }

    @Override
    public void setPattern(int i, @NotNull Pattern pattern) {
        patterns.set(i, pattern);
    }

    @Override
    public int numberOfPatterns() {
        return patterns.size();
    }

    @Override
    void writeNbt(CompoundTag tag) {
        super.writeNbt(tag);
        CompoundTag blockEntityTag = new CompoundTag();

        blockEntityTag.putCompoundList("Patterns", BlockBanner.toNbt(patterns));
        if (baseColor != null) {
            blockEntityTag.putInt("Base", baseColor.getWoolData());
        }
        tag.putCompound("BlockEntityTag", blockEntityTag);
    }

    @Override
    void readNbt(CompoundTag tag) {
        super.readNbt(tag);
        tag.readCompound("BlockEntityTag", blockEntityTag -> {
            blockEntityTag.readCompoundList(
                    "Patterns", patterns -> this.patterns = BlockBanner.fromNbt(patterns));
            blockEntityTag.readInt(
                    "Base", colorInt -> this.baseColor = DyeColor.getByWoolData((byte) colorInt));
        });
    }

    @Override
    public @NotNull ItemMeta clone() {
        return new GlowMetaBanner(this);
    }

    @Override
    public boolean isApplicable(Material material) {
        // TODO: convert to 1.13 types, but meta classes will probably be nuked anyways...
        return material == Material.LEGACY_BANNER;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> result = super.serialize();
        result.put("meta-type", "BANNER");
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
