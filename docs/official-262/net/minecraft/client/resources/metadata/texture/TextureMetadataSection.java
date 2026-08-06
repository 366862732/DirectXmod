/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources.metadata.texture;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.server.packs.metadata.MetadataSectionType;

@Environment(value=EnvType.CLIENT)
public record TextureMetadataSection(boolean blur, boolean clamp, MipmapStrategy mipmapStrategy, float alphaCutoffBias) {
    public static final boolean DEFAULT_BLUR = false;
    public static final boolean DEFAULT_CLAMP = false;
    public static final float DEFAULT_ALPHA_CUTOFF_BIAS = 0.0f;
    public static final Codec<TextureMetadataSection> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.BOOL.optionalFieldOf("blur", (Object)false).forGetter(TextureMetadataSection::blur), (App)Codec.BOOL.optionalFieldOf("clamp", (Object)false).forGetter(TextureMetadataSection::clamp), (App)MipmapStrategy.CODEC.optionalFieldOf("mipmap_strategy", (Object)MipmapStrategy.AUTO).forGetter(TextureMetadataSection::mipmapStrategy), (App)Codec.FLOAT.optionalFieldOf("alpha_cutoff_bias", (Object)Float.valueOf(0.0f)).forGetter(TextureMetadataSection::alphaCutoffBias)).apply((Applicative)i, TextureMetadataSection::new));
    public static final MetadataSectionType<TextureMetadataSection> TYPE = new MetadataSectionType<TextureMetadataSection>("texture", CODEC);
}

