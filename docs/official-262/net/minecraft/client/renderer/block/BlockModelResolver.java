/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.block;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;

@Environment(value=EnvType.CLIENT)
public class BlockModelResolver {
    private static final long MODEL_SEED = 42L;
    private final ModelManager modelManager;

    public BlockModelResolver(ModelManager modelManager) {
        this.modelManager = modelManager;
    }

    public void update(BlockModelRenderState renderState, BlockState blockState, BlockDisplayContext displayContext) {
        renderState.clear();
        this.modelManager.getBlockModelSet().get(blockState).update(renderState, blockState, displayContext, 42L);
        renderState.blockLightCoords = blockState.emissiveRendering() ? 0xF000F0 : LightCoordsUtil.pack(blockState.getLightEmission(), 0);
    }

    public void updateForItemFrame(BlockModelRenderState renderState, boolean isGlowing, boolean map) {
        BlockState fakeState = BlockStateDefinitions.getItemFrameFakeState(isGlowing, map);
        this.update(renderState, fakeState, ItemFrameRenderer.BLOCK_DISPLAY_CONTEXT);
    }
}

