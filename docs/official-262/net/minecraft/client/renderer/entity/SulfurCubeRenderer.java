/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SmallSulfurCubeModel;
import net.minecraft.client.model.monster.slime.SulfurCubeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.AbstractCubeMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.entity.layers.SulfurCubeInnerLayer;
import net.minecraft.client.renderer.entity.state.SulfurCubeRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@Environment(value=EnvType.CLIENT)
public class SulfurCubeRenderer
extends AbstractCubeMobRenderer<SulfurCube, SulfurCubeRenderState, SulfurCubeModel> {
    private static final Identifier SULFUR_CUBE_LOCATION = Identifier.withDefaultNamespace("textures/entity/sulfur_cube/sulfur_cube_outer.png");
    private static final Identifier SULFUR_CUBE_SMALL_LOCATION = Identifier.withDefaultNamespace("textures/entity/sulfur_cube/sulfur_cube_outer_small.png");
    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private final SulfurCubeModel normalModel;
    private final SmallSulfurCubeModel smallModel;
    private final BlockModelResolver blockModelResolver;

    public SulfurCubeRenderer(EntityRendererProvider.Context context) {
        SulfurCubeModel normalModel = new SulfurCubeModel(context.bakeLayer(ModelLayers.SULFUR_CUBE));
        super(context, normalModel);
        this.normalModel = normalModel;
        this.smallModel = new SmallSulfurCubeModel(context.bakeLayer(ModelLayers.SULFUR_CUBE_SMALL));
        this.blockModelResolver = context.getBlockModelResolver();
        this.addLayer(new SulfurCubeInnerLayer(this, context.getModelSet()));
    }

    @Override
    public void submit(SulfurCubeRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        this.model = state.isBaby ? this.smallModel : this.normalModel;
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    protected void scale(SulfurCubeRenderState state, PoseStack poseStack) {
        this.downscaleSlightly(poseStack);
        super.scale(state, poseStack);
        float fuse = state.fuseRemainingTicks;
        if (fuse < 10.0f && fuse > 0.0f) {
            float s = 1.0f + TntRenderer.getSwellAmount(fuse);
            poseStack.scale(s, s, s);
        }
        float vOffset = state.isBaby ? 1.24f : 0.98f;
        float extraDownscale = state.isBaby ? 1.0f : 0.5f;
        float onePixelUpIfVisible = (state.isInvisible ? 0.0f : 1.0f) / 16.0f;
        poseStack.scale(extraDownscale, extraDownscale, extraDownscale);
        poseStack.translate(-0.0f, vOffset - onePixelUpIfVisible, -0.0f);
    }

    @Override
    public Identifier getTextureLocation(SulfurCubeRenderState state) {
        return state.isBaby ? SULFUR_CUBE_SMALL_LOCATION : SULFUR_CUBE_LOCATION;
    }

    @Override
    public SulfurCubeRenderState createRenderState() {
        return new SulfurCubeRenderState();
    }

    @Override
    public void extractRenderState(SulfurCube entity, SulfurCubeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.fuseRemainingTicks = entity.isPrimed() ? (float)entity.getFuse() - partialTicks + 1.0f : 0.0f;
        ItemStack containedBlock = entity.getBodyArmorItem();
        if (!containedBlock.isEmpty()) {
            BlockItemStateProperties blockItemState = containedBlock.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
            BlockState blockState = blockItemState.apply(Block.byItem(containedBlock.getItem()).defaultBlockState());
            this.blockModelResolver.update(state.containedBlock, blockState, BLOCK_DISPLAY_CONTEXT);
        }
    }

    @Override
    protected void applySizeAndSquish(SulfurCubeRenderState state, PoseStack poseStack) {
        float size = state.size;
        float ss = state.containedBlock.isEmpty() ? state.squish / (size * 0.5f + 1.0f) : 0.0f;
        float w = 1.0f / (ss + 1.0f);
        poseStack.scale(w * size, 1.0f / w * size, w * size);
    }
}

