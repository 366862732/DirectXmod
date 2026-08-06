/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.attribute;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvent;

public record BackgroundMusic(Optional<Music> defaultMusic, Optional<Music> creativeMusic, Optional<Music> underwaterMusic) {
    public static final BackgroundMusic EMPTY = new BackgroundMusic(Optional.empty(), Optional.empty(), Optional.empty());
    public static final BackgroundMusic OVERWORLD = new BackgroundMusic(Optional.of(Musics.GAME), Optional.of(Musics.CREATIVE), Optional.empty());
    public static final Codec<BackgroundMusic> CODEC = RecordCodecBuilder.create(i -> i.group((App)Music.CODEC.optionalFieldOf("default").forGetter(BackgroundMusic::defaultMusic), (App)Music.CODEC.optionalFieldOf("creative").forGetter(BackgroundMusic::creativeMusic), (App)Music.CODEC.optionalFieldOf("underwater").forGetter(BackgroundMusic::underwaterMusic)).apply((Applicative)i, BackgroundMusic::new));

    public BackgroundMusic(Music music) {
        this(Optional.of(music), Optional.empty(), Optional.empty());
    }

    public BackgroundMusic(Holder<SoundEvent> sound) {
        this(Musics.createGameMusic(sound));
    }

    public BackgroundMusic withUnderwater(Music underwaterMusic) {
        return new BackgroundMusic(this.defaultMusic, this.creativeMusic, Optional.of(underwaterMusic));
    }

    public Optional<Music> select(boolean isCreative, boolean isUnderwater) {
        if (isUnderwater && this.underwaterMusic.isPresent()) {
            return this.underwaterMusic;
        }
        if (isCreative && this.creativeMusic.isPresent()) {
            return this.creativeMusic;
        }
        return this.defaultMusic;
    }
}

