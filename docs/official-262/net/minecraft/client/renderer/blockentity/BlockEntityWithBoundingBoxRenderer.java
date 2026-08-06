/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityWithBoundingBoxRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BoundingBoxRenderable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class BlockEntityWithBoundingBoxRenderer<T extends BlockEntity>
implements BlockEntityRenderer<T, BlockEntityWithBoundingBoxRenderState> {
    public static final int STRUCTURE_VOIDS_COLOR = ARGB.colorFromFloat(0.2f, 0.75f, 0.75f, 1.0f);

    @Override
    public BlockEntityWithBoundingBoxRenderState createRenderState() {
        return new BlockEntityWithBoundingBoxRenderState();
    }

    @Override
    public void extractRenderState(T blockEntity, BlockEntityWithBoundingBoxRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockEntityWithBoundingBoxRenderer.extract(blockEntity, state);
    }

    public static <T extends BlockEntity> void extract(T blockEntity, BlockEntityWithBoundingBoxRenderState state) {
        LocalPlayer player = Minecraft.getInstance().player;
        state.isVisible = player.canUseGameMasterBlocks() || player.isSpectator();
        state.box = ((BoundingBoxRenderable)((Object)blockEntity)).getRenderableBox();
        state.mode = ((BoundingBoxRenderable)((Object)blockEntity)).renderMode();
        BlockPos pos = state.box.localPos();
        Vec3i size = state.box.size();
        BlockPos entityPos = state.blockPos;
        BlockPos startingPos = entityPos.offset(pos);
        if (state.isVisible && blockEntity.getLevel() != null && state.mode == BoundingBoxRenderable.Mode.BOX_AND_INVISIBLE_BLOCKS) {
            state.invisibleBlocks = new BlockEntityWithBoundingBoxRenderState.InvisibleBlockType[size.getX() * size.getY() * size.getZ()];
            for (int x = 0; x < size.getX(); ++x) {
                for (int y = 0; y < size.getY(); ++y) {
                    for (int z = 0; z < size.getZ(); ++z) {
                        int index = z * size.getX() * size.getY() + y * size.getX() + x;
                        BlockState blockState = blockEntity.getLevel().getBlockState(startingPos.offset(x, y, z));
                        if (blockState.isAir()) {
                            state.invisibleBlocks[index] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR;
                            continue;
                        }
                        if (blockState.is(Blocks.STRUCTURE_VOID)) {
                            state.invisibleBlocks[index] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.STRUCTURE_VOID;
                            continue;
                        }
                        if (blockState.is(Blocks.BARRIER)) {
                            state.invisibleBlocks[index] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.BARRIER;
                            continue;
                        }
                        if (!blockState.is(Blocks.LIGHT)) continue;
                        state.invisibleBlocks[index] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.LIGHT;
                    }
                }
            }
        } else {
            state.invisibleBlocks = null;
        }
        if (state.isVisible) {
            // empty if block
        }
        state.structureVoids = null;
    }

    @Override
    public void submit(BlockEntityWithBoundingBoxRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.isVisible) {
            return;
        }
        BoundingBoxRenderable.Mode mode = state.mode;
        if (mode == BoundingBoxRenderable.Mode.NONE) {
            return;
        }
        BoundingBoxRenderable.RenderableBox box = state.box;
        BlockPos pos = box.localPos();
        Vec3i size = box.size();
        if (size.getX() < 1 || size.getY() < 1 || size.getZ() < 1) {
            return;
        }
        float lineAlpha = 1.0f;
        float lineRGB = 0.9f;
        BlockPos far = pos.offset(size);
        Gizmos.cuboid(new AABB(pos.getX(), pos.getY(), pos.getZ(), far.getX(), far.getY(), far.getZ()).move(state.blockPos), GizmoStyle.stroke(ARGB.colorFromFloat(1.0f, 0.9f, 0.9f, 0.9f)), true);
        this.renderInvisibleBlocks(state, pos, size);
    }

    private void renderInvisibleBlocks(BlockEntityWithBoundingBoxRenderState state, BlockPos localPos, Vec3i size) {
        if (state.invisibleBlocks == null) {
            return;
        }
        BlockPos entityPos = state.blockPos;
        BlockPos startingPos = entityPos.offset(localPos);
        for (int x = 0; x < size.getX(); ++x) {
            for (int y = 0; y < size.getY(); ++y) {
                for (int z = 0; z < size.getZ(); ++z) {
                    int index = z * size.getX() * size.getY() + y * size.getX() + x;
                    BlockEntityWithBoundingBoxRenderState.InvisibleBlockType invisibleBlockType = state.invisibleBlocks[index];
                    if (invisibleBlockType == null) continue;
                    float scale = invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR ? 0.05f : 0.0f;
                    double renderX0 = (float)(startingPos.getX() + x) + 0.45f - scale;
                    double renderY0 = (float)(startingPos.getY() + y) + 0.45f - scale;
                    double renderZ0 = (float)(startingPos.getZ() + z) + 0.45f - scale;
                    double renderX1 = (float)(startingPos.getX() + x) + 0.55f + scale;
                    double renderY1 = (float)(startingPos.getY() + y) + 0.55f + scale;
                    double renderZ1 = (float)(startingPos.getZ() + z) + 0.55f + scale;
                    AABB aabb = new AABB(renderX0, renderY0, renderZ0, renderX1, renderY1, renderZ1);
                    if (invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR) {
                        Gizmos.cuboid(aabb, GizmoStyle.stroke(ARGB.colorFromFloat(1.0f, 0.5f, 0.5f, 1.0f)));
                        continue;
                    }
                    if (invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.STRUCTURE_VOID) {
                        Gizmos.cuboid(aabb, GizmoStyle.stroke(ARGB.colorFromFloat(1.0f, 1.0f, 0.75f, 0.75f)));
                        continue;
                    }
                    if (invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.BARRIER) {
                        Gizmos.cuboid(aabb, GizmoStyle.stroke(-65536));
                        continue;
                    }
                    if (invisibleBlockType != BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.LIGHT) continue;
                    Gizmos.cuboid(aabb, GizmoStyle.stroke(-256));
                }
            }
        }
    }

    private void renderStructureVoids(BlockEntityWithBoundingBoxRenderState state, BlockPos startingPosition, Vec3i size) {
        if (state.structureVoids == null) {
            return;
        }
        BitSetDiscreteVoxelShape shape = new BitSetDiscreteVoxelShape(size.getX(), size.getY(), size.getZ());
        for (int x2 = 0; x2 < size.getX(); ++x2) {
            for (int y2 = 0; y2 < size.getY(); ++y2) {
                for (int z2 = 0; z2 < size.getZ(); ++z2) {
                    int index = z2 * size.getX() * size.getY() + y2 * size.getX() + x2;
                    if (!state.structureVoids[index]) continue;
                    ((DiscreteVoxelShape)shape).fill(x2, y2, z2);
                }
            }
        }
        shape.forAllFaces((direction, x, y, z) -> {
            float scale = 0.48f;
            float x0 = (float)(x + startingPosition.getX()) + 0.5f - 0.48f;
            float y0 = (float)(y + startingPosition.getY()) + 0.5f - 0.48f;
            float z0 = (float)(z + startingPosition.getZ()) + 0.5f - 0.48f;
            float x1 = (float)(x + startingPosition.getX()) + 0.5f + 0.48f;
            float y1 = (float)(y + startingPosition.getY()) + 0.5f + 0.48f;
            float z1 = (float)(z + startingPosition.getZ()) + 0.5f + 0.48f;
            Gizmos.rect(new Vec3(x0, y0, z0), new Vec3(x1, y1, z1), direction, GizmoStyle.fill(STRUCTURE_VOIDS_COLOR));
        });
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}

