package net.glowstone.block.entity.state;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.glowstone.block.GlowBlock;
import net.glowstone.block.GlowBlockState;
import net.glowstone.block.entity.SignEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.DyeColor;
import org.bukkit.Tag;
import org.bukkit.block.Sign;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GlowSign extends GlowBlockState implements Sign {

    private final String[] lines;
    @Getter
    @Setter
    public boolean editable = true;

    /**
     * Creates the instance for the given sign block.
     *
     * @param block a sign block (wall or post)
     */
    public GlowSign(GlowBlock block) {
        super(block);
        if (Tag.WALL_SIGNS.isTagged(block.getType()) && Tag.STANDING_SIGNS.isTagged(block.getType())) {
            throw new IllegalArgumentException(
                "GlowSign: expected WALL_SIGN or STANDING_SIGN got " + block.getType());
        }
        lines = getBlockEntity().getLines();
    }

    private SignEntity getBlockEntity() {
        return (SignEntity) getBlock().getBlockEntity();
    }

    @Override
    public @NotNull List<Component> lines() {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public @NotNull Component line(int i) throws IndexOutOfBoundsException {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @Override
    public void line(int i, @NotNull Component component) throws IndexOutOfBoundsException {
        throw new UnsupportedOperationException("Adventure API is not yet supported.");
    }

    @NotNull
    @Override
    public String[] getLines() {
        return lines.clone();
    }

    @NotNull
    @Override
    public String getLine(int index) throws IndexOutOfBoundsException {
        return lines[index];
    }

    @Override
    public void setLine(int index, @NotNull String line) throws IndexOutOfBoundsException {
        lines[index] = line;
    }

    @Override
    public boolean update(boolean force, boolean applyPhysics) {
        boolean result = super.update(force, applyPhysics);
        if (result) {
            SignEntity sign = getBlockEntity();
            sign.setLines(lines);
            sign.updateInRange();
        }
        return result;
    }

    @Override
    public @NotNull PersistentDataContainer getPersistentDataContainer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable DyeColor getColor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setColor(DyeColor dyeColor) {
        throw new UnsupportedOperationException();
    }
}
