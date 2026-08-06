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
import net.minecraft.client.renderer.blockentity.state.EndPortalRenderState;

@Environment(value=EnvType.CLIENT)
public class EndGatewayRenderState
extends EndPortalRenderState {
    public int height;
    public float scale;
    public int color;
    public float animationTime;
}

