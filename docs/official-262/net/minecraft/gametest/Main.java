/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.gametest;

import net.minecraft.SharedConstants;
import net.minecraft.gametest.framework.GameTestMainUtil;

public class Main {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        GameTestMainUtil.runGameTestServer(args, path -> {});
    }
}

