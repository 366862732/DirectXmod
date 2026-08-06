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
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

@Environment(value=EnvType.CLIENT)
public class SolidFaceRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private final Minecraft minecraft;

    public SolidFaceRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        Level level = this.minecraft.player.level();
        BlockPos playerPos = BlockPos.containing(camX, camY, camZ);
        for (BlockPos blockPos : BlockPos.betweenClosed(playerPos.offset(-6, -6, -6), playerPos.offset(6, 6, 6))) {
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.is(Blocks.AIR)) continue;
            VoxelShape shape = blockState.getShape(level, blockPos);
            for (AABB outlineBox : shape.toAabbs()) {
                AABB aabb = outlineBox.move(blockPos).inflate(0.002);
                int color = -2130771968;
                Vec3 min = aabb.getMinPosition();
                Vec3 max = aabb.getMaxPosition();
                SolidFaceRenderer.addFaceIfSturdy(blockPos, blockState, level, Direction.WEST, min, max, -2130771968);
                SolidFaceRenderer.addFaceIfSturdy(blockPos, blockState, level, Direction.SOUTH, min, max, -2130771968);
                SolidFaceRenderer.addFaceIfSturdy(blockPos, blockState, level, Direction.EAST, min, max, -2130771968);
                SolidFaceRenderer.addFaceIfSturdy(blockPos, blockState, level, Direction.NORTH, min, max, -2130771968);
                SolidFaceRenderer.addFaceIfSturdy(blockPos, blockState, level, Direction.DOWN, min, max, -2130771968);
                SolidFaceRenderer.addFaceIfSturdy(blockPos, blockState, level, Direction.UP, min, max, -2130771968);
            }
        }
    }

    private static void addFaceIfSturdy(BlockPos blockPos, BlockState blockState, BlockGetter level, Direction direction, Vec3 cornerA, Vec3 cornerB, int color) {
        if (blockState.isFaceSturdy(level, blockPos, direction)) {
            Gizmos.rect(cornerA, cornerB, direction, GizmoStyle.fill(color));
        }
    }
}

