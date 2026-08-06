/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.entity.animal.chicken;

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

public record ChickenSoundVariant(ChickenSoundSet adultSounds, ChickenSoundSet babySounds) {
    public static final Codec<ChickenSoundVariant> DIRECT_CODEC = ChickenSoundVariant.codec();
    public static final Codec<ChickenSoundVariant> NETWORK_CODEC = ChickenSoundVariant.codec();
    public static final Codec<Holder<ChickenSoundVariant>> CODEC = RegistryFixedCodec.create(Registries.CHICKEN_SOUND_VARIANT);
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<ChickenSoundVariant>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.CHICKEN_SOUND_VARIANT);

    private static Codec<ChickenSoundVariant> codec() {
        return RecordCodecBuilder.create(i -> i.group((App)ChickenSoundSet.CODEC.fieldOf("adult_sounds").forGetter(ChickenSoundVariant::adultSounds), (App)ChickenSoundSet.CODEC.fieldOf("baby_sounds").forGetter(ChickenSoundVariant::babySounds)).apply((Applicative)i, ChickenSoundVariant::new));
    }

    public record ChickenSoundSet(Holder<SoundEvent> ambientSound, Holder<SoundEvent> hurtSound, Holder<SoundEvent> deathSound, Holder<SoundEvent> stepSound) {
        private static final Codec<ChickenSoundSet> CODEC = RecordCodecBuilder.create(i -> i.group((App)SoundEvent.CODEC.fieldOf("ambient_sound").forGetter(ChickenSoundSet::ambientSound), (App)SoundEvent.CODEC.fieldOf("hurt_sound").forGetter(ChickenSoundSet::hurtSound), (App)SoundEvent.CODEC.fieldOf("death_sound").forGetter(ChickenSoundSet::deathSound), (App)SoundEvent.CODEC.fieldOf("step_sound").forGetter(ChickenSoundSet::stepSound)).apply((Applicative)i, ChickenSoundSet::new));
    }
}

