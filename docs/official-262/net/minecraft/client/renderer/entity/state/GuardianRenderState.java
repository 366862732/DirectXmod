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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class GuardianRenderState
extends LivingEntityRenderState {
    public float spikesAnimation;
    public float tailAnimation;
    public Vec3 eyePosition = Vec3.ZERO;
    public @Nullable Vec3 lookDirection;
    public @Nullable Vec3 lookAtPosition;
    public @Nullable Vec3 attackTargetPosition;
    public float attackTime;
    public float attackScale;
}

