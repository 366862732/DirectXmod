/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.debug.DebugHiveInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class BeehiveBlockEntity
extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_FLOWER_POS = "flower_pos";
    private static final String BEES = "bees";
    private static final List<String> IGNORED_BEE_TAGS = Arrays.asList("Air", "drop_chances", "equipment", "Brain", "CanPickUpLoot", "DeathTime", "fall_distance", "FallFlying", "Fire", "HurtTime", "LeftHanded", "Motion", "NoGravity", "OnGround", "PortalCooldown", "Pos", "Rotation", "sleeping_pos", "CannotEnterHiveTicks", "TicksSincePollination", "CropsGrownSincePollination", "hive_pos", "Passengers", "leash", "UUID");
    public static final int MAX_OCCUPANTS = 3;
    private static final int MIN_TICKS_BEFORE_REENTERING_HIVE = 400;
    private static final int MIN_OCCUPATION_TICKS_NECTAR = 2400;
    public static final int MIN_OCCUPATION_TICKS_NECTARLESS = 600;
    private final List<BeeData> stored = Lists.newArrayList();
    private @Nullable BlockPos savedFlowerPos;

    public BeehiveBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.BEEHIVE, worldPosition, blockState);
    }

    @Override
    public void setChanged() {
        if (this.isFireNearby()) {
            this.emptyAllLivingFromHive(null, this.level.getBlockState(this.getBlockPos()), BeeReleaseStatus.EMERGENCY);
        }
        super.setChanged();
    }

    public boolean isFireNearby() {
        if (this.level == null) {
            return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed(this.worldPosition.offset(-1, -1, -1), this.worldPosition.offset(1, 1, 1))) {
            if (!(this.level.getBlockState(pos).getBlock() instanceof FireBlock)) continue;
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return this.stored.isEmpty();
    }

    public boolean isFull() {
        return this.stored.size() == 3;
    }

    public void emptyAllLivingFromHive(@Nullable Player player, BlockState state, BeeReleaseStatus releaseReason) {
        List<Entity> releasedFromHive = this.releaseAllOccupants(state, releaseReason);
        if (player != null) {
            for (Entity released : releasedFromHive) {
                if (!(released instanceof Bee)) continue;
                Bee bee = (Bee)released;
                if (!(player.position().distanceToSqr(released.position()) <= 16.0)) continue;
                if (!this.isSedated()) {
                    bee.setTarget(player);
                    continue;
                }
                bee.setStayOutOfHiveCountdown(400);
            }
        }
    }

    private List<Entity> releaseAllOccupants(BlockState state, BeeReleaseStatus releaseStatus) {
        ArrayList spawned = Lists.newArrayList();
        this.stored.removeIf(occupantEntry -> BeehiveBlockEntity.releaseOccupant(this.level, this.worldPosition, state, occupantEntry.toOccupant(), spawned, releaseStatus, this.savedFlowerPos));
        if (!spawned.isEmpty()) {
            super.setChanged();
        }
        return spawned;
    }

    @VisibleForDebug
    public int getOccupantCount() {
        return this.stored.size();
    }

    public static int getHoneyLevel(BlockState blockState) {
        return blockState.getValue(BeehiveBlock.HONEY_LEVEL);
    }

    @VisibleForDebug
    public boolean isSedated() {
        return CampfireBlock.isSmokeyPos(this.level, this.getBlockPos());
    }

    public void addOccupant(Bee bee) {
        if (this.stored.size() >= 3) {
            return;
        }
        bee.stopRiding();
        bee.ejectPassengers();
        bee.dropLeash();
        this.storeBee(Occupant.of(bee));
        if (this.level != null) {
            if (bee.hasSavedFlowerPos() && (!this.hasSavedFlowerPos() || this.level.getRandom().nextBoolean())) {
                this.savedFlowerPos = bee.getSavedFlowerPos();
            }
            BlockPos blockPos = this.getBlockPos();
            this.level.playSound(null, (double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0f, 1.0f);
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(bee, this.getBlockState()));
        }
        bee.discard();
        super.setChanged();
    }

    public void storeBee(Occupant occupant) {
        this.stored.add(new BeeData(occupant));
    }

    private static boolean releaseOccupant(Level level, BlockPos blockPos, BlockState state, Occupant beeData, @Nullable List<Entity> spawned, BeeReleaseStatus releaseStatus, @Nullable BlockPos savedFlowerPos) {
        boolean frontBlocked;
        if (level.environmentAttributes().getValue(EnvironmentAttributes.BEES_STAY_IN_HIVE, blockPos).booleanValue() && releaseStatus != BeeReleaseStatus.EMERGENCY) {
            return false;
        }
        Direction facing = state.getValue(BeehiveBlock.FACING);
        BlockPos facingPos = blockPos.relative(facing);
        boolean bl = frontBlocked = !level.getBlockState(facingPos).getCollisionShape(level, facingPos).isEmpty();
        if (frontBlocked && releaseStatus != BeeReleaseStatus.EMERGENCY) {
            return false;
        }
        Entity entity = beeData.createEntity(level, blockPos);
        if (entity != null) {
            if (entity instanceof Bee) {
                Bee bee = (Bee)entity;
                RandomSource random = level.getRandom();
                if (savedFlowerPos != null && !bee.hasSavedFlowerPos() && random.nextFloat() < 0.9f) {
                    bee.setSavedFlowerPos(savedFlowerPos);
                }
                if (releaseStatus == BeeReleaseStatus.HONEY_DELIVERED) {
                    int honeyLevel;
                    bee.dropOffNectar();
                    if (state.is(BlockTags.BEEHIVES, s -> s.hasProperty(BeehiveBlock.HONEY_LEVEL)) && (honeyLevel = BeehiveBlockEntity.getHoneyLevel(state)) < 5) {
                        int levelIncrease;
                        int n = levelIncrease = random.nextInt(100) == 0 ? 2 : 1;
                        if (honeyLevel + levelIncrease > 5) {
                            --levelIncrease;
                        }
                        level.setBlockAndUpdate(blockPos, (BlockState)state.setValue(BeehiveBlock.HONEY_LEVEL, honeyLevel + levelIncrease));
                    }
                }
                if (spawned != null) {
                    spawned.add(bee);
                }
                float bbWidth = entity.getBbWidth();
                double delta = frontBlocked ? 0.0 : 0.55 + (double)(bbWidth / 2.0f);
                double spawnX = (double)blockPos.getX() + 0.5 + delta * (double)facing.getStepX();
                double spawnY = (double)blockPos.getY() + 0.5 - (double)(entity.getBbHeight() / 2.0f);
                double spawnZ = (double)blockPos.getZ() + 0.5 + delta * (double)facing.getStepZ();
                entity.snapTo(spawnX, spawnY, spawnZ, entity.getYRot(), entity.getXRot());
            }
            level.playSound(null, blockPos, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1.0f, 1.0f);
            level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(entity, level.getBlockState(blockPos)));
            return level.addFreshEntity(entity);
        }
        return false;
    }

    private boolean hasSavedFlowerPos() {
        return this.savedFlowerPos != null;
    }

    private static void tickOccupants(Level level, BlockPos pos, BlockState state, List<BeeData> stored, @Nullable BlockPos savedFlowerPos) {
        boolean changed = false;
        Iterator<BeeData> iterator = stored.iterator();
        while (iterator.hasNext()) {
            BeeReleaseStatus releaseStatus;
            BeeData data = iterator.next();
            if (!data.tick()) continue;
            BeeReleaseStatus beeReleaseStatus = releaseStatus = data.hasNectar() ? BeeReleaseStatus.HONEY_DELIVERED : BeeReleaseStatus.BEE_RELEASED;
            if (!BeehiveBlockEntity.releaseOccupant(level, pos, state, data.toOccupant(), null, releaseStatus, savedFlowerPos)) continue;
            changed = true;
            iterator.remove();
        }
        if (changed) {
            BeehiveBlockEntity.setChanged(level, pos, state);
        }
    }

    public static void serverTick(Level level, BlockPos blockPos, BlockState state, BeehiveBlockEntity entity) {
        BeehiveBlockEntity.tickOccupants(level, blockPos, state, entity.stored, entity.savedFlowerPos);
        if (!entity.stored.isEmpty() && level.getRandom().nextDouble() < 0.005) {
            double x = (double)blockPos.getX() + 0.5;
            double y = blockPos.getY();
            double z = (double)blockPos.getZ() + 0.5;
            level.playSound(null, x, y, z, SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.stored.clear();
        input.read(BEES, Occupant.LIST_CODEC).orElse(List.of()).forEach(this::storeBee);
        this.savedFlowerPos = input.read(TAG_FLOWER_POS, BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(BEES, Occupant.LIST_CODEC, this.getBees());
        output.storeNullable(TAG_FLOWER_POS, BlockPos.CODEC, this.savedFlowerPos);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.stored.clear();
        List<Occupant> bees = components.getOrDefault(DataComponents.BEES, Bees.EMPTY).bees();
        bees.forEach(this::storeBee);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.BEES, new Bees(this.getBees()));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard(BEES);
    }

    private List<Occupant> getBees() {
        return this.stored.stream().map(BeeData::toOccupant).toList();
    }

    @Override
    public void registerDebugValues(ServerLevel level, DebugValueSource.Registration registration) {
        registration.register(DebugSubscriptions.BEE_HIVES, () -> DebugHiveInfo.pack(this));
    }

    public static enum BeeReleaseStatus {
        HONEY_DELIVERED,
        BEE_RELEASED,
        EMERGENCY;

    }

    public record Occupant(TypedEntityData<EntityType<?>> entityData, int ticksInHive, int minTicksInHive) {
        public static final Codec<Occupant> CODEC = RecordCodecBuilder.create(i -> i.group((App)TypedEntityData.codec(EntityType.CODEC).fieldOf("entity_data").forGetter(Occupant::entityData), (App)Codec.INT.fieldOf("ticks_in_hive").forGetter(Occupant::ticksInHive), (App)Codec.INT.fieldOf("min_ticks_in_hive").forGetter(Occupant::minTicksInHive)).apply((Applicative)i, Occupant::new));
        public static final Codec<List<Occupant>> LIST_CODEC = CODEC.listOf();
        public static final StreamCodec<RegistryFriendlyByteBuf, Occupant> STREAM_CODEC = StreamCodec.composite(TypedEntityData.streamCodec(EntityType.STREAM_CODEC), Occupant::entityData, ByteBufCodecs.VAR_INT, Occupant::ticksInHive, ByteBufCodecs.VAR_INT, Occupant::minTicksInHive, Occupant::new);

        public static Occupant of(Entity entity) {
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER);){
                TagValueOutput output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
                entity.save(output);
                IGNORED_BEE_TAGS.forEach(output::discard);
                CompoundTag entityTag = output.buildResult();
                boolean hasNectar = entityTag.getBooleanOr("HasNectar", false);
                Occupant occupant = new Occupant(TypedEntityData.of(entity.getType(), entityTag), 0, hasNectar ? 2400 : 600);
                return occupant;
            }
        }

        public static Occupant create(int ticksInHive) {
            return new Occupant(TypedEntityData.of(EntityTypes.BEE, new CompoundTag()), ticksInHive, 600);
        }

        public @Nullable Entity createEntity(Level level, BlockPos hivePos) {
            CompoundTag entityTag = this.entityData.copyTagWithoutId();
            IGNORED_BEE_TAGS.forEach(entityTag::remove);
            Entity entity = EntityType.loadEntityRecursive(this.entityData.type(), entityTag, level, EntitySpawnReason.LOAD, EntityProcessor.NOP);
            if (entity == null || !entity.is(EntityTypeTags.BEEHIVE_INHABITORS)) {
                return null;
            }
            entity.setNoGravity(true);
            if (entity instanceof Bee) {
                Bee bee = (Bee)entity;
                bee.setHivePos(hivePos);
                Occupant.setBeeReleaseData(this.ticksInHive, bee);
            }
            return entity;
        }

        private static void setBeeReleaseData(int ticksInHive, Bee bee) {
            Occupant.updateBeeAge(ticksInHive, bee);
            bee.setInLoveTime(Math.max(0, bee.getInLoveTime() - ticksInHive));
        }

        private static void updateBeeAge(int ticksInHive, Bee bee) {
            if (bee.isAgeLocked()) {
                return;
            }
            int age = bee.getAge();
            if (age < 0) {
                bee.setAge(Math.min(0, age + ticksInHive));
            } else if (age > 0) {
                bee.setAge(Math.max(0, age - ticksInHive));
            }
        }
    }

    private static class BeeData {
        private final Occupant occupant;
        private int ticksInHive;

        private BeeData(Occupant occupant) {
            this.occupant = occupant;
            this.ticksInHive = occupant.ticksInHive();
        }

        public boolean tick() {
            return this.ticksInHive++ > this.occupant.minTicksInHive;
        }

        public Occupant toOccupant() {
            return new Occupant(this.occupant.entityData, this.ticksInHive, this.occupant.minTicksInHive);
        }

        public boolean hasNectar() {
            return this.occupant.entityData.getUnsafe().getBooleanOr("HasNectar", false);
        }
    }
}

