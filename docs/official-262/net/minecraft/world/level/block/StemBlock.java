/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.block;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StemBlock
extends VegetationBlock
implements BonemealableBlock {
    public static final MapCodec<StemBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ResourceKey.codec(Registries.BLOCK).fieldOf("fruit").forGetter(b -> b.fruit), (App)ResourceKey.codec(Registries.BLOCK).fieldOf("attached_stem").forGetter(b -> b.attachedStem), (App)ResourceKey.codec(Registries.ITEM).fieldOf("seed").forGetter(b -> b.seed), (App)TagKey.codec(Registries.BLOCK).fieldOf("stem_support_blocks").forGetter(b -> b.stemSupportBlocks), (App)TagKey.codec(Registries.BLOCK).fieldOf("fruit_support_blocks").forGetter(b -> b.fruitSupportBlocks), StemBlock.propertiesCodec()).apply((Applicative)i, StemBlock::new));
    public static final int MAX_AGE = 7;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    private static final VoxelShape[] SHAPES = Block.boxes(7, age -> Block.column(2.0, 0.0, 2 + age * 2));
    private final ResourceKey<Block> fruit;
    private final ResourceKey<Block> attachedStem;
    private final ResourceKey<Item> seed;
    private final TagKey<Block> stemSupportBlocks;
    private final TagKey<Block> fruitSupportBlocks;

    public MapCodec<StemBlock> codec() {
        return CODEC;
    }

    protected StemBlock(ResourceKey<Block> fruit, ResourceKey<Block> attachedStem, ResourceKey<Item> seed, TagKey<Block> stemSupportBlocks, TagKey<Block> fruitSupportBlocks, BlockBehaviour.Properties properties) {
        super(properties);
        this.fruit = fruit;
        this.attachedStem = attachedStem;
        this.seed = seed;
        this.stemSupportBlocks = stemSupportBlocks;
        this.fruitSupportBlocks = fruitSupportBlocks;
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(AGE, 0));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(this.stemSupportBlocks);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getRawBrightness(pos, 0) < 9) {
            return;
        }
        float growthSpeed = CropBlock.getGrowthSpeed(this, level, pos);
        if (random.nextInt((int)(25.0f / growthSpeed) + 1) == 0) {
            int age = state.getValue(AGE);
            if (age < 7) {
                state = (BlockState)state.setValue(AGE, age + 1);
                level.setBlock(pos, state, 2);
            } else {
                Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos relative = pos.relative(direction);
                BlockState stateBelow = level.getBlockState(relative.below());
                if (level.getBlockState(relative).isAir() && stateBelow.is(this.fruitSupportBlocks)) {
                    HolderLookup.RegistryLookup blocks = level.registryAccess().lookupOrThrow(Registries.BLOCK);
                    Optional<Block> fruit = blocks.getOptional(this.fruit);
                    Optional<Block> stem = blocks.getOptional(this.attachedStem);
                    if (fruit.isPresent() && stem.isPresent()) {
                        level.setBlockAndUpdate(relative, fruit.get().defaultBlockState());
                        level.setBlockAndUpdate(pos, (BlockState)stem.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction));
                    }
                }
            }
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack((ItemLike)DataFixUtils.orElse(level.registryAccess().lookupOrThrow(Registries.ITEM).getOptional(this.seed), (Object)this));
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) != 7;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int age = Math.min(7, state.getValue(AGE) + Mth.nextInt(random, 2, 5));
        BlockState newState = (BlockState)state.setValue(AGE, age);
        level.setBlock(pos, newState, 2);
        if (age == 7) {
            newState.randomTick(level, pos, random);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}

