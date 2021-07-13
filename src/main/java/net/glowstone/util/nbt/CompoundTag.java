package net.glowstone.util.nbt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;
import lombok.Getter;
import net.glowstone.GlowServer;
import net.glowstone.block.data.BlockDataManager;
import net.glowstone.constants.ItemIds;
import net.glowstone.io.nbt.NbtSerialization;
import net.glowstone.util.DynamicallyTypedMapWithDoubles;
import net.glowstone.util.FloatConsumer;
import net.glowstone.util.ShortConsumer;
import net.glowstone.util.UuidUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NonNls;

/**
 * The {@code TAG_Compound} tag.
 */
public class CompoundTag extends Tag<Map<String, Tag>>
    implements DynamicallyTypedMapWithDoubles<String> {

    /**
     * The value.
     */
    @Getter
    private final Map<String, Tag> value = new LinkedHashMap<>();

    /**
     * Creates a new, empty CompoundTag.
     */
    public CompoundTag() {
        super(TagType.COMPOUND);
    }

    public CompoundTag(Map<String, Tag> value) {
        this();
        this.value.putAll(value);
    }

    @Override
    protected void valueToString(StringBuilder builder) {
        builder.append(value.size()).append(" entries\n{\n"); // NON-NLS
        for (Entry<String, Tag> entry : value.entrySet()) {
            builder.append("    ").append(entry.getKey()).append(": ")
                .append(entry.getValue().toString().replaceAll("\n", "\n    ")).append("\n");
        }
        builder.append("}");
    }

    ////////////////////////////////////////////////////////////////////////////
    // Helper stuff

    public boolean isEmpty() {
        return value.isEmpty();
    }

    /**
     * Check if the compound contains the given key.
     *
     * @param key The key.
     * @return True if the key is in the map.
     */
    public boolean containsKey(@NonNls String key) {
        return value.containsKey(key);
    }

    public void remove(@NonNls String key) {
        value.remove(key);
    }

    /**
     * Checks to see if this tag is a strict, deep submap of the given CompoundTag.
     *
     * @param other The CompoundTag that should contain our values.
     */
    public boolean matches(CompoundTag other) {
        for (Entry<String, Tag> entry : value.entrySet()) {
            if (!other.value.containsKey(entry.getKey())) {
                return false;
            }
            Tag value = entry.getValue();
            Tag otherValue = other.value.get(entry.getKey());
            if ((value == null && otherValue != null) || (value != null && otherValue == null)) {
                return false;
            }
            if (value != null) {
                if (value.getClass() != otherValue.getClass()) {
                    return false;
                }
                if (value instanceof CompoundTag) {
                    if (!((CompoundTag) value).matches((CompoundTag) otherValue)) {
                        return false;
                    }
                } else if (value instanceof IntArrayTag) {
                    if (!Arrays.equals(((IntArrayTag) value).getValue(),
                        ((IntArrayTag) otherValue).getValue())) {
                        return false;
                    }
                } else if (value instanceof ByteArrayTag) {
                    if (!Arrays.equals(((ByteArrayTag) value).getValue(),
                        ((ByteArrayTag) otherValue).getValue())) {
                        return false;
                    }
                } else if (!value.equals(otherValue)) {
                    // Note: When Mojang actually starts using lists, revisit this.
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Merges the contents of this compound into the supplied compound.
     *
     * @param other     the other compound to merge into.
     * @param overwrite whether keys already set in the other compound should be
     *                  overwritten.
     */
    public void mergeInto(CompoundTag other, boolean overwrite) {
        for (String key : value.keySet()) {
            if (!overwrite && other.containsKey(key)) {
                continue;
            }
            other.put(key, value.get(key));
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Simple gets


    /**
     * Returns the value of a numeric subtag.
     *
     * @param key the key to look up
     * @return the numeric tag value
     */
    public Number getNumber(String key) {
        return (Number) get(key, NumericTag.class);
    }

    /**
     * Returns the value of a {@code byte} subtag.
     *
     * @param key the key to look up
     * @return the tag value
     */
    public byte getByte(@NonNls String key) {
        if (isInt(key)) {
            return (byte) getInt(key);
        }
        return get(key, ByteTag.class);
    }

    /**
     * Returns the value of a {@code short} subtag.
     *
     * @param key the key to look up
     * @return the tag value
     */
    public short getShort(@NonNls String key) {
        if (isInt(key)) {
            return (short) getInt(key);
        }
        return get(key, ShortTag.class);
    }

    /**
     * Returns the value of an {@code int} subtag.
     *
     * @param key the key to look up
     * @return the tag value
     */
    public int getInt(@NonNls String key) {
        if (isByte(key)) {
            return (int) getByte(key);
        } else if (isShort(key)) {
            return (int) getShort(key);
        } else if (isLong(key)) {
            return (int) getLong(key);
        }
        return get(key, IntTag.class);
    }

    @Override
    public boolean getBoolean(@NonNls String key) {
        return getNumber(key).byteValue() != 0;
    }


    /**
     * Returns the boolean value of a {@code byte} subtag if present, or a default otherwise.
     *
     * @param key          the key to look up
     * @param defaultValue the value to return if the subtag is missing
     * @return the tag value as a boolean, or defaultValue if it's not a byte
     */
    public boolean getBoolean(@NonNls String key, boolean defaultValue) {
        return isNumeric(key) ? getBoolean(key) : defaultValue;
    }

    /**
     * Returns the value of a {@code long} subtag.
     *
     * @param key the key to look up
     * @return the tag value
     */
    public long getLong(@NonNls String key) {
        if (isInt(key)) {
            return (long) getInt(key);
        }
        return get(key, LongTag.class);
    }

    /**
     * Returns the value of a {@code float} subtag.
     *
     * @param key the key to look up
     * @return the tag value
     */
    public float getFloat(@NonNls String key) {
        if (isDouble(key)) {
            return (float) getDouble(key);
        } else if (isInt(key)) {
            return (float) getInt(key);
        }
        return get(key, FloatTag.class);
    }

    /**
     * Returns the value of a {@code double} subtag.
     *
     * @param key the key to look up
     * @return the tag value
     */
    public double getDouble(@NonNls String key) {
        if (isFloat(key)) {
            return (double) getFloat(key);
        } else if (isInt(key)) {
            return (double) getInt(key);
        }
        return get(key, DoubleTag.class);
    }

    /**
     * Returns the value of a {@code byte[]} subtag.
     *
     * @param key the key to look up
     * @return the tag value
     */
    public byte[] getByteArray(@NonNls String key) {
        return get(key, ByteArrayTag.class);
    }

    /**
     * Returns the value of a {@link String} subtag.
     *
     * @param key the key to look up
     * @return the tag value
     */
    public String getString(@NonNls String key) {
        return get(key, StringTag.class);
    }

    /**
     * Returns the value of an {@code int[]} subtag.
     *
     * @param key the key to look up
     * @return the tag value
     */
    public int[] getIntArray(@NonNls String key) {
        return get(key, IntArrayTag.class);
    }

    /**
     * Returns the value of an {@code long[]} subtag.
     *
     * @param key the key to look up
     * @return the tag value
     */
    public long[] getLongArray(@NonNls String key) {
        return get(key, LongArrayTag.class);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Fancy gets

    /**
     * Returns the value of a {@link List} subtag.
     *
     * @param key  the key to look up
     * @param type the list element tag type
     * @param <V>  the list element type
     * @return the tag value
     */
    @SuppressWarnings("unchecked")
    public <V> List<V> getList(@NonNls String key, TagType type) {
        List<? extends Tag> original = getTagList(key, type);
        List<V> result = new ArrayList<>(original.size());
        if (type == TagType.COMPOUND) {
            result.addAll(
                original.stream().map(
                    item -> (V) new CompoundTag((Map<String, Tag>) item.getValue()))
                    .collect(Collectors.toList()));
        } else {
            result.addAll(
                original.stream().map(
                    item -> (V) item.getValue()).collect(Collectors.toList()));
        }

        return result;
    }

    /**
     * Returns the value of a compound subtag.
     *
     * @param key the key to look up
     * @return the tag value
     */
    public CompoundTag getCompound(@NonNls String key) {
        return getTag(key, CompoundTag.class);
    }

    /**
     * Returns the value of a compound subtag, if it exists. Multiple strings can be passed in to
     * retrieve a sub-subtag (e.g. {@code tryGetCompound("foo", "bar")} returns a compound subtag
     * called "bar" of a compound subtag called "foo", or null if either of those tags doesn't exist
     * or isn't compound.
     *
     * @param key the key to look up
     * @return the tag value, or an empty optional if the tag doesn't exist or isn't compound
     */
    public Optional<CompoundTag> tryGetCompound(@NonNls String key) {
        if (isCompound(key)) {
            return Optional.of(getCompound(key));
        }
        return Optional.empty();
    }

    /**
     * Applies the given function to a compound subtag if it is present. Multiple strings can be
     * passed in to operate on a sub-subtag, as with {@link #tryGetCompound(String)}.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readCompound(@NonNls String key, Consumer<? super CompoundTag> consumer) {
        Optional<CompoundTag> tag = tryGetCompound(key);
        tag.ifPresent(consumer);
        return tag.isPresent();
    }

    private <V, T extends Tag<V>> boolean readTag(@NonNls String key, Class<T> clazz,
                                                  Consumer<? super V> consumer) {
        if (is(key, clazz)) {
            consumer.accept(get(key, clazz));
            return true;
        }
        return false;
    }

    private <T> Optional<T> tryGetTag(@NonNls String key, Class<? extends Tag<T>> clazz) {
        return is(key, clazz) ? Optional.of(get(key, clazz)) : Optional.empty();
    }

    /**
     * Applies the given function to a float subtag if it is present.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readFloat(@NonNls String key, FloatConsumer consumer) {
        // Avoid boxing by not delegating to readTag
        if (isFloat(key)) {
            consumer.accept(getFloat(key));
            return true;
        }
        return false;
    }

    /**
     * Applies the given function to a double subtag if it is present.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readDouble(@NonNls String key, DoubleConsumer consumer) {
        // Avoid boxing by not delegating to readTag
        if (isDouble(key)) {
            consumer.accept(getDouble(key));
            return true;
        }
        return false;
    }

    /**
     * Applies the given function to an integer subtag if it is present.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readInt(@NonNls String key, IntConsumer consumer) {
        // Avoid boxing by not delegating to readTag
        if (isInt(key)) {
            consumer.accept(getInt(key));
            return true;
        }
        return false;
    }

    /**
     * Applies the given function to a byte array subtag if it is present.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readByteArray(@NonNls String key, Consumer<? super byte[]> consumer) {
        return readTag(key, ByteArrayTag.class, consumer);
    }

    /**
     * Applies the given function to an integer array subtag if it is present.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readIntArray(@NonNls String key, Consumer<? super int[]> consumer) {
        return readTag(key, IntArrayTag.class, consumer);
    }

    public boolean readLongArray(@NonNls String key, Consumer<? super long[]> consumer) {
        return readTag(key, LongArrayTag.class, consumer);
    }

    /**
     * Applies the given function to a long subtag if it is present.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readLong(@NonNls String key, LongConsumer consumer) {
        // Avoid boxing by not delegating to readTag
        if (isLong(key)) {
            consumer.accept(getLong(key));
            return true;
        }
        return false;
    }

    /**
     * Returns the value of a long subtag if it is present.
     *
     * @param key the key to look up
     * @return an Optional with the value of that tag if it's present and is a long; an empty
     * optional otherwise
     */
    public Optional<Long> tryGetLong(@NonNls String key) {
        return tryGetTag(key, LongTag.class);
    }

    /**
     * Applies the given function to an integer subtag if it is present.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readShort(@NonNls String key, ShortConsumer consumer) {
        // Avoid boxing by not delegating to readTag
        if (isShort(key)) {
            consumer.accept(getShort(key));
            return true;
        }
        return false;
    }


    /**
     * Applies the given function to a compound subtag if it is present, first converting it to an
     * item using {@link NbtSerialization#readItem(CompoundTag)}.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readItem(@NonNls String key, Consumer<? super ItemStack> consumer) {
        return readCompound(key, tag -> consumer.accept(NbtSerialization.readItem(tag)));
    }

    /**
     * Applies the given function to a byte subtag if it is present, converting it to boolean first.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readBoolean(@NonNls String key, Consumer<? super Boolean> consumer) {
        // For a boolean, boxing carries no penalty, per
        // https://stackoverflow.com/questions/27698911/why-there-is-no-booleanconsumer-in-java-8
        return readTag(key, ByteTag.class, byteVal -> consumer.accept(byteVal != 0));
    }

    /**
     * Applies the given function to a byte subtag if it is present.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readByte(@NonNls String key, Consumer<? super Byte> consumer) {
        // For a byte, boxing carries no penalty, per
        // https://stackoverflow.com/questions/27698911/why-there-is-no-booleanconsumer-in-java-8
        return readTag(key, ByteTag.class, consumer);
    }

    /**
     * Applies the given function to a byte subtag if it is present, converting it to boolean and
     * negating it first.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readBooleanNegated(@NonNls String key, Consumer<? super Boolean> consumer) {
        // For a boolean, boxing carries no penalty, per
        // https://stackoverflow.com/questions/27698911/why-there-is-no-booleanconsumer-in-java-8
        return readTag(key, ByteTag.class, byteVal -> consumer.accept(byteVal == 0));
    }

    /**
     * Applies the given function to a list subtag if it is present, converting it to a list of
     * values first.
     *
     * @param <T>      the type to convert the list entries to
     * @param key      the key to look up
     * @param type     the type that the list entries must be
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public <T> boolean readList(@NonNls String key, TagType type,
                                Consumer<? super List<T>> consumer) {
        // Can't use readTag because of the list-element type check
        if (isList(key, type)) {
            consumer.accept(getList(key, type));
            return true;
        }
        return false;
    }

    /**
     * Applies the given function to a list subtag if it is present and its contents are compound
     * tags. Processes the list as a single object; to process each tag separately, instead use
     * {@link #iterateCompoundList(String, Consumer)}.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readCompoundList(@NonNls String key,
                                    Consumer<? super List<CompoundTag>> consumer) {
        return readList(key, TagType.COMPOUND, consumer);
    }

    /**
     * Applies the given function to each compound tag in a compound-list subtag, if that subtag
     * exists.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was iterated over (even if it was empty); false otherwise
     */
    public boolean iterateCompoundList(@NonNls String key, Consumer<? super CompoundTag> consumer) {
        return readCompoundList(key, compoundTags -> compoundTags.forEach(consumer));
    }

    /**
     * Applies the given function to a list subtag if it is present and its contents are string
     * tags.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readStringList(@NonNls String key, Consumer<? super List<String>> consumer) {
        return readList(key, TagType.STRING, consumer);
    }

    /**
     * Applies the given function to a list subtag if it is present and its contents are float
     * tags.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readFloatList(@NonNls String key, Consumer<? super List<Float>> consumer) {
        return readList(key, TagType.FLOAT, consumer);
    }

    /**
     * Applies the given function to a list subtag if it is present and its contents are double
     * tags.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readDoubleList(@NonNls String key, Consumer<? super List<Double>> consumer) {
        return readList(key, TagType.DOUBLE, consumer);
    }

    /**
     * Applies the given function to a string subtag if it is present.
     *
     * @param key      the key to look up
     * @param consumer the function to apply
     * @return true if the tag exists and was passed to the consumer; false otherwise
     */
    public boolean readString(@NonNls String key, Consumer<? super String> consumer) {
        return readTag(key, StringTag.class, consumer);
    }

    /**
     * Reads a material from a string ID or numeric ID, depending on the tag type. Returns null if
     * the tag isn't present, its type is neither string nor any integral type, or its value isn't
     * a valid material ID.
     *
     * @param key the key to look up
     * @return the Material denoted by that key, if present and readable; an empty Optional
     * otherwise
     */
    public Optional<Material> tryGetMaterial(@NonNls String key) {
        if (!containsKey(key)) {
            return Optional.empty();
        }
        BlockDataManager blockDataManager = ((GlowServer) Bukkit.getServer()).getBlockDataManager();
        switch (value.get(key).getType()) {
            case STRING:
                @NonNls String id = getString(key);
                if (id.isEmpty()) {
                    return Optional.empty();
                }
                if (!id.contains(":")) {
                    // There is no namespace, so prepend the default minecraft: namespace
                    id = "minecraft:" + id;
                }
                Material type = ItemIds.getBlock(id);
                if (type == null) {
                    // Not a block, might be an item
                    type = ItemIds.getItem(id);
                }
                return Optional.ofNullable(type);
            case INT:
                return Optional.of(blockDataManager.convertToBlockData(getInt(key)).getMaterial());
            case SHORT:
                return Optional
                    .of(blockDataManager.convertToBlockData(getShort(key)).getMaterial());
            case BYTE:
                return Optional.of(blockDataManager.convertToBlockData(getByte(key)).getMaterial());
            default:
                return Optional.empty();
        }
    }

    public Optional<BlockData> tryGetBlockData(@NonNls String key) {
        if (!containsKey(key)) {
            return Optional.empty();
        }
        if (value.get(key).getType() == TagType.COMPOUND) {
            CompoundTag state = getCompound(key);
            Optional<Material> type = tryGetMaterial("Name");
            // TODO: 1.13 parse properties
            Optional<CompoundTag> properties = tryGetCompound("Properties");
            return Optional.of(Bukkit.getServer().createBlockData(type.orElse(Material.AIR)));
        }
        return Optional.empty();
    }

    /**
     * Returns the value of a string subtag if it is present.
     *
     * @param key the key to look up
     * @return an Optional with the value of that tag if it's present and is a string; an empty
     * optional otherwise
     */
    public Optional<String> tryGetString(@NonNls String key) {
        return tryGetTag(key, StringTag.class);
    }

    /**
     * Returns the value of an int subtag if it is present.
     *
     * @param key the key to look up
     * @return an Optional with the value of that tag if it's present and is an int; an empty
     * optional otherwise
     */
    public Optional<Integer> tryGetInt(@NonNls String key) {
        return tryGetTag(key, IntTag.class);
    }

    /**
     * Applies the given function to a UUID extracted from the given pair of long subtags, if they
     * both exist.
     * <p>
     * Uses Most/Least representation.
     *
     * @param keyMost  the key to look up the high word of the UUID
     * @param keyLeast the key to look up the low word of the UUID
     * @param consumer the function to apply
     * @return true if the tags exist and were passed to the consumer; false otherwise
     * @deprecated use {@link #readUniqueId(String, Consumer)} for modern int-array UUID representation.
     */
    @Deprecated
    public boolean readUniqueId(@NonNls String keyMost,
                                @NonNls String keyLeast,
                                Consumer<? super UUID> consumer) {
        if (isLong(keyMost) && isLong(keyLeast)) {
            consumer.accept(new UUID(getLong(keyMost), getLong(keyLeast)));
            return true;
        }
        return false;
    }

    public boolean readUniqueId(@NonNls String key, Consumer<? super UUID> consumer) {
        return readIntArray(key, arr -> consumer.accept(UuidUtils.fromIntArray(arr)));
    }

    /**
     * Returns a UUID extracted from the given pair of long subtags, if they both exist.
     *
     * @param keyMost  the key to look up the high word of the UUID
     * @param keyLeast the key to look up the low word of the UUID
     * @return the UUID, or an empty Optional if either tag is missing or not long type
     * @deprecated use {@link #tryGetUniqueId(String)}  for modern int-array UUID representation.
     */
    @Deprecated
    public Optional<UUID> tryGetUniqueId(@NonNls String keyMost, @NonNls String keyLeast) {
        if (isLong(keyMost) && isLong(keyLeast)) {
            return Optional.of(new UUID(getLong(keyMost), getLong(keyLeast)));
        }
        return Optional.empty();
    }

    public Optional<UUID> tryGetUniqueId(@NonNls String key) {
        if (!isIntArray(key)) {
            return Optional.empty();
        }
        return Optional.of(UuidUtils.fromIntArray(getIntArray(key)));
    }

    /**
     * Returns the value of a list subtag with CompoundTag elements.
     *
     * @param key the key to look up
     * @return the tag value
     */
    @SuppressWarnings("unchecked")
    public List<CompoundTag> getCompoundList(@NonNls String key) {
        return (List<CompoundTag>) getTagList(key, TagType.COMPOUND);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Simple is

    /**
     * Test whether the subtag with the given key is of a numeric type.
     *
     * @param key the key to look up
     * @return true if the subtag exists and is numeric; false otherwise
     */
    public boolean isNumeric(@NonNls String key) {
        return is(key, NumericTag.class);
    }

    /**
     * Test whether the subtag with the given key is of {@code byte} type.
     *
     * @param key the key to look up
     * @return true if the subtag exists and is a {@code byte}; false otherwise
     */
    public boolean isByte(@NonNls String key) {
        return is(key, ByteTag.class);
    }

    /**
     * Test whether the subtag with the given key is of {@code short} type.
     *
     * @param key the key to look up
     * @return true if the subtag exists and is a {@code short}; false otherwise
     */
    public boolean isShort(@NonNls String key) {
        return is(key, ShortTag.class);
    }

    /**
     * Test whether the subtag with the given key is of {@code int} type.
     *
     * @param key the key to look up
     * @return true if the subtag exists and is an {@code int}; false otherwise
     */
    public boolean isInt(@NonNls String key) {
        return is(key, IntTag.class);
    }

    /**
     * Test whether the subtag with the given key is of {@code long} type.
     *
     * @param key the key to look up
     * @return true if the subtag exists and is a {@code long}; false otherwise
     */
    public boolean isLong(@NonNls String key) {
        return is(key, LongTag.class);
    }

    /**
     * Test whether the subtag with the given key is of {@code float} type.
     *
     * @param key the key to look up
     * @return true if the subtag exists and is a {@code float}; false otherwise
     */
    public boolean isFloat(@NonNls String key) {
        return is(key, FloatTag.class);
    }

    /**
     * Test whether the subtag with the given key is of {@code double} type.
     *
     * @param key the key to look up
     * @return true if the subtag exists and is a {@code double}; false otherwise
     */
    public boolean isDouble(@NonNls String key) {
        return is(key, DoubleTag.class);
    }

    /**
     * Test whether the subtag with the given key is of {@code byte[]} type.
     *
     * @param key the key to look up
     * @return true if the subtag exists and is a {@code byte[]}; false otherwise
     */
    public boolean isByteArray(@NonNls String key) {
        return is(key, ByteArrayTag.class);
    }

    /**
     * Test whether the subtag with the given key is of {@link String} type.
     *
     * @param key the key to look up
     * @return true if the subtag exists and is a {@link String}; false otherwise
     */
    public boolean isString(@NonNls String key) {
        return is(key, StringTag.class);
    }

    /**
     * Test whether the subtag with the given key is of {@code int[]} type.
     *
     * @param key the key to look up
     * @return true if the subtag exists and is an {@code int[]}; false otherwise
     */
    public boolean isIntArray(@NonNls String key) {
        return is(key, IntArrayTag.class);
    }

    /**
     * Test whether the subtag with the given key is of {@code long[]} type.
     *
     * @param key the key to look up
     * @return true if the subtag exists and is an {@code long[]}; false otherwise
     */
    public boolean isLongArray(@NonNls String key) {
        return is(key, LongArrayTag.class);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Fancy is

    /**
     * Test whether the subtag with the given key is of {@link List} type.
     *
     * @param key  the key to look up
     * @param type the {@link TagType} of the list's elements
     * @return true if the subtag exists and is a {@link List}; false otherwise
     */
    public boolean isList(@NonNls String key, TagType type) {
        if (!is(key, ListTag.class)) {
            return false;
        }
        ListTag tag = getTag(key, ListTag.class);
        return tag.getChildType() == type;
    }

    /**
     * Test whether the subtag with the given key is of {@link CompoundTag} type.
     *
     * @param key the key to look up
     * @return true if the subtag exists and is a {@link CompoundTag}; false otherwise
     */
    public boolean isCompound(@NonNls String key) {
        return is(key, CompoundTag.class);
    }

    /**
     * Test whether the subtag with the given key is of {@link List} type with elements of type
     * {@link CompoundTag}.
     *
     * @param key the key to look up
     * @return true if the subtag exists and is a {@link List} with elements of type
     * {@link CompoundTag}; false otherwise
     */
    public boolean isCompoundList(@NonNls String key) {
        return isList(key, TagType.COMPOUND);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Simple sets

    public void putBool(@NonNls String key, boolean value) {
        putByte(key, value ? 1 : 0);
    }

    public void putByte(@NonNls String key, int value) {
        put(key, new ByteTag((byte) value));
    }

    public void putShort(@NonNls String key, int value) {
        put(key, new ShortTag((short) value));
    }

    public void putInt(@NonNls String key, int value) {
        put(key, new IntTag(value));
    }

    public void putLong(@NonNls String key, long value) {
        put(key, new LongTag(value));
    }

    public void putFloat(@NonNls String key, double value) {
        put(key, new FloatTag((float) value));
    }

    public void putDouble(@NonNls String key, double value) {
        put(key, new DoubleTag(value));
    }

    public void putByteArray(@NonNls String key, byte... value) {
        put(key, new ByteArrayTag(value));
    }

    public void putString(@NonNls String key, @NonNls String value) {
        put(key, new StringTag(value));
    }

    public void putIntArray(@NonNls String key, int... value) {
        put(key, new IntArrayTag(value));
    }

    public void putLongArray(@NonNls String key, long... value) {
        put(key, new LongArrayTag(value));
    }

    ////////////////////////////////////////////////////////////////////////////
    // Fancy sets

    /**
     * Adds or replaces a list subtag, converting the list entries to tags.
     *
     * @param <V>        the list elements' Java type
     * @param key        the key to write to
     * @param type       the list elements' tag type
     * @param value      the list contents, as objects to convert to tags
     * @param tagCreator a function that will convert each V to an element tag
     */
    public <V> void putList(@NonNls String key, TagType type, List<V> value,
                            Function<? super V, ? extends Tag> tagCreator) {
        List<Tag> result = new ArrayList<>(value.size());
        for (V item : value) {
            result.add(tagCreator.apply(item));
        }
        put(key, new ListTag<>(type, result));
    }

    public void putCompound(@NonNls String key, CompoundTag tag) {
        put(key, tag);
    }

    /**
     * Adds or replaces a list subtag with a list of compound tags.
     *
     * @param key  the key to write to
     * @param list the list contents as compound tags
     */
    public void putCompoundList(@NonNls String key, List<CompoundTag> list) {
        put(key, new ListTag<>(TagType.COMPOUND, list));
    }

    /**
     * Adds or replaces a list subtag with a list of strings.
     *
     * @param key  the key to write to
     * @param list the list contents as strings, to convert to string tags
     */
    public void putStringList(@NonNls String key, List<String> list) {
        putList(key, TagType.STRING, list, StringTag::new);
    }

    /**
     * Adds or replaces a list subtag with a list of floats.
     *
     * @param key  the key to write to
     * @param list the list contents as floats, to convert to float tags
     */
    public void putFloatList(@NonNls String key, List<Float> list) {
        putList(key, TagType.FLOAT, list, FloatTag::new);
    }

    /**
     * Adds or replaces a list subtag with a list of doubles.
     *
     * @param key  the key to write to
     * @param list the list contents as doubles, to convert to double tags
     */
    public void putDoubleList(@NonNls String key, List<Double> list) {
        putList(key, TagType.DOUBLE, list, DoubleTag::new);
    }

    /**
     * Adds or replaces a list subtag with a list of longs.
     *
     * @param key  the key to write to
     * @param list the list contents as longs, to convert to long tags
     */
    public void putLongList(@NonNls String key, List<Long> list) {
        putList(key, TagType.LONG, list, LongTag::new);
    }

    public void putUniqueId(@NonNls String key, UUID uuid) {
        putIntArray(key, UuidUtils.toIntArray(uuid));
    }

    ////////////////////////////////////////////////////////////////////////////
    // Accessor helpers

    private <T extends Tag<?>> boolean is(@NonNls String key, Class<T> clazz) {
        if (!containsKey(key)) {
            return false;
        }
        Tag tag = value.get(key);
        return tag != null && clazz.isAssignableFrom(tag.getClass());
    }

    void put(String key, Tag tag) {
        checkNotNull(key, "Key cannot be null"); // NON-NLS
        checkNotNull(tag, "Tag cannot be null"); // NON-NLS
        value.put(key, tag);
    }

    private <V, T extends Tag<V>> V get(@NonNls String key, Class<T> clazz) {
        return getTag(key, clazz).getValue();
    }

    @SuppressWarnings("unchecked")
    private <T extends Tag<?>> T getTag(@NonNls String key, Class<T> clazz) {
        if (!is(key, clazz)) {
            throw new IllegalArgumentException(
                "Compound does not contain " + clazz.getSimpleName() + " \"" + key + "\"");
        }
        return (T) value.get(key);
    }

    private List<? extends Tag> getTagList(@NonNls String key, TagType type) {
        ListTag<?> tag = getTag(key, ListTag.class);
        if (tag.getValue().isEmpty()) {
            // empty lists are allowed to be the wrong type
            return Arrays.asList();
        }
        if (tag.getChildType() != type) {
            throw new IllegalArgumentException(
                "List \"" + key + "\" contains " + tag.getChildType() + ", not " + type);
        }
        return tag.getValue();
    }
}

