/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.model.object.banner.BannerModel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.WallAndGroundTransformations;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Unit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class BannerRenderer
implements BlockEntityRenderer<BannerBlockEntity, BannerRenderState> {
    private static final int MAX_PATTERNS = 16;
    private static final float SIZE = 0.6666667f;
    private static final Vector3fc MODEL_SCALE = new Vector3f(0.6666667f, -0.6666667f, -0.6666667f);
    private static final Vector3fc MODEL_TRANSLATION = new Vector3f(0.5f, 0.0f, 0.5f);
    public static final WallAndGroundTransformations<Transformation> TRANSFORMATIONS = new WallAndGroundTransformations<Transformation>(BannerRenderer::createWallTransformation, BannerRenderer::createGroundTransformation, 16);
    private final SpriteGetter sprites;
    private final BannerModel standingModel;
    private final BannerModel wallModel;
    private final BannerFlagModel standingFlagModel;
    private final BannerFlagModel wallFlagModel;

    public BannerRenderer(BlockEntityRendererProvider.Context context) {
        this(context.entityModelSet(), context.sprites());
    }

    public BannerRenderer(SpecialModelRenderer.BakingContext context) {
        this(context.entityModelSet(), context.sprites());
    }

    public BannerRenderer(EntityModelSet modelSet, SpriteGetter sprites) {
        this.sprites = sprites;
        this.standingModel = new BannerModel(modelSet.bakeLayer(ModelLayers.STANDING_BANNER));
        this.wallModel = new BannerModel(modelSet.bakeLayer(ModelLayers.WALL_BANNER));
        this.standingFlagModel = new BannerFlagModel(modelSet.bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
        this.wallFlagModel = new BannerFlagModel(modelSet.bakeLayer(ModelLayers.WALL_BANNER_FLAG));
    }

    @Override
    public BannerRenderState createRenderState() {
        return new BannerRenderState();
    }

    @Override
    public void extractRenderState(BannerBlockEntity blockEntity, BannerRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.baseColor = blockEntity.getBaseColor();
        state.patterns = blockEntity.getPatterns();
        BlockState blockState = blockEntity.getBlockState();
        if (blockState.getBlock() instanceof BannerBlock) {
            state.transformation = TRANSFORMATIONS.freeTransformations(blockState.getValue(BannerBlock.ROTATION));
            state.attachmentType = BannerBlock.AttachmentType.GROUND;
        } else {
            state.transformation = TRANSFORMATIONS.wallTransformation(blockState.getValue(WallBannerBlock.FACING));
            state.attachmentType = BannerBlock.AttachmentType.WALL;
        }
        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;
        BlockPos blockPos = blockEntity.getBlockPos();
        state.phase = ((float)Math.floorMod((long)(blockPos.getX() * 7 + blockPos.getY() * 9 + blockPos.getZ() * 13) + gameTime, 100L) + partialTicks) / 100.0f;
    }

    private BannerModel bannerModel(BannerBlock.AttachmentType type) {
        return switch (type) {
            default -> throw new MatchException(null, null);
            case BannerBlock.AttachmentType.WALL -> this.wallModel;
            case BannerBlock.AttachmentType.GROUND -> this.standingModel;
        };
    }

    private BannerFlagModel flagModel(BannerBlock.AttachmentType type) {
        return switch (type) {
            default -> throw new MatchException(null, null);
            case BannerBlock.AttachmentType.WALL -> this.wallFlagModel;
            case BannerBlock.AttachmentType.GROUND -> this.standingFlagModel;
        };
    }

    @Override
    public void submit(BannerRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(state.transformation);
        BannerRenderer.submitBanner(this.sprites, poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, this.bannerModel(state.attachmentType), this.flagModel(state.attachmentType), state.phase, state.baseColor, state.patterns, state.breakProgress, 0);
        poseStack.popPose();
    }

    public void submitSpecial(BannerBlock.AttachmentType type, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, DyeColor baseColor, BannerPatternLayers patterns, int outlineColor) {
        BannerRenderer.submitBanner(this.sprites, poseStack, submitNodeCollector, lightCoords, overlayCoords, this.bannerModel(type), this.flagModel(type), 0.0f, baseColor, patterns, null, outlineColor);
    }

    private static void submitBanner(SpriteGetter sprites, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, BannerModel model, BannerFlagModel flagModel, float phase, DyeColor baseColor, BannerPatternLayers patterns,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress, int outlineColor) {
        SpriteId sprite = Sheets.BANNER_BASE;
        submitNodeCollector.submitModel(model, Unit.INSTANCE, poseStack, lightCoords, overlayCoords, -1, sprite, sprites, outlineColor, breakProgress);
        submitNodeCollector.submitModel(flagModel, Float.valueOf(phase), poseStack, lightCoords, overlayCoords, -1, sprite, sprites, outlineColor, breakProgress);
        BannerRenderer.submitPatterns(sprites, poseStack, submitNodeCollector, lightCoords, overlayCoords, flagModel, Float.valueOf(phase), true, baseColor, patterns, breakProgress);
    }

    public static <S> void submitPatterns(SpriteGetter sprites, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, Model<S> model, S state, boolean banner, DyeColor baseColor, BannerPatternLayers patterns,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BannerRenderer.submitPatternLayer(sprites, poseStack, submitNodeCollector, lightCoords, overlayCoords, model, state, banner ? Sheets.BANNER_PATTERN_BASE : Sheets.SHIELD_PATTERN_BASE, baseColor, breakProgress);
        for (int maskIndex = 0; maskIndex < 16 && maskIndex < patterns.layers().size(); ++maskIndex) {
            BannerPatternLayers.Layer layer = patterns.layers().get(maskIndex);
            SpriteId sprite = banner ? Sheets.getBannerSprite(layer.pattern()) : Sheets.getShieldSprite(layer.pattern());
            BannerRenderer.submitPatternLayer(sprites, poseStack, submitNodeCollector.order(maskIndex + 1), lightCoords, overlayCoords, model, state, sprite, layer.color(), null);
        }
    }

    private static <S> void submitPatternLayer(SpriteGetter sprites, PoseStack poseStack, OrderedSubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, Model<S> model, S state, SpriteId sprite, DyeColor color,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        int diffuseColor = color.getTextureDiffuseColor();
        submitNodeCollector.submitModel(model, state, poseStack, sprite.renderType(RenderTypes::bannerPattern), lightCoords, overlayCoords, diffuseColor, sprites.get(sprite), 0, breakProgress);
    }

    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.standingModel.root().getExtentsForGui(poseStack, output);
        this.standingFlagModel.setupAnim(Float.valueOf(0.0f));
        this.standingFlagModel.root().getExtentsForGui(poseStack, output);
    }

    private static Transformation modelTransformation(float angle) {
        return new Transformation(MODEL_TRANSLATION, (Quaternionfc)Axis.YP.rotationDegrees(-angle), MODEL_SCALE, null);
    }

    private static Transformation createGroundTransformation(int segment) {
        return BannerRenderer.modelTransformation(RotationSegment.convertToDegrees(segment));
    }

    private static Transformation createWallTransformation(Direction direction) {
        return BannerRenderer.modelTransformation(direction.toYRot());
    }
}

