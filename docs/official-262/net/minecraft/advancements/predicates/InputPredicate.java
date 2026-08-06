/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.advancements.predicates;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.world.entity.player.Input;

public record InputPredicate(Optional<Boolean> forward, Optional<Boolean> backward, Optional<Boolean> left, Optional<Boolean> right, Optional<Boolean> jump, Optional<Boolean> sneak, Optional<Boolean> sprint) {
    public static final Codec<InputPredicate> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.BOOL.optionalFieldOf("forward").forGetter(InputPredicate::forward), (App)Codec.BOOL.optionalFieldOf("backward").forGetter(InputPredicate::backward), (App)Codec.BOOL.optionalFieldOf("left").forGetter(InputPredicate::left), (App)Codec.BOOL.optionalFieldOf("right").forGetter(InputPredicate::right), (App)Codec.BOOL.optionalFieldOf("jump").forGetter(InputPredicate::jump), (App)Codec.BOOL.optionalFieldOf("sneak").forGetter(InputPredicate::sneak), (App)Codec.BOOL.optionalFieldOf("sprint").forGetter(InputPredicate::sprint)).apply((Applicative)i, InputPredicate::new));

    public boolean matches(Input input) {
        return this.matches(this.forward, input.forward()) && this.matches(this.backward, input.backward()) && this.matches(this.left, input.left()) && this.matches(this.right, input.right()) && this.matches(this.jump, input.jump()) && this.matches(this.sneak, input.shift()) && this.matches(this.sprint, input.sprint());
    }

    private boolean matches(Optional<Boolean> match, boolean value) {
        return match.map(b -> b == value).orElse(true);
    }
}

