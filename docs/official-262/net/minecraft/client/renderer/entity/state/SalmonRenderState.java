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
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.animal.fish.Salmon;

@Environment(value=EnvType.CLIENT)
public class SalmonRenderState
extends LivingEntityRenderState {
    public Salmon.Variant variant = Salmon.Variant.MEDIUM;
}

