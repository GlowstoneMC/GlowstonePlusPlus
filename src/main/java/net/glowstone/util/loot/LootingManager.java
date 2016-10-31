package net.glowstone.util.loot;

import net.glowstone.GlowServer;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class LootingManager {

    private static final Map<EntityType, EntityLootTable> entities = new HashMap<>();

    public static void load() throws Exception {
        String baseDir = "builtin/loot/entities/";
        register(EntityType.BAT, baseDir + "bat.json");
        register(EntityType.BLAZE, baseDir + "blaze.json");
        register(EntityType.CAVE_SPIDER, baseDir + "cave_spider.json");
        register(EntityType.CHICKEN, baseDir + "chicken.json");
        register(EntityType.COW, baseDir + "cow.json");
        register(EntityType.CREEPER, baseDir + "creeper.json");
        register(EntityType.ENDER_DRAGON, baseDir + "ender_dragon.json");
        register(EntityType.ENDERMAN, baseDir + "enderman.json");
        register(EntityType.ENDERMITE, baseDir + "endermite.json");
        register(EntityType.GHAST, baseDir + "ghast.json");
        register(EntityType.GUARDIAN, baseDir + "guardian.json");
        register(EntityType.HORSE, baseDir + "horse.json");
        register(EntityType.IRON_GOLEM, baseDir + "iron_golem.json");
        register(EntityType.MAGMA_CUBE, baseDir + "magma_cube.json");
        register(EntityType.MUSHROOM_COW, baseDir + "mushroom_cow.json");
        register(EntityType.OCELOT, baseDir + "ocelot.json");
        register(EntityType.PIG, baseDir + "pig.json");
        register(EntityType.PIG_ZOMBIE, baseDir + "pig_zombie.json");
        register(EntityType.POLAR_BEAR, baseDir + "polar_bear.json");
        register(EntityType.RABBIT, baseDir + "rabbit.json");
        register(EntityType.SHEEP, baseDir + "sheep.json");
        register(EntityType.SHULKER, baseDir + "shulker.json");
        register(EntityType.SILVERFISH, baseDir + "silverfish.json");
        register(EntityType.SKELETON, baseDir + "skeleton.json");
        register(EntityType.SLIME, baseDir + "slime.json");
        register(EntityType.SNOWMAN, baseDir + "snowman.json");
        register(EntityType.SPIDER, baseDir + "spider.json");
        register(EntityType.SQUID, baseDir + "squid.json");
        register(EntityType.VILLAGER, baseDir + "villager.json");
        register(EntityType.WITCH, baseDir + "witch.json");
        register(EntityType.WITHER, baseDir + "wither.json");
        register(EntityType.WOLF, baseDir + "wolf.json");
        register(EntityType.ZOMBIE, baseDir + "zombie.json");
    }

    private static void register(EntityType type, String location) throws Exception {
        InputStream in = LootingManager.class.getClassLoader().getResourceAsStream(location);
        if (in == null) {
            GlowServer.logger.warning("Could not find default entity loot table '" + location + "' on classpath");
            return;
        }
        JSONObject json = (JSONObject) new JSONParser().parse(new InputStreamReader(in));
        entities.put(type, new EntityLootTable(json));
    }

    public static LootData generate(LivingEntity entity) {
        Random random = ThreadLocalRandom.current();
        if (!entities.containsKey(entity.getType())) {
            return new LootData(new ItemStack[0], 0);
        }
        EntityLootTable table = entities.get(entity.getType());
        ArrayList<ItemStack> items = new ArrayList<>();
        for (LootItem lootItem : table.getItems()) {
            DefaultLootItem defaultItem = lootItem.getDefaultItem();
            int count = defaultItem.getCount().generate(random);
            int data = 0;
            if (defaultItem.getData().isPresent()) {
                data = defaultItem.getData().get().generate(random);
            } else if (defaultItem.getReflectiveData().isPresent()) {
                data = ((Number) defaultItem.getReflectiveData().get().process(entity)).intValue();
            }
            String name = defaultItem.getType().generate(random);
            if (name == null) {
                name = "";
            }
            name = name.toUpperCase();

            ConditionalLootItem[] conditions = lootItem.getConditionalItems();
            for (ConditionalLootItem condition : conditions) {
                if (LootingUtil.conditionValue(entity, condition.getCondition())) {
                    if (condition.getCount().isPresent()) {
                        count = condition.getCount().get().generate(random);
                    }
                    if (condition.getType().isPresent()) {
                        name = condition.getType().get().generate(random);
                        if (name == null) {
                            name = "";
                        }
                        name = name.toUpperCase();
                    }
                    if (condition.getData().isPresent()) {
                        data = condition.getData().get().generate(random);
                    } else if (condition.getReflectiveData().isPresent()) {
                        data = ((Number) condition.getReflectiveData().get().process(entity)).intValue();
                    }
                }
            }
            Material material = Material.getMaterial(name);
            if (material != null && count > 0) {
                items.add(new ItemStack(material, count, (byte) data));
            } else {
                GlowServer.logger.info("Not valid: " + material + "(" + name + ")" + "x" + count + " : " + data);
            }
        }
        int experience = table.getExperience().generate(random);
        return new LootData(items.toArray(new ItemStack[items.size()]), experience);
    }
}
