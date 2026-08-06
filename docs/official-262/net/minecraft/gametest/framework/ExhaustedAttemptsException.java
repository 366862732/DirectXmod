/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.gametest.framework;

import net.minecraft.gametest.framework.GameTestInfo;

public class ExhaustedAttemptsException
extends Throwable {
    public ExhaustedAttemptsException(int attempts, int successes, GameTestInfo testInfo) {
        super("Not enough successes: " + successes + " out of " + attempts + " attempts. Required successes: " + testInfo.requiredSuccesses() + ". max attempts: " + testInfo.maxAttempts() + ".", testInfo.getError());
    }
}

