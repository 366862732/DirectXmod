/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FireBlock
extends BaseFireBlock {
    public static final MapCodec<FireBlock> CODEC = FireBlock.simpleCodec(FireBlock::new);
    public static final int MAX_AGE = 15;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final BooleanProperty UP = PipeBlock.UP;
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION.entrySet().stream().filter(e -> e.getKey() != Direction.DOWN).collect(Util.toMap());
    private final Function<BlockState, VoxelShape> shapes;
    private static final int IGNITE_INSTANT = 60;
    private static final int IGNITE_EASY = 30;
    private static final int IGNITE_MEDIUM = 15;
    private static final int IGNITE_HARD = 5;
    private static final int BURN_INSTANT = 100;
    private static final int BURN_EASY = 60;
    private static final int BURN_MEDIUM = 20;
    private static final int BURN_HARD = 5;
    private final Object2IntMap<Block> igniteOdds = new Object2IntOpenHashMap();
    private final Object2IntMap<Block> burnOdds = new Object2IntOpenHashMap();

    public MapCodec<FireBlock> codec() {
        return CODEC;
    }

    public FireBlock(BlockBehaviour.Properties properties) {
        super(properties, 1.0f);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(AGE, 0)).setValue(NORTH, false)).setValue(EAST, false)).setValue(SOUTH, false)).setValue(WEST, false)).setValue(UP, false));
        this.shapes = this.makeShapes();
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        Map<Direction, VoxelShape> shapes = Shapes.rotateAll(Block.boxZ(16.0, 0.0, 1.0));
        return this.getShapeForEachState(state -> {
            VoxelShape shape = Shapes.empty();
            for (Map.Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
                if (!((Boolean)state.getValue(entry.getValue())).booleanValue()) continue;
                shape = Shapes.or(shape, (VoxelShape)shapes.get(entry.getKey()));
            }
            return shape.isEmpty() ? SHAPE : shape;
        }, AGE);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (this.canSurvive(state, level, pos)) {
            return this.getStateWithAge(level, pos, state.getValue(AGE));
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.apply(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.getStateForPlacement(context.getLevel(), context.getClickedPos());
    }

    protected BlockState getStateForPlacement(BlockGetter level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (this.canBurn(belowState) || belowState.isFaceSturdy(level, below, Direction.UP)) {
            return this.defaultBlockState();
        }
        BlockState result = this.defaultBlockState();
        for (Direction direction : Direction.values()) {
            BooleanProperty property = PROPERTY_BY_DIRECTION.get(direction);
            if (property == null) continue;
            result = (BlockState)result.setValue(property, this.canBurn(level.getBlockState(pos.relative(direction))));
        }
        return result;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) || this.isValidFireLocation(level, pos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean increasedBurnout;
        level.scheduleTick(pos, this, FireBlock.getFireTickDelay(level.getRandom()));
        if (!level.canSpreadFireAround(pos)) {
            return;
        }
        if (!state.canSurvive(level, pos)) {
            level.removeBlock(pos, false);
        }
        BlockState belowState = level.getBlockState(pos.below());
        boolean infiniBurn = belowState.is(level.dimensionType().infiniburn());
        int age = state.getValue(AGE);
        if (!infiniBurn && level.isRaining() && this.isNearRain(level, pos) && random.nextFloat() < 0.2f + (float)age * 0.03f) {
            level.removeBlock(pos, false);
            return;
        }
        int newAge = Math.min(15, age + random.nextInt(3) / 2);
        if (age != newAge) {
            state = (BlockState)state.setValue(AGE, newAge);
            level.setBlock(pos, state, 260);
        }
        if (!infiniBurn) {
            if (!this.isValidFireLocation(level, pos)) {
                BlockPos below = pos.below();
                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) || age > 3) {
                    level.removeBlock(pos, false);
                }
                return;
            }
            if (age == 15 && random.nextInt(4) == 0 && !this.canBurn(level.getBlockState(pos.below()))) {
                level.removeBlock(pos, false);
                return;
            }
        }
        int extra = (increasedBurnout = level.environmentAttributes().getValue(EnvironmentAttributes.INCREASED_FIRE_BURNOUT, pos).booleanValue()) ? -50 : 0;
        this.checkBurnOut(level, pos.east(), 300 + extra, random, age);
        this.checkBurnOut(level, pos.west(), 300 + extra, random, age);
        this.checkBurnOut(level, pos.below(), 250 + extra, random, age);
        this.checkBurnOut(level, pos.above(), 250 + extra, random, age);
        this.checkBurnOut(level, pos.north(), 300 + extra, random, age);
        this.checkBurnOut(level, pos.south(), 300 + extra, random, age);
        BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos();
        for (int xx = -1; xx <= 1; ++xx) {
            for (int zz = -1; zz <= 1; ++zz) {
                for (int yy = -1; yy <= 4; ++yy) {
                    if (xx == 0 && yy == 0 && zz == 0) continue;
                    int rate = 100;
                    if (yy > 1) {
                        rate += (yy - 1) * 100;
                    }
                    testPos.setWithOffset(pos, xx, yy, zz);
                    int igniteOdds = this.getIgniteOdds(level, testPos);
                    if (igniteOdds <= 0) continue;
                    int odds = (igniteOdds + 40 + level.getDifficulty().getId() * 7) / (age + 30);
                    if (increasedBurnout) {
                        odds /= 2;
                    }
                    if (odds <= 0 || random.nextInt(rate) > odds || level.isRaining() && this.isNearRain(level, testPos)) continue;
                    int spreadAge = Math.min(15, age + random.nextInt(5) / 4);
                    level.setBlock(testPos, this.getStateWithAge(level, testPos, spreadAge), 3);
                }
            }
        }
    }

    protected boolean isNearRain(Level level, BlockPos testPos) {
        return level.isRainingAt(testPos) || level.isRainingAt(testPos.west()) || level.isRainingAt(testPos.east()) || level.isRainingAt(testPos.north()) || level.isRainingAt(testPos.south());
    }

    private int getBurnOdds(BlockState state) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED).booleanValue()) {
            return 0;
        }
        return this.burnOdds.getInt((Object)state.getBlock());
    }

    private int getIgniteOdds(BlockState state) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED).booleanValue()) {
            return 0;
        }
        return this.igniteOdds.getInt((Object)state.getBlock());
    }

    private void checkBurnOut(Level level, BlockPos pos, int chance, RandomSource random, int age) {
        int odds = this.getBurnOdds(level.getBlockState(pos));
        if (random.nextInt(chance) < odds) {
            BlockState oldState = level.getBlockState(pos);
            if (random.nextInt(age + 10) < 5 && !level.isRainingAt(pos)) {
                int newAge = Math.min(age + random.nextInt(5) / 4, 15);
                level.setBlock(pos, this.getStateWithAge(level, pos, newAge), 3);
            } else {
                level.removeBlock(pos, false);
            }
            Block block = oldState.getBlock();
            if (block instanceof TntBlock) {
                TntBlock.prime(level, pos);
            }
        }
    }

    private BlockState getStateWithAge(LevelReader level, BlockPos pos, int age) {
        BlockState stateForPlacement = FireBlock.getState(level, pos);
        if (stateForPlacement.is(Blocks.FIRE)) {
            return (BlockState)stateForPlacement.setValue(AGE, age);
        }
        return stateForPlacement;
    }

    private boolean isValidFireLocation(BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (!this.canBurn(level.getBlockState(pos.relative(direction)))) continue;
            return true;
        }
        return false;
    }

    private int getIgniteOdds(LevelReader level, BlockPos pos) {
        if (!level.isEmptyBlock(pos)) {
            return 0;
        }
        int odds = 0;
        for (Direction direction : Direction.values()) {
            BlockState blockState = level.getBlockState(pos.relative(direction));
            odds = Math.max(this.getIgniteOdds(blockState), odds);
        }
        return odds;
    }

    @Override
    protected boolean canBurn(BlockState state) {
        return this.getIgniteOdds(state) > 0;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, FireBlock.getFireTickDelay(level.getRandom()));
    }

    private static int getFireTickDelay(RandomSource random) {
        return 30 + random.nextInt(10);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, NORTH, EAST, SOUTH, WEST, UP);
    }

    private void setFlammable(Block block, int igniteOdds, int burnOdds) {
        this.igniteOdds.put((Object)block, igniteOdds);
        this.burnOdds.put((Object)block, burnOdds);
    }

    public static void bootStrap() {
        FireBlock fire = (FireBlock)Blocks.FIRE;
        fire.setFlammable(Blocks.OAK_PLANKS, 5, 20);
        fire.setFlammable(Blocks.SPRUCE_PLANKS, 5, 20);
        fire.setFlammable(Blocks.BIRCH_PLANKS, 5, 20);
        fire.setFlammable(Blocks.JUNGLE_PLANKS, 5, 20);
        fire.setFlammable(Blocks.ACACIA_PLANKS, 5, 20);
        fire.setFlammable(Blocks.CHERRY_PLANKS, 5, 20);
        fire.setFlammable(Blocks.DARK_OAK_PLANKS, 5, 20);
        fire.setFlammable(Blocks.PALE_OAK_PLANKS, 5, 20);
        fire.setFlammable(Blocks.MANGROVE_PLANKS, 5, 20);
        fire.setFlammable(Blocks.BAMBOO_PLANKS, 5, 20);
        fire.setFlammable(Blocks.BAMBOO_MOSAIC, 5, 20);
        fire.setFlammable(Blocks.OAK_SLAB, 5, 20);
        fire.setFlammable(Blocks.SPRUCE_SLAB, 5, 20);
        fire.setFlammable(Blocks.BIRCH_SLAB, 5, 20);
        fire.setFlammable(Blocks.JUNGLE_SLAB, 5, 20);
        fire.setFlammable(Blocks.ACACIA_SLAB, 5, 20);
        fire.setFlammable(Blocks.CHERRY_SLAB, 5, 20);
        fire.setFlammable(Blocks.DARK_OAK_SLAB, 5, 20);
        fire.setFlammable(Blocks.PALE_OAK_SLAB, 5, 20);
        fire.setFlammable(Blocks.MANGROVE_SLAB, 5, 20);
        fire.setFlammable(Blocks.BAMBOO_SLAB, 5, 20);
        fire.setFlammable(Blocks.BAMBOO_MOSAIC_SLAB, 5, 20);
        fire.setFlammable(Blocks.OAK_FENCE_GATE, 5, 20);
        fire.setFlammable(Blocks.SPRUCE_FENCE_GATE, 5, 20);
        fire.setFlammable(Blocks.BIRCH_FENCE_GATE, 5, 20);
        fire.setFlammable(Blocks.JUNGLE_FENCE_GATE, 5, 20);
        fire.setFlammable(Blocks.ACACIA_FENCE_GATE, 5, 20);
        fire.setFlammable(Blocks.CHERRY_FENCE_GATE, 5, 20);
        fire.setFlammable(Blocks.DARK_OAK_FENCE_GATE, 5, 20);
        fire.setFlammable(Blocks.PALE_OAK_FENCE_GATE, 5, 20);
        fire.setFlammable(Blocks.MANGROVE_FENCE_GATE, 5, 20);
        fire.setFlammable(Blocks.BAMBOO_FENCE_GATE, 5, 20);
        fire.setFlammable(Blocks.OAK_FENCE, 5, 20);
        fire.setFlammable(Blocks.SPRUCE_FENCE, 5, 20);
        fire.setFlammable(Blocks.BIRCH_FENCE, 5, 20);
        fire.setFlammable(Blocks.JUNGLE_FENCE, 5, 20);
        fire.setFlammable(Blocks.ACACIA_FENCE, 5, 20);
        fire.setFlammable(Blocks.CHERRY_FENCE, 5, 20);
        fire.setFlammable(Blocks.DARK_OAK_FENCE, 5, 20);
        fire.setFlammable(Blocks.PALE_OAK_FENCE, 5, 20);
        fire.setFlammable(Blocks.MANGROVE_FENCE, 5, 20);
        fire.setFlammable(Blocks.BAMBOO_FENCE, 5, 20);
        fire.setFlammable(Blocks.OAK_STAIRS, 5, 20);
        fire.setFlammable(Blocks.BIRCH_STAIRS, 5, 20);
        fire.setFlammable(Blocks.SPRUCE_STAIRS, 5, 20);
        fire.setFlammable(Blocks.JUNGLE_STAIRS, 5, 20);
        fire.setFlammable(Blocks.ACACIA_STAIRS, 5, 20);
        fire.setFlammable(Blocks.CHERRY_STAIRS, 5, 20);
        fire.setFlammable(Blocks.DARK_OAK_STAIRS, 5, 20);
        fire.setFlammable(Blocks.PALE_OAK_STAIRS, 5, 20);
        fire.setFlammable(Blocks.MANGROVE_STAIRS, 5, 20);
        fire.setFlammable(Blocks.BAMBOO_STAIRS, 5, 20);
        fire.setFlammable(Blocks.BAMBOO_MOSAIC_STAIRS, 5, 20);
        fire.setFlammable(Blocks.OAK_LOG, 5, 5);
        fire.setFlammable(Blocks.SPRUCE_LOG, 5, 5);
        fire.setFlammable(Blocks.BIRCH_LOG, 5, 5);
        fire.setFlammable(Blocks.JUNGLE_LOG, 5, 5);
        fire.setFlammable(Blocks.ACACIA_LOG, 5, 5);
        fire.setFlammable(Blocks.CHERRY_LOG, 5, 5);
        fire.setFlammable(Blocks.PALE_OAK_LOG, 5, 5);
        fire.setFlammable(Blocks.DARK_OAK_LOG, 5, 5);
        fire.setFlammable(Blocks.MANGROVE_LOG, 5, 5);
        fire.setFlammable(Blocks.BAMBOO_BLOCK, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_OAK_LOG, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_SPRUCE_LOG, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_BIRCH_LOG, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_JUNGLE_LOG, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_ACACIA_LOG, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_CHERRY_LOG, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_DARK_OAK_LOG, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_PALE_OAK_LOG, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_MANGROVE_LOG, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_BAMBOO_BLOCK, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_OAK_WOOD, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_SPRUCE_WOOD, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_BIRCH_WOOD, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_JUNGLE_WOOD, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_ACACIA_WOOD, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_CHERRY_WOOD, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_DARK_OAK_WOOD, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_PALE_OAK_WOOD, 5, 5);
        fire.setFlammable(Blocks.STRIPPED_MANGROVE_WOOD, 5, 5);
        fire.setFlammable(Blocks.OAK_WOOD, 5, 5);
        fire.setFlammable(Blocks.SPRUCE_WOOD, 5, 5);
        fire.setFlammable(Blocks.BIRCH_WOOD, 5, 5);
        fire.setFlammable(Blocks.JUNGLE_WOOD, 5, 5);
        fire.setFlammable(Blocks.ACACIA_WOOD, 5, 5);
        fire.setFlammable(Blocks.CHERRY_WOOD, 5, 5);
        fire.setFlammable(Blocks.PALE_OAK_WOOD, 5, 5);
        fire.setFlammable(Blocks.DARK_OAK_WOOD, 5, 5);
        fire.setFlammable(Blocks.MANGROVE_WOOD, 5, 5);
        fire.setFlammable(Blocks.MANGROVE_ROOTS, 5, 20);
        fire.setFlammable(Blocks.OAK_LEAVES, 30, 60);
        fire.setFlammable(Blocks.SPRUCE_LEAVES, 30, 60);
        fire.setFlammable(Blocks.BIRCH_LEAVES, 30, 60);
        fire.setFlammable(Blocks.JUNGLE_LEAVES, 30, 60);
        fire.setFlammable(Blocks.ACACIA_LEAVES, 30, 60);
        fire.setFlammable(Blocks.CHERRY_LEAVES, 30, 60);
        fire.setFlammable(Blocks.DARK_OAK_LEAVES, 30, 60);
        fire.setFlammable(Blocks.PALE_OAK_LEAVES, 30, 60);
        fire.setFlammable(Blocks.MANGROVE_LEAVES, 30, 60);
        fire.setFlammable(Blocks.BOOKSHELF, 30, 20);
        fire.setFlammable(Blocks.TNT, 15, 100);
        fire.setFlammable(Blocks.SHORT_GRASS, 60, 100);
        fire.setFlammable(Blocks.FERN, 60, 100);
        fire.setFlammable(Blocks.DEAD_BUSH, 60, 100);
        fire.setFlammable(Blocks.SHORT_DRY_GRASS, 60, 100);
        fire.setFlammable(Blocks.TALL_DRY_GRASS, 60, 100);
        fire.setFlammable(Blocks.SUNFLOWER, 60, 100);
        fire.setFlammable(Blocks.LILAC, 60, 100);
        fire.setFlammable(Blocks.ROSE_BUSH, 60, 100);
        fire.setFlammable(Blocks.PEONY, 60, 100);
        fire.setFlammable(Blocks.TALL_GRASS, 60, 100);
        fire.setFlammable(Blocks.LARGE_FERN, 60, 100);
        fire.setFlammable(Blocks.DANDELION, 60, 100);
        fire.setFlammable(Blocks.GOLDEN_DANDELION, 60, 100);
        fire.setFlammable(Blocks.POPPY, 60, 100);
        fire.setFlammable(Blocks.OPEN_EYEBLOSSOM, 60, 100);
        fire.setFlammable(Blocks.CLOSED_EYEBLOSSOM, 60, 100);
        fire.setFlammable(Blocks.BLUE_ORCHID, 60, 100);
        fire.setFlammable(Blocks.ALLIUM, 60, 100);
        fire.setFlammable(Blocks.AZURE_BLUET, 60, 100);
        fire.setFlammable(Blocks.RED_TULIP, 60, 100);
        fire.setFlammable(Blocks.ORANGE_TULIP, 60, 100);
        fire.setFlammable(Blocks.WHITE_TULIP, 60, 100);
        fire.setFlammable(Blocks.PINK_TULIP, 60, 100);
        fire.setFlammable(Blocks.OXEYE_DAISY, 60, 100);
        fire.setFlammable(Blocks.CORNFLOWER, 60, 100);
        fire.setFlammable(Blocks.LILY_OF_THE_VALLEY, 60, 100);
        fire.setFlammable(Blocks.TORCHFLOWER, 60, 100);
        fire.setFlammable(Blocks.PITCHER_PLANT, 60, 100);
        fire.setFlammable(Blocks.WITHER_ROSE, 60, 100);
        fire.setFlammable(Blocks.PINK_PETALS, 60, 100);
        fire.setFlammable(Blocks.WILDFLOWERS, 60, 100);
        fire.setFlammable(Blocks.LEAF_LITTER, 60, 100);
        fire.setFlammable(Blocks.CACTUS_FLOWER, 60, 100);
        Blocks.WOOL.forEach(block -> fire.setFlammable((Block)block, 30, 60));
        fire.setFlammable(Blocks.VINE, 15, 100);
        fire.setFlammable(Blocks.COAL_BLOCK, 5, 5);
        fire.setFlammable(Blocks.HAY_BLOCK, 60, 20);
        fire.setFlammable(Blocks.TARGET, 15, 20);
        Blocks.CARPET.forEach(block -> fire.setFlammable((Block)block, 60, 20));
        fire.setFlammable(Blocks.PALE_MOSS_BLOCK, 5, 100);
        fire.setFlammable(Blocks.PALE_MOSS_CARPET, 5, 100);
        fire.setFlammable(Blocks.PALE_HANGING_MOSS, 5, 100);
        fire.setFlammable(Blocks.DRIED_KELP_BLOCK, 30, 60);
        fire.setFlammable(Blocks.BAMBOO, 60, 60);
        fire.setFlammable(Blocks.SCAFFOLDING, 60, 60);
        fire.setFlammable(Blocks.LECTERN, 30, 20);
        fire.setFlammable(Blocks.COMPOSTER, 5, 20);
        fire.setFlammable(Blocks.SWEET_BERRY_BUSH, 60, 100);
        fire.setFlammable(Blocks.BEEHIVE, 5, 20);
        fire.setFlammable(Blocks.BEE_NEST, 30, 20);
        fire.setFlammable(Blocks.AZALEA_LEAVES, 30, 60);
        fire.setFlammable(Blocks.FLOWERING_AZALEA_LEAVES, 30, 60);
        fire.setFlammable(Blocks.CAVE_VINES, 15, 60);
        fire.setFlammable(Blocks.CAVE_VINES_PLANT, 15, 60);
        fire.setFlammable(Blocks.SPORE_BLOSSOM, 60, 100);
        fire.setFlammable(Blocks.AZALEA, 30, 60);
        fire.setFlammable(Blocks.FLOWERING_AZALEA, 30, 60);
        fire.setFlammable(Blocks.BIG_DRIPLEAF, 60, 100);
        fire.setFlammable(Blocks.BIG_DRIPLEAF_STEM, 60, 100);
        fire.setFlammable(Blocks.SMALL_DRIPLEAF, 60, 100);
        fire.setFlammable(Blocks.HANGING_ROOTS, 30, 60);
        fire.setFlammable(Blocks.GLOW_LICHEN, 15, 100);
        fire.setFlammable(Blocks.FIREFLY_BUSH, 60, 100);
        fire.setFlammable(Blocks.BUSH, 60, 100);
        fire.setFlammable(Blocks.ACACIA_SHELF, 30, 20);
        fire.setFlammable(Blocks.BAMBOO_SHELF, 30, 20);
        fire.setFlammable(Blocks.BIRCH_SHELF, 30, 20);
        fire.setFlammable(Blocks.CHERRY_SHELF, 30, 20);
        fire.setFlammable(Blocks.DARK_OAK_SHELF, 30, 20);
        fire.setFlammable(Blocks.JUNGLE_SHELF, 30, 20);
        fire.setFlammable(Blocks.MANGROVE_SHELF, 30, 20);
        fire.setFlammable(Blocks.OAK_SHELF, 30, 20);
        fire.setFlammable(Blocks.PALE_OAK_SHELF, 30, 20);
        fire.setFlammable(Blocks.SPRUCE_SHELF, 30, 20);
    }
}

