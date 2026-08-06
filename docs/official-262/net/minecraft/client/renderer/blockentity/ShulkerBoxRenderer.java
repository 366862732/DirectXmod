/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionfc
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ShulkerBoxRenderer
implements BlockEntityRenderer<ShulkerBoxBlockEntity, ShulkerBoxRenderState> {
    private static final Map<Direction, Transformation> TRANSFORMATIONS = Util.makeEnumMap(Direction.class, ShulkerBoxRenderer::createModelTransform);
    private final SpriteGetter sprites;
    private final ShulkerBoxModel model;

    public ShulkerBoxRenderer(BlockEntityRendererProvider.Context context) {
        this(context.entityModelSet(), context.sprites());
    }

    public ShulkerBoxRenderer(SpecialModelRenderer.BakingContext context) {
        this(context.entityModelSet(), context.sprites());
    }

    public ShulkerBoxRenderer(EntityModelSet context, SpriteGetter sprites) {
        this.sprites = sprites;
        this.model = new ShulkerBoxModel(context.bakeLayer(ModelLayers.SHULKER_BOX));
    }

    @Override
    public ShulkerBoxRenderState createRenderState() {
        return new ShulkerBoxRenderState();
    }

    @Override
    public void extractRenderState(ShulkerBoxBlockEntity blockEntity, ShulkerBoxRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.direction = blockEntity.getBlockState().getValueOrElse(ShulkerBoxBlock.FACING, Direction.UP);
        state.color = blockEntity.getColor();
        state.progress = blockEntity.getProgress(partialTicks);
    }

    @Override
    public void submit(ShulkerBoxRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        DyeColor color = state.color;
        SpriteId sprite = color == null ? Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION : Sheets.getShulkerBoxSprite(color);
        this.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.direction, state.progress, state.breakProgress, sprite, 0);
    }

    private void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, Direction direction, float progress,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress, SpriteId sprite, int outlineColor) {
        poseStack.pushPose();
        poseStack.mulPose(ShulkerBoxRenderer.modelTransform(direction));
        this.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, progress, breakProgress, sprite, outlineColor);
        poseStack.popPose();
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, float progress,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress, SpriteId sprite, int outlineColor) {
        this.model.setupAnim(Float.valueOf(progress));
        submitNodeCollector.submitModel(this.model, Float.valueOf(progress), poseStack, lightCoords, overlayCoords, -1, sprite, this.sprites, outlineColor, breakProgress);
    }

    private static Transformation createModelTransform(Direction direction) {
        float scale = 0.9995f;
        return new Transformation((Matrix4fc)new Matrix4f().translation(0.5f, 0.5f, 0.5f).scale(0.9995f, 0.9995f, 0.9995f).rotate((Quaternionfc)direction.getRotation()).scale(1.0f, -1.0f, -1.0f).translate(0.0f, -1.0f, 0.0f));
    }

    public static Transformation modelTransform(Direction direction) {
        return TRANSFORMATIONS.get(direction);
    }

    public void getExtents(float progress, Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.model.setupAnim(Float.valueOf(progress));
        this.model.root().getExtentsForGui(poseStack, output);
    }

    @Environment(value=EnvType.CLIENT)
    private static class ShulkerBoxModel
    extends Model<Float> {
        private final ModelPart lid;

        public ShulkerBoxModel(ModelPart root) {
            super(root, RenderTypes::entityCutout);
            this.lid = root.getChild("lid");
        }

        @Override
        public void setupAnim(Float progress) {
            super.setupAnim(progress);
            this.lid.setPos(0.0f, 24.0f - progress.floatValue() * 0.5f * 16.0f, 0.0f);
            this.lid.yRot = 270.0f * progress.floatValue() * ((float)Math.PI / 180);
        }
    }
}

