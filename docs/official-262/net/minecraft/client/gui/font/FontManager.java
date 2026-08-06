/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonParseException
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.ints.IntCollection
 *  it.unimi.dsi.fastutil.ints.IntOpenHashSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.font;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.io.BufferedReader;
import java.io.Reader;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.AllMissingGlyphProvider;
import net.minecraft.client.gui.font.AtlasGlyphProvider;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.GlyphStitcher;
import net.minecraft.client.gui.font.PlayerGlyphProvider;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.DependencySorter;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class FontManager
implements AutoCloseable,
PreparableReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FONTS_PATH = "fonts.json";
    public static final Identifier MISSING_FONT = Identifier.withDefaultNamespace("missing");
    private static final FileToIdConverter FONT_DEFINITIONS = FileToIdConverter.json("font");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final FontSet missingFontSet;
    private final List<GlyphProvider> providersToClose = new ArrayList<GlyphProvider>();
    private final Map<Identifier, FontSet> fontSets = new HashMap<Identifier, FontSet>();
    private final TextureManager textureManager;
    private final CachedFontProvider anyGlyphs = new CachedFontProvider(this, false);
    private final CachedFontProvider nonFishyGlyphs = new CachedFontProvider(this, true);
    private final AtlasManager atlasManager;
    private final Map<Identifier, AtlasGlyphProvider> atlasProviders = new HashMap<Identifier, AtlasGlyphProvider>();
    private final PlayerGlyphProvider playerProvider;

    public FontManager(TextureManager textureManager, AtlasManager atlasManager, PlayerSkinRenderCache playerSkinRenderCache) {
        this.textureManager = textureManager;
        this.atlasManager = atlasManager;
        this.missingFontSet = this.createFontSet(MISSING_FONT, List.of(FontManager.createFallbackProvider()), Set.of());
        this.playerProvider = new PlayerGlyphProvider(playerSkinRenderCache);
    }

    private FontSet createFontSet(Identifier id, List<GlyphProvider.Conditional> providers, Set<FontOption> options) {
        GlyphStitcher stitcher = new GlyphStitcher(this.textureManager, id);
        FontSet result = new FontSet(stitcher);
        result.reload(providers, options);
        return result;
    }

    private static GlyphProvider.Conditional createFallbackProvider() {
        return new GlyphProvider.Conditional(new AllMissingGlyphProvider(), FontOption.Filter.ALWAYS_PASS);
    }

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.SharedState currentReload, Executor taskExecutor, PreparableReloadListener.PreparationBarrier preparationBarrier, Executor reloadExecutor) {
        return ((CompletableFuture)this.prepare(currentReload.resourceManager(), taskExecutor).thenCompose(preparationBarrier::wait)).thenAcceptAsync(preparations -> this.apply((Preparation)preparations, Profiler.get()), reloadExecutor);
    }

    private CompletableFuture<Preparation> prepare(ResourceManager manager, Executor executor) {
        ArrayList<CompletableFuture<UnresolvedBuilderBundle>> builderFutures = new ArrayList<CompletableFuture<UnresolvedBuilderBundle>>();
        for (Map.Entry<Identifier, List<Resource>> fontStack : FONT_DEFINITIONS.listMatchingResourceStacks(manager).entrySet()) {
            Identifier fontName = FONT_DEFINITIONS.fileToId(fontStack.getKey());
            builderFutures.add(CompletableFuture.supplyAsync(() -> {
                List<Pair<BuilderId, GlyphProviderDefinition.Conditional>> builderStack = FontManager.loadResourceStack((List)fontStack.getValue(), fontName);
                UnresolvedBuilderBundle bundle = new UnresolvedBuilderBundle(fontName);
                for (Pair<BuilderId, GlyphProviderDefinition.Conditional> stackEntry : builderStack) {
                    BuilderId id = (BuilderId)stackEntry.getFirst();
                    FontOption.Filter options = ((GlyphProviderDefinition.Conditional)stackEntry.getSecond()).filter();
                    ((GlyphProviderDefinition.Conditional)stackEntry.getSecond()).definition().unpack().ifLeft(provider -> {
                        CompletableFuture<Optional<GlyphProvider>> loadResult = this.safeLoad(id, (GlyphProviderDefinition.Loader)provider, manager, executor);
                        bundle.add(id, options, loadResult);
                    }).ifRight(reference -> bundle.add(id, options, (GlyphProviderDefinition.Reference)reference));
                }
                return bundle;
            }, executor));
        }
        return Util.sequence(builderFutures).thenCompose(builders -> {
            List allProviderFutures = builders.stream().flatMap(UnresolvedBuilderBundle::listBuilders).collect(Util.toMutableList());
            GlyphProvider.Conditional fallback = FontManager.createFallbackProvider();
            allProviderFutures.add(CompletableFuture.completedFuture(Optional.of(fallback.provider())));
            return Util.sequence(allProviderFutures).thenCompose(allProviders -> {
                Map<Identifier, List<GlyphProvider.Conditional>> resolved = this.resolveProviders((List<UnresolvedBuilderBundle>)builders);
                CompletableFuture[] finalizers = (CompletableFuture[])resolved.values().stream().map(providers -> CompletableFuture.runAsync(() -> this.finalizeProviderLoading((List<GlyphProvider.Conditional>)providers, fallback), executor)).toArray(CompletableFuture[]::new);
                return CompletableFuture.allOf(finalizers).thenApply(ignored -> {
                    List<GlyphProvider> providersToClose = allProviders.stream().flatMap(Optional::stream).toList();
                    return new Preparation(resolved, providersToClose);
                });
            });
        });
    }

    private CompletableFuture<Optional<GlyphProvider>> safeLoad(BuilderId id, GlyphProviderDefinition.Loader provider, ResourceManager manager, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Optional.of(provider.load(manager));
            }
            catch (Exception e) {
                LOGGER.warn("Failed to load builder {}, rejecting", (Object)id, (Object)e);
                return Optional.empty();
            }
        }, executor);
    }

    private Map<Identifier, List<GlyphProvider.Conditional>> resolveProviders(List<UnresolvedBuilderBundle> unresolvedProviders) {
        HashMap<Identifier, List<GlyphProvider.Conditional>> result = new HashMap<Identifier, List<GlyphProvider.Conditional>>();
        DependencySorter<Identifier, UnresolvedBuilderBundle> sorter = new DependencySorter<Identifier, UnresolvedBuilderBundle>();
        unresolvedProviders.forEach(e -> sorter.addEntry(e.fontId, (UnresolvedBuilderBundle)e));
        sorter.orderByDependencies((id, bundle) -> bundle.resolve(result::get).ifPresent(r -> result.put((Identifier)id, (List<GlyphProvider.Conditional>)r)));
        return result;
    }

    private void finalizeProviderLoading(List<GlyphProvider.Conditional> list, GlyphProvider.Conditional fallback) {
        list.add(0, fallback);
        IntOpenHashSet supportedGlyphs = new IntOpenHashSet();
        for (GlyphProvider.Conditional provider : list) {
            supportedGlyphs.addAll((IntCollection)provider.provider().getSupportedGlyphs());
        }
        supportedGlyphs.forEach(codepoint -> {
            GlyphProvider.Conditional provider;
            if (codepoint == 32) {
                return;
            }
            Iterator i$ = Lists.reverse((List)list).iterator();
            while (i$.hasNext() && (provider = (GlyphProvider.Conditional)i$.next()).provider().getGlyph(codepoint) == null) {
            }
        });
    }

    private static Set<FontOption> getFontOptions(Options options) {
        EnumSet<FontOption> result = EnumSet.noneOf(FontOption.class);
        if (options.forceUnicodeFont().get().booleanValue()) {
            result.add(FontOption.UNIFORM);
        }
        if (options.japaneseGlyphVariants().get().booleanValue()) {
            result.add(FontOption.JAPANESE_VARIANTS);
        }
        return result;
    }

    private void apply(Preparation preparations, ProfilerFiller profiler) {
        profiler.push("closing");
        this.anyGlyphs.invalidate();
        this.nonFishyGlyphs.invalidate();
        this.fontSets.values().forEach(FontSet::close);
        this.fontSets.clear();
        this.providersToClose.forEach(GlyphProvider::close);
        this.providersToClose.clear();
        Set<FontOption> fontOptions = FontManager.getFontOptions(Minecraft.getInstance().options);
        profiler.popPush("reloading");
        preparations.fontSets().forEach((id, newProviders) -> this.fontSets.put((Identifier)id, this.createFontSet((Identifier)id, Lists.reverse((List)newProviders), fontOptions)));
        this.providersToClose.addAll(preparations.allProviders);
        profiler.pop();
        if (!this.fontSets.containsKey(Minecraft.DEFAULT_FONT)) {
            throw new IllegalStateException("Default font failed to load");
        }
        this.atlasProviders.clear();
        this.atlasManager.forEach((atlasId, atlasTexture) -> this.atlasProviders.put((Identifier)atlasId, new AtlasGlyphProvider((TextureAtlas)atlasTexture)));
    }

    public void updateOptions(Options options) {
        Set<FontOption> fontOptions = FontManager.getFontOptions(options);
        for (FontSet value : this.fontSets.values()) {
            value.reload(fontOptions);
        }
    }

    private static List<Pair<BuilderId, GlyphProviderDefinition.Conditional>> loadResourceStack(List<Resource> resourceStack, Identifier fontName) {
        ArrayList<Pair<BuilderId, GlyphProviderDefinition.Conditional>> builderStack = new ArrayList<Pair<BuilderId, GlyphProviderDefinition.Conditional>>();
        for (Resource resource : resourceStack) {
            try {
                BufferedReader reader = resource.openAsReader();
                try {
                    JsonElement jsonContents = (JsonElement)GSON.fromJson((Reader)reader, JsonElement.class);
                    FontDefinitionFile definition = (FontDefinitionFile)FontDefinitionFile.CODEC.parse((DynamicOps)JsonOps.INSTANCE, (Object)jsonContents).getOrThrow(JsonParseException::new);
                    List<GlyphProviderDefinition.Conditional> providers = definition.providers;
                    for (int i = providers.size() - 1; i >= 0; --i) {
                        BuilderId id = new BuilderId(fontName, resource.sourcePackId(), i);
                        builderStack.add((Pair<BuilderId, GlyphProviderDefinition.Conditional>)Pair.of((Object)id, (Object)providers.get(i)));
                    }
                }
                finally {
                    if (reader == null) continue;
                    ((Reader)reader).close();
                }
            }
            catch (Exception e) {
                LOGGER.warn("Unable to load font '{}' in {} in resourcepack: '{}'", new Object[]{fontName, FONTS_PATH, resource.sourcePackId(), e});
            }
        }
        return builderStack;
    }

    public Font createFont() {
        return new Font(this.anyGlyphs);
    }

    public Font createFontFilterFishy() {
        return new Font(this.nonFishyGlyphs);
    }

    private FontSet getFontSetRaw(Identifier id) {
        return this.fontSets.getOrDefault(id, this.missingFontSet);
    }

    private GlyphSource getSpriteFont(FontDescription.AtlasSprite contents) {
        AtlasGlyphProvider provider = this.atlasProviders.get(contents.atlasId());
        if (provider == null) {
            return this.missingFontSet.source(false);
        }
        return provider.sourceForSprite(contents.spriteId());
    }

    @Override
    public void close() {
        this.anyGlyphs.close();
        this.nonFishyGlyphs.close();
        this.fontSets.values().forEach(FontSet::close);
        this.providersToClose.forEach(GlyphProvider::close);
        this.missingFontSet.close();
    }

    @Environment(value=EnvType.CLIENT)
    private class CachedFontProvider
    implements Font.Provider,
    AutoCloseable {
        private final boolean nonFishyOnly;
        private volatile @Nullable CachedEntry lastEntry;
        private volatile @Nullable EffectGlyph whiteGlyph;
        final /* synthetic */ FontManager this$0;

        private CachedFontProvider(FontManager fontManager, boolean nonFishyOnly) {
            FontManager fontManager2 = fontManager;
            Objects.requireNonNull(fontManager2);
            this.this$0 = fontManager2;
            this.nonFishyOnly = nonFishyOnly;
        }

        public void invalidate() {
            this.lastEntry = null;
            this.whiteGlyph = null;
        }

        @Override
        public void close() {
            this.invalidate();
        }

        private GlyphSource getGlyphSource(FontDescription description) {
            FontDescription fontDescription = description;
            Objects.requireNonNull(fontDescription);
            FontDescription fontDescription2 = fontDescription;
            int n = 0;
            return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{FontDescription.Resource.class, FontDescription.AtlasSprite.class, FontDescription.PlayerSprite.class}, (FontDescription)fontDescription2, n)) {
                case 0 -> {
                    FontDescription.Resource resource = (FontDescription.Resource)fontDescription2;
                    yield this.this$0.getFontSetRaw(resource.id()).source(this.nonFishyOnly);
                }
                case 1 -> {
                    FontDescription.AtlasSprite sprite = (FontDescription.AtlasSprite)fontDescription2;
                    yield this.this$0.getSpriteFont(sprite);
                }
                case 2 -> {
                    FontDescription.PlayerSprite player = (FontDescription.PlayerSprite)fontDescription2;
                    yield this.this$0.playerProvider.sourceForPlayer(player);
                }
                default -> this.this$0.missingFontSet.source(this.nonFishyOnly);
            };
        }

        @Override
        public GlyphSource glyphs(FontDescription description) {
            CachedEntry lastEntry = this.lastEntry;
            if (lastEntry != null && description.equals(lastEntry.description)) {
                return lastEntry.source;
            }
            GlyphSource result = this.getGlyphSource(description);
            this.lastEntry = new CachedEntry(description, result);
            return result;
        }

        @Override
        public EffectGlyph effect() {
            EffectGlyph whiteGlyph = this.whiteGlyph;
            if (whiteGlyph == null) {
                this.whiteGlyph = whiteGlyph = this.this$0.getFontSetRaw(FontDescription.DEFAULT.id()).whiteGlyph();
            }
            return whiteGlyph;
        }

        @Environment(value=EnvType.CLIENT)
        private record CachedEntry(FontDescription description, GlyphSource source) {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record BuilderId(Identifier fontId, String pack, int index) {
        @Override
        public String toString() {
            return "(" + String.valueOf(this.fontId) + ": builder #" + this.index + " from pack " + this.pack + ")";
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record Preparation(Map<Identifier, List<GlyphProvider.Conditional>> fontSets, List<GlyphProvider> allProviders) {
    }

    @Environment(value=EnvType.CLIENT)
    private record FontDefinitionFile(List<GlyphProviderDefinition.Conditional> providers) {
        public static final Codec<FontDefinitionFile> CODEC = RecordCodecBuilder.create(i -> i.group((App)GlyphProviderDefinition.Conditional.CODEC.listOf().fieldOf("providers").forGetter(FontDefinitionFile::providers)).apply((Applicative)i, FontDefinitionFile::new));
    }

    @Environment(value=EnvType.CLIENT)
    private record UnresolvedBuilderBundle(Identifier fontId, List<BuilderResult> builders, Set<Identifier> dependencies) implements DependencySorter.Entry<Identifier>
    {
        public UnresolvedBuilderBundle(Identifier fontId) {
            this(fontId, new ArrayList<BuilderResult>(), new HashSet<Identifier>());
        }

        public void add(BuilderId builderId, FontOption.Filter filter, GlyphProviderDefinition.Reference reference) {
            this.builders.add(new BuilderResult(builderId, filter, (Either<CompletableFuture<Optional<GlyphProvider>>, Identifier>)Either.right((Object)reference.id())));
            this.dependencies.add(reference.id());
        }

        public void add(BuilderId builderId, FontOption.Filter filter, CompletableFuture<Optional<GlyphProvider>> provider) {
            this.builders.add(new BuilderResult(builderId, filter, (Either<CompletableFuture<Optional<GlyphProvider>>, Identifier>)Either.left(provider)));
        }

        private Stream<CompletableFuture<Optional<GlyphProvider>>> listBuilders() {
            return this.builders.stream().flatMap(e -> e.result.left().stream());
        }

        public Optional<List<GlyphProvider.Conditional>> resolve(Function<Identifier, List<GlyphProvider.Conditional>> resolver) {
            ArrayList resolved = new ArrayList();
            for (BuilderResult builder : this.builders) {
                Optional<List<GlyphProvider.Conditional>> resolvedBuilder = builder.resolve(resolver);
                if (resolvedBuilder.isPresent()) {
                    resolved.addAll(resolvedBuilder.get());
                    continue;
                }
                return Optional.empty();
            }
            return Optional.of(resolved);
        }

        @Override
        public void visitRequiredDependencies(Consumer<Identifier> output) {
            this.dependencies.forEach(output);
        }

        @Override
        public void visitOptionalDependencies(Consumer<Identifier> output) {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record BuilderResult(BuilderId id, FontOption.Filter filter, Either<CompletableFuture<Optional<GlyphProvider>>, Identifier> result) {
        public Optional<List<GlyphProvider.Conditional>> resolve(Function<Identifier, @Nullable List<GlyphProvider.Conditional>> resolver) {
            return (Optional)this.result.map(provider -> ((Optional)provider.join()).map(p -> List.of(new GlyphProvider.Conditional((GlyphProvider)p, this.filter))), reference -> {
                List resolvedReferences = (List)resolver.apply((Identifier)reference);
                if (resolvedReferences == null) {
                    LOGGER.warn("Can't find font {} referenced by builder {}, either because it's missing, failed to load or is part of loading cycle", reference, (Object)this.id);
                    return Optional.empty();
                }
                return Optional.of(resolvedReferences.stream().map(this::mergeFilters).toList());
            });
        }

        private GlyphProvider.Conditional mergeFilters(GlyphProvider.Conditional original) {
            return new GlyphProvider.Conditional(original.provider(), this.filter.merge(original.filter()));
        }
    }
}

