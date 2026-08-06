/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.entity.animal.feline;

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

public record CatSoundVariant(CatSoundSet adultSounds, CatSoundSet babySounds) {
    public static final Codec<CatSoundVariant> DIRECT_CODEC = CatSoundVariant.codec();
    public static final Codec<CatSoundVariant> NETWORK_CODEC = CatSoundVariant.codec();
    public static final Codec<Holder<CatSoundVariant>> CODEC = RegistryFixedCodec.create(Registries.CAT_SOUND_VARIANT);
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<CatSoundVariant>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.CAT_SOUND_VARIANT);

    private static Codec<CatSoundVariant> codec() {
        return RecordCodecBuilder.create(i -> i.group((App)CatSoundSet.CODEC.fieldOf("adult_sounds").forGetter(CatSoundVariant::adultSounds), (App)CatSoundSet.CODEC.fieldOf("baby_sounds").forGetter(CatSoundVariant::babySounds)).apply((Applicative)i, CatSoundVariant::new));
    }

    public record CatSoundSet(Holder<SoundEvent> ambientSound, Holder<SoundEvent> strayAmbientSound, Holder<SoundEvent> hissSound, Holder<SoundEvent> hurtSound, Holder<SoundEvent> deathSound, Holder<SoundEvent> eatSound, Holder<SoundEvent> begForFoodSound, Holder<SoundEvent> purrSound, Holder<SoundEvent> purreowSound) {
        private static final Codec<CatSoundSet> CODEC = RecordCodecBuilder.create(i -> i.group((App)SoundEvent.CODEC.fieldOf("ambient_sound").forGetter(CatSoundSet::ambientSound), (App)SoundEvent.CODEC.fieldOf("stray_ambient_sound").forGetter(CatSoundSet::strayAmbientSound), (App)SoundEvent.CODEC.fieldOf("hiss_sound").forGetter(CatSoundSet::hissSound), (App)SoundEvent.CODEC.fieldOf("hurt_sound").forGetter(CatSoundSet::hurtSound), (App)SoundEvent.CODEC.fieldOf("death_sound").forGetter(CatSoundSet::deathSound), (App)SoundEvent.CODEC.fieldOf("eat_sound").forGetter(CatSoundSet::eatSound), (App)SoundEvent.CODEC.fieldOf("beg_for_food_sound").forGetter(CatSoundSet::begForFoodSound), (App)SoundEvent.CODEC.fieldOf("purr_sound").forGetter(CatSoundSet::purrSound), (App)SoundEvent.CODEC.fieldOf("purreow_sound").forGetter(CatSoundSet::purreowSound)).apply((Applicative)i, CatSoundSet::new));
    }
}

