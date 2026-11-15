package com.xgdt.dx11.mixin.immediate;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xgdt.dx11.mixin.GameRendererAccessor;
import com.xgdt.dx11.perf.immediate.BufferPool;
import com.xgdt.dx11.perf.immediate.ImmediateFlags;
import com.xgdt.dx11.perf.immediate.ImmediateStats;
import com.xgdt.dx11.perf.immediate.sign.NoTextTransformPoseStack;
import com.xgdt.dx11.perf.immediate.sign.SignAtlas;
import com.xgdt.dx11.perf.immediate.sign.SignTextCache;
import com.xgdt.dx11.perf.immediate.sign.SignTextCacheable;
import com.xgdt.dx11.perf.immediate.sign.SignTextKey;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces per-frame sign glyph submission with a cached atlas quad.
 *
 * <p>On the first frame a sign side is visible its four text lines are
 * rasterised once into a {@link SignAtlas} slot.  Later frames submit a single
 * textured quad, which removes the per-line glyph batching from the sign
 * renderer's hot path.  Text that animates (obfuscated style) or that cannot
 * fit in the atlas falls back to vanilla rendering.</p>
 *
 * <p>The nested font pass is marked with {@link NoTextTransformPoseStack}; the
 * {@code translateSignText} redirect below uses the same marker to suppress
 * the sign's wall-mount rotation while the text is drawn flat into the
 * atlas.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(AbstractSignRenderer.class)
public abstract class AbstractSignRendererMixin {
    @Shadow
    @Final
    private Font font;

    @Shadow
    protected abstract void submitSignText(
        SignRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, boolean isFront);

    @Shadow
    protected abstract void translateSignText(PoseStack poseStack, boolean isFront, Vec3 offset);

    @Shadow
    protected abstract Vec3 getTextOffset();

    @Inject(method = "submitSignText", at = @At("HEAD"), cancellable = true)
    private void dx11$renderBufferedSignText(
        SignRenderState renderState,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        boolean isFront,
        CallbackInfo ci) {
        if (!ImmediateFlags.signTextBuffering() || poseStack instanceof NoTextTransformPoseStack) {
            return;
        }

        SignText signText = isFront ? renderState.frontText : renderState.backText;
        if (!(signText instanceof SignTextCacheable cacheable) || !cacheable.dx11$shouldCache()) {
            return;
        }

        SignTextCache cache;
        try {
            cache = SignTextCache.get();
        } catch (RuntimeException failure) {
            SignTextCache.reportFailure(failure);
            cacheable.dx11$setShouldCache(false);
            return;
        }

        SignTextKey key = cacheable.dx11$signTextKey(
            renderState.isTextFilteringEnabled,
            renderState.textLineHeight,
            renderState.maxTextLineWidth);
        SignAtlas.Slot slot = cache.lookup(key);
        if (slot == null) {
            slot = this.dx11$rasterize(cache, cacheable, signText, renderState, isFront);
            if (slot == null) {
                return;
            }
            cache.store(key, slot);
        }

        int atlasSize = cache.atlas().atlasSize();
        float u1 = (float) slot.x() / atlasSize;
        float u2 = (float) (slot.x() + slot.width()) / atlasSize;
        float v1 = 1.0F - (float) slot.y() / atlasSize;
        float v2 = 1.0F - (float) (slot.y() + slot.height()) / atlasSize;
        int light = signText.hasGlowingText() ? LightTexture.FULL_BRIGHT : renderState.lightCoords;

        poseStack.pushPose();
        this.translateSignText(poseStack, isFront, this.getTextOffset());
        poseStack.translate(-slot.width() / 2.0F, -slot.height() / 2.0F, 0.0F);
        SignAtlas.Slot finalSlot = slot;
        nodeCollector.submitCustomGeometry(poseStack, cache.renderType(), (entry, vertexConsumer) -> {
            vertexConsumer.addVertex(entry, 0.0F, finalSlot.height(), 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u1, v2)
                .setLight(light);
            vertexConsumer.addVertex(entry, finalSlot.width(), finalSlot.height(), 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u2, v2)
                .setLight(light);
            vertexConsumer.addVertex(entry, finalSlot.width(), 0.0F, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u2, v1)
                .setLight(light);
            vertexConsumer.addVertex(entry, 0.0F, 0.0F, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u1, v1)
                .setLight(light);
        });
        poseStack.popPose();

        ImmediateStats.onSignRender();
        ci.cancel();
    }

    @Unique
    private SignAtlas.Slot dx11$rasterize(
        SignTextCache cache,
        SignTextCacheable cacheable,
        SignText signText,
        SignRenderState renderState,
        boolean isFront) {
        int width = this.dx11$getTextWidth(signText, renderState);
        int height = 4 * renderState.textLineHeight;
        if (width <= 0 || height <= 0) {
            cacheable.dx11$setShouldCache(false);
            return null;
        }

        int padding = signText.hasGlowingText() ? 2 : 0;
        SignAtlas.Slot slot = cache.atlas().allocate(width + padding, height + padding);
        if (slot == null) {
            // The atlas is full.  Keep this sign on the vanilla path until its
            // messages change; retrying the same oversized/full allocation on
            // every frame would only burn CPU.
            cacheable.dx11$setShouldCache(false);
            return null;
        }

        GpuBufferSlice fog = RenderSystem.getShaderFog();
        GpuTextureView previousColor = RenderSystem.outputColorTextureOverride;
        GpuTextureView previousDepth = RenderSystem.outputDepthTextureOverride;
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        ByteBufferBuilder buffer = BufferPool.borrow();
        boolean previousOutline = renderState.drawOutline;
        int previousLight = renderState.lightCoords;

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(cache.projectionMatrix(), ProjectionType.ORTHOGRAPHIC);
        modelView.pushMatrix().identity();

        try {
            RenderSystem.setShaderFog(
                ((GameRendererAccessor) Minecraft.getInstance().gameRenderer)
                    .dx11$fogRenderer()
                    .getBuffer(FogRenderer.FogMode.NONE));
            RenderSystem.outputColorTextureOverride = cache.atlas().getColorTextureView();
            RenderSystem.outputDepthTextureOverride = cache.atlas().getDepthTextureView();
            renderState.drawOutline = true;
            // Bake unlit text into the atlas.  The world quad supplies the
            // actual lightmap, so applying it here as well would square the
            // light and make a cached sign keep stale lighting after a torch
            // is placed or removed.
            renderState.lightCoords = LightTexture.FULL_BRIGHT;

            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(buffer);
            SubmitNodeStorage storage = new SubmitNodeStorage();
            FeatureRenderDispatcher dispatcher = new FeatureRenderDispatcher(
                storage,
                Minecraft.getInstance().getBlockRenderer(),
                bufferSource,
                Minecraft.getInstance().getAtlasManager(),
                null,
                null,
                this.font);

            PoseStack textPoseStack = new NoTextTransformPoseStack();
            textPoseStack.translate(
                slot.x() + slot.width() / 2.0F,
                slot.y() + slot.height() / 2.0F,
                0.0F);
            this.submitSignText(renderState, textPoseStack, storage, isFront);
            dispatcher.renderAllFeatures();
            bufferSource.endBatch();
            dispatcher.close();
        } catch (RuntimeException failure) {
            cache.release(slot);
            SignTextCache.reportFailure(failure);
            cacheable.dx11$setShouldCache(false);
            return null;
        } finally {
            renderState.drawOutline = previousOutline;
            renderState.lightCoords = previousLight;
            RenderSystem.outputColorTextureOverride = previousColor;
            RenderSystem.outputDepthTextureOverride = previousDepth;
            RenderSystem.setShaderFog(fog);
            modelView.popMatrix();
            RenderSystem.restoreProjectionMatrix();
            BufferPool.release(buffer);
        }

        return slot;
    }

    @Unique
    private int dx11$getTextWidth(SignText signText, SignRenderState renderState) {
        FormattedCharSequence[] renderMessages = signText.getRenderMessages(
            renderState.isTextFilteringEnabled,
            component -> {
                List<FormattedCharSequence> lines =
                    this.font.split(component, renderState.maxTextLineWidth);
                return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.getFirst();
            });

        int width = 0;
        for (FormattedCharSequence line : renderMessages) {
            width = Math.max(width, this.font.width(line));
        }
        if ((width & 1) != 0) {
            // Odd widths make the glyph layout squish by half a pixel.
            width++;
        }
        return width;
    }

    @WrapOperation(
        method = "submitSignText",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/blockentity/AbstractSignRenderer;translateSignText(Lcom/mojang/blaze3d/vertex/PoseStack;ZLnet/minecraft/world/phys/Vec3;)V"))
    private void dx11$skipTextTransform(
        AbstractSignRenderer instance,
        PoseStack poseStack,
        boolean isFront,
        Vec3 offset,
        Operation<Void> original) {
        if (poseStack instanceof NoTextTransformPoseStack) {
            return;
        }
        original.call(instance, poseStack, isFront, offset);
    }
}
