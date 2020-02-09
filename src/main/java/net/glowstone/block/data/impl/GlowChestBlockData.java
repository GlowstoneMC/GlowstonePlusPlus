package net.glowstone.block.data.impl;

import net.glowstone.block.data.AbstractBlockData;
import net.glowstone.block.data.impl.inter.GlowDirectional;
import net.glowstone.block.data.impl.inter.GlowWaterlogged;
import net.glowstone.block.data.state.generator.StateGenerator;
import net.glowstone.block.data.state.value.EnumStateValue;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;
import org.jetbrains.annotations.NotNull;

public class GlowChestBlockData extends AbstractBlockData implements GlowDirectional, GlowWaterlogged, Chest {

    public GlowChestBlockData(Material material) {
        super(material, StateGenerator.FOUR_FACING, StateGenerator.WATER_LOGGED, StateGenerator.CHEST_TYPE);
    }

    public EnumStateValue<Chest.Type> getTypeStateValue(){
        return (EnumStateValue<Type>) this.<Type>getStateValue("type").get();
    }

    @Override
    public @NotNull Type getType() {
        return this.getTypeStateValue().getValue();
    }

    @Override
    public void setType(@NotNull Type type) {
        this.getTypeStateValue().setValue(type);
    }


    @Override
    public @NotNull BlockData clone() {
        GlowChestBlockData data = new GlowChestBlockData(this.getMaterial());
        data.setFacing(this.getFacing());
        data.setWaterlogged(this.isWaterlogged());
        data.setType(this.getType());
        return data;
    }
}