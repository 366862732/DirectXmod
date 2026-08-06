/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

public interface WorldGenLevel
extends ServerLevelAccessor {
    public long getSeed();

    default public boolean ensureCanWrite(BlockPos pos) {
        return true;
    }

    default public void setCurrentlyGenerating(@Nullable Supplier<String> currentlyGenerating) {
    }
}

