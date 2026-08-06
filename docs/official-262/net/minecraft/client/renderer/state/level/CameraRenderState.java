/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionf
 */
package net.minecraft.client.renderer.state.level;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.state.level.CameraEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;

@Environment(value=EnvType.CLIENT)
public class CameraRenderState {
    public BlockPos blockPos = BlockPos.ZERO;
    public Vec3 pos = new Vec3(0.0, 0.0, 0.0);
    public float xRot;
    public float yRot;
    public boolean initialized;
    public boolean isPanoramicMode;
    public boolean isFrustumCaptured;
    public boolean smartCull;
    public Quaternionf orientation = new Quaternionf();
    public Frustum cullFrustum = new Frustum((Matrix4fc)new Matrix4f(), new Matrix4f());
    public FogType fogType = FogType.NONE;
    public FogData fogData = new FogData();
    public float hudFov;
    public float depthFar;
    public Matrix4f projectionMatrix = new Matrix4f();
    public Matrix4f viewRotationMatrix = new Matrix4f();
    public CameraEntityRenderState entityRenderState = new CameraEntityRenderState();
}

