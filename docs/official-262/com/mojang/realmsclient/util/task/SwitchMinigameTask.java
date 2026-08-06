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
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.util.task.LongRunningTask;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class SwitchMinigameTask
extends LongRunningTask {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component TITLE = Component.translatable("mco.minigame.world.starting.screen.title");
    private final long realmId;
    private final WorldTemplate worldTemplate;
    private final RealmsConfigureWorldScreen nextScreen;

    public SwitchMinigameTask(long realmId, WorldTemplate worldTemplate, RealmsConfigureWorldScreen nextScreen) {
        this.realmId = realmId;
        this.worldTemplate = worldTemplate;
        this.nextScreen = nextScreen;
    }

    @Override
    public void run() {
        RealmsClient client = RealmsClient.getOrCreate();
        for (int i = 0; i < 25; ++i) {
            try {
                if (this.aborted()) {
                    return;
                }
                if (!client.putIntoMinigameMode(this.realmId, this.worldTemplate.id()).booleanValue()) continue;
                SwitchMinigameTask.setScreen(this.nextScreen);
                break;
            }
            catch (RetryCallException e) {
                if (this.aborted()) {
                    return;
                }
                SwitchMinigameTask.pause(e.delaySeconds);
                continue;
            }
            catch (Exception e) {
                if (this.aborted()) {
                    return;
                }
                LOGGER.error("Couldn't start mini game!");
                this.error(e);
            }
        }
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }
}

