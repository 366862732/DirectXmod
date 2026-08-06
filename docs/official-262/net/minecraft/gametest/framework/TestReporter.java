/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.gametest.framework;

import net.minecraft.gametest.framework.GameTestInfo;

public interface TestReporter {
    public void onTestFailed(GameTestInfo var1);

    public void onTestSuccess(GameTestInfo var1);

    default public void finish() {
    }
}

