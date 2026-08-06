/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.entity.animal.pig;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.sounds.SoundEvent;

public record PigSoundVariant(PigSoundSet adultSounds, PigSoundSet babySounds) {
    public static final Codec<PigSoundVariant> DIRECT_CODEC = PigSoundVariant.codec();
    public static final Codec<PigSoundVariant> NETWORK_CODEC = PigSoundVariant.codec();
    public static final Codec<Holder<PigSoundVariant>> CODEC = RegistryFixedCodec.create(Registries.PIG_SOUND_VARIANT);
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<PigSoundVariant>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.PIG_SOUND_VARIANT);

    private static Codec<PigSoundVariant> codec() {
        return RecordCodecBuilder.create(i -> i.group((App)PigSoundSet.CODEC.fieldOf("adult_sounds").forGetter(PigSoundVariant::adultSounds), (App)PigSoundSet.CODEC.fieldOf("baby_sounds").forGetter(PigSoundVariant::babySounds)).apply((Applicative)i, PigSoundVariant::new));
    }

    public record PigSoundSet(Holder<SoundEvent> ambientSound, Holder<SoundEvent> hurtSound, Holder<SoundEvent> deathSound, Holder<SoundEvent> stepSound, Holder<SoundEvent> eatSound) {
        public static final Codec<PigSoundSet> CODEC = RecordCodecBuilder.create(i -> i.group((App)SoundEvent.CODEC.fieldOf("ambient_sound").forGetter(PigSoundSet::ambientSound), (App)SoundEvent.CODEC.fieldOf("hurt_sound").forGetter(PigSoundSet::hurtSound), (App)SoundEvent.CODEC.fieldOf("death_sound").forGetter(PigSoundSet::deathSound), (App)SoundEvent.CODEC.fieldOf("step_sound").forGetter(PigSoundSet::stepSound), (App)SoundEvent.CODEC.fieldOf("eat_sound").forGetter(PigSoundSet::eatSound)).apply((Applicative)i, PigSoundSet::new));
    }
}

