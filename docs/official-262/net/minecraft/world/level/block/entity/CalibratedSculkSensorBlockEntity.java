/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.entity;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CalibratedSculkSensorBlock;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.jspecify.annotations.Nullable;

public class CalibratedSculkSensorBlockEntity
extends SculkSensorBlockEntity {
    public CalibratedSculkSensorBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.CALIBRATED_SCULK_SENSOR, worldPosition, blockState);
    }

    @Override
    public VibrationSystem.User createVibrationUser() {
        return new VibrationUser(this, this.getBlockPos());
    }

    protected class VibrationUser
    extends SculkSensorBlockEntity.VibrationUser {
        final /* synthetic */ CalibratedSculkSensorBlockEntity this$0;

        public VibrationUser(CalibratedSculkSensorBlockEntity this$0, BlockPos blockPos) {
            CalibratedSculkSensorBlockEntity calibratedSculkSensorBlockEntity = this$0;
            Objects.requireNonNull(calibratedSculkSensorBlockEntity);
            this.this$0 = calibratedSculkSensorBlockEntity;
            super(this$0, blockPos);
        }

        @Override
        public int getListenerRadius() {
            return 16;
        }

        @Override
        public boolean canReceiveVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> event, @Nullable GameEvent.Context context) {
            int comparisonType = this.getBackSignal(level, this.blockPos, this.this$0.getBlockState());
            if (comparisonType != 0 && VibrationSystem.getGameEventFrequency(event) != comparisonType) {
                return false;
            }
            return super.canReceiveVibration(level, pos, event, context);
        }

        private int getBackSignal(Level level, BlockPos pos, BlockState state) {
            Direction direction = state.getValue(CalibratedSculkSensorBlock.FACING).getOpposite();
            return level.getSignal(pos.relative(direction), direction);
        }
    }
}

