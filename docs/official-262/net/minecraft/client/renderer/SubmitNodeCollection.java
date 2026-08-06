/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.feature.GizmoFeatureRenderer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.LeashFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.MovingBlockFeatureRenderer;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import net.minecraft.client.renderer.feature.ShapeOutlineFeatureRenderer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SubmitNodeCollection
implements OrderedSubmitNodeCollector {
    public final SimpleFeatureRenderPhase solid = new SimpleFeatureRenderPhase();
    public final SimpleFeatureRenderPhase shadows = new SimpleFeatureRenderPhase();
    public final SimpleFeatureRenderPhase nameTags = new SimpleFeatureRenderPhase();
    public final TranslucentFeatureRenderPhase seeThroughNameTags = new TranslucentFeatureRenderPhase();
    public final SimpleFeatureRenderPhase texts = new SimpleFeatureRenderPhase();
    public final SimpleFeatureRenderPhase shapeOutlines = new SimpleFeatureRenderPhase();
    public final TranslucentFeatureRenderPhase translucentBlocksAndItems = new TranslucentFeatureRenderPhase();
    public final TranslucentFeatureRenderPhase translucentModels = new TranslucentFeatureRenderPhase();
    public final SimpleFeatureRenderPhase translucentCustomGeometry = new SimpleFeatureRenderPhase();
    public final SimpleFeatureRenderPhase gizmos = new SimpleFeatureRenderPhase();
    public final SimpleFeatureRenderPhase breakingOverlay = new SimpleFeatureRenderPhase();
    public final SimpleFeatureRenderPhase waterMask = new SimpleFeatureRenderPhase();
    public final SimpleFeatureRenderPhase afterTerrain = new SimpleFeatureRenderPhase();
    public final SimpleFeatureRenderPhase alwaysOnTop = new SimpleFeatureRenderPhase();
    public final SimpleFeatureRenderPhase outline = new SimpleFeatureRenderPhase();
    private final List<FeatureRenderPhase<?>> allPhases = List.of(this.solid, this.shadows, this.nameTags, this.seeThroughNameTags, this.texts, this.shapeOutlines, this.translucentBlocksAndItems, this.translucentModels, this.translucentCustomGeometry, this.gizmos, this.breakingOverlay, this.waterMask, this.afterTerrain, this.alwaysOnTop, this.outline);

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
        PoseStack.Pose pose = poseStack.last();
        this.shadows.submit(new ShadowFeatureRenderer.Submit((Matrix4fc)new Matrix4f((Matrix4fc)pose.pose()), radius, pieces));
    }

    @Override
    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords, CameraRenderState camera) {
        if (nameTagAttachment == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        poseStack.pushPose();
        poseStack.translate(nameTagAttachment.x, nameTagAttachment.y + 0.5, nameTagAttachment.z);
        poseStack.mulPose((Quaternionfc)camera.orientation);
        poseStack.scale(0.025f, -0.025f, 0.025f);
        Matrix4f pose = new Matrix4f((Matrix4fc)poseStack.last().pose());
        float x = (float)(-minecraft.font.width(name)) / 2.0f;
        int backgroundColor = ARGB.color(minecraft.gameRenderer.gameRenderState().optionsRenderState.getBackgroundOpacity(0.25f), -16777216);
        if (seeThrough) {
            this.nameTags.submit(new NameTagFeatureRenderer.Submit((Matrix4fc)pose, x, offset, name, LightCoordsUtil.lightCoordsWithEmission(lightCoords, 2), -1, 0, Font.DisplayMode.NORMAL));
            this.seeThroughNameTags.submit(new NameTagFeatureRenderer.Submit((Matrix4fc)pose, x, offset, name, lightCoords, -2130706433, backgroundColor, Font.DisplayMode.SEE_THROUGH));
        } else {
            this.nameTags.submit(new NameTagFeatureRenderer.Submit((Matrix4fc)pose, x, offset, name, lightCoords, -2130706433, backgroundColor, Font.DisplayMode.NORMAL));
        }
        poseStack.popPose();
    }

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow, Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
        this.texts.submit(new TextFeatureRenderer.Submit((Matrix4fc)new Matrix4f((Matrix4fc)poseStack.last().pose()), x, y, string, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor));
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
        this.solid.submit(new FlameFeatureRenderer.Submit(poseStack.last().copy(), renderState, rotation));
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        this.solid.submit(new LeashFeatureRenderer.Submit(new Matrix4f((Matrix4fc)poseStack.last().pose()), leashState));
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor,  @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        RenderType outlineRenderType;
        PoseStack.Pose pose = poseStack.last().copy();
        if (!renderType.isOutline()) {
            ModelFeatureRenderer.Submit<? super S> submit = new ModelFeatureRenderer.Submit<S>(renderType, pose, model, state, lightCoords, overlayCoords, tintedColor, sprite, null);
            if (renderType == RenderTypes.waterMask()) {
                this.waterMask.submit(submit);
            } else if (renderType.hasBlending()) {
                this.translucentModels.submit(submit);
            } else {
                this.solid.submit(submit);
            }
        }
        if (outlineColor != 0 && (outlineRenderType = SubmitNodeCollection.getOutlineRenderType(renderType)) != null) {
            this.outline.submit(new ModelFeatureRenderer.Submit<S>(outlineRenderType, pose, model, state, 0xF000F0, OverlayTexture.NO_OVERLAY, outlineColor, sprite, null));
        }
        if (crumblingOverlay != null && renderType.affectsCrumbling()) {
            RenderType crumblingRenderType = ModelBakery.DESTROY_TYPES.get(crumblingOverlay.progress());
            this.breakingOverlay.submit(new ModelFeatureRenderer.Submit<S>(crumblingRenderType, pose, model, state, lightCoords, overlayCoords, tintedColor, null, crumblingOverlay.cameraPose()));
        }
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
        MovingBlockFeatureRenderer.Submit submit = new MovingBlockFeatureRenderer.Submit((Matrix4fc)new Matrix4f((Matrix4fc)poseStack.last().pose()), movingBlockRenderState, 0);
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(movingBlockRenderState.blockState);
        if (model.hasMaterialFlag(1)) {
            this.translucentBlocksAndItems.submit(submit);
        } else {
            this.solid.submit(submit);
        }
        if (outlineColor != 0) {
            this.outline.submit(new MovingBlockFeatureRenderer.Submit((Matrix4fc)new Matrix4f((Matrix4fc)poseStack.last().pose()), movingBlockRenderState, outlineColor));
        }
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> modelParts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        RenderType outlineRenderType;
        PoseStack.Pose pose = poseStack.last().copy();
        if (!renderType.isOutline()) {
            BlockModelFeatureRenderer.Submit submit = new BlockModelFeatureRenderer.Submit(pose, renderType, modelParts, tintLayers, lightCoords, overlayCoords, -1, null);
            if (renderType.hasBlending()) {
                this.translucentBlocksAndItems.submit(submit);
            } else {
                this.solid.submit(submit);
            }
        }
        if (outlineColor != 0 && (outlineRenderType = SubmitNodeCollection.getOutlineRenderType(renderType)) != null) {
            this.outline.submit(new BlockModelFeatureRenderer.Submit(pose, outlineRenderType, modelParts, BlockModelRenderState.EMPTY_TINTS, 0xF000F0, OverlayTexture.NO_OVERLAY, outlineColor, null));
        }
    }

    private static @Nullable RenderType getOutlineRenderType(RenderType renderType) {
        if (renderType.isOutline()) {
            return renderType;
        }
        if (renderType.outline().isPresent()) {
            return renderType.outline().get();
        }
        return null;
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress) {
        PoseStack.Pose pose = poseStack.last().copy();
        this.breakingOverlay.submit(new BlockModelFeatureRenderer.Submit(pose, ModelBakery.DESTROY_TYPES.get(progress), List.copyOf(parts), BlockModelRenderState.EMPTY_TINTS, 0xF000F0, OverlayTexture.NO_OVERLAY, 0, pose));
    }

    @Override
    public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean afterTerrain) {
        ShapeOutlineFeatureRenderer.Submit submit = new ShapeOutlineFeatureRenderer.Submit(poseStack.last().copy(), shape, renderType, color, width);
        if (afterTerrain) {
            this.afterTerrain.submit(submit);
        } else {
            this.shapeOutlines.submit(submit);
        }
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
        PoseStack.Pose pose = poseStack.last().copy();
        ItemFeatureRenderer.Submit submit = new ItemFeatureRenderer.Submit(pose, displayContext, lightCoords, overlayCoords, 0, tintLayers, quads, foilType);
        if (submit.hasTranslucency()) {
            this.translucentBlocksAndItems.submit(submit);
        } else {
            this.solid.submit(submit);
        }
        if (outlineColor != 0) {
            this.outline.submit(new ItemFeatureRenderer.Submit(pose, displayContext, 0xF000F0, OverlayTexture.NO_OVERLAY, outlineColor, ItemStackRenderState.LayerRenderState.EMPTY_TINTS, quads, ItemStackRenderState.FoilType.NONE));
        }
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
        CustomFeatureRenderer.Submit submit = new CustomFeatureRenderer.Submit(poseStack.last().copy(), renderType, customGeometryRenderer);
        if (renderType.isOutline()) {
            this.outline.submit(submit);
        } else if (renderType.hasBlending()) {
            this.translucentCustomGeometry.submit(submit);
        } else {
            this.solid.submit(submit);
        }
    }

    @Override
    public void submitQuadParticleGroup(QuadParticleRenderState particles) {
        this.solid.submit(new QuadParticleFeatureRenderer.Submit(particles, false));
        this.afterTerrain.submit(new QuadParticleFeatureRenderer.Submit(particles, true));
    }

    @Override
    public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean onTop) {
        GizmoFeatureRenderer.Submit submit = new GizmoFeatureRenderer.Submit(group, camera);
        if (onTop) {
            this.alwaysOnTop.submit(submit);
        } else {
            this.gizmos.submit(submit);
        }
    }

    public List<FeatureRenderPhase<?>> allPhases() {
        return this.allPhases;
    }
}

