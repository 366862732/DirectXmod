/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class PlayerHeadBlock
extends SkullBlock {
    public static final MapCodec<PlayerHeadBlock> CODEC = PlayerHeadBlock.simpleCodec(PlayerHeadBlock::new);

    public MapCodec<PlayerHeadBlock> codec() {
        return CODEC;
    }

    protected PlayerHeadBlock(BlockBehaviour.Properties properties) {
        super(SkullBlock.Types.PLAYER, properties);
    }
}

