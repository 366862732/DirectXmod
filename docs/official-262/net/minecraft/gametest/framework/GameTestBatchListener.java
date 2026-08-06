/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.gametest.framework;

import net.minecraft.gametest.framework.GameTestBatch;

public interface GameTestBatchListener {
    public void testBatchStarting(GameTestBatch var1);

    public void testBatchFinished(GameTestBatch var1);
}

