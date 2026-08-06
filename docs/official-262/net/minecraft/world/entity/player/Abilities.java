/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.entity.player;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

public class Abilities {
    private static final boolean DEFAULT_INVULNERABLE = false;
    private static final boolean DEFAULY_FLYING = false;
    private static final boolean DEFAULT_MAY_FLY = false;
    private static final boolean DEFAULT_INSTABUILD = false;
    private static final boolean DEFAULT_MAY_BUILD = true;
    private static final float DEFAULT_FLYING_SPEED = 0.05f;
    private static final float DEFAULT_WALKING_SPEED = 0.1f;
    public boolean invulnerable;
    public boolean flying;
    public boolean mayfly;
    public boolean instabuild;
    public boolean mayBuild = true;
    private float flyingSpeed = 0.05f;
    private float walkingSpeed = 0.1f;

    public float getFlyingSpeed() {
        return this.flyingSpeed;
    }

    public void setFlyingSpeed(float value) {
        this.flyingSpeed = value;
    }

    public float getWalkingSpeed() {
        return this.walkingSpeed;
    }

    public void setWalkingSpeed(float value) {
        this.walkingSpeed = value;
    }

    public Packed pack() {
        return new Packed(this.invulnerable, this.flying, this.mayfly, this.instabuild, this.mayBuild, this.flyingSpeed, this.walkingSpeed);
    }

    public void apply(Packed packed) {
        this.invulnerable = packed.invulnerable;
        this.flying = packed.flying;
        this.mayfly = packed.mayFly;
        this.instabuild = packed.instabuild;
        this.mayBuild = packed.mayBuild;
        this.flyingSpeed = packed.flyingSpeed;
        this.walkingSpeed = packed.walkingSpeed;
    }

    public record Packed(boolean invulnerable, boolean flying, boolean mayFly, boolean instabuild, boolean mayBuild, float flyingSpeed, float walkingSpeed) {
        public static final Codec<Packed> CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "invulnerable", false).forGetter(Packed::invulnerable), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "flying", false).forGetter(Packed::flying), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "mayfly", false).forGetter(Packed::mayFly), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "instabuild", false).forGetter(Packed::instabuild), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "mayBuild", true).forGetter(Packed::mayBuild), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.FLOAT, "flySpeed", Float.valueOf(0.05f)).forGetter(Packed::flyingSpeed), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.FLOAT, "walkSpeed", Float.valueOf(0.1f)).forGetter(Packed::walkingSpeed)).apply((Applicative)i, Packed::new));
    }
}

