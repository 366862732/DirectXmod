/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.IdentifierPattern;

@Environment(value=EnvType.CLIENT)
public record SourceFilter(IdentifierPattern filter) implements SpriteSource
{
    public static final MapCodec<SourceFilter> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)IdentifierPattern.CODEC.fieldOf("pattern").forGetter(SourceFilter::filter)).apply((Applicative)i, SourceFilter::new));

    @Override
    public void run(ResourceManager resourceManager, SpriteSource.Output output) {
        output.removeAll(this.filter.locationPredicate());
    }

    public MapCodec<SourceFilter> codec() {
        return MAP_CODEC;
    }
}

