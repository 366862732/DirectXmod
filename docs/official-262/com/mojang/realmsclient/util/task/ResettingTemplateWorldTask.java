/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.realmsclient.util.task;

import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.task.ResettingWorldTask;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

@Environment(value=EnvType.CLIENT)
public class ResettingTemplateWorldTask
extends ResettingWorldTask {
    private final WorldTemplate template;

    public ResettingTemplateWorldTask(WorldTemplate template, long serverId, Component title, Runnable callback) {
        super(serverId, title, callback);
        this.template = template;
    }

    @Override
    protected void sendResetRequest(RealmsClient client, long serverId) throws RealmsServiceException {
        client.resetWorldWithTemplate(serverId, this.template.id());
    }
}

