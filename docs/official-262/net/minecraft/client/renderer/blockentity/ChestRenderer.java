/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2IntFunction
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionfc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.util.SpecialDates;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ChestRenderer<T extends BlockEntity>
implements BlockEntityRenderer<T, ChestRenderState> {
    public static final MultiblockChestResources<ModelLayerLocation> LAYERS = new MultiblockChestResources<ModelLayerLocation>(ModelLayers.CHEST, ModelLayers.DOUBLE_CHEST_LEFT, ModelLayers.DOUBLE_CHEST_RIGHT);
    private static final Map<Direction, Transformation> TRANSFORMATIONS = Util.makeEnumMap(Direction.class, ChestRenderer::createModelTransformation);
    private final SpriteGetter sprites;
    private final MultiblockChestResources<ChestModel> models;
    private final boolean xmasTextures;

    public ChestRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.xmasTextures = ChestRenderer.xmasTextures();
        this.models = LAYERS.map(layer -> new ChestModel(context.bakeLayer((ModelLayerLocation)layer)));
    }

    public static boolean xmasTextures() {
        return SpecialDates.isExtendedChristmas();
    }

    @Override
    public ChestRenderState createRenderState() {
        return new ChestRenderState();
    }

    @Override
    public void extractRenderState(T blockEntity, ChestRenderState state, float partialTicks, Vec3 cameraPosition,  @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        DoubleBlockCombiner.NeighborCombineResult<Object> combineResult;
        Block block;
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        boolean hasLevel = ((BlockEntity)blockEntity).getLevel() != null;
        BlockState blockState = hasLevel ? ((BlockEntity)blockEntity).getBlockState() : (BlockState)Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);
        state.type = blockState.hasProperty(ChestBlock.TYPE) ? blockState.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        state.facing = blockState.getValue(ChestBlock.FACING);
        state.material = ChestRenderer.getChestMaterial(blockEntity, this.xmasTextures);
        if (hasLevel && (block = blockState.getBlock()) instanceof ChestBlock) {
            ChestBlock chestBlock = (ChestBlock)block;
            combineResult = chestBlock.combine(blockState, ((BlockEntity)blockEntity).getLevel(), ((BlockEntity)blockEntity).getBlockPos(), true);
        } else {
            combineResult = DoubleBlockCombiner.Combiner::acceptNone;
        }
        state.open = combineResult.apply(ChestBlock.opennessCombiner((LidBlockEntity)blockEntity)).get(partialTicks);
        if (state.type != ChestType.SINGLE) {
            state.lightCoords = ((Int2IntFunction)combineResult.apply(new BrightnessCombiner())).applyAsInt(state.lightCoords);
        }
    }

    @Override
    public void submit(ChestRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(ChestRenderer.modelTransformation(state.facing));
        float open = state.open;
        open = 1.0f - open;
        open = 1.0f - open * open * open;
        SpriteId spriteId = Sheets.chooseSprite(state.material, state.type);
        ChestModel model = this.models.select(state.type);
        submitNodeCollector.submitModel(model, Float.valueOf(open), poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, spriteId, this.sprites, 0, state.breakProgress);
        poseStack.popPose();
    }

    private static ChestRenderState.ChestMaterialType getChestMaterial(BlockEntity entity, boolean xmasTextures) {
        Block block = entity.getBlockState().getBlock();
        if (block instanceof CopperChestBlock) {
            CopperChestBlock copperChestBlock = (CopperChestBlock)block;
            return switch (copperChestBlock.getState()) {
                default -> throw new MatchException(null, null);
                case WeatheringCopper.WeatherState.UNAFFECTED -> ChestRenderState.ChestMaterialType.COPPER_UNAFFECTED;
                case WeatheringCopper.WeatherState.EXPOSED -> ChestRenderState.ChestMaterialType.COPPER_EXPOSED;
                case WeatheringCopper.WeatherState.WEATHERED -> ChestRenderState.ChestMaterialType.COPPER_WEATHERED;
                case WeatheringCopper.WeatherState.OXIDIZED -> ChestRenderState.ChestMaterialType.COPPER_OXIDIZED;
            };
        }
        if (entity instanceof EnderChestBlockEntity) {
            return ChestRenderState.ChestMaterialType.ENDER_CHEST;
        }
        if (xmasTextures) {
            return ChestRenderState.ChestMaterialType.CHRISTMAS;
        }
        if (entity instanceof TrappedChestBlockEntity) {
            return ChestRenderState.ChestMaterialType.TRAPPED;
        }
        return ChestRenderState.ChestMaterialType.REGULAR;
    }

    public static Transformation modelTransformation(Direction facing) {
        return TRANSFORMATIONS.get(facing);
    }

    private static Transformation createModelTransformation(Direction facing) {
        return new Transformation((Matrix4fc)new Matrix4f().rotationAround((Quaternionfc)Axis.YP.rotationDegrees(-facing.toYRot()), 0.5f, 0.0f, 0.5f));
    }
}

