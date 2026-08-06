/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionfc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ScreenEffectRenderer {
    private static final Identifier UNDERWATER_LOCATION = Identifier.withDefaultNamespace("textures/misc/underwater.png");
    private final Minecraft minecraft;
    private final SpriteGetter sprites;
    public static final int ITEM_ACTIVATION_ANIMATION_LENGTH = 40;
    private @Nullable ItemStack itemActivationItem;
    private int itemActivationTicks;
    private float itemActivationOffX;
    private float itemActivationOffY;

    public ScreenEffectRenderer(Minecraft minecraft, SpriteGetter sprites) {
        this.minecraft = minecraft;
        this.sprites = sprites;
    }

    public void tick() {
        if (this.itemActivationTicks > 0) {
            --this.itemActivationTicks;
            if (this.itemActivationTicks == 0) {
                this.itemActivationItem = null;
            }
        }
    }

    public void submit(boolean isFirstPerson, boolean isSleeping, float partialTicks, SubmitNodeCollector submitNodeCollector, boolean hideGui) {
        PoseStack poseStack = new PoseStack();
        LocalPlayer player = this.minecraft.player;
        if (isFirstPerson && !isSleeping) {
            BlockState blockState = ScreenEffectRenderer.getViewBlockingState(player);
            if (blockState != null) {
                BlockStateModelSet blockStateModelSet = this.minecraft.getModelManager().getBlockStateModelSet();
                TextureAtlasSprite sprite = blockStateModelSet.getParticleMaterial(blockState).sprite();
                ScreenEffectRenderer.submitBlockSprite(sprite, poseStack, submitNodeCollector, -15132391);
            }
            if (!this.minecraft.player.isSpectator()) {
                if (this.minecraft.player.isEyeInFluid(FluidTags.WATER)) {
                    ScreenEffectRenderer.submitWater(this.minecraft, poseStack, submitNodeCollector);
                }
                if (this.minecraft.player.isOnFire()) {
                    TextureAtlasSprite fireSprite = this.sprites.get(ModelBakery.FIRE_1);
                    ScreenEffectRenderer.submitFire(poseStack, submitNodeCollector, fireSprite);
                }
            }
        }
        if (!hideGui) {
            this.renderItemActivationAnimation(poseStack, partialTicks, submitNodeCollector);
        }
    }

    private void renderItemActivationAnimation(PoseStack poseStack, float partialTicks, SubmitNodeCollector submitNodeCollector) {
        if (this.itemActivationItem == null || this.itemActivationTicks <= 0) {
            return;
        }
        int tick = 40 - this.itemActivationTicks;
        float scale = ((float)tick + partialTicks) / 40.0f;
        float ts = scale * scale;
        float tc = scale * ts;
        float smoothScale = 10.25f * tc * ts - 24.95f * ts * ts + 25.5f * tc - 13.8f * ts + 4.0f * scale;
        float piScale = smoothScale * (float)Math.PI;
        WindowRenderState windowState = this.minecraft.gameRenderer.gameRenderState().windowRenderState;
        float aspectRatio = (float)windowState.width / (float)windowState.height;
        float offX = this.itemActivationOffX * 0.3f * aspectRatio;
        float offY = this.itemActivationOffY * 0.3f;
        poseStack.pushPose();
        poseStack.translate(offX * Mth.abs(Mth.sin(piScale * 2.0f)), offY * Mth.abs(Mth.sin(piScale * 2.0f)), -10.0f + 9.0f * Mth.sin(piScale));
        float size = 0.8f;
        poseStack.scale(0.8f, 0.8f, 0.8f);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(900.0f * Mth.abs(Mth.sin(piScale))));
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(6.0f * Mth.cos(scale * 8.0f)));
        poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(6.0f * Mth.cos(scale * 8.0f)));
        this.minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
        ItemStackRenderState itemState = new ItemStackRenderState();
        this.minecraft.getItemModelResolver().updateForTopItem(itemState, this.itemActivationItem, ItemDisplayContext.FIXED, this.minecraft.level, null, 0);
        itemState.submit(poseStack, submitNodeCollector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    public void resetItemActivation() {
        this.itemActivationItem = null;
    }

    public void displayItemActivation(ItemStack itemStack, RandomSource random) {
        this.itemActivationItem = itemStack;
        this.itemActivationTicks = 40;
        this.itemActivationOffX = random.nextFloat() * 2.0f - 1.0f;
        this.itemActivationOffY = random.nextFloat() * 2.0f - 1.0f;
    }

    private static @Nullable BlockState getViewBlockingState(Player player) {
        if (player.noPhysics) {
            return null;
        }
        BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 8; ++i) {
            testPos.set(player.getX() + (double)(((float)((i >> 0) % 2) - 0.5f) * player.getBbWidth() * 0.8f), player.getEyeY() + (double)(((float)((i >> 1) % 2) - 0.5f) * 0.1f * player.getScale()), player.getZ() + (double)(((float)((i >> 2) % 2) - 0.5f) * player.getBbWidth() * 0.8f));
            BlockState blockState = player.level().getBlockState(testPos);
            if (blockState.getRenderShape() == RenderShape.INVISIBLE || !blockState.isViewBlocking(player.level(), testPos)) continue;
            return blockState;
        }
        return null;
    }

    private static void submitBlockSprite(TextureAtlasSprite sprite, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int color) {
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.blockScreenEffect(sprite.atlasLocation()), (pose, builder) -> ScreenEffectRenderer.buildSpriteQuad(builder, pose.pose(), sprite, -1.0f, -1.0f, 1.0f, 1.0f, -0.5f, color));
    }

    private static void submitWater(Minecraft minecraft, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        LocalPlayer player = minecraft.player;
        BlockPos pos = BlockPos.containing(player.getEyePosition());
        float brightness = Lightmap.getBrightness(player.level().dimensionType(), player.level().getMaxLocalRawBrightness(pos));
        int color = ARGB.colorFromFloat(0.1f, brightness, brightness, brightness);
        float u0 = -player.getYRot() / 64.0f;
        float v0 = player.getXRot() / 64.0f;
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.blockScreenEffect(UNDERWATER_LOCATION), (pose, builder) -> {
            float uvSize = 4.0f;
            ScreenEffectRenderer.buildQuad(builder, pose.pose(), -1.0f, -1.0f, 1.0f, 1.0f, -0.5f, u0 + 4.0f, v0 + 4.0f, u0, v0, color);
        });
    }

    private static void submitFire(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, TextureAtlasSprite sprite) {
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.fireScreenEffect(sprite.atlasLocation()), (basePose, builder) -> {
            Matrix4f pose = new Matrix4f();
            pose.set((Matrix4fc)basePose.pose());
            pose.translate(0.24f, -0.3f, 0.0f);
            pose.rotateY(-0.17453292f);
            ScreenEffectRenderer.buildFireQuad(sprite, builder, pose);
            pose.set((Matrix4fc)basePose.pose());
            pose.translate(-0.24f, -0.3f, 0.0f);
            pose.rotateY(0.17453292f);
            ScreenEffectRenderer.buildFireQuad(sprite, builder, pose);
        });
    }

    private static void buildFireQuad(TextureAtlasSprite sprite, VertexConsumer builder, Matrix4f pose) {
        float size = 1.0f;
        ScreenEffectRenderer.buildSpriteQuad(builder, pose, sprite, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -436207617);
    }

    private static void buildSpriteQuad(VertexConsumer builder, Matrix4f pose, TextureAtlasSprite sprite, float x0, float y0, float x1, float y1, float z, int color) {
        ScreenEffectRenderer.buildQuad(builder, pose, x0, y0, x1, y1, z, sprite.getU1(), sprite.getV1(), sprite.getU0(), sprite.getV0(), color);
    }

    private static void buildQuad(VertexConsumer builder, Matrix4f pose, float x0, float y0, float x1, float y1, float z, float u0, float v0, float u1, float v1, int color) {
        builder.addVertex((Matrix4fc)pose, x0, y0, z).setUv(u0, v0).setColor(color);
        builder.addVertex((Matrix4fc)pose, x1, y0, z).setUv(u1, v0).setColor(color);
        builder.addVertex((Matrix4fc)pose, x1, y1, z).setUv(u1, v1).setColor(color);
        builder.addVertex((Matrix4fc)pose, x0, y1, z).setUv(u0, v1).setColor(color);
    }
}

