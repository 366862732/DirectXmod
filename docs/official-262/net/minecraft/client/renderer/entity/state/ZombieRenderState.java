/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.UndeadRenderState;

@Environment(value=EnvType.CLIENT)
public class ZombieRenderState
extends UndeadRenderState {
    public boolean isAggressive;
    public boolean isConverting;
}

