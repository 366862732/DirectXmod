/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.task.LongRunningTask;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class RealmCreationTask
extends LongRunningTask {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component TITLE = Component.translatable("mco.create.world.wait");
    private final String name;
    private final String motd;
    private final long realmId;

    public RealmCreationTask(long realmId, String name, String motd) {
        this.realmId = realmId;
        this.name = name;
        this.motd = motd;
    }

    @Override
    public void run() {
        RealmsClient client = RealmsClient.getOrCreate();
        try {
            client.initializeRealm(this.realmId, this.name, this.motd);
        }
        catch (RealmsServiceException e) {
            LOGGER.error("Couldn't create world", (Throwable)e);
            this.error(e);
        }
        catch (Exception e) {
            LOGGER.error("Could not create world", (Throwable)e);
            this.error(e);
        }
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }
}

