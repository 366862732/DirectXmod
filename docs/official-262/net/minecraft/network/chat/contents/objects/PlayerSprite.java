/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.network.chat.contents.objects;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.contents.objects.ObjectInfo;
import net.minecraft.world.item.component.ResolvableProfile;

public record PlayerSprite(ResolvableProfile player, boolean hat) implements ObjectInfo
{
    public static final MapCodec<PlayerSprite> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ResolvableProfile.CODEC.fieldOf("player").forGetter(PlayerSprite::player), (App)Codec.BOOL.optionalFieldOf("hat", (Object)true).forGetter(PlayerSprite::hat)).apply((Applicative)i, PlayerSprite::new));

    @Override
    public FontDescription fontDescription() {
        return new FontDescription.PlayerSprite(this.player, this.hat);
    }

    @Override
    public String defaultFallback() {
        return this.player.name().map(name -> "[" + name + " head]").orElse("[unknown player head]");
    }

    public MapCodec<PlayerSprite> codec() {
        return MAP_CODEC;
    }
}

