/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionfc
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.DecoratedPotRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DecoratedPotRenderer
implements BlockEntityRenderer<DecoratedPotBlockEntity, DecoratedPotRenderState> {
    private static final Map<Direction, Transformation> TRANSFORMATIONS = Util.makeEnumMap(Direction.class, DecoratedPotRenderer::createModelTransformation);
    private static final Map<ResourceKey<Item>, SpriteId> DECORATED_POT_SPRITES = (Map)Util.make(() -> {
        ImmutableMap.Builder builder = ImmutableMap.builder();
        DecoratedPotPatterns.itemToPatternMappings((itemId, patternId) -> {
            Holder.Reference pattern = BuiltInRegistries.DECORATED_POT_PATTERN.getOrThrow((ResourceKey)patternId);
            builder.put(itemId, (Object)Sheets.DECORATED_POT_MAPPER.apply(((DecoratedPotPattern)pattern.value()).assetId()));
        });
        return builder.buildOrThrow();
    });
    private final SpriteGetter sprites;
    private static final String NECK = "neck";
    private static final String FRONT = "front";
    private static final String BACK = "back";
    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final String TOP = "top";
    private static final String BOTTOM = "bottom";
    private final ModelPart neck;
    private final ModelPart frontSide;
    private final ModelPart backSide;
    private final ModelPart leftSide;
    private final ModelPart rightSide;
    private final ModelPart top;
    private final ModelPart bottom;
    private static final float WOBBLE_AMPLITUDE = 0.125f;

    public DecoratedPotRenderer(BlockEntityRendererProvider.Context context) {
        this(context.entityModelSet(), context.sprites());
    }

    public DecoratedPotRenderer(SpecialModelRenderer.BakingContext context) {
        this(context.entityModelSet(), context.sprites());
    }

    public DecoratedPotRenderer(EntityModelSet entityModelSet, SpriteGetter sprites) {
        this.sprites = sprites;
        ModelPart baseRoot = entityModelSet.bakeLayer(ModelLayers.DECORATED_POT_BASE);
        this.neck = baseRoot.getChild(NECK);
        this.top = baseRoot.getChild(TOP);
        this.bottom = baseRoot.getChild(BOTTOM);
        ModelPart sidesRoot = entityModelSet.bakeLayer(ModelLayers.DECORATED_POT_SIDES);
        this.frontSide = sidesRoot.getChild(FRONT);
        this.backSide = sidesRoot.getChild(BACK);
        this.leftSide = sidesRoot.getChild(LEFT);
        this.rightSide = sidesRoot.getChild(RIGHT);
    }

    public static LayerDefinition createBaseLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation inflate = new CubeDeformation(0.2f);
        CubeDeformation deflate = new CubeDeformation(-0.1f);
        root.addOrReplaceChild(NECK, CubeListBuilder.create().texOffs(0, 0).addBox(4.0f, 17.0f, 4.0f, 8.0f, 3.0f, 8.0f, deflate).texOffs(0, 5).addBox(5.0f, 20.0f, 5.0f, 6.0f, 1.0f, 6.0f, inflate), PartPose.offsetAndRotation(0.0f, 37.0f, 16.0f, (float)Math.PI, 0.0f, 0.0f));
        CubeListBuilder topBottomPlane = CubeListBuilder.create().texOffs(-14, 13).addBox(0.0f, 0.0f, 0.0f, 14.0f, 0.0f, 14.0f);
        root.addOrReplaceChild(TOP, topBottomPlane, PartPose.offsetAndRotation(1.0f, 16.0f, 1.0f, 0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild(BOTTOM, topBottomPlane, PartPose.offsetAndRotation(1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh, 32, 32);
    }

    public static LayerDefinition createSidesLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder sidePlane = CubeListBuilder.create().texOffs(1, 0).addBox(0.0f, 0.0f, 0.0f, 14.0f, 16.0f, 0.0f, EnumSet.of(Direction.NORTH));
        root.addOrReplaceChild(BACK, sidePlane, PartPose.offsetAndRotation(15.0f, 16.0f, 1.0f, 0.0f, 0.0f, (float)Math.PI));
        root.addOrReplaceChild(LEFT, sidePlane, PartPose.offsetAndRotation(1.0f, 16.0f, 1.0f, 0.0f, -1.5707964f, (float)Math.PI));
        root.addOrReplaceChild(RIGHT, sidePlane, PartPose.offsetAndRotation(15.0f, 16.0f, 15.0f, 0.0f, 1.5707964f, (float)Math.PI));
        root.addOrReplaceChild(FRONT, sidePlane, PartPose.offsetAndRotation(1.0f, 16.0f, 15.0f, (float)Math.PI, 0.0f, 0.0f));
        return LayerDefinition.create(mesh, 16, 16);
    }

    private static SpriteId getSideSprite(Optional<Item> item) {
        SpriteId result;
        if (item.isPresent() && (result = DECORATED_POT_SPRITES.get(item.get().builtInRegistryHolder().key())) != null) {
            return result;
        }
        return Sheets.DECORATED_POT_SIDE;
    }

    @Override
    public DecoratedPotRenderState createRenderState() {
        return new DecoratedPotRenderState();
    }

    @Override
    public void extractRenderState(DecoratedPotBlockEntity blockEntity, DecoratedPotRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.decorations = blockEntity.getDecorations();
        state.direction = blockEntity.getDirection();
        DecoratedPotBlockEntity.WobbleStyle wobbleStyle = blockEntity.lastWobbleStyle;
        state.wobbleProgress = wobbleStyle != null && blockEntity.getLevel() != null ? ((float)(blockEntity.getLevel().getGameTime() - blockEntity.wobbleStartedAtTick) + partialTicks) / (float)wobbleStyle.duration : 0.0f;
    }

    @Override
    public void submit(DecoratedPotRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(DecoratedPotRenderer.modelTransformation(state.direction));
        if (state.wobbleProgress >= 0.0f && state.wobbleProgress <= 1.0f) {
            if (state.wobbleStyle == DecoratedPotBlockEntity.WobbleStyle.POSITIVE) {
                float amplitude = 0.015625f;
                float deltaTime = state.wobbleProgress * ((float)Math.PI * 2);
                float tiltX = -1.5f * (Mth.cos(deltaTime) + 0.5f) * Mth.sin(deltaTime / 2.0f);
                poseStack.rotateAround((Quaternionfc)Axis.XP.rotation(tiltX * 0.015625f), 0.5f, 0.0f, 0.5f);
                float tiltZ = Mth.sin(deltaTime);
                poseStack.rotateAround((Quaternionfc)Axis.ZP.rotation(tiltZ * 0.015625f), 0.5f, 0.0f, 0.5f);
            } else {
                float turnAngle = Mth.sin(-state.wobbleProgress * 3.0f * (float)Math.PI) * 0.125f;
                float linearDecayFactor = 1.0f - state.wobbleProgress;
                poseStack.rotateAround((Quaternionfc)Axis.YP.rotation(turnAngle * linearDecayFactor), 0.5f, 0.0f, 0.5f);
            }
        }
        this.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.decorations, 0);
        poseStack.popPose();
    }

    public static Transformation modelTransformation(Direction facing) {
        return TRANSFORMATIONS.get(facing);
    }

    private static Transformation createModelTransformation(Direction entityDirection) {
        return new Transformation((Matrix4fc)new Matrix4f().rotateAround((Quaternionfc)Axis.YP.rotationDegrees(180.0f - entityDirection.toYRot()), 0.5f, 0.5f, 0.5f));
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, PotDecorations decorations, int outlineColor) {
        RenderType renderType = Sheets.DECORATED_POT_BASE.renderType(RenderTypes::entitySolid);
        TextureAtlasSprite sprite = this.sprites.get(Sheets.DECORATED_POT_BASE);
        submitNodeCollector.submitModelPart(this.neck, poseStack, renderType, lightCoords, overlayCoords, sprite, -1, null, outlineColor);
        submitNodeCollector.submitModelPart(this.top, poseStack, renderType, lightCoords, overlayCoords, sprite, -1, null, outlineColor);
        submitNodeCollector.submitModelPart(this.bottom, poseStack, renderType, lightCoords, overlayCoords, sprite, -1, null, outlineColor);
        SpriteId frontSprite = DecoratedPotRenderer.getSideSprite(decorations.front());
        submitNodeCollector.submitModelPart(this.frontSide, poseStack, frontSprite.renderType(RenderTypes::entitySolid), lightCoords, overlayCoords, this.sprites.get(frontSprite), -1, null, outlineColor);
        SpriteId backSprite = DecoratedPotRenderer.getSideSprite(decorations.back());
        submitNodeCollector.submitModelPart(this.backSide, poseStack, backSprite.renderType(RenderTypes::entitySolid), lightCoords, overlayCoords, this.sprites.get(backSprite), -1, null, outlineColor);
        SpriteId leftSprite = DecoratedPotRenderer.getSideSprite(decorations.left());
        submitNodeCollector.submitModelPart(this.leftSide, poseStack, leftSprite.renderType(RenderTypes::entitySolid), lightCoords, overlayCoords, this.sprites.get(leftSprite), -1, null, outlineColor);
        SpriteId rightSprite = DecoratedPotRenderer.getSideSprite(decorations.right());
        submitNodeCollector.submitModelPart(this.rightSide, poseStack, rightSprite.renderType(RenderTypes::entitySolid), lightCoords, overlayCoords, this.sprites.get(rightSprite), -1, null, outlineColor);
    }

    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.neck.getExtentsForGui(poseStack, output);
        this.top.getExtentsForGui(poseStack, output);
        this.bottom.getExtentsForGui(poseStack, output);
    }
}

