/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.block.model.properties.select;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.model.properties.select.SelectBlockModelProperty;
import net.minecraft.world.level.block.state.BlockState;

@Environment(value=EnvType.CLIENT)
public record DisplayContext() implements SelectBlockModelProperty<BlockDisplayContext>
{
    @Override
    public BlockDisplayContext get(BlockState blockState, BlockDisplayContext displayContext) {
        return displayContext;
    }
}

