/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.monster.warden;

import java.util.Arrays;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

public enum AngerLevel {
    CALM(0, SoundEvents.WARDEN_AMBIENT, SoundEvents.WARDEN_LISTENING),
    AGITATED(40, SoundEvents.WARDEN_AGITATED, SoundEvents.WARDEN_LISTENING_ANGRY),
    ANGRY(80, SoundEvents.WARDEN_ANGRY, SoundEvents.WARDEN_LISTENING_ANGRY);

    private static final AngerLevel[] SORTED_LEVELS;
    private final int minimumAnger;
    private final SoundEvent ambientSound;
    private final SoundEvent listeningSound;

    private AngerLevel(int minimumAnger, SoundEvent ambientSound, SoundEvent listeningSound) {
        this.minimumAnger = minimumAnger;
        this.ambientSound = ambientSound;
        this.listeningSound = listeningSound;
    }

    public int getMinimumAnger() {
        return this.minimumAnger;
    }

    public SoundEvent getAmbientSound() {
        return this.ambientSound;
    }

    public SoundEvent getListeningSound() {
        return this.listeningSound;
    }

    public static AngerLevel byAnger(int anger) {
        for (AngerLevel level : SORTED_LEVELS) {
            if (anger < level.minimumAnger) continue;
            return level;
        }
        return CALM;
    }

    public boolean isAngry() {
        return this == ANGRY;
    }

    static {
        SORTED_LEVELS = Util.make(AngerLevel.values(), values -> Arrays.sort(values, (a, b) -> Integer.compare(b.minimumAnger, a.minimumAnger)));
    }
}

