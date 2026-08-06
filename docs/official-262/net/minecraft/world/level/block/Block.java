/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.cache.CacheBuilder
 *  com.google.common.cache.CacheLoader
 *  com.google.common.cache.LoadingCache
 *  com.google.common.collect.ImmutableMap
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.MapCodec
 *  it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.block;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMapper;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Block
extends BlockBehaviour
implements ItemLike {
    public static final MapCodec<Block> CODEC = Block.simpleCodec(Block::new);
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Holder.Reference<Block> builtInRegistryHolder = BuiltInRegistries.BLOCK.createIntrusiveHolder(this);
    public static final IdMapper<BlockState> BLOCK_STATE_REGISTRY = new IdMapper();
    private static final LoadingCache<VoxelShape, Boolean> SHAPE_FULL_BLOCK_CACHE = CacheBuilder.newBuilder().maximumSize(512L).weakKeys().build((CacheLoader)new CacheLoader<VoxelShape, Boolean>(){

        public Boolean load(VoxelShape shape) {
            return !Shapes.joinIsNotEmpty(Shapes.block(), shape, BooleanOp.NOT_SAME);
        }
    });
    public static final int UPDATE_NEIGHBORS = 1;
    public static final int UPDATE_CLIENTS = 2;
    public static final int UPDATE_INVISIBLE = 4;
    public static final int UPDATE_IMMEDIATE = 8;
    public static final int UPDATE_KNOWN_SHAPE = 16;
    public static final int UPDATE_SUPPRESS_DROPS = 32;
    public static final int UPDATE_MOVE_BY_PISTON = 64;
    public static final int UPDATE_SKIP_SHAPE_UPDATE_ON_WIRE = 128;
    public static final int UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS = 256;
    public static final int UPDATE_SKIP_ON_PLACE = 512;
    public static final @UpdateFlags int UPDATE_NONE = 260;
    public static final @UpdateFlags int UPDATE_ALL = 3;
    public static final @UpdateFlags int UPDATE_ALL_IMMEDIATE = 11;
    public static final @UpdateFlags int UPDATE_SKIP_ALL_SIDEEFFECTS = 816;
    public static final float INDESTRUCTIBLE = -1.0f;
    public static final float INSTANT = 0.0f;
    public static final int UPDATE_LIMIT = 512;
    protected final StateDefinition<Block, BlockState> stateDefinition;
    private BlockState defaultBlockState;
    private @Nullable Item item;
    private static final int CACHE_SIZE = 256;
    private static final ThreadLocal<Object2ByteLinkedOpenHashMap<ShapePairKey>> OCCLUSION_CACHE = ThreadLocal.withInitial(() -> {
        Object2ByteLinkedOpenHashMap<ShapePairKey> map = new Object2ByteLinkedOpenHashMap<ShapePairKey>(256, 0.25f){

            protected void rehash(int newN) {
            }
        };
        map.defaultReturnValue((byte)127);
        return map;
    });

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public static int getId(@Nullable BlockState blockState) {
        if (blockState == null) {
            return 0;
        }
        int id = BLOCK_STATE_REGISTRY.getId(blockState);
        return id == -1 ? 0 : id;
    }

    public static BlockState stateById(int idWithData) {
        BlockState state = BLOCK_STATE_REGISTRY.byId(idWithData);
        return state == null ? Blocks.AIR.defaultBlockState() : state;
    }

    public static Block byItem(@Nullable Item item) {
        if (item instanceof BlockItem) {
            BlockItem blockItem = (BlockItem)item;
            return blockItem.getBlock();
        }
        return Blocks.AIR;
    }

    public static BlockState pushEntitiesUp(BlockState state, BlockState newState, LevelAccessor level, BlockPos pos) {
        VoxelShape offsetShape = Shapes.joinUnoptimized(state.getCollisionShape(level, pos), newState.getCollisionShape(level, pos), BooleanOp.ONLY_SECOND).move(pos);
        if (offsetShape.isEmpty()) {
            return newState;
        }
        List<Entity> collidingEntities = level.getEntities(null, offsetShape.bounds());
        for (Entity collidingEntity : collidingEntities) {
            double offset = Shapes.collide(Direction.Axis.Y, collidingEntity.getBoundingBox().move(0.0, 1.0, 0.0), List.of(offsetShape), -1.0);
            collidingEntity.teleportRelative(0.0, 1.0 + offset, 0.0);
        }
        return newState;
    }

    public static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return Shapes.box(minX / 16.0, minY / 16.0, minZ / 16.0, maxX / 16.0, maxY / 16.0, maxZ / 16.0);
    }

    public static VoxelShape[] boxes(int endInclusive, IntFunction<VoxelShape> voxelShapeFactory) {
        return (VoxelShape[])IntStream.rangeClosed(0, endInclusive).mapToObj(voxelShapeFactory).toArray(VoxelShape[]::new);
    }

    public static VoxelShape cube(double size) {
        return Block.cube(size, size, size);
    }

    public static VoxelShape cube(double sizeX, double sizeY, double sizeZ) {
        double halfY = sizeY / 2.0;
        return Block.column(sizeX, sizeZ, 8.0 - halfY, 8.0 + halfY);
    }

    public static VoxelShape column(double sizeXZ, double minY, double maxY) {
        return Block.column(sizeXZ, sizeXZ, minY, maxY);
    }

    public static VoxelShape column(double sizeX, double sizeZ, double minY, double maxY) {
        double halfX = sizeX / 2.0;
        double halfZ = sizeZ / 2.0;
        return Block.box(8.0 - halfX, minY, 8.0 - halfZ, 8.0 + halfX, maxY, 8.0 + halfZ);
    }

    public static VoxelShape boxZ(double sizeXY, double minZ, double maxZ) {
        return Block.boxZ(sizeXY, sizeXY, minZ, maxZ);
    }

    public static VoxelShape boxZ(double sizeX, double sizeY, double minZ, double maxZ) {
        double halfY = sizeY / 2.0;
        return Block.boxZ(sizeX, 8.0 - halfY, 8.0 + halfY, minZ, maxZ);
    }

    public static VoxelShape boxZ(double sizeX, double minY, double maxY, double minZ, double maxZ) {
        double halfX = sizeX / 2.0;
        return Block.box(8.0 - halfX, minY, minZ, 8.0 + halfX, maxY, maxZ);
    }

    public static BlockState updateFromNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockState newState = state;
        BlockPos.MutableBlockPos neighbourPos = new BlockPos.MutableBlockPos();
        for (Direction direction : UPDATE_SHAPE_ORDER) {
            neighbourPos.setWithOffset((Vec3i)pos, direction);
            newState = newState.updateShape(level, level, pos, direction, neighbourPos, level.getBlockState(neighbourPos), level.getRandom());
        }
        return newState;
    }

    public static void updateOrDestroy(BlockState blockState, BlockState newState, LevelAccessor level, BlockPos blockPos, @UpdateFlags int updateFlags) {
        Block.updateOrDestroy(blockState, newState, level, blockPos, updateFlags, 512);
    }

    public static void updateOrDestroy(BlockState blockState, BlockState newState, LevelAccessor level, BlockPos blockPos, @UpdateFlags int updateFlags, int updateLimit) {
        if (newState != blockState) {
            if (newState.isAir()) {
                if (!level.isClientSide()) {
                    level.destroyBlock(blockPos, (updateFlags & 0x20) == 0, null, updateLimit);
                }
            } else {
                level.setBlock(blockPos, newState, updateFlags & 0xFFFFFFDF, updateLimit);
            }
        }
    }

    public Block(BlockBehaviour.Properties properties) {
        super(properties);
        String className;
        StateDefinition.Builder<Block, BlockState> builder = new StateDefinition.Builder<Block, BlockState>(this);
        this.createBlockStateDefinition(builder);
        this.stateDefinition = builder.create(Block::defaultBlockState, BlockState::new);
        this.registerDefaultState(this.stateDefinition.any());
        if (SharedConstants.IS_RUNNING_IN_IDE && !(className = this.getClass().getSimpleName()).endsWith("Block")) {
            LOGGER.error("Block classes should end with Block and {} doesn't.", (Object)className);
        }
    }

    public static boolean isExceptionForConnection(BlockState state) {
        return state.getBlock() instanceof LeavesBlock || state.is(Blocks.BARRIER) || state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.JACK_O_LANTERN) || state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN) || state.is(BlockTags.SHULKER_BOXES);
    }

    protected static boolean dropFromBlockInteractLootTable(ServerLevel level, ResourceKey<LootTable> key, BlockState interactedBlockState, @Nullable BlockEntity interactedBlockEntity, @Nullable ItemInstance tool, @Nullable Entity interactingEntity, BiConsumer<ServerLevel, ItemStack> consumer) {
        return Block.dropFromLootTable(level, key, params -> params.withParameter(LootContextParams.BLOCK_STATE, interactedBlockState).withOptionalParameter(LootContextParams.BLOCK_ENTITY, interactedBlockEntity).withOptionalParameter(LootContextParams.INTERACTING_ENTITY, interactingEntity).withOptionalParameter(LootContextParams.TOOL, tool).create(LootContextParamSets.BLOCK_INTERACT), consumer);
    }

    protected static boolean dropFromLootTable(ServerLevel level, ResourceKey<LootTable> key, Function<LootParams.Builder, LootParams> paramsBuilder, BiConsumer<ServerLevel, ItemStack> consumer) {
        LootParams params;
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(key);
        ObjectArrayList<ItemStack> drops = lootTable.getRandomItems(params = paramsBuilder.apply(new LootParams.Builder(level)));
        if (!drops.isEmpty()) {
            drops.forEach(stack -> consumer.accept(level, (ItemStack)stack));
            return true;
        }
        return false;
    }

    public static boolean shouldRenderFace(BlockState state, BlockState neighborState, Direction direction) {
        VoxelShape occluder = neighborState.getFaceOcclusionShape(direction.getOpposite());
        if (occluder == Shapes.block()) {
            return false;
        }
        if (state.skipRendering(neighborState, direction)) {
            return false;
        }
        if (occluder == Shapes.empty()) {
            return true;
        }
        VoxelShape shape = state.getFaceOcclusionShape(direction);
        if (shape == Shapes.empty()) {
            return true;
        }
        ShapePairKey key = new ShapePairKey(shape, occluder);
        Object2ByteLinkedOpenHashMap<ShapePairKey> cache = OCCLUSION_CACHE.get();
        byte cached = cache.getAndMoveToFirst((Object)key);
        if (cached != 127) {
            return cached != 0;
        }
        boolean result = Shapes.joinIsNotEmpty(shape, occluder, BooleanOp.ONLY_FIRST);
        if (cache.size() == 256) {
            cache.removeLastByte();
        }
        cache.putAndMoveToFirst((Object)key, (byte)(result ? 1 : 0));
        return result;
    }

    public static boolean canSupportRigidBlock(BlockGetter level, BlockPos below) {
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP, SupportType.RIGID);
    }

    public static boolean canSupportCenter(LevelReader level, BlockPos belowPos, Direction direction) {
        BlockState state = level.getBlockState(belowPos);
        if (direction == Direction.DOWN && state.is(BlockTags.UNSTABLE_BOTTOM_CENTER)) {
            return false;
        }
        return state.isFaceSturdy(level, belowPos, direction, SupportType.CENTER);
    }

    public static boolean isFaceFull(VoxelShape shape, Direction direction) {
        VoxelShape faceShape = shape.getFaceShape(direction);
        return Block.isShapeFullBlock(faceShape);
    }

    public static boolean isShapeFullBlock(VoxelShape shape) {
        return (Boolean)SHAPE_FULL_BLOCK_CACHE.getUnchecked((Object)shape);
    }

    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }

    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
    }

    public static List<ItemStack> getDrops(BlockState state, ServerLevel level, BlockPos pos, @Nullable BlockEntity blockEntity) {
        LootParams.Builder params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
        return state.getDrops(params);
    }

    public static List<ItemStack> getDrops(BlockState state, ServerLevel level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity breaker, ItemInstance tool) {
        LootParams.Builder params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL, tool).withOptionalParameter(LootContextParams.THIS_ENTITY, breaker).withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
        return state.getDrops(params);
    }

    public static void dropResources(BlockState state, Level level, BlockPos pos) {
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            Block.getDrops(state, serverLevel, pos, null).forEach(stack -> Block.popResource(level, pos, stack));
            state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, true);
        }
    }

    public static void dropResources(BlockState state, LevelAccessor level, BlockPos pos, @Nullable BlockEntity blockEntity) {
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            Block.getDrops(state, serverLevel, pos, blockEntity).forEach(stack -> Block.popResource((Level)serverLevel, pos, stack));
            state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, true);
        }
    }

    public static void dropResources(BlockState state, Level level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity breaker, ItemStack tool) {
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            Block.getDrops(state, serverLevel, pos, blockEntity, breaker, tool).forEach(stack -> Block.popResource(level, pos, stack));
            state.spawnAfterBreak(serverLevel, pos, tool, true);
        }
    }

    public static void popResource(Level level, BlockPos pos, ItemStack itemStack) {
        double halfHeight = (double)EntityTypes.ITEM.getHeight() / 2.0;
        RandomSource random = level.getRandom();
        double x = (double)pos.getX() + 0.5 + Mth.nextDouble(random, -0.25, 0.25);
        double y = (double)pos.getY() + 0.5 + Mth.nextDouble(random, -0.25, 0.25) - halfHeight;
        double z = (double)pos.getZ() + 0.5 + Mth.nextDouble(random, -0.25, 0.25);
        Block.popResource(level, () -> new ItemEntity(level, x, y, z, itemStack), itemStack);
    }

    public static void popResourceFromFace(Level level, BlockPos pos, Direction face, ItemStack itemStack) {
        int stepX = face.getStepX();
        int stepY = face.getStepY();
        int stepZ = face.getStepZ();
        double halfWidth = (double)EntityTypes.ITEM.getWidth() / 2.0;
        double halfHeight = (double)EntityTypes.ITEM.getHeight() / 2.0;
        RandomSource random = level.getRandom();
        double x = (double)pos.getX() + 0.5 + (stepX == 0 ? Mth.nextDouble(random, -0.25, 0.25) : (double)stepX * (0.5 + halfWidth));
        double y = (double)pos.getY() + 0.5 + (stepY == 0 ? Mth.nextDouble(random, -0.25, 0.25) : (double)stepY * (0.5 + halfHeight)) - halfHeight;
        double z = (double)pos.getZ() + 0.5 + (stepZ == 0 ? Mth.nextDouble(random, -0.25, 0.25) : (double)stepZ * (0.5 + halfWidth));
        double deltaX = stepX == 0 ? Mth.nextDouble(random, -0.1, 0.1) : (double)stepX * 0.1;
        double deltaY = stepY == 0 ? Mth.nextDouble(random, 0.0, 0.1) : (double)stepY * 0.1 + 0.1;
        double deltaZ = stepZ == 0 ? Mth.nextDouble(random, -0.1, 0.1) : (double)stepZ * 0.1;
        Block.popResource(level, () -> new ItemEntity(level, x, y, z, itemStack, deltaX, deltaY, deltaZ), itemStack);
    }

    private static void popResource(Level level, Supplier<ItemEntity> entityFactory, ItemStack itemStack) {
        block3: {
            block2: {
                if (!(level instanceof ServerLevel)) break block2;
                ServerLevel serverLevel = (ServerLevel)level;
                if (!itemStack.isEmpty() && serverLevel.getGameRules().get(GameRules.BLOCK_DROPS).booleanValue()) break block3;
            }
            return;
        }
        ItemEntity entity = entityFactory.get();
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    protected void popExperience(ServerLevel level, BlockPos pos, int amount) {
        if (level.getGameRules().get(GameRules.BLOCK_DROPS).booleanValue()) {
            ExperienceOrb.award(level, Vec3.atCenterOf(pos), amount);
        }
    }

    public float getExplosionResistance() {
        return this.explosionResistance;
    }

    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
    }

    public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
    }

    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack destroyedWith) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005f);
        Block.dropResources(state, level, pos, blockEntity, player, destroyedWith);
    }

    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
    }

    public boolean isPossibleToRespawnInThis(BlockState state) {
        return !state.isSolid() && !state.liquid();
    }

    public MutableComponent getName() {
        return Component.translatable(this.getDescriptionId());
    }

    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, 1.0f, entity.damageSources().fall());
    }

    public float getBounceRestitution() {
        return this.bounceRestitution;
    }

    public float getFriction() {
        return this.friction;
    }

    public float getSpeedFactor() {
        return this.speedFactor;
    }

    public float getJumpFactor() {
        return this.jumpFactor;
    }

    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        level.levelEvent(player, 2001, pos, Block.getId(state));
    }

    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        this.spawnDestroyParticles(level, player, pos, state);
        if (state.is(BlockTags.GUARDED_BY_PIGLINS) && level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            PiglinAi.angerNearbyPiglins(serverLevel, player, false);
        }
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        return state;
    }

    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
    }

    public boolean dropFromExplosion(Explosion explosion) {
        return true;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    }

    public StateDefinition<Block, BlockState> getStateDefinition() {
        return this.stateDefinition;
    }

    protected final void registerDefaultState(BlockState state) {
        this.defaultBlockState = state;
    }

    public final BlockState defaultBlockState() {
        return this.defaultBlockState;
    }

    public final BlockState withPropertiesOf(BlockState source) {
        BlockState result = this.defaultBlockState();
        for (Property<?> property : source.getBlock().getStateDefinition().getProperties()) {
            if (!result.hasProperty(property)) continue;
            result = Block.copyProperty(source, result, property);
        }
        return result;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return (BlockState)to.setValue(property, from.getValue(property));
    }

    @Override
    public Item asItem() {
        if (this.item == null) {
            this.item = Item.byBlock(this);
        }
        return this.item;
    }

    public boolean hasDynamicShape() {
        return this.dynamicShape;
    }

    public String toString() {
        return "Block{" + BuiltInRegistries.BLOCK.wrapAsHolder(this).getRegisteredName() + "}";
    }

    @Override
    protected Block asBlock() {
        return this;
    }

    protected Function<BlockState, VoxelShape> getShapeForEachState(Function<BlockState, VoxelShape> shapeCalculator) {
        return arg_0 -> ((ImmutableMap)((ImmutableMap)this.stateDefinition.getPossibleStates().stream().collect(ImmutableMap.toImmutableMap(Function.identity(), shapeCalculator)))).get(arg_0);
    }

    protected Function<BlockState, VoxelShape> getShapeForEachState(Function<BlockState, VoxelShape> shapeCalculator, Property<?> ... ignoredProperties) {
        Map<Property, Object> defaults = Arrays.stream(ignoredProperties).collect(Collectors.toMap(k -> k, k -> k.getPossibleValues().getFirst()));
        ImmutableMap map = (ImmutableMap)this.stateDefinition.getPossibleStates().stream().filter(state -> defaults.entrySet().stream().allMatch(entry -> state.getValue((Property)entry.getKey()) == entry.getValue())).collect(ImmutableMap.toImmutableMap(Function.identity(), shapeCalculator));
        return blockState -> {
            for (Map.Entry entry : defaults.entrySet()) {
                blockState = Block.setValueHelper(blockState, (Property)entry.getKey(), entry.getValue());
            }
            return (VoxelShape)map.get(blockState);
        };
    }

    private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(S state, Property<T> property, Object value) {
        return (S)((StateHolder)state.setValue(property, (Comparable)((Comparable)value)));
    }

    @Deprecated
    public Holder.Reference<Block> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    protected void tryDropExperience(ServerLevel level, BlockPos pos, ItemStack tool, IntProvider xpRange) {
        int experience = EnchantmentHelper.processBlockExperience(level, tool, xpRange.sample(level.getRandom()));
        if (experience > 0) {
            this.popExperience(level, pos, experience);
        }
    }

    private record ShapePairKey(VoxelShape first, VoxelShape second) {
        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ShapePairKey)) return false;
            ShapePairKey that = (ShapePairKey)o;
            if (this.first != that.first) return false;
            if (this.second != that.second) return false;
            return true;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this.first) * 31 + System.identityHashCode(this.second);
        }
    }

    @Retention(value=RetentionPolicy.CLASS)
    @Target(value={ElementType.TYPE_USE})
    public static @interface UpdateFlags {
    }
}

