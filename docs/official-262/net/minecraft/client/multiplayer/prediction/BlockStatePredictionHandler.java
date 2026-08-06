/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.objects.ObjectIterator
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.multiplayer.prediction;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class BlockStatePredictionHandler
implements AutoCloseable {
    private final Long2ObjectOpenHashMap<ServerVerifiedState> serverVerifiedStates = new Long2ObjectOpenHashMap();
    private int currentSequenceNr;
    private boolean isPredicting;
    private int lastTeleportSequence = -1;

    public void retainKnownServerState(BlockPos pos, BlockState state, LocalPlayer player) {
        this.serverVerifiedStates.compute(pos.asLong(), (key, serverVerifiedState) -> {
            if (serverVerifiedState != null) {
                return serverVerifiedState.setSequence(this.currentSequenceNr);
            }
            return new ServerVerifiedState(this.currentSequenceNr, state, player.position());
        });
    }

    public boolean updateKnownServerState(BlockPos pos, BlockState blockState) {
        ServerVerifiedState serverVerifiedState = (ServerVerifiedState)this.serverVerifiedStates.get(pos.asLong());
        if (serverVerifiedState == null) {
            return false;
        }
        serverVerifiedState.setBlockState(blockState);
        return true;
    }

    public void endPredictionsUpTo(int sequence, ClientLevel clientLevel) {
        ObjectIterator stateIterator = this.serverVerifiedStates.long2ObjectEntrySet().iterator();
        while (stateIterator.hasNext()) {
            Long2ObjectMap.Entry next = (Long2ObjectMap.Entry)stateIterator.next();
            ServerVerifiedState serverVerifiedState = (ServerVerifiedState)next.getValue();
            if (serverVerifiedState.sequence > sequence) continue;
            BlockPos pos = BlockPos.of(next.getLongKey());
            stateIterator.remove();
            clientLevel.syncBlockState(pos, serverVerifiedState.blockState, this.lastTeleportSequence < sequence ? serverVerifiedState.playerPos : null);
        }
    }

    public BlockStatePredictionHandler startPredicting() {
        ++this.currentSequenceNr;
        this.isPredicting = true;
        return this;
    }

    @Override
    public void close() {
        this.isPredicting = false;
    }

    public int currentSequence() {
        return this.currentSequenceNr;
    }

    public void onTeleport() {
        this.lastTeleportSequence = this.currentSequenceNr;
    }

    public boolean isPredicting() {
        return this.isPredicting;
    }

    @Environment(value=EnvType.CLIENT)
    private static class ServerVerifiedState {
        private final Vec3 playerPos;
        private int sequence;
        private BlockState blockState;

        private ServerVerifiedState(int sequence, BlockState blockState, Vec3 playerPos) {
            this.sequence = sequence;
            this.blockState = blockState;
            this.playerPos = playerPos;
        }

        private ServerVerifiedState setSequence(int sequence) {
            this.sequence = sequence;
            return this;
        }

        private void setBlockState(BlockState blockState) {
            this.blockState = blockState;
        }
    }
}

