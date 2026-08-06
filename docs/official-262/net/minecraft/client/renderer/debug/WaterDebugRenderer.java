/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class WaterDebugRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private final Minecraft minecraft;

    public WaterDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        FluidState fluidState;
        BlockPos pos = this.minecraft.player.blockPosition();
        Level level = this.minecraft.player.level();
        for (BlockPos blockPos : BlockPos.betweenClosed(pos.offset(-10, -10, -10), pos.offset(10, 10, 10))) {
            fluidState = level.getFluidState(blockPos);
            if (!fluidState.is(FluidTags.WATER)) continue;
            double height = (float)blockPos.getY() + fluidState.getHeight(level, blockPos);
            Gizmos.cuboid(new AABB((float)blockPos.getX() + 0.01f, (float)blockPos.getY() + 0.01f, (float)blockPos.getZ() + 0.01f, (float)blockPos.getX() + 0.99f, height, (float)blockPos.getZ() + 0.99f), GizmoStyle.fill(ARGB.colorFromFloat(0.15f, 0.0f, 1.0f, 0.0f)));
        }
        for (BlockPos blockPos : BlockPos.betweenClosed(pos.offset(-10, -10, -10), pos.offset(10, 10, 10))) {
            fluidState = level.getFluidState(blockPos);
            if (!fluidState.is(FluidTags.WATER)) continue;
            Gizmos.billboardText(String.valueOf(fluidState.getAmount()), Vec3.atLowerCornerWithOffset(blockPos, 0.5, fluidState.getHeight(level, blockPos), 0.5), TextGizmo.Style.forColorAndCentered(-16777216));
        }
    }
}

