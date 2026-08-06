/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.chunk;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LightChunk;
import org.jspecify.annotations.Nullable;

public interface LightChunkGetter {
    public @Nullable LightChunk getChunkForLighting(int var1, int var2);

    default public void onLightUpdate(LightLayer layer, SectionPos pos) {
    }

    public BlockGetter getLevel();
}

