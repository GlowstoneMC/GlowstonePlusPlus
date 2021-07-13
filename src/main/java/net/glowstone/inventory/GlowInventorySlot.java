package net.glowstone.inventory;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.glowstone.constants.ItemIds;
import net.glowstone.util.InventoryUtil;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GlowInventorySlot {

    private static final SlotType DEFAULT_TYPE = SlotType.CONTAINER;

    private ItemStack item;
    @Getter
    @Setter
    private SlotType type;
    @Getter
    @Setter
    private EquipmentSlot equipmentSlot;

    public GlowInventorySlot() {
        this(DEFAULT_TYPE);
    }

    public GlowInventorySlot(SlotType type) {
        this(InventoryUtil.createEmptyStack(), type);
    }

    public GlowInventorySlot(ItemStack item) {
        this(item, DEFAULT_TYPE);
    }

    public GlowInventorySlot(ItemStack item, SlotType type) {
        this.item = item;
        this.type = type;
    }

    /**
     * Returns an array of new container-type slots.
     *
     * @param len the length of the array
     * @return an array, each of whose elements is a new {@link SlotType#CONTAINER}
     * {@link GlowInventorySlot}
     */
    public static GlowInventorySlot[] createArray(int len) {
        GlowInventorySlot[] result = new GlowInventorySlot[len];

        for (int i = 0; i < len; i++) {
            result[i] = new GlowInventorySlot();
        }

        return result;
    }

    public static List<GlowInventorySlot> createList(int len) {
        return Arrays.asList(createArray(len));
    }


    public @NotNull ItemStack getItem() {
        return InventoryUtil.itemOrEmpty(item);
    }

    public void setItem(ItemStack item) {
        this.item = ItemIds.sanitize(item);
    }
}
