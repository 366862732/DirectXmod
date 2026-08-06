/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2IntFunction
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.blockentity;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;

@Environment(value=EnvType.CLIENT)
public class BrightnessCombiner<S extends BlockEntity>
implements DoubleBlockCombiner.Combiner<S, Int2IntFunction> {
    @Override
    public Int2IntFunction acceptDouble(S first, S second) {
        return i -> LightCoordsUtil.max(LightCoordsUtil.getLightCoords(first.getLevel(), first.getBlockPos()), LightCoordsUtil.getLightCoords(second.getLevel(), second.getBlockPos()));
    }

    @Override
    public Int2IntFunction acceptSingle(S single) {
        return i -> i;
    }

    @Override
    public Int2IntFunction acceptNone() {
        return i -> i;
    }
}

