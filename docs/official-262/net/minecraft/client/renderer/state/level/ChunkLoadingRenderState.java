/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.state.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class ChunkLoadingRenderState {
    public LongOpenHashSet addedEmptySections = new LongOpenHashSet();
    public LongOpenHashSet removedEmptySections = new LongOpenHashSet();
    public LongOpenHashSet addedLoadedChunks = new LongOpenHashSet();
    public LongOpenHashSet removedLoadedChunks = new LongOpenHashSet();
    public LongOpenHashSet loadedExpectedChunks = new LongOpenHashSet();

    public void reset() {
        this.loadedExpectedChunks.clear();
    }
}

