package net.glowstone.block;

import com.destroystokyo.paper.MaterialSetTag;
import com.destroystokyo.paper.MaterialTags;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.data.Bisected;

/**
 * Useful constant groups of materials, many of which were just 1 or 2 materials pre-Flattening.
 */
public class GlowTags {
    private static NamespacedKey keyFor(String key) {
        //noinspection deprecation
        return new NamespacedKey("glowstone", key + "_settag");
    }

    public static final Tag<Material> WOODS =
        new MaterialSetTag(keyFor("woods"))
            .endsWith("_WOOD")
            .notStartsWith("STRIPPED_")
            .ensureSize("WOODS", 6);

    public static final Tag<Material> AIR_VARIANTS =
        new MaterialSetTag(keyFor("air_variants"))
            .endsWith("_AIR")
            .add(Material.AIR)
            .add(Material.STRUCTURE_VOID)
            .ensureSize("AIR_VARIANTS", 4);

    public static final Tag<Material> BISECTED_BLOCKS =
        new MaterialSetTag(keyFor("bisected_blocks"))
            .add(mat -> Bisected.class.isAssignableFrom(mat.getData()));

    public static final Tag<Material> BEACON_BASE =
        new MaterialSetTag(keyFor("beacon_base"))
            .add(
                Material.EMERALD_BLOCK,
                Material.GOLD_BLOCK,
                Material.DIAMOND_BLOCK,
                Material.IRON_BLOCK
            );

    public static final Tag<Material> TRANSPARENT_BLOCKS =
        new MaterialSetTag(keyFor("transparent_blocks"))
            .add(
                Material.AIR,
                Material.GLASS,
                Material.GLASS_PANE
            )
            .add(
                MaterialTags.STAINED_GLASS_PANES,
                MaterialTags.STAINED_GLASS
            );
}
