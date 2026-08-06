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
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class MinecartRenderState
extends EntityRenderState {
    public float xRot;
    public float yRot;
    public long offsetSeed;
    public int hurtDir;
    public float hurtTime;
    public float damageTime;
    public int displayOffset;
    public BlockModelRenderState displayBlockModel = new BlockModelRenderState();
    public boolean isNewRender;
    public @Nullable Vec3 renderPos;
    public @Nullable Vec3 posOnRail;
    public @Nullable Vec3 frontPos;
    public @Nullable Vec3 backPos;
}

