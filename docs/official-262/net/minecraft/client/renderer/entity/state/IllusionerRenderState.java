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
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class IllusionerRenderState
extends IllagerRenderState {
    public Vec3[] illusionOffsets = new Vec3[0];
    public boolean isCastingSpell;
}

