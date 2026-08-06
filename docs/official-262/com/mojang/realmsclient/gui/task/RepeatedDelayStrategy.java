/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.gui.task;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public interface RepeatedDelayStrategy {
    public static final RepeatedDelayStrategy CONSTANT = new RepeatedDelayStrategy(){

        @Override
        public long delayCyclesAfterSuccess() {
            return 1L;
        }

        @Override
        public long delayCyclesAfterFailure() {
            return 1L;
        }
    };

    public long delayCyclesAfterSuccess();

    public long delayCyclesAfterFailure();

    public static RepeatedDelayStrategy exponentialBackoff(final int maxBackoff) {
        return new RepeatedDelayStrategy(){
            private static final Logger LOGGER = LogUtils.getLogger();
            private int failureCount;

            @Override
            public long delayCyclesAfterSuccess() {
                this.failureCount = 0;
                return 1L;
            }

            @Override
            public long delayCyclesAfterFailure() {
                ++this.failureCount;
                long expandedDelay = Math.min(1L << this.failureCount, (long)maxBackoff);
                LOGGER.debug("Skipping for {} extra cycles", (Object)expandedDelay);
                return expandedDelay;
            }
        };
    }
}

