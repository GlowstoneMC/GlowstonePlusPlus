package net.glowstone.block.itemtype;

import java.util.Optional;
import net.glowstone.block.GlowBlock;
import net.glowstone.block.ItemTable;
import net.glowstone.block.blocktype.BlockType;
import net.glowstone.entity.GlowPlayer;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class ItemSign extends ItemType {

    @Override
    public void rightClickBlock(GlowPlayer player, GlowBlock target, BlockFace face,
                                ItemStack holding, Vector clickedLoc, EquipmentSlot hand) {
        String woodType = holding.getType().name().split("_SIGN")[0];
        Optional<Material> blockType;
        if (face == BlockFace.UP) {
            blockType = findMaterial(woodType, Tag.STANDING_SIGNS);
        } else if (face == BlockFace.DOWN) {
            blockType = Optional.empty();
        } else {
            blockType = findMaterial(woodType, Tag.WALL_SIGNS);
        }
        if (!blockType.isPresent()) {
            return;
        }
        BlockType placeAs = ItemTable.instance().getBlock(blockType.get());
        placeAs.rightClickBlock(player, target, face, holding, clickedLoc, hand);
    }

    private Optional<Material> findMaterial(String type, Tag<Material> materials) {
        for (Material material : materials.getValues()) {
            if (material.name().startsWith(type)) {
                return Optional.of(material);
            }
        }
        return Optional.empty();
    }

}
