/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.sounds;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;

public record Music(Holder<SoundEvent> sound, int minDelay, int maxDelay, boolean replaceCurrentMusic) {
    public static final Codec<Music> CODEC = RecordCodecBuilder.create(i -> i.group((App)SoundEvent.CODEC.fieldOf("sound").forGetter(Music::sound), (App)ExtraCodecs.NON_NEGATIVE_INT.fieldOf("min_delay").forGetter(Music::minDelay), (App)ExtraCodecs.NON_NEGATIVE_INT.fieldOf("max_delay").forGetter(Music::maxDelay), (App)Codec.BOOL.optionalFieldOf("replace_current_music", (Object)false).forGetter(Music::replaceCurrentMusic)).apply((Applicative)i, Music::new));
}

