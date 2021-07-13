package net.glowstone.block.blocktype;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import net.glowstone.block.GlowBlock;
import net.glowstone.i18n.ConsoleMessages;
import net.glowstone.inventory.ToolType;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

// TODO: Merge into BlockSlab.
public class BlockDoubleSlab extends BlockType {

    private ItemStack getDrops(GlowBlock block) {
        Material type = block.getType();
        if (Tag.SLABS.isTagged(type)) {
            return new ItemStack(type, 2);
        } else {
            ConsoleMessages.Warn.Block.DoubleSlab.WRONG_MATERIAL.log(block.getType());
            return new ItemStack(Material.OAK_SLAB, 2);
        }
    }

    @NotNull
    @Override
    public Collection<ItemStack> getDrops(GlowBlock block, ItemStack tool) {
        if (Tag.WOODEN_SLABS.isTagged(block.getType())
                || tool != null && ToolType.PICKAXE.matches(tool.getType())) {
            return getMinedDrops(block);
        }
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<ItemStack> getMinedDrops(GlowBlock block) {
        return Arrays.asList(getDrops(block));
    }
}
