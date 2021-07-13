package net.glowstone.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import net.glowstone.EventFactory;
import net.glowstone.constants.GlowEnchantment;
import net.glowstone.entity.GlowPlayer;
import net.glowstone.i18n.ConsoleMessages;
import net.glowstone.util.WeightedRandom;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.InventoryView.Property;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

public class EnchantmentManager {

    private static final MaterialMatcher[] ENCHANTABLE_TOOLS = new MaterialMatcher[]{ToolType.AXE,
        ToolType.PICKAXE, ToolType.SHOVEL};

    private final Random random = new Random();
    private final GlowPlayer player;
    private final GlowEnchantingInventory inventory;
    private final int[] enchLevelCosts = new int[3];
    private final int[] enchId = new int[3];
    private final int[] enchLevel = new int[3];
    private int xpSeed;

    /**
     * Creates an instance to manage the given enchanting table for the given player.
     *
     * @param inventory the enchanting table
     * @param player the user
     */
    public EnchantmentManager(GlowEnchantingInventory inventory, GlowPlayer player) {
        this.player = player;
        this.inventory = inventory;
        xpSeed = player.getXpSeed();
    }

    ////////////////////////////
    // Public functions

    private static int calculateRandomizedModifier(Random random, ItemStack itemStack, int cost) {
        int modifier = calculateModifier(itemStack);
        if (modifier <= 0) {
            return -1;
        }

        modifier /= 4;
        modifier += 1;
        modifier = random.nextInt(modifier) + random.nextInt(modifier);
        modifier += 1 + cost;

        float randomValue = 1 + (random.nextFloat() + random.nextFloat() - 1.0F) * 0.15F;
        modifier = Math.round(modifier * randomValue);
        modifier = Math.max(1, modifier);

        return modifier;
    }

    private static int calculateModifier(ItemStack item) {
        //TODO: replace this by a better system?
        Material type = item.getType();

        switch (type) {
            case BOOK:
            case BOW:
            case FISHING_ROD:
                return 1;
            default:
                if (ClothType.CHAINMAIL.matches(type)) {
                    return 12;
                } else if (ClothType.IRON.matches(type)) {
                    return 9;
                } else if (ClothType.DIAMOND.matches(type)) {
                    return 10;
                } else if (ClothType.LEATHER.matches(type)) {
                    return 15;
                } else if (ClothType.GOLD.matches(type)) {
                    return 25;
                } else if (MaterialToolType.WOOD.matches(type)) {
                    return 15;
                } else if (MaterialToolType.STONE.matches(type)) {
                    return 5;
                } else if (MaterialToolType.DIAMOND.matches(type)) {
                    return 10;
                } else if (MaterialToolType.IRON.matches(type)) {
                    return 14;
                } else if (MaterialToolType.GOLD.matches(type)) {
                    return 22;
                }

                return 0;
        }
    }

    /////////////////////////////
    // Enchantments calculating

    private static boolean canEnchant(ItemStack item) {
        Material type = item.getType();

        switch (type) {
            case ENCHANTED_BOOK:
                return false;
            case BOOK:
                return item.getAmount() == 1;
            case FISHING_ROD:
            case BOW:
                return item.getEnchantments().isEmpty();
            default:
                return (isEnchantableTool(type) || isCloth(type) || ToolType.SWORD.matches(type))
                    && item.getEnchantments().isEmpty();

        }
    }

    private static boolean isCloth(Material type) {
        for (MaterialMatcher mm : ClothType.values()) {
            if (mm.matches(type)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isEnchantableTool(Material type) {
        for (MaterialMatcher mm : ENCHANTABLE_TOOLS) {
            if (mm.matches(type)) {
                return true;
            }
        }

        return false;
    }

    //////////////////////////////////
    // Modifier calculation

    private static Map<Enchantment, Integer> toMap(List<LeveledEnchant> list) {
        Map<Enchantment, Integer> map = new HashMap<>(list.size());
        for (LeveledEnchant enchant : list) {
            map.put(enchant.getEnchantment(), enchant.getEnchantmentLevel());
        }
        return map;
    }

    private static List<LeveledEnchant> getAllPossibleEnchants(ItemStack item, int modifier,
        int cost) {
        List<LeveledEnchant> enchantments = new ArrayList<>();

        boolean isBook = item.getType() == Material.BOOK;

        for (Enchantment enchantment : Enchantment.values()) {
            if (isBook || enchantment.canEnchantItem(item)) {
                for (int level = enchantment.getStartLevel(); level <= enchantment.getMaxLevel();
                    level++) {
                    if (((GlowEnchantment) enchantment).isInRange(level, modifier)) {
                        enchantments.add(new LeveledEnchant(enchantment, level, cost));
                    }
                }
            }
        }

        return enchantments;
    }

    /////////////////////////////////////
    // Internal stuff / helper functions

    private static void removeConflicting(List<LeveledEnchant> enchants,
        List<LeveledEnchant> toReduce) {
        Iterator<LeveledEnchant> it = toReduce.iterator();

        while (it.hasNext()) {
            Enchantment currentEnchantment = it.next().getEnchantment();

            boolean conflicts = false;
            for (LeveledEnchant entry : enchants) {
                if (entry.getEnchantment().conflictsWith(currentEnchantment)) {
                    conflicts = true;
                    break;
                }
            }
            if (conflicts) {
                it.remove();
            }
        }
    }

    /**
     * Resets the enchantments.
     */
    public void invalidate() {
        ItemStack item = inventory.getItem();
        ItemStack resource = inventory.getSecondary();

        if (item == null || !canEnchant(item) || player.getGameMode() != GameMode.CREATIVE && (
            resource == null || resource.getType() != Material.INK_SAC
                || resource.getDurability() != 4)) {
            clearEnch();
        } else {
            calculateNewEnchantsAndLevels();
        }
    }

    /**
     * Handles a click on an enchantment button.
     *
     * @param clicked The button that was clicked
     */
    public void onPlayerEnchant(int clicked) {
        if (enchLevelCosts[clicked] <= 0 || isMaliciousClicked(clicked)) {
            return;
        }

        ItemStack item = inventory.getItem();

        List<LeveledEnchant> enchants = calculateCurrentEnchants(item, clicked,
            enchLevelCosts[clicked]);
        if (enchants == null) {
            enchants = new ArrayList<>();
        }

        EnchantItemEvent event = EventFactory.getInstance().callEvent(
            new EnchantItemEvent(player, player.getOpenInventory(),
                inventory.getLocation().getBlock(), item.clone(), enchLevelCosts[clicked],
                toMap(enchants), clicked));
        if (event.isCancelled()
            || player.getGameMode() != GameMode.CREATIVE && event.getExpLevelCost() > player
            .getLevel()) {
            return;
        }

        boolean isBook = item.getType() == Material.BOOK;

        if (isBook) {
            item.setType(Material.ENCHANTED_BOOK);
        }

        Map<Enchantment, Integer> toAdd = event.getEnchantsToAdd();
        if (toAdd == null || toAdd.isEmpty()) {
            return;
        }

        for (Entry<Enchantment, Integer> enchantment : toAdd.entrySet()) {
            try {
                if (isBook) {
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                    meta.addStoredEnchant(enchantment.getKey(), enchantment.getValue(),
                        true); //TODO is true correct here?
                    item.setItemMeta(meta);
                } else {
                    item.addUnsafeEnchantment(enchantment.getKey(), enchantment.getValue());
                }
            } catch (IllegalArgumentException e) {
                //ignore, since plugins are allowed to add enchantments that can't be applied
            }
        }

        player.enchanted(clicked);

        if (player.getGameMode() != GameMode.CREATIVE) {
            ItemStack res = inventory.getSecondary();
            res.setAmount(res.getAmount() - clicked + 1);
            if (res.getAmount() <= 0) {
                inventory.setSecondary(null);
            }
        }

        xpSeed = player.getXpSeed();

        update();
    }

    private void calculateNewEnchantsAndLevels() {
        random.setSeed(xpSeed);

        int realBookshelfs = inventory.getBookshelfCount();
        int countBookshelf = Math.min(15, realBookshelfs);

        for (int i = 0; i < enchLevelCosts.length; i++) {
            enchLevelCosts[i] = calculateLevelCost(i, countBookshelf);
            enchId[i] = -1;
            enchLevel[i] = -1;
        }

        ItemStack item = inventory.getItem();

        List<LeveledEnchant> enchants = null;
        for (int i = 0; i < enchLevelCosts.length; i++) {
            if (enchLevelCosts[i] == 0) {
                continue;
            }
            enchants = calculateCurrentEnchants(item, i, enchLevelCosts[i]);
            if (enchants != null && !enchants.isEmpty()) {
                LeveledEnchant chosen = WeightedRandom.getRandom(random, enchants);
                // TODO: rework enchantment associations to be friendly with new API and namespaced keys
                enchId[i] = ((GlowEnchantment) GlowEnchantment.getByVanillaId(chosen.getEnchantment().getKey().toString())).getId();
                enchLevel[i] = chosen.getEnchantmentLevel();
            }
        }
        EnchantmentOffer[] offers = null;

        if (enchants != null) {
            offers = new EnchantmentOffer[enchants.size()];
            enchants.toArray(offers);
        }

        PrepareItemEnchantEvent event = new PrepareItemEnchantEvent(player,
            player.getOpenInventory(), inventory.getLocation().getBlock(), item, offers,
            realBookshelfs);
        event.setCancelled(!canEnchant(item));
        EventFactory.getInstance().callEvent(event);
        if (event.isCancelled()) {
            Arrays.fill(enchLevelCosts, 0);
        }

        update();
    }

    private List<LeveledEnchant> calculateCurrentEnchants(ItemStack item, int level, int cost) {
        random.setSeed(xpSeed + level);
        int modifier = calculateRandomizedModifier(random, item, cost);
        if (modifier <= 0) {
            return null;
        }

        List<LeveledEnchant> possibleEnchants = getAllPossibleEnchants(item, modifier, cost);
        if (possibleEnchants == null || possibleEnchants.isEmpty()) {
            return null;
        }

        LeveledEnchant chosen = WeightedRandom.getRandom(random, possibleEnchants);
        if (chosen == null) {
            return null;
        }

        List<LeveledEnchant> enchants = new ArrayList<>();
        enchants.add(chosen);

        while (random.nextInt(50) <= modifier) {
            removeConflicting(enchants, possibleEnchants);

            if (!possibleEnchants.isEmpty()) {
                enchants.add(WeightedRandom.getRandom(random, possibleEnchants));
            }
            modifier /= 2;
        }

        if (item.getType() == Material.BOOK && enchants.size() > 1) {
            enchants.remove(random.nextInt(enchants.size()));
        }

        return enchants;
    }

    private int calculateLevelCost(int stage, int countBookshelf) {
        int modifier = calculateModifier(inventory.getItem());
        if (modifier <= 0) {
            return 0;
        }

        int rand = random.nextInt(8) + random.nextInt(countBookshelf + 1);
        rand += 1;
        rand += countBookshelf / 2;

        int result;
        if (stage == 0) {
            result = Math.max(rand / 3, 1);
        } else if (stage == 1) {
            result = (rand << 1) / 3 + 1;
        } else {
            result = Math.max(rand, countBookshelf << 1);
        }

        if (result < stage + 1) {
            return 0;
        } else {
            return result;
        }
    }

    private void clearEnch() {
        for (int i = 0; i < 3; i++) {
            enchLevelCosts[i] = 0;
            enchId[i] = -1;
            enchLevel[i] = -1;
        }

        update();
    }

    private void update() {
        player.setWindowProperty(Property.ENCHANT_BUTTON1, enchLevelCosts[0]);
        player.setWindowProperty(Property.ENCHANT_BUTTON2, enchLevelCosts[1]);
        player.setWindowProperty(Property.ENCHANT_BUTTON3, enchLevelCosts[2]);
        player.setWindowProperty(Property.ENCHANT_XP_SEED, xpSeed & -16);
        player.setWindowProperty(Property.ENCHANT_ID1, enchId[0]);
        player.setWindowProperty(Property.ENCHANT_ID2, enchId[1]);
        player.setWindowProperty(Property.ENCHANT_ID3, enchId[2]);
    }

    private boolean isMaliciousClicked(int clicked) {
        //TODO: better handling of for malicious clients?

        if (clicked < 0 || clicked > enchLevelCosts.length) {
            ConsoleMessages.Info.Enchant.BAD_SLOT.log(clicked);
            update();
            return true;
        }

        int level = enchLevelCosts[clicked];
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (player.getLevel() < level || inventory.getSecondary() == null
                || inventory.getSecondary().getAmount() < clicked) {
                ConsoleMessages.Info.Enchant.MISSING_RESOURCES.log();
                update();
                return true;
            }
        }

        return false;
    }
}
