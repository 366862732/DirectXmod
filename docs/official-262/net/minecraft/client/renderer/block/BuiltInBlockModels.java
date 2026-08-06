/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package net.minecraft.client.renderer.block;

import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.block.SelectBlockModel;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockStateModelWrapper;
import net.minecraft.client.renderer.block.model.CompositeBlockModel;
import net.minecraft.client.renderer.block.model.ConditionalBlockModel;
import net.minecraft.client.renderer.block.model.EmptyBlockModel;
import net.minecraft.client.renderer.block.model.SpecialBlockModelWrapper;
import net.minecraft.client.renderer.block.model.properties.conditional.IsXmas;
import net.minecraft.client.renderer.block.model.properties.select.DisplayContext;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.renderer.blockentity.CopperGolemStatueBlockRenderer;
import net.minecraft.client.renderer.blockentity.DecoratedPotRenderer;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.client.renderer.entity.CopperGolemRenderer;
import net.minecraft.client.renderer.special.BannerSpecialRenderer;
import net.minecraft.client.renderer.special.BellSpecialRenderer;
import net.minecraft.client.renderer.special.BookSpecialRenderer;
import net.minecraft.client.renderer.special.ChestSpecialRenderer;
import net.minecraft.client.renderer.special.ConduitSpecialRenderer;
import net.minecraft.client.renderer.special.CopperGolemStatueSpecialRenderer;
import net.minecraft.client.renderer.special.DecoratedPotSpecialRenderer;
import net.minecraft.client.renderer.special.EndCubeSpecialRenderer;
import net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer;
import net.minecraft.client.renderer.special.ShulkerBoxSpecialRenderer;
import net.minecraft.client.renderer.special.SkullSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.PlayerHeadBlock;
import net.minecraft.world.level.block.PlayerWallHeadBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public class BuiltInBlockModels {
    private static void addDefaults(Builder builder) {
        BuiltInBlockModels.createAir(builder, Blocks.AIR);
        BuiltInBlockModels.createAir(builder, Blocks.CAVE_AIR);
        BuiltInBlockModels.createAir(builder, Blocks.VOID_AIR);
        BuiltInBlockModels.createMobHeads(builder, SkullBlock.Types.SKELETON, Blocks.SKELETON_SKULL, Blocks.SKELETON_WALL_SKULL);
        BuiltInBlockModels.createMobHeads(builder, SkullBlock.Types.ZOMBIE, Blocks.ZOMBIE_HEAD, Blocks.ZOMBIE_WALL_HEAD);
        BuiltInBlockModels.createMobHeads(builder, SkullBlock.Types.CREEPER, Blocks.CREEPER_HEAD, Blocks.CREEPER_WALL_HEAD);
        BuiltInBlockModels.createMobHeads(builder, SkullBlock.Types.DRAGON, Blocks.DRAGON_HEAD, Blocks.DRAGON_WALL_HEAD);
        BuiltInBlockModels.createMobHeads(builder, SkullBlock.Types.PIGLIN, Blocks.PIGLIN_HEAD, Blocks.PIGLIN_WALL_HEAD);
        BuiltInBlockModels.createMobHeads(builder, SkullBlock.Types.WITHER_SKELETON, Blocks.WITHER_SKELETON_SKULL, Blocks.WITHER_SKELETON_WALL_SKULL);
        builder.put(BuiltInBlockModels.createPlayerHead(), Blocks.PLAYER_HEAD);
        builder.put(BuiltInBlockModels.createPlayerWallHead(), Blocks.PLAYER_WALL_HEAD);
        ColorCollection.zipApply(ColorCollection.VALUES, Blocks.BANNER, (color, banner) -> builder.put(BuiltInBlockModels.createBanner(color), (Block)banner));
        ColorCollection.zipApply(ColorCollection.VALUES, Blocks.WALL_BANNER, (color, wallBanner) -> builder.put(BuiltInBlockModels.createWallBanner(color), (Block)wallBanner));
        builder.put(BuiltInBlockModels.createShulkerBox(), Blocks.SHULKER_BOX);
        ColorCollection.zipApply(ColorCollection.VALUES, Blocks.DYED_SHULKER_BOX, (color, box) -> builder.put(BuiltInBlockModels.createDyedShulkerBox(color), (Block)box));
        builder.put(BuiltInBlockModels.createSingletonChest(ChestSpecialRenderer.ENDER_CHEST), Blocks.ENDER_CHEST);
        builder.put(BuiltInBlockModels.createXmasChest(ChestSpecialRenderer.REGULAR), Blocks.CHEST);
        builder.put(BuiltInBlockModels.createXmasChest(ChestSpecialRenderer.TRAPPED), Blocks.TRAPPED_CHEST);
        WeatheringCopper.WeatherState.forEach(state -> {
            builder.put(BuiltInBlockModels.createChest(ChestSpecialRenderer.COPPER.pick((WeatheringCopper.WeatherState)state)), Blocks.COPPER_CHEST.weathering().pick((WeatheringCopper.WeatherState)state), Blocks.COPPER_CHEST.waxed().pick((WeatheringCopper.WeatherState)state));
            builder.put(BuiltInBlockModels.createCopperGolem(state), Blocks.COPPER_GOLEM_STATUE.weathering().pick((WeatheringCopper.WeatherState)state), Blocks.COPPER_GOLEM_STATUE.waxed().pick((WeatheringCopper.WeatherState)state));
        });
        builder.put(BuiltInBlockModels.special(new BellSpecialRenderer.Unbaked()), Blocks.BELL);
        builder.put(BuiltInBlockModels.special(new ConduitSpecialRenderer.Unbaked(), ConduitRenderer.DEFAULT_TRANSFORMATION), Blocks.CONDUIT);
        builder.put(BuiltInBlockModels.createDecoratedPot(), Blocks.DECORATED_POT);
        builder.put(BuiltInBlockModels.createEnchantingTable(), Blocks.ENCHANTING_TABLE);
        builder.put(BuiltInBlockModels.special(new EndCubeSpecialRenderer.Unbaked(EndCubeSpecialRenderer.Type.GATEWAY)), Blocks.END_GATEWAY);
        builder.put(BuiltInBlockModels.special(new EndCubeSpecialRenderer.Unbaked(EndCubeSpecialRenderer.Type.PORTAL), TheEndPortalRenderer.TRANSFORMATION), Blocks.END_PORTAL);
        builder.put(BuiltInBlockModels::createFlowerBedModel, Blocks.WILDFLOWERS, Blocks.PINK_PETALS);
    }

    private static void createAir(Builder builder, Block block) {
        builder.put(new EmptyBlockModel.Unbaked(), block);
    }

    private static BlockModel.Unbaked special(SpecialModelRenderer.Unbaked<?> model) {
        return new SpecialBlockModelWrapper.Unbaked(model, Optional.empty());
    }

    private static BlockModel.Unbaked special(SpecialModelRenderer.Unbaked<?> model, Transformation transformation) {
        return new SpecialBlockModelWrapper.Unbaked(model, Optional.of(transformation));
    }

    private static SpecialModelFactory createMobHead(SkullBlock.Types type) {
        return BuiltInBlockModels.specialModelWithPropertyDispatch(SkullBlock.ROTATION, rotation -> BuiltInBlockModels.special(new SkullSpecialRenderer.Unbaked(type), SkullBlockRenderer.TRANSFORMATIONS.freeTransformations((int)rotation)));
    }

    private static SpecialModelFactory createMobWallHead(SkullBlock.Types type) {
        return BuiltInBlockModels.specialModelWithPropertyDispatch(WallSkullBlock.FACING, facing -> BuiltInBlockModels.special(new SkullSpecialRenderer.Unbaked(type), SkullBlockRenderer.TRANSFORMATIONS.wallTransformation((Direction)facing)));
    }

    private static void createMobHeads(Builder builder, SkullBlock.Types type, Block ground, Block wall) {
        builder.put(BuiltInBlockModels.createMobHead(type), ground);
        builder.put(BuiltInBlockModels.createMobWallHead(type), wall);
    }

    private static SpecialModelFactory createPlayerHead() {
        return BuiltInBlockModels.specialModelWithPropertyDispatch(PlayerHeadBlock.ROTATION, rotation -> BuiltInBlockModels.special(new PlayerHeadSpecialRenderer.Unbaked(), SkullBlockRenderer.TRANSFORMATIONS.freeTransformations((int)rotation)));
    }

    private static SpecialModelFactory createPlayerWallHead() {
        return BuiltInBlockModels.specialModelWithPropertyDispatch(PlayerWallHeadBlock.FACING, facing -> BuiltInBlockModels.special(new PlayerHeadSpecialRenderer.Unbaked(), SkullBlockRenderer.TRANSFORMATIONS.wallTransformation((Direction)facing)));
    }

    private static SpecialModelFactory createBanner(DyeColor color) {
        return BuiltInBlockModels.specialModelWithPropertyDispatch(BannerBlock.ROTATION, rotation -> BuiltInBlockModels.special(new BannerSpecialRenderer.Unbaked(color, BannerBlock.AttachmentType.GROUND), BannerRenderer.TRANSFORMATIONS.freeTransformations((int)rotation)));
    }

    private static SpecialModelFactory createWallBanner(DyeColor color) {
        return BuiltInBlockModels.specialModelWithPropertyDispatch(WallBannerBlock.FACING, facing -> BuiltInBlockModels.special(new BannerSpecialRenderer.Unbaked(color, BannerBlock.AttachmentType.WALL), BannerRenderer.TRANSFORMATIONS.wallTransformation((Direction)facing)));
    }

    private static SpecialModelFactory createShulkerBox() {
        return BuiltInBlockModels.specialModelWithPropertyDispatch(ShulkerBoxBlock.FACING, facing -> BuiltInBlockModels.special(new ShulkerBoxSpecialRenderer.Unbaked(), ShulkerBoxRenderer.modelTransform(facing)));
    }

    private static SpecialModelFactory createDyedShulkerBox(DyeColor color) {
        return BuiltInBlockModels.specialModelWithPropertyDispatch(ShulkerBoxBlock.FACING, facing -> BuiltInBlockModels.special(new ShulkerBoxSpecialRenderer.Unbaked(color), ShulkerBoxRenderer.modelTransform(facing)));
    }

    private static BlockModel.Unbaked createChest(Identifier texture, ChestType chestType, Direction facing) {
        return BuiltInBlockModels.special(new ChestSpecialRenderer.Unbaked(texture, chestType), ChestRenderer.modelTransformation(facing));
    }

    private static SpecialModelFactory createSingletonChest(Identifier texture) {
        return BuiltInBlockModels.specialModelWithPropertyDispatch(ChestBlock.FACING, facing -> BuiltInBlockModels.createChest(texture, ChestType.SINGLE, facing));
    }

    private static SpecialModelFactory createChest(MultiblockChestResources<Identifier> textures) {
        return BuiltInBlockModels.specialModelWithPropertyDispatch(ChestBlock.FACING, ChestBlock.TYPE, (facing, type) -> BuiltInBlockModels.createChest((Identifier)textures.select((ChestType)type), type, facing));
    }

    private static SpecialModelFactory createXmasChest(MultiblockChestResources<Identifier> textures) {
        return BuiltInBlockModels.specialModelWithPropertyDispatch(ChestBlock.FACING, ChestBlock.TYPE, (facing, type) -> new ConditionalBlockModel.Unbaked(Optional.empty(), new IsXmas(), BuiltInBlockModels.createChest(ChestSpecialRenderer.CHRISTMAS.select((ChestType)type), type, facing), BuiltInBlockModels.createChest((Identifier)textures.select((ChestType)type), type, facing)));
    }

    private static SpecialModelFactory createCopperGolem(WeatheringCopper.WeatherState weatherState) {
        return BuiltInBlockModels.specialModelWithPropertyDispatch(CopperGolemStatueBlock.FACING, CopperGolemStatueBlock.POSE, (facing, pose) -> BuiltInBlockModels.special(new CopperGolemStatueSpecialRenderer.Unbaked(weatherState, (CopperGolemStatueBlock.Pose)pose), CopperGolemStatueBlockRenderer.modelTransformation(facing)));
    }

    private static SpecialModelFactory createDecoratedPot() {
        return BuiltInBlockModels.specialModelWithPropertyDispatch(DecoratedPotBlock.HORIZONTAL_FACING, facing -> BuiltInBlockModels.special(new DecoratedPotSpecialRenderer.Unbaked(), DecoratedPotRenderer.modelTransformation(facing)));
    }

    private static BlockStateModelWrapper.Unbaked createBlockStateModelWrapper(BlockColors blockColors, BlockState blockState) {
        return new BlockStateModelWrapper.Unbaked(blockState, blockColors.getTintSources(blockState), Optional.empty());
    }

    private static CompositeBlockModel.Unbaked combineSpecialAndBlockModels(BlockModel.Unbaked specialModel, BlockColors blockColors, BlockState blockState) {
        return new CompositeBlockModel.Unbaked(BuiltInBlockModels.createBlockStateModelWrapper(blockColors, blockState), specialModel, Optional.empty());
    }

    private static SelectBlockModel.Unbaked createFlowerBedModel(BlockColors blockColors, BlockState blockState) {
        List<BlockTintSource> tintSources = blockColors.getTintSources(blockState);
        Transformation customFlowerTransform = new Transformation((Vector3fc)new Vector3f(0.25f, 0.0f, 0.25f), null, null, null);
        BlockStateModelWrapper.Unbaked customTransformModel = new BlockStateModelWrapper.Unbaked(blockState, tintSources, Optional.of(customFlowerTransform));
        BlockStateModelWrapper.Unbaked normalTransformModel = new BlockStateModelWrapper.Unbaked(blockState, tintSources, Optional.empty());
        return new SelectBlockModel.Unbaked(Optional.empty(), new SelectBlockModel.UnbakedSwitch(new DisplayContext(), List.of(new SelectBlockModel.SwitchCase<BlockDisplayContext>(List.of(CopperGolemRenderer.BLOCK_DISPLAY_CONTEXT), customTransformModel))), Optional.of(normalTransformModel));
    }

    private static BlockModel.Unbaked createEnchantingTable() {
        return BuiltInBlockModels.special(new BookSpecialRenderer.Unbaked(0.0f, 0.0f, 0.0f), new Transformation((Vector3fc)new Vector3f(0.5f, 0.8125f, 0.5f), (Quaternionfc)Axis.ZP.rotationDegrees(180.0f), null, (Quaternionfc)Axis.XP.rotationDegrees(90.0f)));
    }

    private static <P extends Comparable<P>> SpecialModelFactory specialModelWithPropertyDispatch(Property<P> property, Function<P, BlockModel.Unbaked> blockModel) {
        return state -> {
            Object value = state.getValue(property);
            return (BlockModel.Unbaked)blockModel.apply(value);
        };
    }

    private static <P1 extends Comparable<P1>, P2 extends Comparable<P2>> SpecialModelFactory specialModelWithPropertyDispatch(Property<P1> property1, Property<P2> property2, BiFunction<P1, P2, BlockModel.Unbaked> blockModel) {
        return state -> {
            Object value1 = state.getValue(property1);
            Object value2 = state.getValue(property2);
            return (BlockModel.Unbaked)blockModel.apply(value1, value2);
        };
    }

    public static Map<BlockState, BlockModel.Unbaked> createBlockModels(BlockColors blockColors) {
        Builder builder = new Builder(blockColors);
        BuiltInBlockModels.addDefaults(builder);
        return builder.build();
    }

    @Environment(value=EnvType.CLIENT)
    private static class Builder {
        private final BlockColors blockColors;
        private final Map<BlockState, BlockModel.Unbaked> result = new HashMap<BlockState, BlockModel.Unbaked>();

        private Builder(BlockColors blockColors) {
            this.blockColors = blockColors;
        }

        private void put(ModelFactory factory, Block a, Block b) {
            this.put(factory, a);
            this.put(factory, b);
        }

        private void put(BlockModel.Unbaked specialModel, Block block) {
            this.put(blockState -> specialModel, block);
        }

        private void put(ModelFactory factory, Block block) {
            for (BlockState blockState : block.getStateDefinition().getPossibleStates()) {
                this.result.put(blockState, factory.create(this.blockColors, blockState));
            }
        }

        public Map<BlockState, BlockModel.Unbaked> build() {
            return Map.copyOf(this.result);
        }
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    private static interface SpecialModelFactory
    extends ModelFactory {
        @Override
        default public BlockModel.Unbaked create(BlockColors colors, BlockState state) {
            return BuiltInBlockModels.combineSpecialAndBlockModels(this.createSpecial(state), colors, state);
        }

        public BlockModel.Unbaked createSpecial(BlockState var1);
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    private static interface ModelFactory {
        public BlockModel.Unbaked create(BlockColors var1, BlockState var2);
    }
}

