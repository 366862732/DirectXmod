/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.player;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.PlayerModelType;
import org.jspecify.annotations.Nullable;

public record PlayerSkin(ClientAsset.Texture body, @Nullable ClientAsset.Texture cape, @Nullable ClientAsset.Texture elytra, PlayerModelType model, boolean secure) {
    public static PlayerSkin insecure(ClientAsset.Texture body, @Nullable ClientAsset.Texture cape, @Nullable ClientAsset.Texture elytra, PlayerModelType model) {
        return new PlayerSkin(body, cape, elytra, model, false);
    }

    public PlayerSkin with(Patch patch) {
        if (patch.equals(Patch.EMPTY)) {
            return this;
        }
        return PlayerSkin.insecure((ClientAsset.Texture)DataFixUtils.orElse(patch.body, (Object)this.body), (ClientAsset.Texture)DataFixUtils.orElse(patch.cape, (Object)this.cape), (ClientAsset.Texture)DataFixUtils.orElse(patch.elytra, (Object)this.elytra), patch.model.orElse(this.model));
    }

    public record Patch(Optional<ClientAsset.ResourceTexture> body, Optional<ClientAsset.ResourceTexture> cape, Optional<ClientAsset.ResourceTexture> elytra, Optional<PlayerModelType> model) {
        public static final Patch EMPTY = new Patch(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        public static final MapCodec<Patch> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ClientAsset.ResourceTexture.CODEC.optionalFieldOf("texture").forGetter(Patch::body), (App)ClientAsset.ResourceTexture.CODEC.optionalFieldOf("cape").forGetter(Patch::cape), (App)ClientAsset.ResourceTexture.CODEC.optionalFieldOf("elytra").forGetter(Patch::elytra), (App)PlayerModelType.CODEC.optionalFieldOf("model").forGetter(Patch::model)).apply((Applicative)i, Patch::create));
        public static final StreamCodec<ByteBuf, Patch> STREAM_CODEC = StreamCodec.composite(ClientAsset.ResourceTexture.STREAM_CODEC.apply(ByteBufCodecs::optional), Patch::body, ClientAsset.ResourceTexture.STREAM_CODEC.apply(ByteBufCodecs::optional), Patch::cape, ClientAsset.ResourceTexture.STREAM_CODEC.apply(ByteBufCodecs::optional), Patch::elytra, PlayerModelType.STREAM_CODEC.apply(ByteBufCodecs::optional), Patch::model, Patch::create);

        public static Patch create(Optional<ClientAsset.ResourceTexture> texture, Optional<ClientAsset.ResourceTexture> capeTexture, Optional<ClientAsset.ResourceTexture> elytraTexture, Optional<PlayerModelType> model) {
            if (texture.isEmpty() && capeTexture.isEmpty() && elytraTexture.isEmpty() && model.isEmpty()) {
                return EMPTY;
            }
            return new Patch(texture, capeTexture, elytraTexture, model);
        }
    }
}

