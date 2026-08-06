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
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.attribute.AmbientAdditionsSettings;
import net.minecraft.world.attribute.AmbientMoodSettings;

public record AmbientSounds(Optional<Holder<SoundEvent>> loop, Optional<AmbientMoodSettings> mood, List<AmbientAdditionsSettings> additions) {
    public static final AmbientSounds EMPTY = new AmbientSounds(Optional.empty(), Optional.empty(), List.of());
    public static final AmbientSounds LEGACY_CAVE_SETTINGS = new AmbientSounds(Optional.empty(), Optional.of(AmbientMoodSettings.LEGACY_CAVE_SETTINGS), List.of());
    public static final Codec<AmbientSounds> CODEC = RecordCodecBuilder.create(i -> i.group((App)SoundEvent.CODEC.optionalFieldOf("loop").forGetter(AmbientSounds::loop), (App)AmbientMoodSettings.CODEC.optionalFieldOf("mood").forGetter(AmbientSounds::mood), (App)ExtraCodecs.compactListCodec(AmbientAdditionsSettings.CODEC).optionalFieldOf("additions", List.of()).forGetter(AmbientSounds::additions)).apply((Applicative)i, AmbientSounds::new));
}

