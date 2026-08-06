/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.flat;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;

public class FlatLayerInfo {
    public static final Codec<FlatLayerInfo> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.intRange((int)0, (int)DimensionType.Y_SIZE).fieldOf("height").forGetter(FlatLayerInfo::getHeight), (App)BuiltInRegistries.BLOCK.holderByNameCodec().fieldOf("block").forGetter(l -> l.block)).apply((Applicative)i, FlatLayerInfo::new));
    private final Holder<Block> block;
    private final int height;

    public FlatLayerInfo(int height, Block block) {
        this(height, block.builtInRegistryHolder());
    }

    public FlatLayerInfo(int height, Holder<Block> block) {
        this.height = height;
        this.block = block;
    }

    public int getHeight() {
        return this.height;
    }

    public BlockState getBlockState() {
        return this.block.value().defaultBlockState();
    }

    public FlatLayerInfo heightLimited(int maxHeight) {
        if (this.height > maxHeight) {
            return new FlatLayerInfo(maxHeight, this.block);
        }
        return this;
    }

    public String toString() {
        return (String)(this.height != 1 ? this.height + "*" : "") + this.block.getRegisteredName();
    }
}

