/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.entity;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SculkSensorBlockEntity
extends BlockEntity
implements GameEventListener.Provider<VibrationSystem.Listener>,
VibrationSystem {
    private static final int DEFAULT_LAST_VIBRATION_FREQUENCY = 0;
    private VibrationSystem.Data vibrationData;
    private final VibrationSystem.Listener vibrationListener;
    private final VibrationSystem.User vibrationUser = this.createVibrationUser();
    private int lastVibrationFrequency = 0;

    protected SculkSensorBlockEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState) {
        super(type, worldPosition, blockState);
        this.vibrationData = new VibrationSystem.Data();
        this.vibrationListener = new VibrationSystem.Listener(this);
    }

    public SculkSensorBlockEntity(BlockPos worldPosition, BlockState blockState) {
        this(BlockEntityTypes.SCULK_SENSOR, worldPosition, blockState);
    }

    public VibrationSystem.User createVibrationUser() {
        return new VibrationUser(this, this.getBlockPos());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.lastVibrationFrequency = input.getIntOr("last_vibration_frequency", 0);
        this.vibrationData = input.read("listener", VibrationSystem.Data.CODEC).orElseGet(VibrationSystem.Data::new);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("last_vibration_frequency", this.lastVibrationFrequency);
        output.store("listener", VibrationSystem.Data.CODEC, this.vibrationData);
    }

    @Override
    public VibrationSystem.Data getVibrationData() {
        return this.vibrationData;
    }

    @Override
    public VibrationSystem.User getVibrationUser() {
        return this.vibrationUser;
    }

    public int getLastVibrationFrequency() {
        return this.lastVibrationFrequency;
    }

    public void setLastVibrationFrequency(int lastVibrationFrequency) {
        this.lastVibrationFrequency = lastVibrationFrequency;
    }

    @Override
    public VibrationSystem.Listener getListener() {
        return this.vibrationListener;
    }

    protected class VibrationUser
    implements VibrationSystem.User {
        public static final int LISTENER_RANGE = 8;
        protected final BlockPos blockPos;
        private final PositionSource positionSource;
        final /* synthetic */ SculkSensorBlockEntity this$0;

        public VibrationUser(SculkSensorBlockEntity this$0, BlockPos blockPos) {
            SculkSensorBlockEntity sculkSensorBlockEntity = this$0;
            Objects.requireNonNull(sculkSensorBlockEntity);
            this.this$0 = sculkSensorBlockEntity;
            this.blockPos = blockPos;
            this.positionSource = new BlockPositionSource(blockPos);
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
        public boolean canTriggerAvoidVibration() {
            return true;
        }

        @Override
        public boolean canReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> event, @Nullable GameEvent.Context context) {
            if (pos.equals(this.blockPos) && (event.is(GameEvent.BLOCK_DESTROY) || event.is(GameEvent.BLOCK_PLACE))) {
                return false;
            }
            if (VibrationSystem.getGameEventFrequency(event) == 0) {
                return false;
            }
            return SculkSensorBlock.canActivate(this.this$0.getBlockState());
        }

        @Override
        public void onReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> event, @Nullable Entity sourceEntity, @Nullable Entity projectileOwner, float receivingDistance) {
            BlockState state = this.this$0.getBlockState();
            if (SculkSensorBlock.canActivate(state)) {
                int eventFrequency = VibrationSystem.getGameEventFrequency(event);
                this.this$0.setLastVibrationFrequency(eventFrequency);
                int calculatedPower = VibrationSystem.getRedstoneStrengthForDistance(receivingDistance, this.getListenerRadius());
                Block block = state.getBlock();
                if (block instanceof SculkSensorBlock) {
                    SculkSensorBlock sculkSensorBlock = (SculkSensorBlock)block;
                    sculkSensorBlock.activate(sourceEntity, level, this.blockPos, state, calculatedPower, eventFrequency);
                }
            }
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

