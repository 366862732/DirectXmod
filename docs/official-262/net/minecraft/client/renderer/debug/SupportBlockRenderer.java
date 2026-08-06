/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.debug;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@Environment(value=EnvType.CLIENT)
public class SupportBlockRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private final Minecraft minecraft;
    private double lastUpdateTime = Double.MIN_VALUE;
    private List<Entity> surroundEntities = Collections.emptyList();

    public SupportBlockRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        LocalPlayer player;
        double time = Util.getNanos();
        if (time - this.lastUpdateTime > 1.0E8) {
            this.lastUpdateTime = time;
            Entity cameraEntity = this.minecraft.getCameraEntity();
            this.surroundEntities = ImmutableList.copyOf(cameraEntity.level().getEntities(cameraEntity, cameraEntity.getBoundingBox().inflate(16.0)));
        }
        if ((player = this.minecraft.player) != null && player.mainSupportingBlockPos.isPresent()) {
            this.drawHighlights(player, () -> 0.0, -65536);
        }
        for (Entity entity : this.surroundEntities) {
            if (entity == player) continue;
            this.drawHighlights(entity, () -> this.getBias(entity), -16711936);
        }
    }

    private void drawHighlights(Entity entity, DoubleSupplier biasGetter, int color) {
        entity.mainSupportingBlockPos.ifPresent(bp -> {
            double bias = biasGetter.getAsDouble();
            BlockPos supportingBlock = entity.getOnPos();
            this.highlightPosition(supportingBlock, 0.02 + bias, color);
            BlockPos effect = entity.getOnPosLegacy();
            if (!effect.equals(supportingBlock)) {
                this.highlightPosition(effect, 0.04 + bias, -16711681);
            }
        });
    }

    private double getBias(Entity entity) {
        return 0.02 * (double)(String.valueOf((double)entity.getId() + 0.132453657).hashCode() % 1000) / 1000.0;
    }

    private void highlightPosition(BlockPos pos, double offset, int color) {
        double fromX = (double)pos.getX() - 2.0 * offset;
        double fromY = (double)pos.getY() - 2.0 * offset;
        double fromZ = (double)pos.getZ() - 2.0 * offset;
        double toX = fromX + 1.0 + 4.0 * offset;
        double toY = fromY + 1.0 + 4.0 * offset;
        double toZ = fromZ + 1.0 + 4.0 * offset;
        Gizmos.cuboid(new AABB(fromX, fromY, fromZ, toX, toY, toZ), GizmoStyle.stroke(ARGB.color(0.4f, color)));
        VoxelShape shape = this.minecraft.level.getBlockState(pos).getCollisionShape(this.minecraft.level, pos, CollisionContext.empty()).move(pos);
        GizmoStyle style = GizmoStyle.stroke(color);
        for (AABB aabb : shape.toAabbs()) {
            Gizmos.cuboid(aabb, style);
        }
    }
}

