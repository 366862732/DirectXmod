/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ChickenRenderState
extends LivingEntityRenderState {
    public float flap;
    public float flapSpeed;
    public @Nullable ChickenVariant variant;
}

