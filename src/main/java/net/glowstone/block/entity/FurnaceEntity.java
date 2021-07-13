package net.glowstone.block.entity;

import lombok.Getter;
import lombok.Setter;
import net.glowstone.EventFactory;
import net.glowstone.GlowServer;
import net.glowstone.ServerProvider;
import net.glowstone.block.GlowBlock;
import net.glowstone.block.GlowBlockState;
import net.glowstone.block.entity.state.GlowFurnace;
import net.glowstone.datapack.FuelManager;
import net.glowstone.datapack.RecipeManager;
import net.glowstone.entity.GlowPlayer;
import net.glowstone.inventory.GlowFurnaceInventory;
import net.glowstone.util.InventoryUtil;
import net.glowstone.util.nbt.CompoundTag;
import org.bukkit.Material;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.InventoryView.Property;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class FurnaceEntity extends ContainerEntity {

    @Getter
    @Setter
    private short burnTime;
    @Getter
    @Setter
    private short cookTime;
    private short burnTimeFuel;

    public FurnaceEntity(GlowBlock block) {
        super(block, new GlowFurnaceInventory(new GlowFurnace(block, (short) 0, (short) 0)));
        setSaveId("minecraft:furnace");
    }

    @Override
    public GlowBlockState getState() {
        return new GlowFurnace(block);
    }

    @Override
    public void update(GlowPlayer player) {
        super.update(player);
        // TODO: 1.13 furnace burning block data
        player.sendBlockChange(getBlock().getLocation(),
            getBurnTime() > 0 ? Material.LEGACY_BURNING_FURNACE : Material.FURNACE,
            getBlock().getData());
    }

    @Override
    public void saveNbt(CompoundTag tag) {
        super.saveNbt(tag);
        tag.putShort("BurnTime", burnTime);
        tag.putShort("CookTime", cookTime);
    }

    @Override
    public void loadNbt(CompoundTag tag) {
        super.loadNbt(tag);
        tag.readShort("BurnTime", this::setBurnTime);
        tag.readShort("CookTime", this::setCookTime);
    }

    /**
     * Advances the cooking process for the tick.
     */
    // TODO: Change block on burning
    public void burn() {
        GlowFurnaceInventory inv = (GlowFurnaceInventory) getInventory();
        boolean sendChange = false;
        if (burnTime > 0) {
            burnTime--;
            sendChange = true;
        }
        boolean isBurnable = isBurnable();
        if (cookTime > 0 && isBurnable) {
            cookTime++;
            sendChange = true;
        } else if (burnTime != 0) {
            cookTime = 0;
            sendChange = true;
        }

        if (cookTime == 0 && isBurnable) {
            cookTime = 1;
            sendChange = true;
        }

        if (burnTime == 0) {
            if (isBurnable) {
                FuelManager fm = ((GlowServer) ServerProvider.getServer()).getFuelManager();
                FurnaceBurnEvent burnEvent = new FurnaceBurnEvent(block, inv.getFuel(),
                    fm.getFuelTime(inv.getFuel().getType()));
                EventFactory.getInstance().callEvent(burnEvent);
                if (!burnEvent.isCancelled() && burnEvent.isBurning()) {
                    burnTime = (short) burnEvent.getBurnTime();
                    burnTimeFuel = burnTime;
                    if (inv.getFuel().getAmount() == 1) {
                        if (inv.getFuel().getType().equals(Material.LAVA_BUCKET)) {
                            inv.setFuel(new ItemStack(Material.BUCKET));
                        } else {
                            inv.setFuel(null);
                        }
                    } else {
                        inv.getFuel().setAmount(inv.getFuel().getAmount() - 1);
                    }
                    sendChange = true;
                } else if (cookTime != 0) {
                    if (cookTime % 2 == 0) {
                        cookTime = (short) (cookTime - 2);
                    } else {
                        cookTime--;
                    }
                    sendChange = true;
                }
            } else if (cookTime != 0) {
                if (cookTime % 2 == 0) {
                    cookTime = (short) (cookTime - 2);
                } else {
                    cookTime--;
                }
                sendChange = true;
            }
        }

        if (cookTime == 200) {
            RecipeManager rm = ((GlowServer) ServerProvider.getServer()).getRecipeManager();
            Recipe recipe = rm.getRecipe(inv);
            if (recipe != null) {
                FurnaceSmeltEvent smeltEvent = new FurnaceSmeltEvent(block, inv.getSmelting(),
                    recipe.getResult());
                EventFactory.getInstance().callEvent(smeltEvent);
                if (!smeltEvent.isCancelled()) {
                    if (inv.getSmelting().getType().equals(Material.SPONGE)
                        && inv.getSmelting().getData().getData() == 1 && inv.getFuel() != null
                        && inv.getFuel().getType().equals(Material.BUCKET)
                        && inv.getFuel().getAmount() == 1) {
                        inv.setFuel(new ItemStack(Material.WATER_BUCKET));
                    }
                    if (inv.getResult() == null || inv.getResult().getType().equals(Material.AIR)) {
                        inv.setResult(smeltEvent.getResult());
                    } else if (inv.getResult().getType().equals(smeltEvent.getResult().getType())) {
                        inv.getResult().setAmount(
                            inv.getResult().getAmount() + smeltEvent.getResult().getAmount());
                    }
                    if (inv.getSmelting().getAmount() == 1) {
                        inv.setSmelting(null);
                    } else {
                        inv.getSmelting().setAmount(inv.getSmelting().getAmount() - 1);
                    }
                }
                cookTime = 0;
                sendChange = true;
            }
        }
        inv.getViewersSet().forEach(human -> {
            human.setWindowProperty(Property.BURN_TIME, burnTime);
            human.setWindowProperty(Property.TICKS_FOR_CURRENT_FUEL, burnTimeFuel);
            human.setWindowProperty(Property.COOK_TIME, cookTime);
            human.setWindowProperty(Property.TICKS_FOR_CURRENT_SMELTING, 200);
        });
        if (!isBurnable && burnTime == 0 && cookTime == 0) {
            getState().getBlock().getWorld().cancelPulse(getState().getBlock());
            sendChange = true;
        }
        if (sendChange) {
            updateInRange();
        }
    }

    private boolean isBurnable() {
        GlowFurnaceInventory inv = (GlowFurnaceInventory) getInventory();
        if ((burnTime != 0 || !InventoryUtil.isEmpty(inv.getFuel())) && !InventoryUtil
            .isEmpty(inv.getSmelting())) {
            if ((InventoryUtil.isEmpty(inv.getFuel()) || InventoryUtil.isEmpty(inv.getSmelting()))
                && burnTime == 0) {
                return false;
            }
            RecipeManager rm = ((GlowServer) ServerProvider.getServer()).getRecipeManager();
            FuelManager fm = ((GlowServer) ServerProvider.getServer()).getFuelManager();
            if (burnTime != 0 || fm.isFuel(inv.getFuel().getType())) {
                Recipe recipe = rm.getRecipe(inv);
                if (recipe != null && (InventoryUtil.isEmpty(inv.getResult())
                    || inv.getResult().getType().equals(recipe.getResult().getType())
                    && inv.getResult().getAmount() + recipe.getResult().getAmount() <= recipe
                    .getResult().getMaxStackSize())) {
                    return true;
                }
            }
        }
        return false;
    }
}
