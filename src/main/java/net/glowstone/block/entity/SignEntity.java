package net.glowstone.block.entity;

import java.util.Arrays;
import net.glowstone.block.GlowBlock;
import net.glowstone.block.GlowBlockState;
import net.glowstone.block.entity.state.GlowSign;
import net.glowstone.entity.GlowPlayer;
import net.glowstone.util.TextMessage;
import net.glowstone.util.nbt.CompoundTag;
import org.bukkit.Tag;

public class SignEntity extends BlockEntity {

    private final TextMessage[] lines = new TextMessage[4];

    /**
     * Creates the entity for the given sign block.
     *
     * @param block a sign block (wall or post)
     */
    public SignEntity(GlowBlock block) {
        super(block);
        setSaveId("minecraft:sign");

        if (Tag.WALL_SIGNS.isTagged(block.getType()) && Tag.STANDING_SIGNS.isTagged(block.getType())) {
            throw new IllegalArgumentException(
                "Sign must be WALL_SIGN or SIGN, got " + block.getType());
        }

        Arrays.fill(lines, new TextMessage(""));
    }

    @Override
    public void update(GlowPlayer player) {
        player.sendSignChange(this, getBlock().getLocation(), lines);
    }

    @Override
    public void loadNbt(CompoundTag tag) {
        super.loadNbt(tag);
        for (int i = 0; i < lines.length; ++i) {
            final int finalI = i;
            tag.readString("Text" + (i + 1), line -> lines[finalI] = TextMessage.decode(line));
        }
    }

    @Override
    public void saveNbt(CompoundTag tag) {
        super.saveNbt(tag);
        for (int i = 0; i < lines.length; ++i) {
            tag.putString("Text" + (i + 1), lines[i].encode());
        }
    }

    @Override
    public GlowBlockState getState() {
        return new GlowSign(block);
    }

    /**
     * Get the lines of text on the sign.
     *
     * @return The sign's lines.
     */
    public String[] getLines() {
        String[] result = new String[lines.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = lines[i].asPlaintext();
        }
        return result;
    }

    /**
     * Set the lines of text on the sign.
     *
     * @param text The lines of text.
     * @throws IllegalArgumentException If the wrong number of lines is provided.
     */
    public void setLines(String... text) {
        if (text.length != lines.length) {
            throw new IllegalArgumentException(
                "Provided lines were length " + text.length + ", must be " + lines.length);
        }

        for (int i = 0; i < lines.length; ++i) {
            lines[i] = new TextMessage(text[i] == null ? "" : text[i]);
        }
    }
}
