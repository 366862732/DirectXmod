/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@Environment(value=EnvType.CLIENT)
public class FluidRenderer {
    private static final float MAX_FLUID_HEIGHT = 0.8888889f;
    private final FluidStateModelSet fluidModels;

    public FluidRenderer(FluidStateModelSet fluidModels) {
        this.fluidModels = fluidModels;
    }

    private static boolean isNeighborSameFluid(FluidState fluidState, FluidState neighborFluidState) {
        return neighborFluidState.getType().isSame(fluidState.getType());
    }

    private static boolean isFaceOccludedByState(Direction direction, float height, BlockState state) {
        VoxelShape occluder = state.getFaceOcclusionShape(direction.getOpposite());
        if (occluder == Shapes.empty()) {
            return false;
        }
        if (occluder == Shapes.block()) {
            boolean fullBlock = height == 1.0f;
            return direction != Direction.UP || fullBlock;
        }
        VoxelShape shape = Shapes.box(0.0, 0.0, 0.0, 1.0, height, 1.0);
        return Shapes.blockOccludes(shape, occluder, direction);
    }

    private static boolean isFaceOccludedByNeighbor(Direction direction, float height, BlockState neighborState) {
        return FluidRenderer.isFaceOccludedByState(direction, height, neighborState);
    }

    private static boolean isFaceOccludedBySelf(BlockState state, Direction direction) {
        return FluidRenderer.isFaceOccludedByState(direction.getOpposite(), 1.0f, state);
    }

    public static boolean shouldRenderFace(FluidState fluidState, BlockState blockState, Direction direction, FluidState neighborFluidState) {
        return !FluidRenderer.isNeighborSameFluid(fluidState, neighborFluidState) && !FluidRenderer.isFaceOccludedBySelf(blockState, direction);
    }

    public void tesselate(BlockAndTintGetter level, BlockPos pos, Output output, BlockState blockState, FluidState fluidState) {
        float bottomOffs;
        float heightSouthWest;
        float heightSouthEast;
        float heightNorthWest;
        float heightNorthEast;
        BlockState blockStateDown = level.getBlockState(pos.relative(Direction.DOWN));
        FluidState fluidStateDown = blockStateDown.getFluidState();
        BlockState blockStateUp = level.getBlockState(pos.relative(Direction.UP));
        FluidState fluidStateUp = blockStateUp.getFluidState();
        BlockState blockStateNorth = level.getBlockState(pos.relative(Direction.NORTH));
        FluidState fluidStateNorth = blockStateNorth.getFluidState();
        BlockState blockStateSouth = level.getBlockState(pos.relative(Direction.SOUTH));
        FluidState fluidStateSouth = blockStateSouth.getFluidState();
        BlockState blockStateWest = level.getBlockState(pos.relative(Direction.WEST));
        FluidState fluidStateWest = blockStateWest.getFluidState();
        BlockState blockStateEast = level.getBlockState(pos.relative(Direction.EAST));
        FluidState fluidStateEast = blockStateEast.getFluidState();
        boolean renderUp = !FluidRenderer.isNeighborSameFluid(fluidState, fluidStateUp);
        boolean renderDown = FluidRenderer.shouldRenderFace(fluidState, blockState, Direction.DOWN, fluidStateDown) && !FluidRenderer.isFaceOccludedByNeighbor(Direction.DOWN, 0.8888889f, blockStateDown);
        boolean renderNorth = FluidRenderer.shouldRenderFace(fluidState, blockState, Direction.NORTH, fluidStateNorth);
        boolean renderSouth = FluidRenderer.shouldRenderFace(fluidState, blockState, Direction.SOUTH, fluidStateSouth);
        boolean renderWest = FluidRenderer.shouldRenderFace(fluidState, blockState, Direction.WEST, fluidStateWest);
        boolean renderEast = FluidRenderer.shouldRenderFace(fluidState, blockState, Direction.EAST, fluidStateEast);
        if (!(renderUp || renderDown || renderEast || renderWest || renderNorth || renderSouth)) {
            return;
        }
        FluidModel model = this.fluidModels.get(fluidState);
        VertexConsumer builder = output.getBuilder(model.layer());
        int tintColor = model.tintSource() != null ? model.tintSource().colorInWorld(blockState, level, pos) : -1;
        CardinalLighting cardinalLighting = level.cardinalLighting();
        Fluid type = fluidState.getType();
        float heightSelf = this.getHeight(level, type, pos, blockState, fluidState);
        if (heightSelf >= 1.0f) {
            heightNorthEast = 1.0f;
            heightNorthWest = 1.0f;
            heightSouthEast = 1.0f;
            heightSouthWest = 1.0f;
        } else {
            float heightNorth = this.getHeight(level, type, pos.north(), blockStateNorth, fluidStateNorth);
            float heightSouth = this.getHeight(level, type, pos.south(), blockStateSouth, fluidStateSouth);
            float heightEast = this.getHeight(level, type, pos.east(), blockStateEast, fluidStateEast);
            float heightWest = this.getHeight(level, type, pos.west(), blockStateWest, fluidStateWest);
            heightNorthEast = this.calculateAverageHeight(level, type, heightSelf, heightNorth, heightEast, pos.relative(Direction.NORTH).relative(Direction.EAST));
            heightNorthWest = this.calculateAverageHeight(level, type, heightSelf, heightNorth, heightWest, pos.relative(Direction.NORTH).relative(Direction.WEST));
            heightSouthEast = this.calculateAverageHeight(level, type, heightSelf, heightSouth, heightEast, pos.relative(Direction.SOUTH).relative(Direction.EAST));
            heightSouthWest = this.calculateAverageHeight(level, type, heightSelf, heightSouth, heightWest, pos.relative(Direction.SOUTH).relative(Direction.WEST));
        }
        float x = pos.getX() & 0xF;
        float y = pos.getY() & 0xF;
        float z = pos.getZ() & 0xF;
        float offs = 0.001f;
        float f = bottomOffs = renderDown ? 0.001f : 0.0f;
        if (renderUp && !FluidRenderer.isFaceOccludedByNeighbor(Direction.UP, Math.min(Math.min(heightNorthWest, heightSouthWest), Math.min(heightSouthEast, heightNorthEast)), blockStateUp)) {
            float v11;
            float u11;
            float v10;
            float u10;
            float v01;
            float u01;
            float v00;
            float u00;
            heightNorthWest -= 0.001f;
            heightSouthWest -= 0.001f;
            heightSouthEast -= 0.001f;
            heightNorthEast -= 0.001f;
            Vec3 flow = fluidState.getFlow(level, pos);
            if (flow.x == 0.0 && flow.z == 0.0) {
                TextureAtlasSprite stillSprite = model.stillMaterial().sprite();
                u00 = stillSprite.getU0();
                v00 = stillSprite.getV0();
                u01 = u00;
                v01 = stillSprite.getV1();
                u10 = stillSprite.getU1();
                v10 = v01;
                u11 = u10;
                v11 = v00;
            } else {
                float angle = (float)Mth.atan2(flow.z, flow.x) - 1.5707964f;
                float s = Mth.sin(angle) * 0.25f;
                float c = Mth.cos(angle) * 0.25f;
                float cc = 0.5f;
                TextureAtlasSprite flowingSprite = model.flowingMaterial().sprite();
                u00 = flowingSprite.getU(0.5f + (-c - s));
                v00 = flowingSprite.getV(0.5f + (-c + s));
                u01 = flowingSprite.getU(0.5f + (-c + s));
                v01 = flowingSprite.getV(0.5f + (c + s));
                u10 = flowingSprite.getU(0.5f + (c + s));
                v10 = flowingSprite.getV(0.5f + (c - s));
                u11 = flowingSprite.getU(0.5f + (c - s));
                v11 = flowingSprite.getV(0.5f + (-c - s));
            }
            int topLightCoords = this.getLightCoords(level, pos);
            int topColor = ARGB.scaleRGB(tintColor, cardinalLighting.up());
            this.addFace(builder, x + 0.0f, y + heightNorthWest, z + 0.0f, u00, v00, x + 0.0f, y + heightSouthWest, z + 1.0f, u01, v01, x + 1.0f, y + heightSouthEast, z + 1.0f, u10, v10, x + 1.0f, y + heightNorthEast, z + 0.0f, u11, v11, topColor, topLightCoords, fluidState.shouldRenderBackwardUpFace(level, pos.above()));
        }
        if (renderDown) {
            TextureAtlasSprite stillSprite = model.stillMaterial().sprite();
            float u0 = stillSprite.getU0();
            float u1 = stillSprite.getU1();
            float v0 = stillSprite.getV0();
            float v1 = stillSprite.getV1();
            int belowLightCoords = this.getLightCoords(level, pos.below());
            int belowColor = ARGB.scaleRGB(tintColor, cardinalLighting.down());
            this.addFace(builder, x, y + bottomOffs, z, u0, v0, x + 1.0f, y + bottomOffs, z, u1, v0, x + 1.0f, y + bottomOffs, z + 1.0f, u1, v1, x, y + bottomOffs, z + 1.0f, u0, v1, belowColor, belowLightCoords, false);
        }
        int sideLightCoords = this.getLightCoords(level, pos);
        for (Direction faceDir : Direction.Plane.HORIZONTAL) {
            Block relativeBlock;
            boolean renderCondition;
            float z1;
            float z0;
            float x1;
            float x0;
            float hh1;
            float hh0;
            BlockState faceState = switch (faceDir) {
                case Direction.NORTH -> {
                    hh0 = heightNorthWest;
                    hh1 = heightNorthEast;
                    x0 = x;
                    x1 = x + 1.0f;
                    z0 = z + 0.001f;
                    z1 = z + 0.001f;
                    renderCondition = renderNorth;
                    yield blockStateNorth;
                }
                case Direction.SOUTH -> {
                    hh0 = heightSouthEast;
                    hh1 = heightSouthWest;
                    x0 = x + 1.0f;
                    x1 = x;
                    z0 = z + 1.0f - 0.001f;
                    z1 = z + 1.0f - 0.001f;
                    renderCondition = renderSouth;
                    yield blockStateSouth;
                }
                case Direction.WEST -> {
                    hh0 = heightSouthWest;
                    hh1 = heightNorthWest;
                    x0 = x + 0.001f;
                    x1 = x + 0.001f;
                    z0 = z + 1.0f;
                    z1 = z;
                    renderCondition = renderWest;
                    yield blockStateWest;
                }
                case Direction.EAST -> {
                    hh0 = heightNorthEast;
                    hh1 = heightSouthEast;
                    x0 = x + 1.0f - 0.001f;
                    x1 = x + 1.0f - 0.001f;
                    z0 = z;
                    z1 = z + 1.0f;
                    renderCondition = renderEast;
                    yield blockStateEast;
                }
                default -> throw new UnsupportedOperationException();
            };
            if (!renderCondition || FluidRenderer.isFaceOccludedByNeighbor(faceDir, Math.max(hh0, hh1), faceState)) continue;
            TextureAtlasSprite sprite = model.flowingMaterial().sprite();
            boolean isOverlay = false;
            if (model.overlayMaterial() != null && ((relativeBlock = faceState.getBlock()) instanceof HalfTransparentBlock || relativeBlock instanceof LeavesBlock)) {
                sprite = model.overlayMaterial().sprite();
                isOverlay = true;
            }
            float u0 = sprite.getU(0.0f);
            float u1 = sprite.getU(0.5f);
            float v01 = sprite.getV((1.0f - hh0) * 0.5f);
            float v02 = sprite.getV((1.0f - hh1) * 0.5f);
            float v1 = sprite.getV(0.5f);
            float shadeSide = faceDir.getAxis() == Direction.Axis.Z ? cardinalLighting.north() : cardinalLighting.west();
            int faceColor = ARGB.scaleRGB(tintColor, cardinalLighting.up() * shadeSide);
            this.addFace(builder, x0, y + hh0, z0, u0, v01, x1, y + hh1, z1, u1, v02, x1, y + bottomOffs, z1, u1, v1, x0, y + bottomOffs, z0, u0, v1, faceColor, sideLightCoords, !isOverlay);
        }
    }

    private void addFace(VertexConsumer builder, float x0, float y0, float z0, float u0, float v0, float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2, float x3, float y3, float z3, float u3, float v3, int color, int lightCoords, boolean addBackFace) {
        this.vertex(builder, x0, y0, z0, color, u0, v0, lightCoords);
        this.vertex(builder, x1, y1, z1, color, u1, v1, lightCoords);
        this.vertex(builder, x2, y2, z2, color, u2, v2, lightCoords);
        this.vertex(builder, x3, y3, z3, color, u3, v3, lightCoords);
        if (addBackFace) {
            this.vertex(builder, x0, y0, z0, color, u0, v0, lightCoords);
            this.vertex(builder, x3, y3, z3, color, u3, v3, lightCoords);
            this.vertex(builder, x2, y2, z2, color, u2, v2, lightCoords);
            this.vertex(builder, x1, y1, z1, color, u1, v1, lightCoords);
        }
    }

    private float calculateAverageHeight(BlockAndTintGetter level, Fluid type, float heightSelf, float height2, float height1, BlockPos cornerPos) {
        if (height1 >= 1.0f || height2 >= 1.0f) {
            return 1.0f;
        }
        float[] weightedHeight = new float[2];
        if (height1 > 0.0f || height2 > 0.0f) {
            float heightCorner = this.getHeight(level, type, cornerPos);
            if (heightCorner >= 1.0f) {
                return 1.0f;
            }
            this.addWeightedHeight(weightedHeight, heightCorner);
        }
        this.addWeightedHeight(weightedHeight, heightSelf);
        this.addWeightedHeight(weightedHeight, height1);
        this.addWeightedHeight(weightedHeight, height2);
        return weightedHeight[0] / weightedHeight[1];
    }

    private void addWeightedHeight(float[] weightedHeight, float height) {
        if (height >= 0.8f) {
            weightedHeight[0] = weightedHeight[0] + height * 10.0f;
            weightedHeight[1] = weightedHeight[1] + 10.0f;
        } else if (height >= 0.0f) {
            weightedHeight[0] = weightedHeight[0] + height;
            weightedHeight[1] = weightedHeight[1] + 1.0f;
        }
    }

    private float getHeight(BlockAndTintGetter level, Fluid fluidType, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return this.getHeight(level, fluidType, pos, state, state.getFluidState());
    }

    private float getHeight(BlockAndTintGetter level, Fluid fluidType, BlockPos pos, BlockState state, FluidState fluidState) {
        if (fluidType.isSame(fluidState.getType())) {
            BlockState aboveState = level.getBlockState(pos.above());
            if (fluidType.isSame(aboveState.getFluidState().getType())) {
                return 1.0f;
            }
            return fluidState.getOwnHeight();
        }
        if (!state.isSolid()) {
            return 0.0f;
        }
        return -1.0f;
    }

    private void vertex(VertexConsumer builder, float x, float y, float z, int color, float u, float v, int lightCoords) {
        builder.addVertex(x, y, z, color, u, v, OverlayTexture.NO_OVERLAY, lightCoords, 0.0f, 1.0f, 0.0f);
    }

    private int getLightCoords(BlockAndTintGetter level, BlockPos pos) {
        return LightCoordsUtil.max(LightCoordsUtil.getLightCoords(level, pos), LightCoordsUtil.getLightCoords(level, pos.above()));
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface Output {
        public VertexConsumer getBuilder(ChunkSectionLayer var1);
    }
}

