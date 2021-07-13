package net.glowstone.inventory.crafting;

import com.destroystokyo.paper.MaterialTags;
import java.util.List;
import lombok.Getter;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.material.Dye;

/**
 * Recipe for adding patterns to a banner item.
 */
public class GlowBannerMatcher extends ItemMatcher {

    ItemStack result = new ItemStack(Material.WHITE_BANNER); // Default result

    @Override
    public ItemStack getResult(ItemStack[] matrix) {
        DyeColor color = null;
        Pattern texture = null;
        ItemStack banner = null;

        for (ItemStack item : matrix) {
            if (item == null) {
                continue;
            }
            if (Tag.BANNERS.isTagged(item.getType())) {
                if (banner != null) {
                    return null; // Multiple banners found
                }
                banner = item;
                continue;
            }
            if (MaterialTags.DYES.isTagged(item.getType())) {
                DyeColor itemColor = ((Dye) item.getData()).getColor();
                if (color != null && itemColor != color) {
                    return null; // Can't have multiple colors
                }
                color = itemColor;
            }
        }
        if (banner == null) {
            return null; // Couldn't found a banner to alter
        }

        recipe:
        for (LayerRecipe recipe : LayerRecipe.values()) {
            if (recipe.hasItem()) {
                boolean foundDye = false;
                for (ItemStack item : matrix) {
                    if (item == null) {
                        continue; // Ignore blanks
                    }

                    if (Tag.BANNERS.isTagged(item.getType())) {
                        continue; // Banner is already checked
                    }

                    if (MaterialTags.DYES.isTagged(item.getType())) {
                        if (foundDye) {
                            continue recipe; // Can't have multiple dyes
                        }
                        foundDye = true;
                        continue;
                    }

                    if (item.getType() == recipe.getType() && item.getDurability() == recipe
                        .getData()) {
                        if (texture != null) {
                            return null; // Can't have multiple of same item
                        }
                        texture = recipe.getPattern(); // Matches texture type
                        continue;
                    }
                    continue recipe; // Non-recipe item in grid
                }
                if (texture == null) {
                    continue; // No item type for this recipe found
                }
                if (color == null) {
                    color = DyeColor.BLACK;
                }
                break; // Recipe matches
            } else {
                if (matrix.length != 9) {
                    return null; // Non-item recipes only work on 3x3
                }

                for (int i = 0; i < 9; i++) {
                    boolean hasValue = recipe.getValues()[i] == '#';
                    ItemStack item = matrix[i];
                    if (hasValue && item != null && MaterialTags.DYES.isTagged(item.getType())) {
                        continue;
                    }
                    if (!hasValue && (item == null || Tag.BANNERS.isTagged(item.getType()))) {
                        continue; // Allow banner and blanks
                    }
                    continue recipe; // Non-recipe item found or no dye where dye should be
                }
                texture = recipe.getPattern();
                break; // Recipe matches
            }
        }

        if (texture == null) {
            return null; // No texture found
        }

        // Create result banner
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        List<Pattern> layers = meta.getPatterns();
        meta.setPatterns(layers);
        result = banner.clone();
        result.setItemMeta(meta);
        return result;
    }

    private enum LayerRecipe {
        BORDER("###", "# #", "###"),
        BOTTOM_HALF("   ", "###", "###"),
        BRICK(Material.BRICK),
        CIRCLE("   ", " # ", "   "),
        CREEPER(Material.CREEPER_HEAD),
        CURLY_BORDER(Material.VINE),
        DIAGONAL_CROSS("# #", " # ", "# #"),
        FLOWER(Material.OXEYE_DAISY),
        GRADIENT("# #", " # ", " # "),
        GRADIENT_UP(" # ", " # ", "# #"),
        LEFT_HALF("## ", "## ", "## "),
        MOJANG(Material.GOLDEN_APPLE),
        RIGHT_HALF(" ##", " ##", " ##"),
        RHOMBUS(" # ", "# #", " # "),
        SAW_BOTTOM("   ", "# #", " # "),
        SAW_TOP(" # ", "# #", "   "),
        SKULL(Material.WITHER_SKELETON_SKULL),
        STRIPES("# #", "# #", "   "),
        SQUARE_BOTTOM_LEFT("   ", "   ", "#  "),
        SQUARE_BOTTOM_RIGHT("   ", "   ", "  #"),
        SQUARE_TOP_LEFT("#  ", "   ", "   "),
        SQUARE_TOP_RIGHT("  #", "   ", "   "),
        STRIPE_BOTTOM("   ", "   ", "###"),
        STRAIGHT_CROSS(" # ", "###", " # "),
        STRIPE_DOWN_LEFT("  #", " # ", "#  "),
        STRIPE_DOWN_RIGHT("#  ", " # ", "  #"),
        STRIPE_LEFT("#  ", "#  ", "#  "),
        STRIPE_HORIZONTAL("   ", "###", "   "),
        STRIPE_RIGHT("  #", "  #", "  #"),
        STRIPE_TOP("###", "   ", "   "),
        STRIPE_VERTICAL(" # ", " # ", " # "),
        TOP_HALF("###", "###", "   "),
        TRIANGLE_BOTTOM("   ", " # ", "# #"),
        TRIANGLE_BOTTOM_LEFT("   ", "#  ", "## "),
        TRIANGLE_BOTTOM_RIGHT("   ", "  #", " ##"),
        TRIANGLE_TOP("# #", " # ", "   "),
        TRIANGLE_TOP_LEFT("## ", "#  ", "   "),
        TRIANGLE_TOP_RIGHT(" ##", "  #", "   ");
        @Getter
        char[] values;
        @Getter
        Material type;
        @Getter
        short data;

        LayerRecipe(String... rows) {
            values = new char[9];
            int index = 0;
            for (String row : rows) {
                for (char c : row.toCharArray()) {
                    values[index++] = c;
                }
            }
        }

        LayerRecipe(Material item) {
            this.type = item;
        }

        LayerRecipe(Material item, short data) {
            this.type = item;
            this.data = data;
        }

        public boolean hasItem() {
            return type != null;
        }

        public Pattern getPattern() {
            DyeColor dyeColor = DyeColor.getByDyeData((byte) data);
            PatternType patternType = PatternType.getByIdentifier(toString());
            return new Pattern(dyeColor, patternType);
        }
    }
}
