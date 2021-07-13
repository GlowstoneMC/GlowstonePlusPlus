package net.glowstone.inventory;

import net.glowstone.GlowServer;
import net.glowstone.ServerProvider;
import net.glowstone.block.GlowBlock;
import net.glowstone.entity.GlowPlayer;
import org.bukkit.Material;
import org.bukkit.block.Furnace;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class GlowFurnaceInventory extends GlowInventory implements FurnaceInventory {

    private static final int INPUT_SLOT = 0;
    private static final int FUEL_SLOT = 1;
    private static final int RESULT_SLOT = 2;

    /**
     * Creates the given furnace's inventory.
     *
     * @param owner the furnace BlockState
     */
    public GlowFurnaceInventory(Furnace owner) {
        super(owner, InventoryType.FURNACE);

        getSlot(INPUT_SLOT).setType(SlotType.CRAFTING);
        getSlot(FUEL_SLOT).setType(SlotType.FUEL);
        getSlot(RESULT_SLOT).setType(SlotType.RESULT);
    }

    @Override
    public void setItem(int index, ItemStack item) {
        super.setItem(index, item);
        GlowBlock block = (GlowBlock) getHolder().getBlock();
        block.getWorld().requestPulse(block);
    }

    @Override
    public ItemStack getResult() {
        return getItem(RESULT_SLOT);
    }

    @Override
    public void setResult(ItemStack stack) {
        setItem(RESULT_SLOT, stack);
    }

    @Override
    public ItemStack getFuel() {
        return getItem(FUEL_SLOT);
    }

    @Override
    public void setFuel(ItemStack stack) {
        setItem(FUEL_SLOT, stack);
    }

    @Override
    public ItemStack getSmelting() {
        return getItem(INPUT_SLOT);
    }

    @Override
    public void setSmelting(ItemStack stack) {
        setItem(INPUT_SLOT, stack);
    }

    @Override
    public Furnace getHolder() {
        return (Furnace) super.getHolder();
    }

    @Override
    public void handleShiftClick(GlowPlayer player, InventoryView view, int clickedSlot,
        ItemStack clickedItem) {
        if (getSlotType(view.convertSlot(clickedSlot)) == SlotType.RESULT) {
            // Place the items in the player's inventory (right to left)
            clickedItem = player.getInventory().tryToFillSlots(clickedItem, 8, -1, 35, 8);
        } else {
            // Clicked in the crafting grid, no special handling required (just place them left to
            // right)
            clickedItem = player.getInventory().tryToFillSlots(clickedItem, 9, 36, 0, 9);
        }
        view.setItem(clickedSlot, clickedItem);
    }

    @Override
    public boolean itemPlaceAllowed(int slot, ItemStack stack) {
        if (slot == FUEL_SLOT) {
            return ((GlowServer) ServerProvider.getServer())
                    .getFuelManager().isFuel(stack.getType())
                || stack.getType().equals(Material.BUCKET);
        }
        return super.itemPlaceAllowed(slot, stack);
    }
}
