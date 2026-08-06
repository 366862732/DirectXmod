/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.gameevent.vibrations;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.world.level.gameevent.vibrations.VibrationInfo;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;

public class VibrationSelector {
    public static final Codec<VibrationSelector> CODEC = RecordCodecBuilder.create(i -> i.group((App)VibrationInfo.CODEC.lenientOptionalFieldOf("event").forGetter(o -> o.currentVibrationData.map(VibrationEvent::event)), (App)Codec.LONG.fieldOf("tick").forGetter(o -> o.currentVibrationData.map(VibrationEvent::tick).orElse(-1L))).apply((Applicative)i, VibrationSelector::new));
    private Optional<VibrationEvent> currentVibrationData;

    public VibrationSelector(Optional<VibrationInfo> currentVibration, long tick) {
        this.currentVibrationData = currentVibration.map(vibrationInfo -> new VibrationEvent((VibrationInfo)vibrationInfo, tick));
    }

    public VibrationSelector() {
        this.currentVibrationData = Optional.empty();
    }

    public void addCandidate(VibrationInfo newVibration, long tickTime) {
        if (this.shouldReplaceVibration(newVibration, tickTime)) {
            this.currentVibrationData = Optional.of(new VibrationEvent(newVibration, tickTime));
        }
    }

    private boolean shouldReplaceVibration(VibrationInfo newVibration, long tickTime) {
        if (this.currentVibrationData.isEmpty()) {
            return true;
        }
        VibrationEvent previousData = this.currentVibrationData.get();
        long previousTick = previousData.tick();
        if (tickTime != previousTick) {
            return false;
        }
        VibrationInfo previousVibration = previousData.event();
        if (newVibration.distance() < previousVibration.distance()) {
            return true;
        }
        if (newVibration.distance() > previousVibration.distance()) {
            return false;
        }
        return VibrationSystem.getGameEventFrequency(newVibration.gameEvent()) > VibrationSystem.getGameEventFrequency(previousVibration.gameEvent());
    }

    public Optional<VibrationInfo> chosenCandidate(long time) {
        if (this.currentVibrationData.isEmpty()) {
            return Optional.empty();
        }
        if (this.currentVibrationData.get().tick() < time) {
            return Optional.of(this.currentVibrationData.get().event());
        }
        return Optional.empty();
    }

    public void startOver() {
        this.currentVibrationData = Optional.empty();
    }

    private record VibrationEvent(VibrationInfo event, long tick) {
    }
}

