/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources.server;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.packs.DownloadQueue;

@Environment(value=EnvType.CLIENT)
public interface PackDownloader {
    public void download(Map<UUID, DownloadQueue.DownloadRequest> var1, Consumer<DownloadQueue.BatchResult> var2);
}

