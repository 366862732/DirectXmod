/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.DisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public abstract class DisplayRenderer<T extends Display, S, ST extends DisplayEntityRenderState>
extends EntityRenderer<T, ST> {
    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private final EntityRenderDispatcher entityRenderDispatcher;
    protected final BlockModelResolver blockModelResolver;

    protected DisplayRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.entityRenderDispatcher = context.getEntityRenderDispatcher();
        this.blockModelResolver = context.getBlockModelResolver();
    }

    @Override
    protected AABB getBoundingBoxForCulling(T entity) {
        return ((Display)entity).getBoundingBoxForCulling();
    }

    @Override
    protected boolean affectedByCulling(T entity) {
        return ((Display)entity).affectedByCulling();
    }

    private static int getBrightnessOverride(Display entity) {
        Display.RenderState renderState = entity.renderState();
        return renderState != null ? renderState.brightnessOverride() : -1;
    }

    @Override
    protected int getSkyLightLevel(T entity, BlockPos blockPos) {
        int packedBrightnessOverride = DisplayRenderer.getBrightnessOverride(entity);
        if (packedBrightnessOverride != -1) {
            return LightCoordsUtil.sky(packedBrightnessOverride);
        }
        return super.getSkyLightLevel(entity, blockPos);
    }

    @Override
    protected int getBlockLightLevel(T entity, BlockPos blockPos) {
        int packedBrightnessOverride = DisplayRenderer.getBrightnessOverride(entity);
        if (packedBrightnessOverride != -1) {
            return LightCoordsUtil.block(packedBrightnessOverride);
        }
        return super.getBlockLightLevel(entity, blockPos);
    }

    @Override
    protected float getShadowRadius(ST state) {
        Display.RenderState renderState = ((DisplayEntityRenderState)state).renderState;
        if (renderState == null) {
            return 0.0f;
        }
        return renderState.shadowRadius().get(((DisplayEntityRenderState)state).interpolationProgress);
    }

    @Override
    protected float getShadowStrength(ST state) {
        Display.RenderState renderState = ((DisplayEntityRenderState)state).renderState;
        if (renderState == null) {
            return 0.0f;
        }
        return renderState.shadowStrength().get(((DisplayEntityRenderState)state).interpolationProgress);
    }

    @Override
    public void submit(ST state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        Display.RenderState renderState = ((DisplayEntityRenderState)state).renderState;
        if (renderState == null || !((DisplayEntityRenderState)state).hasSubState()) {
            return;
        }
        float interpolationProgress = ((DisplayEntityRenderState)state).interpolationProgress;
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.pushPose();
        poseStack.mulPose((Quaternionfc)this.calculateOrientation(renderState, state, new Quaternionf()));
        Transformation transformation = renderState.transformation().get(interpolationProgress);
        poseStack.mulPose(transformation);
        this.submitInner(state, poseStack, submitNodeCollector, ((DisplayEntityRenderState)state).lightCoords, interpolationProgress);
        poseStack.popPose();
    }

    private Quaternionf calculateOrientation(Display.RenderState renderState, ST state, Quaternionf output) {
        return switch (renderState.billboardConstraints()) {
            default -> throw new MatchException(null, null);
            case Display.BillboardConstraints.FIXED -> output.rotationYXZ((float)(-Math.PI) / 180 * ((DisplayEntityRenderState)state).entityYRot, (float)Math.PI / 180 * ((DisplayEntityRenderState)state).entityXRot, 0.0f);
            case Display.BillboardConstraints.HORIZONTAL -> output.rotationYXZ((float)(-Math.PI) / 180 * ((DisplayEntityRenderState)state).entityYRot, (float)Math.PI / 180 * DisplayRenderer.transformXRot(((DisplayEntityRenderState)state).cameraXRot), 0.0f);
            case Display.BillboardConstraints.VERTICAL -> output.rotationYXZ((float)(-Math.PI) / 180 * DisplayRenderer.transformYRot(((DisplayEntityRenderState)state).cameraYRot), (float)Math.PI / 180 * ((DisplayEntityRenderState)state).entityXRot, 0.0f);
            case Display.BillboardConstraints.CENTER -> output.rotationYXZ((float)(-Math.PI) / 180 * DisplayRenderer.transformYRot(((DisplayEntityRenderState)state).cameraYRot), (float)Math.PI / 180 * DisplayRenderer.transformXRot(((DisplayEntityRenderState)state).cameraXRot), 0.0f);
        };
    }

    private static float transformYRot(float cameraYRot) {
        return cameraYRot - 180.0f;
    }

    private static float transformXRot(float cameraXRot) {
        return -cameraXRot;
    }

    private static <T extends Display> float entityYRot(T entity, float partialTicks) {
        return entity.getYRot(partialTicks);
    }

    private static <T extends Display> float entityXRot(T entity, float partialTicks) {
        return entity.getXRot(partialTicks);
    }

    protected abstract void submitInner(ST var1, PoseStack var2, SubmitNodeCollector var3, int var4, float var5);

    @Override
    public void extractRenderState(T entity, ST state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ((DisplayEntityRenderState)state).renderState = ((Display)entity).renderState();
        ((DisplayEntityRenderState)state).interpolationProgress = ((Display)entity).calculateInterpolationProgress(partialTicks);
        ((DisplayEntityRenderState)state).entityYRot = DisplayRenderer.entityYRot(entity, partialTicks);
        ((DisplayEntityRenderState)state).entityXRot = DisplayRenderer.entityXRot(entity, partialTicks);
        Camera camera = this.entityRenderDispatcher.camera;
        ((DisplayEntityRenderState)state).cameraXRot = camera.xRot();
        ((DisplayEntityRenderState)state).cameraYRot = camera.yRot();
    }

    @Environment(value=EnvType.CLIENT)
    public static class TextDisplayRenderer
    extends DisplayRenderer<Display.TextDisplay, Display.TextDisplay.TextRenderState, TextDisplayEntityRenderState> {
        private static final float TEXT_BACKGROUND_OFFSET = -0.01f;
        private final Font font;

        protected TextDisplayRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.font = context.getFont();
        }

        @Override
        public TextDisplayEntityRenderState createRenderState() {
            return new TextDisplayEntityRenderState();
        }

        @Override
        public void extractRenderState(Display.TextDisplay entity, TextDisplayEntityRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.textRenderState = entity.textRenderState();
            state.cachedInfo = entity.cacheDisplay(this::splitLines);
        }

        private Display.TextDisplay.CachedInfo splitLines(Component input, int width) {
            List<FormattedCharSequence> lines = this.font.split(input, width);
            ArrayList<Display.TextDisplay.CachedLine> result = new ArrayList<Display.TextDisplay.CachedLine>(lines.size());
            int maxLineWidth = 0;
            for (FormattedCharSequence line : lines) {
                int lineWidth = this.font.width(line);
                maxLineWidth = Math.max(maxLineWidth, lineWidth);
                result.add(new Display.TextDisplay.CachedLine(line, lineWidth));
            }
            return new Display.TextDisplay.CachedInfo(result, maxLineWidth);
        }

        @Override
        public void submitInner(TextDisplayEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, float interpolationProgress) {
            int backgroundColor;
            Display.TextDisplay.TextRenderState renderState = state.textRenderState;
            byte flags = renderState.flags();
            boolean seeThrough = (flags & 2) != 0;
            boolean useDefaultBackground = (flags & 4) != 0;
            boolean shadow = (flags & 1) != 0;
            Display.TextDisplay.Align alignment = Display.TextDisplay.getAlign(flags);
            byte textOpacity = (byte)renderState.textOpacity().get(interpolationProgress);
            if (useDefaultBackground) {
                float backgroundAlpha = Minecraft.getInstance().gameRenderer.gameRenderState().optionsRenderState.getBackgroundOpacity(0.25f);
                backgroundColor = (int)(backgroundAlpha * 255.0f) << 24;
            } else {
                backgroundColor = renderState.backgroundColor().get(interpolationProgress);
            }
            float y = 0.0f;
            Matrix4f pose = poseStack.last().pose();
            pose.rotate((float)Math.PI, 0.0f, 1.0f, 0.0f);
            pose.scale(-0.025f, -0.025f, -0.025f);
            Display.TextDisplay.CachedInfo cachedInfo = state.cachedInfo;
            boolean lineSpacing = true;
            int lineHeight = this.font.lineHeight + 1;
            int width = cachedInfo.width();
            int height = cachedInfo.lines().size() * lineHeight - 1;
            pose.translate(1.0f - (float)width / 2.0f, (float)(-height), 0.0f);
            if (backgroundColor != 0) {
                submitNodeCollector.submitCustomGeometry(poseStack, seeThrough ? RenderTypes.textBackgroundSeeThrough() : RenderTypes.textBackground(), (lambdaPose, buffer) -> {
                    buffer.addVertex(lambdaPose, -1.0f, -1.0f, -0.01f).setColor(backgroundColor).setLight(lightCoords);
                    buffer.addVertex(lambdaPose, -1.0f, (float)height, -0.01f).setColor(backgroundColor).setLight(lightCoords);
                    buffer.addVertex(lambdaPose, (float)width, (float)height, -0.01f).setColor(backgroundColor).setLight(lightCoords);
                    buffer.addVertex(lambdaPose, (float)width, -1.0f, -0.01f).setColor(backgroundColor).setLight(lightCoords);
                });
            }
            OrderedSubmitNodeCollector textCollector = submitNodeCollector.order(backgroundColor != 0 ? 1 : 0);
            for (Display.TextDisplay.CachedLine line : cachedInfo.lines()) {
                float offset = switch (alignment) {
                    default -> throw new MatchException(null, null);
                    case Display.TextDisplay.Align.LEFT -> 0.0f;
                    case Display.TextDisplay.Align.RIGHT -> width - line.width();
                    case Display.TextDisplay.Align.CENTER -> (float)width / 2.0f - (float)line.width() / 2.0f;
                };
                textCollector.submitText(poseStack, offset, y, line.contents(), shadow, seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.POLYGON_OFFSET, lightCoords, textOpacity << 24 | 0xFFFFFF, 0, 0);
                y += (float)lineHeight;
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class ItemDisplayRenderer
    extends DisplayRenderer<Display.ItemDisplay, Display.ItemDisplay.ItemRenderState, ItemDisplayEntityRenderState> {
        private final ItemModelResolver itemModelResolver;

        protected ItemDisplayRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.itemModelResolver = context.getItemModelResolver();
        }

        @Override
        public ItemDisplayEntityRenderState createRenderState() {
            return new ItemDisplayEntityRenderState();
        }

        @Override
        public void extractRenderState(Display.ItemDisplay entity, ItemDisplayEntityRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            Display.ItemDisplay.ItemRenderState itemRenderState = entity.itemRenderState();
            if (itemRenderState != null) {
                this.itemModelResolver.updateForNonLiving(state.item, itemRenderState.itemStack(), itemRenderState.itemTransform(), entity);
            } else {
                state.item.clear();
            }
        }

        @Override
        public void submitInner(ItemDisplayEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, float interpolationProgress) {
            if (state.item.isEmpty()) {
                return;
            }
            poseStack.mulPose((Quaternionfc)Axis.YP.rotation((float)Math.PI));
            state.item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class BlockDisplayRenderer
    extends DisplayRenderer<Display.BlockDisplay, Display.BlockDisplay.BlockRenderState, BlockDisplayEntityRenderState> {
        protected BlockDisplayRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public BlockDisplayEntityRenderState createRenderState() {
            return new BlockDisplayEntityRenderState();
        }

        @Override
        public void extractRenderState(Display.BlockDisplay entity, BlockDisplayEntityRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            Display.BlockDisplay.BlockRenderState blockRenderState = entity.blockRenderState();
            if (blockRenderState != null) {
                this.blockModelResolver.update(state.blockModel, blockRenderState.blockState(), BLOCK_DISPLAY_CONTEXT);
            } else {
                state.blockModel.clear();
            }
        }

        @Override
        public void submitInner(BlockDisplayEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, float interpolationProgress) {
            state.blockModel.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
    }
}

