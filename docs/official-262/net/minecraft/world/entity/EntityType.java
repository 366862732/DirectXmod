/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.DependantName;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PostSpawnProcessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class EntityType<T extends Entity>
implements EntityTypeTest<Entity, T>,
FeatureElement {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Holder.Reference<EntityType<?>> builtInRegistryHolder = BuiltInRegistries.ENTITY_TYPE.createIntrusiveHolder(this);
    public static final Codec<EntityType<?>> CODEC = BuiltInRegistries.ENTITY_TYPE.byNameCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, EntityType<?>> STREAM_CODEC = ByteBufCodecs.registry(Registries.ENTITY_TYPE);
    private final EntityFactory<T> factory;
    private final MobCategory category;
    private final TagKey<Block> immuneTo;
    private final boolean serialize;
    private final boolean summon;
    private final boolean fireImmune;
    private final boolean canSpawnFarFromPlayer;
    private final int clientTrackingRange;
    private final int updateInterval;
    private final String descriptionId;
    private @Nullable Component description;
    private final Optional<ResourceKey<LootTable>> lootTable;
    private final EntityDimensions dimensions;
    private final float spawnDimensionsScale;
    private final FeatureFlagSet requiredFeatures;
    private final boolean allowedInPeaceful;

    public static Identifier getKey(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }

    public EntityType(EntityFactory<T> factory, MobCategory category, boolean serialize, boolean summon, boolean fireImmune, boolean canSpawnFarFromPlayer, TagKey<Block> immuneTo, EntityDimensions dimensions, float spawnDimensionsScale, int clientTrackingRange, int updateInterval, String descriptionId, Optional<ResourceKey<LootTable>> lootTable, FeatureFlagSet requiredFeatures, boolean allowedInPeaceful) {
        this.factory = factory;
        this.category = category;
        this.canSpawnFarFromPlayer = canSpawnFarFromPlayer;
        this.serialize = serialize;
        this.summon = summon;
        this.fireImmune = fireImmune;
        this.immuneTo = immuneTo;
        this.dimensions = dimensions;
        this.spawnDimensionsScale = spawnDimensionsScale;
        this.clientTrackingRange = clientTrackingRange;
        this.updateInterval = updateInterval;
        this.descriptionId = descriptionId;
        this.lootTable = lootTable;
        this.requiredFeatures = requiredFeatures;
        this.allowedInPeaceful = allowedInPeaceful;
    }

    public @Nullable T spawn(ServerLevel level, @Nullable ItemStack itemStack, @Nullable LivingEntity user, BlockPos spawnPos, EntitySpawnReason spawnReason, boolean tryMoveDown, boolean movedUp) {
        PostSpawnProcessor postSpawnConfig = itemStack != null ? EntityType.createDefaultStackConfig(level, itemStack, user) : PostSpawnProcessor.nop();
        return this.spawn(level, postSpawnConfig, spawnPos, spawnReason, tryMoveDown, movedUp);
    }

    public static <T extends Entity> PostSpawnProcessor<T> createDefaultStackConfig(Level level, ItemStack itemStack, @Nullable LivingEntity user) {
        return EntityType.appendDefaultStackConfig(PostSpawnProcessor.nop(), level, itemStack, user);
    }

    public static <T extends Entity> PostSpawnProcessor<T> appendDefaultStackConfig(PostSpawnProcessor<T> initialConfig, Level level, ItemStack itemStack, @Nullable LivingEntity user) {
        return EntityType.appendCustomEntityStackConfig(EntityType.appendComponentsConfig(initialConfig, itemStack), level, itemStack, user);
    }

    public static <T extends Entity> PostSpawnProcessor<T> appendComponentsConfig(PostSpawnProcessor<T> initialConfig, ItemStack itemStack) {
        return initialConfig.andThen(entity -> entity.applyComponentsFromItemStack(itemStack));
    }

    public static <T extends Entity> PostSpawnProcessor<T> appendCustomEntityStackConfig(PostSpawnProcessor<T> initialConfig, Level level, ItemStack itemStack, @Nullable LivingEntity user) {
        TypedEntityData<EntityType<?>> entityData = itemStack.get(DataComponents.ENTITY_DATA);
        if (entityData != null) {
            return initialConfig.andThen(entity -> EntityType.updateCustomEntityTag(level, user, entity, entityData));
        }
        return initialConfig;
    }

    public @Nullable T spawn(ServerLevel level, BlockPos spawnPos, EntitySpawnReason spawnReason) {
        return this.spawn(level, null, spawnPos, spawnReason, false, false);
    }

    public @Nullable T spawn(ServerLevel level, @Nullable PostSpawnProcessor<T> postSpawnConfig, BlockPos spawnPos, EntitySpawnReason spawnReason, boolean tryMoveDown, boolean movedUp) {
        T entity = this.create(level, postSpawnConfig, spawnPos, spawnReason, tryMoveDown, movedUp);
        if (entity != null) {
            level.addFreshEntityWithPassengers((Entity)entity);
            if (entity instanceof Mob) {
                Mob mob = (Mob)entity;
                mob.playAmbientSound();
            }
        }
        return entity;
    }

    public @Nullable T create(ServerLevel level, @Nullable PostSpawnProcessor<T> postSpawnConfig, BlockPos spawnPos, EntitySpawnReason spawnReason, boolean tryMoveDown, boolean movedUp) {
        double yOff;
        T entity = this.create((Level)level, spawnReason);
        if (entity == null) {
            return null;
        }
        if (tryMoveDown) {
            ((Entity)entity).setPos((double)spawnPos.getX() + 0.5, spawnPos.getY() + 1, (double)spawnPos.getZ() + 0.5);
            yOff = EntityType.getYOffset(level, spawnPos, movedUp, ((Entity)entity).getBoundingBox());
        } else {
            yOff = 0.0;
        }
        ((Entity)entity).snapTo((double)spawnPos.getX() + 0.5, (double)spawnPos.getY() + yOff, (double)spawnPos.getZ() + 0.5, Mth.wrapDegrees(level.getRandom().nextFloat() * 360.0f), 0.0f);
        if (entity instanceof Mob) {
            Mob mob = (Mob)entity;
            mob.yHeadRot = mob.getYRot();
            mob.yBodyRot = mob.getYRot();
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), spawnReason, null);
        }
        if (postSpawnConfig != null) {
            postSpawnConfig.apply(entity);
        }
        return entity;
    }

    protected static double getYOffset(LevelReader level, BlockPos spawnPos, boolean movedUp, AABB entityBox) {
        AABB aabb = new AABB(spawnPos);
        if (movedUp) {
            aabb = aabb.expandTowards(0.0, -1.0, 0.0);
        }
        Iterable<VoxelShape> shapes = level.getCollisions(null, aabb);
        return 1.0 + Shapes.collide(Direction.Axis.Y, entityBox, shapes, movedUp ? -2.0 : -1.0);
    }

    public static void updateCustomEntityTag(Level level, @Nullable LivingEntity user, @Nullable Entity entity, TypedEntityData<EntityType<?>> entityData) {
        block5: {
            block6: {
                MinecraftServer server = level.getServer();
                if (server == null || entity == null) {
                    return;
                }
                if (entity.getType() != entityData.type()) {
                    return;
                }
                if (level.isClientSide() || !entity.getType().onlyOpCanSetNbt()) break block5;
                if (!(user instanceof Player)) break block6;
                Player player = (Player)user;
                if (server.getPlayerList().isOp(player.nameAndId())) break block5;
            }
            return;
        }
        entityData.loadInto(entity);
    }

    public boolean canSerialize() {
        return this.serialize;
    }

    public boolean canSummon() {
        return this.summon;
    }

    public boolean fireImmune() {
        return this.fireImmune;
    }

    public boolean canSpawnFarFromPlayer() {
        return this.canSpawnFarFromPlayer;
    }

    public MobCategory getCategory() {
        return this.category;
    }

    public String getDescriptionId() {
        return this.descriptionId;
    }

    public Component getDescription() {
        if (this.description == null) {
            this.description = Component.translatable(this.getDescriptionId());
        }
        return this.description;
    }

    public String toString() {
        return this.getDescriptionId();
    }

    public String toShortString() {
        int dot = this.getDescriptionId().lastIndexOf(46);
        return dot == -1 ? this.getDescriptionId() : this.getDescriptionId().substring(dot + 1);
    }

    public Optional<ResourceKey<LootTable>> getDefaultLootTable() {
        return this.lootTable;
    }

    public float getWidth() {
        return this.dimensions.width();
    }

    public float getHeight() {
        return this.dimensions.height();
    }

    @Override
    public FeatureFlagSet requiredFeatures() {
        return this.requiredFeatures;
    }

    public boolean canSpawn(Level level) {
        if (!this.isEnabled(level.enabledFeatures())) {
            return false;
        }
        return this.isAllowedInPeaceful() || level.getDifficulty() != Difficulty.PEACEFUL;
    }

    public @Nullable T create(Level level, EntitySpawnReason reason) {
        return this.create(level, new EntitySpawnRequest(reason, false));
    }

    public @Nullable T create(Level level, EntitySpawnRequest request) {
        if (!request.ignoreChecks() && !this.canSpawn(level)) {
            return null;
        }
        return this.factory.create(this, level);
    }

    public static Optional<Entity> create(ValueInput input, Level level, EntitySpawnRequest request) {
        return Util.ifElse(EntityType.by(input).map(type -> type.create(level, request)), entity -> entity.load(input), () -> LOGGER.warn("Skipping Entity with id {}", (Object)input.getStringOr("id", "[invalid]")));
    }

    public static Optional<Entity> create(EntityType<?> type, ValueInput input, Level level, EntitySpawnReason reason) {
        Optional<Entity> entity = Optional.ofNullable(type.create(level, reason));
        entity.ifPresent(e -> e.load(input));
        return entity;
    }

    public AABB getSpawnAABB(double x, double y, double z) {
        float halfWidth = this.spawnDimensionsScale * this.getWidth() / 2.0f;
        float height = this.spawnDimensionsScale * this.getHeight();
        return new AABB(x - (double)halfWidth, y, z - (double)halfWidth, x + (double)halfWidth, y + (double)height, z + (double)halfWidth);
    }

    public boolean isBlockDangerous(BlockState state) {
        if (state.is(this.immuneTo)) {
            return false;
        }
        if (!this.fireImmune && NodeEvaluator.isBurningBlock(state)) {
            return true;
        }
        return state.is(Blocks.WITHER_ROSE) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.CACTUS) || state.is(Blocks.POWDER_SNOW);
    }

    public EntityDimensions getDimensions() {
        return this.dimensions;
    }

    public static Optional<EntityType<?>> by(ValueInput input) {
        return input.read("id", CODEC);
    }

    public static @Nullable Entity loadEntityRecursive(CompoundTag tag, Level level, EntitySpawnRequest request, EntityProcessor postLoad) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(LOGGER);){
            Entity entity = EntityType.loadEntityRecursive(TagValueInput.create((ProblemReporter)reporter, (HolderLookup.Provider)level.registryAccess(), tag), level, request, postLoad);
            return entity;
        }
    }

    public static @Nullable Entity loadEntityRecursive(EntityType<?> type, CompoundTag tag, Level level, EntitySpawnReason reason, EntityProcessor postLoad) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(LOGGER);){
            Entity entity = EntityType.loadEntityRecursive(type, TagValueInput.create((ProblemReporter)reporter, (HolderLookup.Provider)level.registryAccess(), tag), level, reason, postLoad);
            return entity;
        }
    }

    public static @Nullable Entity loadEntityRecursive(ValueInput input, Level level, EntitySpawnReason reason, EntityProcessor postLoad) {
        return EntityType.loadEntityRecursive(input, level, new EntitySpawnRequest(reason, false), postLoad);
    }

    public static @Nullable Entity loadEntityRecursive(ValueInput input, Level level, EntitySpawnRequest request, EntityProcessor postLoad) {
        return EntityType.loadStaticEntity(input, level, request).map(postLoad::process).map(entity -> EntityType.loadPassengersRecursive(entity, input, level, request, postLoad)).orElse(null);
    }

    public static @Nullable Entity loadEntityRecursive(EntityType<?> type, ValueInput input, Level level, EntitySpawnReason reason, EntityProcessor postLoad) {
        return EntityType.loadStaticEntity(type, input, level, reason).map(postLoad::process).map(entity -> EntityType.loadPassengersRecursive(entity, input, level, new EntitySpawnRequest(reason, false), postLoad)).orElse(null);
    }

    private static Entity loadPassengersRecursive(Entity entity, ValueInput input, Level level, EntitySpawnRequest request, EntityProcessor postLoad) {
        for (ValueInput passengerTag : input.childrenListOrEmpty("Passengers")) {
            Entity passenger = EntityType.loadEntityRecursive(passengerTag, level, request, postLoad);
            if (passenger == null) continue;
            passenger.startRiding(entity, true, false);
        }
        return entity;
    }

    public static Stream<Entity> loadEntitiesRecursive(ValueInput.ValueInputList entities, Level level, EntitySpawnReason reason) {
        return entities.stream().mapMulti((tag, output) -> EntityType.loadEntityRecursive(tag, level, reason, (Entity entity) -> {
            output.accept(entity);
            return entity;
        }));
    }

    private static Optional<Entity> loadStaticEntity(ValueInput input, Level level, EntitySpawnRequest request) {
        try {
            return EntityType.create(input, level, request);
        }
        catch (RuntimeException e) {
            LOGGER.warn("Exception loading entity: ", (Throwable)e);
            return Optional.empty();
        }
    }

    private static Optional<Entity> loadStaticEntity(EntityType<?> type, ValueInput input, Level level, EntitySpawnReason reason) {
        try {
            return EntityType.create(type, input, level, reason);
        }
        catch (RuntimeException e) {
            LOGGER.warn("Exception loading entity: ", (Throwable)e);
            return Optional.empty();
        }
    }

    public int clientTrackingRange() {
        return this.clientTrackingRange;
    }

    public int updateInterval() {
        return this.updateInterval;
    }

    public boolean trackDeltas() {
        return this != EntityTypes.PLAYER && this != EntityTypes.LLAMA_SPIT && this != EntityTypes.WITHER && this != EntityTypes.BAT && this != EntityTypes.ITEM_FRAME && this != EntityTypes.GLOW_ITEM_FRAME && this != EntityTypes.LEASH_KNOT && this != EntityTypes.PAINTING && this != EntityTypes.END_CRYSTAL && this != EntityTypes.EVOKER_FANGS;
    }

    @Override
    public @Nullable T tryCast(Entity entity) {
        return (T)(entity.getType() == this ? entity : null);
    }

    @Override
    public Class<? extends Entity> getBaseClass() {
        return Entity.class;
    }

    @Deprecated
    public Holder.Reference<EntityType<?>> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    public boolean isAllowedInPeaceful() {
        return this.allowedInPeaceful;
    }

    public boolean onlyOpCanSetNbt() {
        return EntityTypes.OP_ONLY_CUSTOM_DATA.contains(this);
    }

    @FunctionalInterface
    public static interface EntityFactory<T extends Entity> {
        public @Nullable T create(EntityType<T> var1, Level var2);
    }

    public static class Builder<T extends Entity> {
        private final EntityFactory<T> factory;
        private final MobCategory category;
        private TagKey<Block> immuneTo = BlockTags.DEFAULT_IMMUNE_TO;
        private boolean serialize = true;
        private boolean summon = true;
        private boolean fireImmune;
        private boolean canSpawnFarFromPlayer;
        private int clientTrackingRange = 5;
        private int updateInterval = 3;
        private EntityDimensions dimensions = EntityDimensions.scalable(0.6f, 1.8f);
        private float spawnDimensionsScale = 1.0f;
        private EntityAttachments.Builder attachments = EntityAttachments.builder();
        private FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;
        private DependantName<EntityType<?>, Optional<ResourceKey<LootTable>>> lootTable = id -> Optional.of(ResourceKey.create(Registries.LOOT_TABLE, id.identifier().withPrefix("entities/")));
        private final DependantName<EntityType<?>, String> descriptionId = id -> Util.makeDescriptionId("entity", id.identifier());
        private boolean allowedInPeaceful = true;

        private Builder(EntityFactory<T> factory, MobCategory category) {
            this.factory = factory;
            this.category = category;
            this.canSpawnFarFromPlayer = category == MobCategory.CREATURE || category == MobCategory.MISC;
        }

        public static <T extends Entity> Builder<T> of(EntityFactory<T> factory, MobCategory category) {
            return new Builder<T>(factory, category);
        }

        public static <T extends Entity> Builder<T> createNothing(MobCategory category) {
            return new Builder<Entity>((t, l) -> null, category);
        }

        public Builder<T> sized(float width, float height) {
            this.dimensions = EntityDimensions.scalable(width, height);
            return this;
        }

        public Builder<T> spawnDimensionsScale(float scale) {
            this.spawnDimensionsScale = scale;
            return this;
        }

        public Builder<T> eyeHeight(float eyeHeight) {
            this.dimensions = this.dimensions.withEyeHeight(eyeHeight);
            return this;
        }

        public Builder<T> passengerAttachments(float ... offsetYs) {
            for (float offsetY : offsetYs) {
                this.attachments = this.attachments.attach(EntityAttachment.PASSENGER, 0.0f, offsetY, 0.0f);
            }
            return this;
        }

        public Builder<T> passengerAttachments(Vec3 ... points) {
            for (Vec3 point : points) {
                this.attachments = this.attachments.attach(EntityAttachment.PASSENGER, point);
            }
            return this;
        }

        public Builder<T> vehicleAttachment(Vec3 point) {
            return this.attach(EntityAttachment.VEHICLE, point);
        }

        public Builder<T> ridingOffset(float ridingOffset) {
            return this.attach(EntityAttachment.VEHICLE, 0.0f, -ridingOffset, 0.0f);
        }

        public Builder<T> nameTagOffset(float nameTagOffset) {
            return this.attach(EntityAttachment.NAME_TAG, 0.0f, nameTagOffset, 0.0f);
        }

        public Builder<T> attach(EntityAttachment attachment, float x, float y, float z) {
            this.attachments = this.attachments.attach(attachment, x, y, z);
            return this;
        }

        public Builder<T> attach(EntityAttachment attachment, Vec3 point) {
            this.attachments = this.attachments.attach(attachment, point);
            return this;
        }

        public Builder<T> noSummon() {
            this.summon = false;
            return this;
        }

        public Builder<T> noSave() {
            this.serialize = false;
            return this;
        }

        public Builder<T> fireImmune() {
            this.fireImmune = true;
            return this;
        }

        public Builder<T> immuneTo(TagKey<Block> tag) {
            this.immuneTo = tag;
            return this;
        }

        public Builder<T> canSpawnFarFromPlayer() {
            this.canSpawnFarFromPlayer = true;
            return this;
        }

        public Builder<T> clientTrackingRange(int clientChunkRange) {
            this.clientTrackingRange = clientChunkRange;
            return this;
        }

        public Builder<T> updateInterval(int updateInterval) {
            this.updateInterval = updateInterval;
            return this;
        }

        public Builder<T> requiredFeatures(FeatureFlag ... flags) {
            this.requiredFeatures = FeatureFlags.REGISTRY.subset(flags);
            return this;
        }

        public Builder<T> noLootTable() {
            this.lootTable = DependantName.fixed(Optional.empty());
            return this;
        }

        public Builder<T> notInPeaceful() {
            this.allowedInPeaceful = false;
            return this;
        }

        public EntityType<T> build(ResourceKey<EntityType<?>> name) {
            if (this.serialize) {
                Util.fetchChoiceType(References.ENTITY_TREE, name.identifier().toString());
            }
            return new EntityType<T>(this.factory, this.category, this.serialize, this.summon, this.fireImmune, this.canSpawnFarFromPlayer, this.immuneTo, this.dimensions.withAttachments(this.attachments), this.spawnDimensionsScale, this.clientTrackingRange, this.updateInterval, this.descriptionId.get(name), this.lootTable.get(name), this.requiredFeatures, this.allowedInPeaceful);
        }
    }
}

