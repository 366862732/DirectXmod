/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.server.jsonrpc.api;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public record PlayerDto(Optional<UUID> id, Optional<String> name) {
    public static final MapCodec<PlayerDto> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)UUIDUtil.STRING_CODEC.optionalFieldOf("id").forGetter(PlayerDto::id), (App)Codec.STRING.optionalFieldOf("name").forGetter(PlayerDto::name)).apply((Applicative)i, PlayerDto::new));

    public static PlayerDto from(GameProfile gameProfile) {
        return new PlayerDto(Optional.of(gameProfile.id()), Optional.of(gameProfile.name()));
    }

    public static PlayerDto from(NameAndId nameAndId) {
        return new PlayerDto(Optional.of(nameAndId.id()), Optional.of(nameAndId.name()));
    }

    public static PlayerDto from(ServerPlayer player) {
        GameProfile gameProfile = player.getGameProfile();
        return PlayerDto.from(gameProfile);
    }
}

