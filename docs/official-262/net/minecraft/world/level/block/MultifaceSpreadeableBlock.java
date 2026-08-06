/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.state.BlockBehaviour;

public abstract class MultifaceSpreadeableBlock
extends MultifaceBlock {
    public MultifaceSpreadeableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public abstract MapCodec<? extends MultifaceSpreadeableBlock> codec();

    public abstract MultifaceSpreader getSpreader();
}

