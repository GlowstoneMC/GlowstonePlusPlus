package net.glowstone.io.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import net.glowstone.entity.AttributeManager;
import net.glowstone.entity.AttributeManager.Property;
import net.glowstone.entity.GlowLivingEntity;
import net.glowstone.entity.objects.GlowLeashHitch;
import net.glowstone.io.nbt.NbtSerialization;
import net.glowstone.util.InventoryUtil;
import net.glowstone.util.nbt.CompoundTag;
import org.bukkit.Location;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public abstract class LivingEntityStore<T extends GlowLivingEntity> extends EntityStore<T> {

    public LivingEntityStore(Class<T> clazz, String type) {
        super(clazz, type);
    }

    public LivingEntityStore(Class<T> clazz, EntityType type) {
        super(clazz, type);
    }

    // these tags that apply to living entities only are documented as global:
    // - short "Air"
    // - string "CustomName"
    // - bool "CustomNameVisible"

    // todo: the following tags
    // - float "AbsorptionAmount"
    // - short "HurtTime"
    // - int "HurtByTimestamp"
    // - short "DeathTime"
    // - bool "PersistenceRequired"
    // on ActiveEffects, bool "ShowParticles"

    @Override
    public void load(T entity, CompoundTag compound) {
        super.load(entity, compound);
        compound.readShort("Air", entity::setRemainingAir);
        compound.readString("CustomName", entity::setCustomName);
        compound.readBoolean("CustomNameVisible", entity::setCustomNameVisible);
        if (!compound.readFloat("HealF", entity::setHealth)) {
            compound.readShort("Health", entity::setHealth);
        }
        compound.readShort("AttackTime", entity::setNoDamageTicks);
        compound.readBoolean("FallFlying", entity::setFallFlying);
        compound.iterateCompoundList("ActiveEffects", effect -> {
            // should really always have every field, but be forgiving if possible
            if (!effect.isByte("Id") || !effect.isInt("Duration")) {
                return;
            }

            PotionEffectType type = PotionEffectType.getById(effect.getByte("Id"));
            int duration = effect.getInt("Duration");
            if (type == null || duration < 0) {
                return;
            }
            final int amplifier = compound.tryGetInt("Amplifier").orElse(0);
            boolean ambient = compound.getBoolean("Ambient", false);
            // bool "ShowParticles"

            entity.addPotionEffect(new PotionEffect(type, duration, amplifier, ambient), true);
        });

        EntityEquipment equip = entity.getEquipment();
        if (equip != null) {
            loadEquipment(entity, equip, compound);
        }
        compound.readBoolean("CanPickUpLoot", entity::setCanPickupItems);
        AttributeManager am = entity.getAttributeManager();
        compound.iterateCompoundList("Attributes", tag -> {
            if (!tag.isString("Name") || !tag.isDouble("Base")) {
                return;
            }
            List<AttributeModifier> modifiers = new ArrayList<>();
            tag.iterateCompoundList("Modifiers", modifierTag -> {
                if (modifierTag.isDouble("Amount")
                    && modifierTag.isString("Name")
                    && modifierTag.isInt("Operation")
                    && modifierTag.isLong("UUIDLeast")
                    && modifierTag.isLong("UUIDMost")) {
                    modifiers.add(new AttributeModifier(
                        new UUID(modifierTag.getLong("UUIDLeast"),
                            modifierTag.getLong("UUIDMost")),
                        modifierTag.getString("Name"),
                        modifierTag.getDouble("Amount"),
                        AttributeModifier.Operation.values()[modifierTag.getInt("Operation")]));
                }
            });
            AttributeManager.Key key = AttributeManager.Key.fromName(tag.getString("Name"));
            am.setProperty(key, tag.getDouble("Base"), modifiers);
        });
        Optional<CompoundTag> maybeLeash = compound.tryGetCompound("Leash");
        if (maybeLeash.isPresent()) {
            CompoundTag leash = maybeLeash.get();
            if (!leash.readUniqueId("UUIDMost", "UUIDLeast", entity::setLeashHolderUniqueId)
                && leash.isInt("X") && leash.isInt("Y") && leash.isInt("Z")) {
                int x = leash.getInt("X");
                int y = leash.getInt("Y");
                int z = leash.getInt("Z");

                LeashHitch leashHitch = GlowLeashHitch
                    .getLeashHitchAt(new Location(entity.getWorld(), x, y, z).getBlock());
                entity.setLeashHolder(leashHitch);
            }
        } else {
            compound.readBoolean("Leashed", leashSet -> {
                if (leashSet) {
                    // We know that there was something leashed, but not what entity it was
                    // This can happen, when for example Minecart got leashed
                    // We still have to make sure that we drop a Leash Item
                    entity.setLeashHolderUniqueId(UUID.randomUUID());
                }
            });
        }
    }

    private void loadEquipment(T entity, EntityEquipment equip, CompoundTag compound) {
        // Deprecated since 15w31a, left here for compatibilty for now
        compound.readCompoundList("Equipment", list -> {
            equip.setItemInMainHand(getItem(list, 0));
            equip.setBoots(getItem(list, 1));
            equip.setLeggings(getItem(list, 2));
            equip.setChestplate(getItem(list, 3));
            equip.setHelmet(getItem(list, 4));
        });
        // Deprecated since 15w31a, left here for compatibilty for now
        compound.readFloatList("DropChances", list -> {
            equip.setItemInMainHandDropChance(getOrDefault(list, 0, 1f));
            equip.setBootsDropChance(getOrDefault(list, 1, 1f));
            equip.setLeggingsDropChance(getOrDefault(list, 2, 1f));
            equip.setChestplateDropChance(getOrDefault(list, 3, 1f));
            equip.setHelmetDropChance(getOrDefault(list, 4, 1f));
        });
        compound.readCompoundList("HandItems", list -> {
            equip.setItemInMainHand(getItem(list, 0));
            equip.setItemInOffHand(getItem(list, 1));
        });
        compound.readCompoundList("ArmorItems", list -> {
            equip.setBoots(getItem(list, 0));
            equip.setLeggings(getItem(list, 1));
            equip.setChestplate(getItem(list, 2));
            equip.setHelmet(getItem(list, 3));
        });

        // set of dropchances on a player throws an UnsupportedOperationException
        if (!(entity instanceof Player)) {
            compound.readFloatList("HandDropChances", list -> {
                equip.setItemInMainHandDropChance(getOrDefault(list, 0, 1f));
                equip.setItemInOffHandDropChance(getOrDefault(list, 1, 1f));
            });
            compound.readFloatList("ArmorDropChances", list -> {
                equip.setBootsDropChance(getOrDefault(list, 0, 1f));
                equip.setLeggingsDropChance(getOrDefault(list, 1, 1f));
                equip.setChestplateDropChance(getOrDefault(list, 2, 1f));
                equip.setHelmetDropChance(getOrDefault(list, 3, 1f));
            });
        }
    }

    private ItemStack getItem(List<CompoundTag> list, int index) {
        if (list == null) {
            return InventoryUtil.createEmptyStack();
        }

        if (index >= list.size()) {
            return InventoryUtil.createEmptyStack();
        }

        return NbtSerialization.readItem(list.get(index));
    }

    private float getOrDefault(List<Float> list, int index, float defaultValue) {
        if (list == null) {
            return defaultValue;
        }

        if (index >= list.size()) {
            return defaultValue;
        }

        return list.get(index);
    }

    @Override
    public void save(T entity, CompoundTag tag) {
        super.save(entity, tag);

        tag.putShort("Air", entity.getRemainingAir());
        if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) {
            tag.putString("CustomName", entity.getCustomName());
            tag.putBool("CustomNameVisible", entity.isCustomNameVisible());
        }

        tag.putFloat("HealF", entity.getHealth());
        tag.putShort("Health", (int) entity.getHealth());
        tag.putShort("AttackTime", entity.getNoDamageTicks());
        tag.putBool("FallFlying", entity.isFallFlying());

        Map<String, Property> properties = entity.getAttributeManager().getAllProperties();
        if (!properties.isEmpty()) {
            List<CompoundTag> attributes = new ArrayList<>(properties.size());

            properties.forEach((key, property) -> {
                CompoundTag attribute = new CompoundTag();
                attribute.putString("Name", key);
                attribute.putDouble("Base", property.getValue());

                Collection<AttributeModifier> modifiers = property.getModifiers();
                if (modifiers != null && !modifiers.isEmpty()) {
                    List<CompoundTag> modifierTags = modifiers.stream().map(modifier -> {
                        CompoundTag modifierTag = new CompoundTag();
                        modifierTag.putDouble("Amount", modifier.getAmount());
                        modifierTag.putString("Name", modifier.getName());
                        modifierTag.putInt("Operation", modifier.getOperation().ordinal());
                        UUID uuid = modifier.getUniqueId();
                        modifierTag.putLong("UUIDLeast", uuid.getLeastSignificantBits());
                        modifierTag.putLong("UUIDMost", uuid.getMostSignificantBits());
                        return modifierTag;
                    }).collect(Collectors.toList());
                    attribute.putCompoundList("Modifiers", modifierTags);
                }

                attributes.add(attribute);
            });

            tag.putCompoundList("Attributes", attributes);
        }

        List<CompoundTag> effects = new LinkedList<>();
        for (PotionEffect effect : entity.getActivePotionEffects()) {
            CompoundTag effectTag = new CompoundTag();
            effectTag.putByte("Id", effect.getType().getId());
            effectTag.putByte("Amplifier", effect.getAmplifier());
            effectTag.putInt("Duration", effect.getDuration());
            effectTag.putBool("Ambient", effect.isAmbient());
            effectTag.putBool("ShowParticles", true);
            effects.add(effectTag);
        }
        tag.putCompoundList("ActiveEffects", effects);

        EntityEquipment equip = entity.getEquipment();
        if (equip != null) {
            tag.putCompoundList("HandItems", Arrays.asList(
                NbtSerialization.writeItem(equip.getItemInMainHand(), -1),
                NbtSerialization.writeItem(equip.getItemInOffHand(), -1)
            ));
            tag.putCompoundList("ArmorItems", Arrays.asList(
                NbtSerialization.writeItem(equip.getBoots(), -1),
                NbtSerialization.writeItem(equip.getLeggings(), -1),
                NbtSerialization.writeItem(equip.getChestplate(), -1),
                NbtSerialization.writeItem(equip.getHelmet(), -1)
            ));

            tag.putFloatList("HandDropChances", Arrays.asList(
                equip.getItemInMainHandDropChance(),
                equip.getItemInOffHandDropChance()
            ));
            tag.putFloatList("ArmorDropChances", Arrays.asList(
                equip.getBootsDropChance(),
                equip.getLeggingsDropChance(),
                equip.getChestplateDropChance(),
                equip.getHelmetDropChance()
            ));
        }
        tag.putBool("CanPickUpLoot", entity.getCanPickupItems());

        tag.putBool("Leashed", entity.isLeashed());

        if (entity.isLeashed()) {
            Entity leashHolder = entity.getLeashHolder();
            CompoundTag leash = new CompoundTag();

            // "Non-living entities excluding leashes will not persist as leash holders."
            // The empty Leash tag is still persisted tough
            if (leashHolder instanceof LeashHitch) {
                Location location = leashHolder.getLocation();

                leash.putInt("X", location.getBlockX());
                leash.putInt("Y", location.getBlockY());
                leash.putInt("Z", location.getBlockZ());
            } else if (leashHolder instanceof LivingEntity) {
                leash.putLong("UUIDMost", entity.getUniqueId().getMostSignificantBits());
                leash.putLong("UUIDLeast", entity.getUniqueId().getLeastSignificantBits());
            }
            tag.putCompound("Leash", leash);
        }
    }
}
