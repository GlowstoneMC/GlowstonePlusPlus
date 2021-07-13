package net.glowstone.block.blocktype;

import net.glowstone.block.GlowBlock;
import net.glowstone.block.GlowBlockState;
import net.glowstone.block.ItemTable;
import net.glowstone.block.entity.BlockEntity;
import net.glowstone.block.entity.SignEntity;
import net.glowstone.chunk.GlowChunk;
import net.glowstone.entity.GlowPlayer;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Sign;
import org.bukkit.util.Vector;

public class BlockSign extends BlockNeedsAttached {

    public BlockSign(Material material) {
        setDrops(new ItemStack(material));
    }

    @Override
    public BlockEntity createBlockEntity(GlowChunk chunk, int cx, int cy, int cz) {
        return new SignEntity(chunk.getBlock(cx, cy, cz));
    }

    @Override
    public void placeBlock(GlowPlayer player, GlowBlockState state, BlockFace face,
                           ItemStack holding, Vector clickedLoc) {
        state.setType(getMaterial());
        if (!(state.getData() instanceof Sign)) {
            warnMaterialData(Sign.class, state.getData());
            return;
        }
        Sign sign = (Sign) state.getData();
        sign.setFacingDirection(sign.isWallSign() ? face : player.getFacing().getOppositeFace());
    }

    @Override
    public void afterPlace(GlowPlayer player, GlowBlock block, ItemStack holding,
                           GlowBlockState oldState) {
        player.openSignEditor(block.getLocation());
    }

    @Override
    public boolean canPlaceAt(GlowPlayer player, GlowBlock block, BlockFace against) {
        Material targetMat = ItemTable.instance().getBlock(
            block.getRelative(against.getOppositeFace()).getType()).getMaterial();
        return canAttachTo(block, against) || Tag.SIGNS.isTagged(targetMat);
    }
}
