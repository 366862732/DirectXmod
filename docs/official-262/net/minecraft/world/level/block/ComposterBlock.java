/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  it.unimi.dsi.fastutil.objects.Object2FloatMap
 *  it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ComposterBlock
extends Block
implements WorldlyContainerHolder {
    public static final MapCodec<ComposterBlock> CODEC = ComposterBlock.simpleCodec(ComposterBlock::new);
    public static final int READY = 8;
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 7;
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_COMPOSTER;
    public static final Object2FloatMap<ItemLike> COMPOSTABLES = new Object2FloatOpenHashMap();
    private static final int HOLE_WIDTH = 12;
    private static final VoxelShape[] SHAPES = Util.make(() -> {
        VoxelShape[] shapes = Block.boxes(8, level -> Shapes.join(Shapes.block(), Block.column(12.0, Math.clamp((long)(1 + level * 2), 2, 16), 16.0), BooleanOp.ONLY_FIRST));
        shapes[8] = shapes[7];
        return shapes;
    });

    public MapCodec<ComposterBlock> codec() {
        return CODEC;
    }

    public static void bootStrap() {
        COMPOSTABLES.defaultReturnValue(-1.0f);
        float low = 0.3f;
        float lowMid = 0.5f;
        float mid = 0.65f;
        float midHigh = 0.85f;
        float high = 1.0f;
        ComposterBlock.add(0.3f, Items.JUNGLE_LEAVES);
        ComposterBlock.add(0.3f, Items.OAK_LEAVES);
        ComposterBlock.add(0.3f, Items.SPRUCE_LEAVES);
        ComposterBlock.add(0.3f, Items.DARK_OAK_LEAVES);
        ComposterBlock.add(0.3f, Items.PALE_OAK_LEAVES);
        ComposterBlock.add(0.3f, Items.ACACIA_LEAVES);
        ComposterBlock.add(0.3f, Items.CHERRY_LEAVES);
        ComposterBlock.add(0.3f, Items.BIRCH_LEAVES);
        ComposterBlock.add(0.3f, Items.AZALEA_LEAVES);
        ComposterBlock.add(0.3f, Items.MANGROVE_LEAVES);
        ComposterBlock.add(0.3f, Items.OAK_SAPLING);
        ComposterBlock.add(0.3f, Items.SPRUCE_SAPLING);
        ComposterBlock.add(0.3f, Items.BIRCH_SAPLING);
        ComposterBlock.add(0.3f, Items.JUNGLE_SAPLING);
        ComposterBlock.add(0.3f, Items.ACACIA_SAPLING);
        ComposterBlock.add(0.3f, Items.CHERRY_SAPLING);
        ComposterBlock.add(0.3f, Items.DARK_OAK_SAPLING);
        ComposterBlock.add(0.3f, Items.PALE_OAK_SAPLING);
        ComposterBlock.add(0.3f, Items.MANGROVE_PROPAGULE);
        ComposterBlock.add(0.3f, Items.BEETROOT_SEEDS);
        ComposterBlock.add(0.3f, Items.DRIED_KELP);
        ComposterBlock.add(0.3f, Items.SHORT_GRASS);
        ComposterBlock.add(0.3f, Items.KELP);
        ComposterBlock.add(0.3f, Items.MELON_SEEDS);
        ComposterBlock.add(0.3f, Items.PUMPKIN_SEEDS);
        ComposterBlock.add(0.3f, Items.SEAGRASS);
        ComposterBlock.add(0.3f, Items.SWEET_BERRIES);
        ComposterBlock.add(0.3f, Items.GLOW_BERRIES);
        ComposterBlock.add(0.3f, Items.WHEAT_SEEDS);
        ComposterBlock.add(0.3f, Items.MOSS_CARPET);
        ComposterBlock.add(0.3f, Items.PALE_MOSS_CARPET);
        ComposterBlock.add(0.3f, Items.PALE_HANGING_MOSS);
        ComposterBlock.add(0.3f, Items.PINK_PETALS);
        ComposterBlock.add(0.3f, Items.WILDFLOWERS);
        ComposterBlock.add(0.3f, Items.LEAF_LITTER);
        ComposterBlock.add(0.3f, Items.SMALL_DRIPLEAF);
        ComposterBlock.add(0.3f, Items.HANGING_ROOTS);
        ComposterBlock.add(0.3f, Items.MANGROVE_ROOTS);
        ComposterBlock.add(0.3f, Items.TORCHFLOWER_SEEDS);
        ComposterBlock.add(0.3f, Items.PITCHER_POD);
        ComposterBlock.add(0.3f, Items.FIREFLY_BUSH);
        ComposterBlock.add(0.3f, Items.BUSH);
        ComposterBlock.add(0.3f, Items.CACTUS_FLOWER);
        ComposterBlock.add(0.3f, Items.DRY_SHORT_GRASS);
        ComposterBlock.add(0.3f, Items.DRY_TALL_GRASS);
        ComposterBlock.add(0.5f, Items.DRIED_KELP_BLOCK);
        ComposterBlock.add(0.5f, Items.TALL_GRASS);
        ComposterBlock.add(0.5f, Items.FLOWERING_AZALEA_LEAVES);
        ComposterBlock.add(0.5f, Items.CACTUS);
        ComposterBlock.add(0.5f, Items.SUGAR_CANE);
        ComposterBlock.add(0.5f, Items.VINE);
        ComposterBlock.add(0.5f, Items.NETHER_SPROUTS);
        ComposterBlock.add(0.5f, Items.WEEPING_VINES);
        ComposterBlock.add(0.5f, Items.TWISTING_VINES);
        ComposterBlock.add(0.5f, Items.MELON_SLICE);
        ComposterBlock.add(0.5f, Items.GLOW_LICHEN);
        ComposterBlock.add(0.65f, Items.SEA_PICKLE);
        ComposterBlock.add(0.65f, Items.LILY_PAD);
        ComposterBlock.add(0.65f, Items.PUMPKIN);
        ComposterBlock.add(0.65f, Items.CARVED_PUMPKIN);
        ComposterBlock.add(0.65f, Items.MELON);
        ComposterBlock.add(0.65f, Items.APPLE);
        ComposterBlock.add(0.65f, Items.BEETROOT);
        ComposterBlock.add(0.65f, Items.CARROT);
        ComposterBlock.add(0.65f, Items.COCOA_BEANS);
        ComposterBlock.add(0.65f, Items.POTATO);
        ComposterBlock.add(0.65f, Items.WHEAT);
        ComposterBlock.add(0.65f, Items.BROWN_MUSHROOM);
        ComposterBlock.add(0.65f, Items.RED_MUSHROOM);
        ComposterBlock.add(0.65f, Items.MUSHROOM_STEM);
        ComposterBlock.add(0.65f, Items.CRIMSON_FUNGUS);
        ComposterBlock.add(0.65f, Items.WARPED_FUNGUS);
        ComposterBlock.add(0.65f, Items.NETHER_WART);
        ComposterBlock.add(0.65f, Items.CRIMSON_ROOTS);
        ComposterBlock.add(0.65f, Items.WARPED_ROOTS);
        ComposterBlock.add(0.65f, Items.SHROOMLIGHT);
        ComposterBlock.add(0.65f, Items.DANDELION);
        ComposterBlock.add(0.65f, Items.POPPY);
        ComposterBlock.add(0.65f, Items.BLUE_ORCHID);
        ComposterBlock.add(0.65f, Items.ALLIUM);
        ComposterBlock.add(0.65f, Items.AZURE_BLUET);
        ComposterBlock.add(0.65f, Items.RED_TULIP);
        ComposterBlock.add(0.65f, Items.ORANGE_TULIP);
        ComposterBlock.add(0.65f, Items.WHITE_TULIP);
        ComposterBlock.add(0.65f, Items.PINK_TULIP);
        ComposterBlock.add(0.65f, Items.OXEYE_DAISY);
        ComposterBlock.add(0.65f, Items.CORNFLOWER);
        ComposterBlock.add(0.65f, Items.LILY_OF_THE_VALLEY);
        ComposterBlock.add(0.65f, Items.WITHER_ROSE);
        ComposterBlock.add(0.65f, Items.OPEN_EYEBLOSSOM);
        ComposterBlock.add(0.65f, Items.CLOSED_EYEBLOSSOM);
        ComposterBlock.add(0.65f, Items.FERN);
        ComposterBlock.add(0.65f, Items.SUNFLOWER);
        ComposterBlock.add(0.65f, Items.LILAC);
        ComposterBlock.add(0.65f, Items.ROSE_BUSH);
        ComposterBlock.add(0.65f, Items.PEONY);
        ComposterBlock.add(0.65f, Items.LARGE_FERN);
        ComposterBlock.add(0.65f, Items.SPORE_BLOSSOM);
        ComposterBlock.add(0.65f, Items.AZALEA);
        ComposterBlock.add(0.65f, Items.MOSS_BLOCK);
        ComposterBlock.add(0.65f, Items.PALE_MOSS_BLOCK);
        ComposterBlock.add(0.65f, Items.BIG_DRIPLEAF);
        ComposterBlock.add(0.85f, Items.HAY_BLOCK);
        ComposterBlock.add(0.85f, Items.BROWN_MUSHROOM_BLOCK);
        ComposterBlock.add(0.85f, Items.RED_MUSHROOM_BLOCK);
        ComposterBlock.add(0.85f, Items.NETHER_WART_BLOCK);
        ComposterBlock.add(0.85f, Items.WARPED_WART_BLOCK);
        ComposterBlock.add(0.85f, Items.FLOWERING_AZALEA);
        ComposterBlock.add(0.85f, Items.BREAD);
        ComposterBlock.add(0.85f, Items.BAKED_POTATO);
        ComposterBlock.add(0.85f, Items.COOKIE);
        ComposterBlock.add(0.85f, Items.TORCHFLOWER);
        ComposterBlock.add(0.85f, Items.PITCHER_PLANT);
        ComposterBlock.add(1.0f, Items.CAKE);
        ComposterBlock.add(1.0f, Items.PUMPKIN_PIE);
    }

    private static void add(float value, ItemLike item) {
        COMPOSTABLES.put((Object)item.asItem(), value);
    }

    public ComposterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(LEVEL, 0));
    }

    public static void handleFill(Level level, BlockPos pos, boolean success) {
        BlockState state = level.getBlockState(pos);
        level.playLocalSound(pos, success ? SoundEvents.COMPOSTER_FILL_SUCCESS : SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0f, 1.0f, false);
        double centerHeight = state.getShape(level, pos).max(Direction.Axis.Y, 0.5, 0.5) + 0.03125;
        double sideOffsetPixels = 2.0;
        double sideOffset = 0.1875;
        double width = 0.625;
        RandomSource random = level.getRandom();
        for (int i = 0; i < 10; ++i) {
            double xa = random.nextGaussian() * 0.02;
            double ya = random.nextGaussian() * 0.02;
            double za = random.nextGaussian() * 0.02;
            level.addParticle(ParticleTypes.COMPOSTER, (double)pos.getX() + 0.1875 + 0.625 * (double)random.nextFloat(), (double)pos.getY() + centerHeight + (double)random.nextFloat() * (1.0 - centerHeight), (double)pos.getZ() + 0.1875 + 0.625 * (double)random.nextFloat(), xa, ya, za);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LEVEL)];
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[0];
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (state.getValue(LEVEL) == 7) {
            level.scheduleTick(pos, state.getBlock(), 20);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        int fillLevel = state.getValue(LEVEL);
        if (fillLevel < 8 && COMPOSTABLES.containsKey((Object)itemStack.getItem())) {
            if (fillLevel < 7 && !level.isClientSide()) {
                BlockState newState = ComposterBlock.addItem(player, state, level, pos, itemStack);
                level.levelEvent(1500, pos, state != newState ? 1 : 0);
                player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                itemStack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        int fillLevel = state.getValue(LEVEL);
        if (fillLevel == 8) {
            ComposterBlock.extractProduce(player, state, level, pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public static BlockState insertItem(Entity sourceEntity, BlockState state, ServerLevel level, ItemStack itemStack, BlockPos pos) {
        int fillLevel = state.getValue(LEVEL);
        if (fillLevel < 7 && COMPOSTABLES.containsKey((Object)itemStack.getItem())) {
            BlockState newState = ComposterBlock.addItem(sourceEntity, state, level, pos, itemStack);
            itemStack.shrink(1);
            return newState;
        }
        return state;
    }

    public static BlockState extractProduce(Entity sourceEntity, BlockState state, Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            Vec3 itemPos = Vec3.atLowerCornerWithOffset(pos, 0.5, 1.01, 0.5).offsetRandomXZ(level.getRandom(), 0.7f);
            ItemEntity entity = new ItemEntity(level, itemPos.x(), itemPos.y(), itemPos.z(), new ItemStack(Items.BONE_MEAL));
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
        BlockState emptyState = ComposterBlock.empty(sourceEntity, state, level, pos);
        level.playSound(null, pos, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
        return emptyState;
    }

    private static BlockState empty(@Nullable Entity sourceEntity, BlockState state, LevelAccessor level, BlockPos pos) {
        BlockState newState = (BlockState)state.setValue(LEVEL, 0);
        level.setBlock(pos, newState, 3);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(sourceEntity, newState));
        return newState;
    }

    private static BlockState addItem(@Nullable Entity sourceEntity, BlockState state, LevelAccessor level, BlockPos pos, ItemStack itemStack) {
        int fillLevel = state.getValue(LEVEL);
        float chance = COMPOSTABLES.getFloat((Object)itemStack.getItem());
        if (fillLevel == 0 && chance > 0.0f || level.getRandom().nextDouble() < (double)chance) {
            int newLevel = fillLevel + 1;
            BlockState newState = (BlockState)state.setValue(LEVEL, newLevel);
            level.setBlock(pos, newState, 3);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(sourceEntity, newState));
            if (newLevel == 7) {
                level.scheduleTick(pos, state.getBlock(), 20);
            }
            return newState;
        }
        return state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LEVEL) == 7) {
            level.setBlock(pos, (BlockState)state.cycle(LEVEL), 3);
            level.playSound(null, pos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(LEVEL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        int contentLevel = state.getValue(LEVEL);
        if (contentLevel == 8) {
            return new OutputContainer(state, level, pos, new ItemStack(Items.BONE_MEAL));
        }
        if (contentLevel < 7) {
            return new InputContainer(state, level, pos);
        }
        return new EmptyContainer();
    }

    private static class OutputContainer
    extends SimpleContainer
    implements WorldlyContainer {
        private final BlockState state;
        private final LevelAccessor level;
        private final BlockPos pos;
        private boolean changed;

        public OutputContainer(BlockState state, LevelAccessor level, BlockPos pos, ItemStack contents) {
            super(contents);
            this.state = state;
            this.level = level;
            this.pos = pos;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int[] getSlotsForFace(Direction direction) {
            int[] nArray;
            if (direction == Direction.DOWN) {
                int[] nArray2 = new int[1];
                nArray = nArray2;
                nArray2[0] = 0;
            } else {
                nArray = new int[]{};
            }
            return nArray;
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction direction) {
            return false;
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
            return !this.changed && direction == Direction.DOWN && itemStack.is(Items.BONE_MEAL);
        }

        @Override
        public void setChanged() {
            ComposterBlock.empty(null, this.state, this.level, this.pos);
            this.changed = true;
        }
    }

    private static class InputContainer
    extends SimpleContainer
    implements WorldlyContainer {
        private final BlockState state;
        private final LevelAccessor level;
        private final BlockPos pos;
        private boolean changed;

        public InputContainer(BlockState state, LevelAccessor level, BlockPos pos) {
            super(1);
            this.state = state;
            this.level = level;
            this.pos = pos;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int[] getSlotsForFace(Direction direction) {
            int[] nArray;
            if (direction == Direction.UP) {
                int[] nArray2 = new int[1];
                nArray = nArray2;
                nArray2[0] = 0;
            } else {
                nArray = new int[]{};
            }
            return nArray;
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction direction) {
            return !this.changed && direction == Direction.UP && COMPOSTABLES.containsKey((Object)itemStack.getItem());
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
            return false;
        }

        @Override
        public void setChanged() {
            ItemStack contents = this.getItem(0);
            if (!contents.isEmpty()) {
                this.changed = true;
                BlockState newState = ComposterBlock.addItem(null, this.state, this.level, this.pos, contents);
                this.level.levelEvent(1500, this.pos, newState != this.state ? 1 : 0);
                this.removeItemNoUpdate(0);
            }
        }
    }

    private static class EmptyContainer
    extends SimpleContainer
    implements WorldlyContainer {
        public EmptyContainer() {
            super(0);
        }

        @Override
        public int[] getSlotsForFace(Direction direction) {
            return new int[0];
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction direction) {
            return false;
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
            return false;
        }
    }
}

