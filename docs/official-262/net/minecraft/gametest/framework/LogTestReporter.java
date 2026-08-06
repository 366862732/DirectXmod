/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.gametest.framework;

import com.mojang.logging.LogUtils;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.TestReporter;
import net.minecraft.util.Util;
import org.slf4j.Logger;

public class LogTestReporter
implements TestReporter {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onTestFailed(GameTestInfo testInfo) {
        String testPosition = testInfo.getTestBlockPos().toShortString();
        if (testInfo.isRequired()) {
            LOGGER.error("{} failed at {}! {}", new Object[]{testInfo.id(), testPosition, Util.describeError(testInfo.getError())});
        } else {
            LOGGER.warn("(optional) {} failed at {}. {}", new Object[]{testInfo.id(), testPosition, Util.describeError(testInfo.getError())});
        }
    }

    @Override
    public void onTestSuccess(GameTestInfo testInfo) {
    }
}

