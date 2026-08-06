/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

@Environment(value=EnvType.CLIENT)
public record DirectoryLister(String sourcePath, String idPrefix) implements SpriteSource
{
    public static final MapCodec<DirectoryLister> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.STRING.fieldOf("source").forGetter(DirectoryLister::sourcePath), (App)Codec.STRING.fieldOf("prefix").forGetter(DirectoryLister::idPrefix)).apply((Applicative)i, DirectoryLister::new));

    @Override
    public void run(ResourceManager resourceManager, SpriteSource.Output output) {
        FileToIdConverter converter = new FileToIdConverter("textures/" + this.sourcePath, ".png");
        converter.listMatchingResources(resourceManager).forEach((identifier, resource) -> {
            Identifier spriteLocation = converter.fileToId((Identifier)identifier).withPrefix(this.idPrefix);
            output.add(spriteLocation, (Resource)resource);
        });
    }

    public MapCodec<DirectoryLister> codec() {
        return MAP_CODEC;
    }
}

