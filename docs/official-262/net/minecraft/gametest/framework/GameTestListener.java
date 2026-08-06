/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.gametest.framework;

import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestRunner;

public interface GameTestListener {
    public void testStructureLoaded(GameTestInfo var1);

    public void testPassed(GameTestInfo var1, GameTestRunner var2);

    public void testFailed(GameTestInfo var1, GameTestRunner var2);

    public void testAddedForRerun(GameTestInfo var1, GameTestInfo var2, GameTestRunner var3);
}

