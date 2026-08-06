/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.ints.IntSet
 *  it.unimi.dsi.fastutil.ints.IntSets
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.blaze3d.font;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.gui.font.providers.GlyphProviderType;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SpaceProvider
implements GlyphProvider {
    private final Int2ObjectMap<EmptyGlyph> glyphs;

    public SpaceProvider(Map<Integer, Float> advances) {
        this.glyphs = new Int2ObjectOpenHashMap(advances.size());
        advances.forEach((codepoint, advance) -> this.glyphs.put(codepoint.intValue(), (Object)new EmptyGlyph(advance.floatValue())));
    }

    @Override
    public @Nullable UnbakedGlyph getGlyph(int codepoint) {
        return (UnbakedGlyph)this.glyphs.get(codepoint);
    }

    @Override
    public IntSet getSupportedGlyphs() {
        return IntSets.unmodifiable((IntSet)this.glyphs.keySet());
    }

    @Environment(value=EnvType.CLIENT)
    public record Definition(Map<Integer, Float> advances) implements GlyphProviderDefinition
    {
        public static final MapCodec<Definition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.unboundedMap(ExtraCodecs.CODEPOINT, (Codec)Codec.FLOAT).fieldOf("advances").forGetter(Definition::advances)).apply((Applicative)i, Definition::new));

        @Override
        public GlyphProviderType type() {
            return GlyphProviderType.SPACE;
        }

        @Override
        public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
            GlyphProviderDefinition.Loader loader = resourceManager -> new SpaceProvider(this.advances);
            return Either.left((Object)loader);
        }
    }
}

