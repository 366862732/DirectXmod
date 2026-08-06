/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonParseException
 *  com.google.gson.JsonSyntaxException
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  it.unimi.dsi.fastutil.objects.ObjectArraySet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.io.IOUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.IdentifierException;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.FileUtil;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class ShaderManager
extends SimplePreparableReloadListener<Configs>
implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int MAX_LOG_LENGTH = 32768;
    public static final String SHADER_PATH = "shaders";
    private static final String SHADER_INCLUDE_PATH = "shaders/include/";
    private static final FileToIdConverter POST_CHAIN_ID_CONVERTER = FileToIdConverter.json("post_effect");
    private final TextureManager textureManager;
    private final Consumer<Exception> recoveryHandler;
    private CompilationCache compilationCache = new CompilationCache(this, Configs.EMPTY);
    private final Projection postChainProjection = new Projection();
    private final ProjectionMatrixBuffer postChainProjectionMatrixBuffer = new ProjectionMatrixBuffer("post");

    public ShaderManager(TextureManager textureManager, Consumer<Exception> recoveryHandler) {
        this.textureManager = textureManager;
        this.recoveryHandler = recoveryHandler;
        this.postChainProjection.setupOrtho(0.1f, 1000.0f, 1.0f, 1.0f, false);
    }

    @Override
    protected Configs prepare(ResourceManager manager, ProfilerFiller profiler) {
        ImmutableMap.Builder shaderSources = ImmutableMap.builder();
        Map<Identifier, Resource> files = manager.listResources(SHADER_PATH, ShaderManager::isShader);
        for (Map.Entry<Identifier, Resource> entry : files.entrySet()) {
            Identifier location = entry.getKey();
            ShaderType shaderType = ShaderType.byLocation(location);
            if (shaderType == null) continue;
            ShaderManager.loadShader(location, entry.getValue(), shaderType, files, (ImmutableMap.Builder<ShaderSourceKey, String>)shaderSources);
        }
        ImmutableMap.Builder postChains = ImmutableMap.builder();
        for (Map.Entry<Identifier, Resource> entry : POST_CHAIN_ID_CONVERTER.listMatchingResources(manager).entrySet()) {
            ShaderManager.loadPostChain(entry.getKey(), entry.getValue(), (ImmutableMap.Builder<Identifier, PostChainConfig>)postChains);
        }
        return new Configs((Map<ShaderSourceKey, String>)shaderSources.build(), (Map<Identifier, PostChainConfig>)postChains.build());
    }

    private static void loadShader(Identifier location, Resource resource, ShaderType type, Map<Identifier, Resource> files, ImmutableMap.Builder<ShaderSourceKey, String> output) {
        Identifier id = type.idConverter().fileToId(location);
        GlslPreprocessor preprocessor = ShaderManager.createPreprocessor(files, location);
        try (BufferedReader reader = resource.openAsReader();){
            String source = IOUtils.toString((Reader)reader);
            output.put((Object)new ShaderSourceKey(id, type), (Object)String.join((CharSequence)"", preprocessor.process(source)));
        }
        catch (IOException e) {
            LOGGER.error("Failed to load shader source at {}", (Object)location, (Object)e);
        }
    }

    private static GlslPreprocessor createPreprocessor(final Map<Identifier, Resource> files, Identifier location) {
        final Identifier parentLocation = location.withPath(FileUtil::getFullResourcePath);
        return new GlslPreprocessor(){
            private final Set<Identifier> importedLocations = new ObjectArraySet();

            @Override
            public @Nullable String applyImport(boolean isRelative, String path) {
                String string;
                block11: {
                    Identifier location;
                    try {
                        location = isRelative ? parentLocation.withPath(parentPath -> FileUtil.normalizeResourcePath(parentPath + path)) : Identifier.parse(path).withPrefix(ShaderManager.SHADER_INCLUDE_PATH);
                    }
                    catch (IdentifierException e) {
                        LOGGER.error("Malformed GLSL import {}: {}", (Object)path, (Object)e.getMessage());
                        return "#error " + e.getMessage();
                    }
                    if (!this.importedLocations.add(location)) {
                        return null;
                    }
                    BufferedReader importResource = ((Resource)files.get(location)).openAsReader();
                    try {
                        string = IOUtils.toString((Reader)importResource);
                        if (importResource == null) break block11;
                    }
                    catch (Throwable throwable) {
                        try {
                            if (importResource != null) {
                                try {
                                    ((Reader)importResource).close();
                                }
                                catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }
                            throw throwable;
                        }
                        catch (IOException e) {
                            LOGGER.error("Could not open GLSL import {}: {}", (Object)location, (Object)e.getMessage());
                            return "#error " + e.getMessage();
                        }
                    }
                    ((Reader)importResource).close();
                }
                return string;
            }
        };
    }

    private static void loadPostChain(Identifier location, Resource resource, ImmutableMap.Builder<Identifier, PostChainConfig> output) {
        Identifier id = POST_CHAIN_ID_CONVERTER.fileToId(location);
        try (BufferedReader reader = resource.openAsReader();){
            JsonElement json = StrictJsonParser.parse(reader);
            output.put((Object)id, (Object)((PostChainConfig)PostChainConfig.CODEC.parse((DynamicOps)JsonOps.INSTANCE, (Object)json).getOrThrow(JsonSyntaxException::new)));
        }
        catch (JsonParseException | IOException e) {
            LOGGER.error("Failed to parse post chain at {}", (Object)location, (Object)e);
        }
    }

    private static boolean isShader(Identifier location) {
        return ShaderType.byLocation(location) != null || location.getPath().endsWith(".glsl");
    }

    @Override
    protected void apply(Configs preparations, ResourceManager manager, ProfilerFiller profiler) {
        CompilationCache newCompilationCache = new CompilationCache(this, preparations);
        HashSet<RenderPipeline> pipelinesToPreload = new HashSet<RenderPipeline>(RenderPipelines.getStaticPipelines());
        ArrayList<Identifier> failedLoads = new ArrayList<Identifier>();
        GpuDevice device = RenderSystem.getDevice();
        device.clearPipelineCache();
        for (RenderPipeline pipeline : pipelinesToPreload) {
            CompiledRenderPipeline compiled = device.precompilePipeline(pipeline, newCompilationCache::getShaderSource);
            if (compiled.isValid()) continue;
            failedLoads.add(pipeline.getLocation());
        }
        if (!failedLoads.isEmpty()) {
            device.clearPipelineCache();
            device.loadCriticalShaders();
            throw new RuntimeException("Failed to load required shader programs:\n" + failedLoads.stream().map(entry -> " - " + String.valueOf(entry)).collect(Collectors.joining("\n")));
        }
        this.compilationCache.close();
        this.compilationCache = newCompilationCache;
    }

    @Override
    public String getName() {
        return "Shader Loader";
    }

    private void tryTriggerRecovery(Exception exception) {
        if (this.compilationCache.triggeredRecovery) {
            return;
        }
        this.recoveryHandler.accept(exception);
        this.compilationCache.triggeredRecovery = true;
    }

    public @Nullable PostChain getPostChain(Identifier id, Set<Identifier> allowedTargets) {
        try {
            return this.compilationCache.getOrLoadPostChain(id, allowedTargets);
        }
        catch (CompilationException e) {
            LOGGER.error("Failed to load post chain: {}", (Object)id, (Object)e);
            this.compilationCache.postChains.put(id, Optional.empty());
            this.tryTriggerRecovery(e);
            return null;
        }
    }

    @Override
    public void close() {
        this.compilationCache.close();
        this.postChainProjectionMatrixBuffer.close();
    }

    public @Nullable String getShader(Identifier id, ShaderType type) {
        return this.compilationCache.getShaderSource(id, type);
    }

    @Environment(value=EnvType.CLIENT)
    private class CompilationCache
    implements AutoCloseable {
        private final Configs configs;
        private final Map<Identifier, Optional<PostChain>> postChains;
        private boolean triggeredRecovery;
        final /* synthetic */ ShaderManager this$0;

        private CompilationCache(ShaderManager shaderManager, Configs configs) {
            ShaderManager shaderManager2 = shaderManager;
            Objects.requireNonNull(shaderManager2);
            this.this$0 = shaderManager2;
            this.postChains = new HashMap<Identifier, Optional<PostChain>>();
            this.configs = configs;
        }

        public @Nullable PostChain getOrLoadPostChain(Identifier id, Set<Identifier> allowedTargets) throws CompilationException {
            Optional<PostChain> cached = this.postChains.get(id);
            if (cached != null) {
                return cached.orElse(null);
            }
            PostChain postChain = this.loadPostChain(id, allowedTargets);
            this.postChains.put(id, Optional.of(postChain));
            return postChain;
        }

        private PostChain loadPostChain(Identifier id, Set<Identifier> allowedTargets) throws CompilationException {
            PostChainConfig config = this.configs.postChains.get(id);
            if (config == null) {
                throw new CompilationException("Could not find post chain with id: " + String.valueOf(id));
            }
            return PostChain.load(config, this.this$0.textureManager, allowedTargets, id, this.this$0.postChainProjection, this.this$0.postChainProjectionMatrixBuffer);
        }

        @Override
        public void close() {
            this.postChains.values().forEach(chain -> chain.ifPresent(PostChain::close));
            this.postChains.clear();
        }

        public @Nullable String getShaderSource(Identifier id, ShaderType type) {
            return this.configs.shaderSources.get(new ShaderSourceKey(id, type));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Configs(Map<ShaderSourceKey, String> shaderSources, Map<Identifier, PostChainConfig> postChains) {
        public static final Configs EMPTY = new Configs(Map.of(), Map.of());
    }

    @Environment(value=EnvType.CLIENT)
    private record ShaderSourceKey(Identifier id, ShaderType type) {
        @Override
        public String toString() {
            return String.valueOf(this.id) + " (" + String.valueOf((Object)this.type) + ")";
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class CompilationException
    extends Exception {
        public CompilationException(String message) {
            super(message);
        }
    }
}

