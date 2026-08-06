/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.ints.IntCollection
 *  it.unimi.dsi.fastutil.ints.IntList
 *  it.unimi.dsi.fastutil.ints.IntOpenHashSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.font;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.GlyphStitcher;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class FontSet
implements AutoCloseable {
    private static final float LARGE_FORWARD_ADVANCE = 32.0f;
    private static final BakedGlyph INVISIBLE_MISSING_GLYPH = new BakedGlyph(){

        @Override
        public GlyphInfo info() {
            return SpecialGlyphs.MISSING;
        }

        @Override
        public @Nullable TextRenderable.Styled createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
            return null;
        }
    };
    private final GlyphStitcher stitcher;
    private final UnbakedGlyph.Stitcher wrappedStitcher = new UnbakedGlyph.Stitcher(this){
        final /* synthetic */ FontSet this$0;
        {
            FontSet fontSet = this$0;
            Objects.requireNonNull(fontSet);
            this.this$0 = fontSet;
        }

        @Override
        public BakedGlyph stitch(GlyphInfo glyphInfo, GlyphBitmap glyphBitmap) {
            return Objects.requireNonNullElse(this.this$0.stitcher.stitch(glyphInfo, glyphBitmap), this.this$0.missingGlyph);
        }

        @Override
        public BakedGlyph getMissing() {
            return this.this$0.missingGlyph;
        }
    };
    private List<GlyphProvider.Conditional> allProviders = List.of();
    private List<GlyphProvider> activeProviders = List.of();
    private final Int2ObjectMap<IntList> glyphsByWidth = new Int2ObjectOpenHashMap();
    private final CodepointMap<SelectedGlyphs> glyphCache = new CodepointMap(SelectedGlyphs[]::new, x$0 -> new SelectedGlyphs[x$0][]);
    private final IntFunction<SelectedGlyphs> glyphGetter = this::computeGlyphInfo;
    private BakedGlyph missingGlyph = INVISIBLE_MISSING_GLYPH;
    private final Supplier<BakedGlyph> missingGlyphGetter = () -> this.missingGlyph;
    private final SelectedGlyphs missingSelectedGlyphs = new SelectedGlyphs(this.missingGlyphGetter, this.missingGlyphGetter);
    private @Nullable EffectGlyph whiteGlyph;
    private final GlyphSource anyGlyphs = new Source(this, false);
    private final GlyphSource nonFishyGlyphs = new Source(this, true);

    public FontSet(GlyphStitcher stitcher) {
        this.stitcher = stitcher;
    }

    public void reload(List<GlyphProvider.Conditional> providers, Set<FontOption> options) {
        this.allProviders = providers;
        this.reload(options);
    }

    public void reload(Set<FontOption> options) {
        this.activeProviders = List.of();
        this.resetTextures();
        this.activeProviders = this.selectProviders(this.allProviders, options);
    }

    private void resetTextures() {
        this.stitcher.reset();
        this.glyphCache.clear();
        this.glyphsByWidth.clear();
        this.missingGlyph = Objects.requireNonNull(SpecialGlyphs.MISSING.bake(this.stitcher));
        this.whiteGlyph = SpecialGlyphs.WHITE.bake(this.stitcher);
    }

    private List<GlyphProvider> selectProviders(List<GlyphProvider.Conditional> providers, Set<FontOption> options) {
        IntOpenHashSet supportedGlyphs = new IntOpenHashSet();
        ArrayList<GlyphProvider> selectedProviders = new ArrayList<GlyphProvider>();
        for (GlyphProvider.Conditional conditionalProvider : providers) {
            if (!conditionalProvider.filter().apply(options)) continue;
            selectedProviders.add(conditionalProvider.provider());
            supportedGlyphs.addAll((IntCollection)conditionalProvider.provider().getSupportedGlyphs());
        }
        HashSet usedProviders = Sets.newHashSet();
        supportedGlyphs.forEach(codepoint -> {
            for (GlyphProvider provider : selectedProviders) {
                UnbakedGlyph glyph = provider.getGlyph(codepoint);
                if (glyph == null) continue;
                usedProviders.add(provider);
                if (glyph.info() == SpecialGlyphs.MISSING) break;
                ((IntList)this.glyphsByWidth.computeIfAbsent(Mth.ceil(glyph.info().getAdvance(false)), w -> new IntArrayList())).add(codepoint);
                break;
            }
        });
        return selectedProviders.stream().filter(usedProviders::contains).toList();
    }

    @Override
    public void close() {
        this.stitcher.close();
    }

    private static boolean hasFishyAdvance(GlyphInfo glyph) {
        float advance = glyph.getAdvance(false);
        if (advance < 0.0f || advance > 32.0f) {
            return true;
        }
        float boldAdvance = glyph.getAdvance(true);
        return boldAdvance < 0.0f || boldAdvance > 32.0f;
    }

    private SelectedGlyphs computeGlyphInfo(int codepoint) {
        DelayedBake firstGlyph = null;
        for (GlyphProvider provider : this.activeProviders) {
            UnbakedGlyph glyph = provider.getGlyph(codepoint);
            if (glyph == null) continue;
            if (firstGlyph == null) {
                firstGlyph = new DelayedBake(this, glyph);
            }
            if (FontSet.hasFishyAdvance(glyph.info())) continue;
            if (firstGlyph.unbaked == glyph) {
                return new SelectedGlyphs(firstGlyph, firstGlyph);
            }
            return new SelectedGlyphs(firstGlyph, new DelayedBake(this, glyph));
        }
        if (firstGlyph != null) {
            return new SelectedGlyphs(firstGlyph, this.missingGlyphGetter);
        }
        return this.missingSelectedGlyphs;
    }

    private SelectedGlyphs getGlyph(int codepoint) {
        return this.glyphCache.computeIfAbsent(codepoint, this.glyphGetter);
    }

    public BakedGlyph getRandomGlyph(RandomSource random, int width) {
        IntList chars = (IntList)this.glyphsByWidth.get(width);
        if (chars != null && !chars.isEmpty()) {
            return this.getGlyph(chars.getInt(random.nextInt(chars.size()))).nonFishy().get();
        }
        return this.missingGlyph;
    }

    public EffectGlyph whiteGlyph() {
        return Objects.requireNonNull(this.whiteGlyph);
    }

    public GlyphSource source(boolean nonFishyOnly) {
        return nonFishyOnly ? this.nonFishyGlyphs : this.anyGlyphs;
    }

    @Environment(value=EnvType.CLIENT)
    private record SelectedGlyphs(Supplier<BakedGlyph> any, Supplier<BakedGlyph> nonFishy) {
        private Supplier<BakedGlyph> select(boolean filterFishy) {
            return filterFishy ? this.nonFishy : this.any;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public class Source
    implements GlyphSource {
        private final boolean filterFishyGlyphs;
        final /* synthetic */ FontSet this$0;

        public Source(FontSet this$0, boolean filterFishyGlyphs) {
            FontSet fontSet = this$0;
            Objects.requireNonNull(fontSet);
            this.this$0 = fontSet;
            this.filterFishyGlyphs = filterFishyGlyphs;
        }

        @Override
        public BakedGlyph getGlyph(int codepoint) {
            return this.this$0.getGlyph(codepoint).select(this.filterFishyGlyphs).get();
        }

        @Override
        public BakedGlyph getRandomGlyph(RandomSource random, int width) {
            return this.this$0.getRandomGlyph(random, width);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class DelayedBake
    implements Supplier<BakedGlyph> {
        private final UnbakedGlyph unbaked;
        private @Nullable BakedGlyph baked;
        final /* synthetic */ FontSet this$0;

        private DelayedBake(FontSet fontSet, UnbakedGlyph unbaked) {
            FontSet fontSet2 = fontSet;
            Objects.requireNonNull(fontSet2);
            this.this$0 = fontSet2;
            this.unbaked = unbaked;
        }

        @Override
        public BakedGlyph get() {
            if (this.baked == null) {
                this.baked = this.unbaked.bake(this.this$0.wrappedStitcher);
            }
            return this.baked;
        }
    }
}

