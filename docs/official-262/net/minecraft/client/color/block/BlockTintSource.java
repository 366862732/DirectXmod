/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.color.block;

import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

@Environment(value=EnvType.CLIENT)
public interface BlockTintSource {
    public int color(BlockState var1);

    default public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return this.color(state);
    }

    default public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return this.colorInWorld(state, level, pos);
    }

    default public Set<Property<?>> relevantProperties() {
        return Set.of();
    }
}

