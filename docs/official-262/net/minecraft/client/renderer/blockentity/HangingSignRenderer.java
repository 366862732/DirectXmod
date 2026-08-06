/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity;

import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.WallAndGroundTransformations;
import net.minecraft.client.renderer.blockentity.state.HangingSignRenderState;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.HangingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class HangingSignRenderer
extends AbstractSignRenderer<HangingSignRenderState> {
    private static final float TEXT_RENDER_SCALE = 0.9f;
    private static final Vector3fc TEXT_OFFSET = new Vector3f(0.0f, -0.32f, 0.073f);
    public static final WallAndGroundTransformations<SignRenderState.SignTransformations> TRANSFORMATIONS = new WallAndGroundTransformations<SignRenderState.SignTransformations>(HangingSignRenderer::createWallTransformation, HangingSignRenderer::createGroundTransformation, 16);

    public HangingSignRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public HangingSignRenderState createRenderState() {
        return new HangingSignRenderState();
    }

    @Override
    public void extractRenderState(SignBlockEntity blockEntity, HangingSignRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = blockEntity.getBlockState();
        state.attachmentType = HangingSignBlock.getAttachmentPoint(blockState);
        state.transformations = blockState.getBlock() instanceof WallHangingSignBlock ? TRANSFORMATIONS.wallTransformation(blockState.getValue(WallHangingSignBlock.FACING)) : TRANSFORMATIONS.freeTransformations(blockState.getValue(CeilingHangingSignBlock.ROTATION));
    }

    private static Transformation textTransformation(float angle, boolean isFrontText) {
        Matrix4f result = new Matrix4f().translation(0.5f, 0.9375f, 0.5f).rotate((Quaternionfc)Axis.YP.rotationDegrees(-angle)).translate(0.0f, -0.3125f, 0.0f);
        if (!isFrontText) {
            result.rotate((Quaternionfc)Axis.YP.rotationDegrees(180.0f));
        }
        float s = 0.0140625f;
        result.translate(TEXT_OFFSET);
        result.scale(0.0140625f, -0.0140625f, 0.0140625f);
        return new Transformation((Matrix4fc)result);
    }

    private static SignRenderState.SignTransformations createTransformations(float angle) {
        return new SignRenderState.SignTransformations(HangingSignRenderer.textTransformation(angle, true), HangingSignRenderer.textTransformation(angle, false));
    }

    private static SignRenderState.SignTransformations createGroundTransformation(int segment) {
        return HangingSignRenderer.createTransformations(RotationSegment.convertToDegrees(segment));
    }

    private static SignRenderState.SignTransformations createWallTransformation(Direction direction) {
        return HangingSignRenderer.createTransformations(direction.toYRot());
    }
}

