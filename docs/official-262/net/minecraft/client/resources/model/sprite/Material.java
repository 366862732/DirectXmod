/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources.model.sprite;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public record Material(Identifier sprite, boolean forceTranslucent) {
    private static final Codec<Material> SIMPLE_CODEC = Identifier.CODEC.xmap(Material::new, Material::sprite);
    private static final Codec<Material> FULL_CODEC = RecordCodecBuilder.create(i -> i.group((App)Identifier.CODEC.fieldOf("sprite").forGetter(Material::sprite), (App)Codec.BOOL.optionalFieldOf("force_translucent", (Object)false).forGetter(Material::forceTranslucent)).apply((Applicative)i, Material::new));
    public static final Codec<Material> CODEC = Codec.either(SIMPLE_CODEC, FULL_CODEC).xmap(Either::unwrap, material -> material.forceTranslucent ? Either.right((Object)material) : Either.left((Object)material));

    public Material(Identifier sprite) {
        this(sprite, false);
    }

    public Material withForceTranslucent(boolean forceTranslucent) {
        return new Material(this.sprite, forceTranslucent);
    }

    @Environment(value=EnvType.CLIENT)
    public record Baked(TextureAtlasSprite sprite, boolean forceTranslucent) {
    }
}

