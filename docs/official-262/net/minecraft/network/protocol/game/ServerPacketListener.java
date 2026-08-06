/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.network.protocol.game;

import com.mojang.logging.LogUtils;
import net.minecraft.ReportedException;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.protocol.Packet;
import org.slf4j.Logger;

public interface ServerPacketListener
extends ServerboundPacketListener {
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    default public void onPacketError(Packet packet, Exception e) throws ReportedException {
        LOGGER.error("Failed to handle packet {}, suppressing error", (Object)packet, (Object)e);
    }
}

