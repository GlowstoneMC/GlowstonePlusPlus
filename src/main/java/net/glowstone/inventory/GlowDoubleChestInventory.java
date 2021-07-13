package net.glowstone.inventory;

import com.google.common.collect.ImmutableList;
import net.glowstone.block.entity.state.GlowChest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class GlowDoubleChestInventory extends GlowSuperInventory implements DoubleChestInventory {

    private GlowChest first;

    /**
     * Creates an instance for the given double chest.
     *
     * @param first the north or west half of the chest
     * @param second the south or east half of the chest
     */
    public GlowDoubleChestInventory(GlowChest first, GlowChest second) {
        initialize(
            // Inventories
            ImmutableList.of(
                (GlowInventory) first.getBlockInventory(),
                (GlowInventory) second.getBlockInventory()
            ),
            new DoubleChest(this), // Holder
            InventoryType.CHEST // Type
        );
        this.first = first;
    }

    @Override
    public @NotNull Inventory getLeftSide() {
        return getParents().get(0);
    }

    @Override
    public @NotNull Inventory getRightSide() {
        return getParents().get(1);
    }

    @Override
    public DoubleChest getHolder() {
        return (DoubleChest) super.getHolder();
    }

    @Override
    public void addViewer(HumanEntity viewer) {
        super.addViewer(viewer);
        first.getBlockEntity().addViewer();
    }


    @Override
    public void removeViewer(HumanEntity viewer) {
        super.removeViewer(viewer);
        first.getBlockEntity().removeViewer();
    }
}
