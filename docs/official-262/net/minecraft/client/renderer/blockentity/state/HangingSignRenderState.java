/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.blockentity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.world.level.block.HangingSignBlock;

@Environment(value=EnvType.CLIENT)
public class HangingSignRenderState
extends SignRenderState {
    public HangingSignBlock.Attachment attachmentType = HangingSignBlock.Attachment.CEILING;
}

