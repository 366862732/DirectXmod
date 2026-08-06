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
import java.security.PrivateKey;
import java.time.Instant;
import net.minecraft.util.Crypt;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.ProfilePublicKey;

public record ProfileKeyPair(PrivateKey privateKey, ProfilePublicKey publicKey, Instant refreshedAfter) {
    public static final Codec<ProfileKeyPair> CODEC = RecordCodecBuilder.create(i -> i.group((App)Crypt.PRIVATE_KEY_CODEC.fieldOf("private_key").forGetter(ProfileKeyPair::privateKey), (App)ProfilePublicKey.TRUSTED_CODEC.fieldOf("public_key").forGetter(ProfileKeyPair::publicKey), (App)ExtraCodecs.INSTANT_ISO8601.fieldOf("refreshed_after").forGetter(ProfileKeyPair::refreshedAfter)).apply((Applicative)i, ProfileKeyPair::new));

    public boolean dueRefresh() {
        return this.refreshedAfter.isBefore(Instant.now());
    }
}

