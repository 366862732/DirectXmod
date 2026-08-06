/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Objects;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SpawnUtil;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SculkShriekerBlockEntity
extends BlockEntity
implements GameEventListener.Provider<VibrationSystem.Listener>,
VibrationSystem {
    private static final int WARNING_SOUND_RADIUS = 10;
    private static final int WARDEN_SPAWN_ATTEMPTS = 20;
    private static final int WARDEN_SPAWN_RANGE_XZ = 5;
    private static final int WARDEN_SPAWN_RANGE_Y = 6;
    private static final int DARKNESS_RADIUS = 40;
    private static final int SHRIEKING_TICKS = 90;
    private static final Int2ObjectMap<SoundEvent> SOUND_BY_LEVEL = (Int2ObjectMap)Util.make(new Int2ObjectOpenHashMap(), map -> {
        map.put(1, (Object)SoundEvents.WARDEN_NEARBY_CLOSE);
        map.put(2, (Object)SoundEvents.WARDEN_NEARBY_CLOSER);
        map.put(3, (Object)SoundEvents.WARDEN_NEARBY_CLOSEST);
        map.put(4, (Object)SoundEvents.WARDEN_LISTENING_ANGRY);
    });
    private static final int DEFAULT_WARNING_LEVEL = 0;
    private int warningLevel = 0;
    private final VibrationSystem.User vibrationUser = new VibrationUser(this);
    private VibrationSystem.Data vibrationData = new VibrationSystem.Data();
    private final VibrationSystem.Listener vibrationListener = new VibrationSystem.Listener(this);

    public SculkShriekerBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.SCULK_SHRIEKER, worldPosition, blockState);
    }

    @Override
    public VibrationSystem.Data getVibrationData() {
        return this.vibrationData;
    }

    @Override
    public VibrationSystem.User getVibrationUser() {
        return this.vibrationUser;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.warningLevel = input.getIntOr("warning_level", 0);
        this.vibrationData = input.read("listener", VibrationSystem.Data.CODEC).orElseGet(VibrationSystem.Data::new);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("warning_level", this.warningLevel);
        output.store("listener", VibrationSystem.Data.CODEC, this.vibrationData);
    }

    public static @Nullable ServerPlayer tryGetPlayer(@Nullable Entity sourceEntity) {
        ItemEntity item;
        ServerPlayer player;
        Projectile projectile;
        Entity entity;
        LivingEntity livingEntity;
        if (sourceEntity instanceof ServerPlayer) {
            ServerPlayer player2 = (ServerPlayer)sourceEntity;
            return player2;
        }
        if (sourceEntity != null && (livingEntity = sourceEntity.getControllingPassenger()) instanceof ServerPlayer) {
            ServerPlayer player3 = (ServerPlayer)livingEntity;
            return player3;
        }
        if (sourceEntity instanceof Projectile && (entity = (projectile = (Projectile)sourceEntity).getOwner()) instanceof ServerPlayer) {
            player = (ServerPlayer)entity;
            return player;
        }
        if (sourceEntity instanceof ItemEntity && (entity = (item = (ItemEntity)sourceEntity).getOwner()) instanceof ServerPlayer) {
            player = (ServerPlayer)entity;
            return player;
        }
        return null;
    }

    public void tryShriek(ServerLevel level, @Nullable ServerPlayer player) {
        if (player == null) {
            return;
        }
        BlockState state = this.getBlockState();
        if (state.getValue(SculkShriekerBlock.SHRIEKING).booleanValue()) {
            return;
        }
        this.warningLevel = 0;
        if (this.canRespond(level) && !this.tryToWarn(level, player)) {
            return;
        }
        this.shriek(level, player);
    }

    private boolean tryToWarn(ServerLevel level, ServerPlayer player) {
        OptionalInt maybeWarningLevel = WardenSpawnTracker.tryWarn(level, this.getBlockPos(), player);
        maybeWarningLevel.ifPresent(warningLevel -> {
            this.warningLevel = warningLevel;
        });
        return maybeWarningLevel.isPresent();
    }

    private void shriek(ServerLevel level, @Nullable Entity sourceEntity) {
        BlockPos pos = this.getBlockPos();
        BlockState state = this.getBlockState();
        level.setBlock(pos, (BlockState)state.setValue(SculkShriekerBlock.SHRIEKING, true), 2);
        level.scheduleTick(pos, state.getBlock(), 90);
        level.levelEvent(3007, pos, 0);
        level.gameEvent(GameEvent.SHRIEK, pos, GameEvent.Context.of(sourceEntity));
    }

    private boolean canRespond(ServerLevel level) {
        return this.getBlockState().getValue(SculkShriekerBlock.CAN_SUMMON) != false && level.getDifficulty() != Difficulty.PEACEFUL && level.getGameRules().get(GameRules.SPAWN_WARDENS) != false;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        Level level;
        if (state.getValue(SculkShriekerBlock.SHRIEKING).booleanValue() && (level = this.level) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            this.tryRespond(serverLevel);
        }
    }

    public void tryRespond(ServerLevel level) {
        if (this.canRespond(level) && this.warningLevel > 0) {
            if (!this.trySummonWarden(level)) {
                this.playWardenReplySound(level);
            }
            Warden.applyDarknessAround(level, Vec3.atCenterOf(this.getBlockPos()), null, 40);
        }
    }

    private void playWardenReplySound(Level level) {
        SoundEvent sound = (SoundEvent)SOUND_BY_LEVEL.get(this.warningLevel);
        if (sound != null) {
            BlockPos pos = this.getBlockPos();
            RandomSource random = level.getRandom();
            int x = pos.getX() + Mth.randomBetweenInclusive(random, -10, 10);
            int y = pos.getY() + Mth.randomBetweenInclusive(random, -10, 10);
            int z = pos.getZ() + Mth.randomBetweenInclusive(random, -10, 10);
            level.playSound(null, (double)x, (double)y, (double)z, sound, SoundSource.HOSTILE, 5.0f, 1.0f);
        }
    }

    private boolean trySummonWarden(ServerLevel level) {
        if (this.warningLevel < 4) {
            return false;
        }
        return SpawnUtil.trySpawnMob(EntityTypes.WARDEN, EntitySpawnReason.TRIGGERED, level, this.getBlockPos(), 20, 5, 6, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER, false).isPresent();
    }

    @Override
    public VibrationSystem.Listener getListener() {
        return this.vibrationListener;
    }

    private class VibrationUser
    implements VibrationSystem.User {
        private static final int LISTENER_RADIUS = 8;
        private final PositionSource positionSource;
        final /* synthetic */ SculkShriekerBlockEntity this$0;

        public VibrationUser(SculkShriekerBlockEntity sculkShriekerBlockEntity) {
            SculkShriekerBlockEntity sculkShriekerBlockEntity2 = sculkShriekerBlockEntity;
            Objects.requireNonNull(sculkShriekerBlockEntity2);
            this.this$0 = sculkShriekerBlockEntity2;
            this.positionSource = new BlockPositionSource(sculkShriekerBlockEntity.worldPosition);
        }

        @Override
        public int getListenerRadius() {
            return 8;
        }

        @Override
        public PositionSource getPositionSource() {
            return this.positionSource;
        }

        @Override
        public TagKey<GameEvent> getListenableEvents() {
            return GameEventTags.SHRIEKER_CAN_LISTEN;
        }

        @Override
        public boolean canReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> event, GameEvent.Context context) {
            return this.this$0.getBlockState().getValue(SculkShriekerBlock.SHRIEKING) == false && SculkShriekerBlockEntity.tryGetPlayer(context.sourceEntity()) != null;
        }

        @Override
        public void onReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> event, @Nullable Entity sourceEntity, @Nullable Entity projectileOwner, float receivingDistance) {
            this.this$0.tryShriek(level, SculkShriekerBlockEntity.tryGetPlayer(projectileOwner != null ? projectileOwner : sourceEntity));
        }

        @Override
        public void onDataChanged() {
            this.this$0.setChanged();
        }

        @Override
        public boolean requiresAdjacentChunksToBeTicking() {
            return true;
        }
    }
}

