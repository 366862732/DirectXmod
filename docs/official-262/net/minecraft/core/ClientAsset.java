/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public interface ClientAsset {
    public Identifier id();

    public record DownloadedTexture(Identifier texturePath, String url) implements Texture
    {
        @Override
        public Identifier id() {
            return this.texturePath;
        }
    }

    public record ResourceTexture(Identifier id, Identifier texturePath) implements Texture
    {
        public static final Codec<ResourceTexture> CODEC = Identifier.CODEC.xmap(ResourceTexture::new, ResourceTexture::id);
        public static final MapCodec<ResourceTexture> DEFAULT_FIELD_CODEC = CODEC.fieldOf("asset_id");
        public static final StreamCodec<ByteBuf, ResourceTexture> STREAM_CODEC = Identifier.STREAM_CODEC.map(ResourceTexture::new, ResourceTexture::id);

        public ResourceTexture(Identifier texture) {
            this(texture, texture.withPath(path -> "textures/" + path + ".png"));
        }
    }

    public static interface Texture
    extends ClientAsset {
        public Identifier texturePath();
    }
}

