/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.google.common.collect.Sets
 *  com.google.common.collect.Sets$SetView
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector4fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class PostChain
implements AutoCloseable {
    public static final Identifier MAIN_TARGET_ID = Identifier.withDefaultNamespace("main");
    private final List<PostPass> passes;
    private final Map<Identifier, PostChainConfig.InternalTarget> internalTargets;
    private final Set<Identifier> externalTargets;
    private final Map<Identifier, RenderTarget> persistentTargets = new HashMap<Identifier, RenderTarget>();
    private final Projection projection;
    private final ProjectionMatrixBuffer projectionMatrixBuffer;

    private PostChain(List<PostPass> passes, Map<Identifier, PostChainConfig.InternalTarget> internalTargets, Set<Identifier> externalTargets, Projection projection, ProjectionMatrixBuffer projectionMatrixBuffer) {
        this.passes = passes;
        this.internalTargets = internalTargets;
        this.externalTargets = externalTargets;
        this.projection = projection;
        this.projectionMatrixBuffer = projectionMatrixBuffer;
    }

    public static PostChain load(PostChainConfig config, TextureManager textureManager, Set<Identifier> allowedExternalTargets, Identifier id, Projection projection, ProjectionMatrixBuffer projectionMatrixBuffer) throws ShaderManager.CompilationException {
        Stream referencedTargets = config.passes().stream().flatMap(PostChainConfig.Pass::referencedTargets);
        Set<Identifier> referencedExternalTargets = referencedTargets.filter(targetId -> !config.internalTargets().containsKey(targetId)).collect(Collectors.toSet());
        Sets.SetView invalidExternalTargets = Sets.difference(referencedExternalTargets, allowedExternalTargets);
        if (!invalidExternalTargets.isEmpty()) {
            throw new ShaderManager.CompilationException("Referenced external targets are not available in this context: " + String.valueOf(invalidExternalTargets));
        }
        ImmutableList.Builder passes = ImmutableList.builder();
        for (int i = 0; i < config.passes().size(); ++i) {
            PostChainConfig.Pass pass = config.passes().get(i);
            passes.add((Object)PostChain.createPass(textureManager, pass, id.withSuffix("/" + i)));
        }
        return new PostChain((List<PostPass>)passes.build(), config.internalTargets(), referencedExternalTargets, projection, projectionMatrixBuffer);
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static PostPass createPass(TextureManager textureManager, PostChainConfig.Pass config, Identifier id) throws ShaderManager.CompilationException {
        RenderPipeline.Builder pipelineBuilder = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET).withFragmentShader(config.fragmentShaderId()).withVertexShader(config.vertexShaderId()).withLocation(id);
        BindGroupLayout.Builder bindGroupLayoutBuilder = BindGroupLayout.builder();
        for (PostChainConfig.Input input : config.inputs()) {
            bindGroupLayoutBuilder.withSampler(input.samplerName() + "Sampler");
        }
        bindGroupLayoutBuilder.withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER);
        for (String uniformGroupName : config.uniforms().keySet()) {
            bindGroupLayoutBuilder.withUniform(uniformGroupName, UniformType.UNIFORM_BUFFER);
        }
        pipelineBuilder.withBindGroupLayout(bindGroupLayoutBuilder.build());
        RenderPipeline pipeline = pipelineBuilder.build();
        if (!RenderSystem.getDevice().precompilePipeline(pipeline).isValid()) {
            throw new ShaderManager.CompilationException("Failed to compile post processing pipeline " + String.valueOf(pipeline.getLocation()));
        }
        ArrayList<PostPass.Input> inputs = new ArrayList<PostPass.Input>();
        Iterator<PostChainConfig.Input> iterator = config.inputs().iterator();
        block9: while (true) {
            PostChainConfig.Input input;
            if (!iterator.hasNext()) {
                return new PostPass(pipeline, config.outputTarget(), config.uniforms(), inputs);
            }
            PostChainConfig.Input input2 = iterator.next();
            Objects.requireNonNull(input2);
            int n = 0;
            switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{PostChainConfig.TextureInput.class, PostChainConfig.TargetInput.class}, (PostChainConfig.Input)input, n)) {
                case 0: {
                    boolean bl;
                    PostChainConfig.TextureInput textureInput = (PostChainConfig.TextureInput)input;
                    Object object = textureInput.samplerName();
                    String samplerName = object;
                    Object location = object = textureInput.location();
                    boolean bl2 = bl = textureInput.width();
                    boolean width = bl;
                    bl2 = bl = textureInput.height();
                    boolean height = bl;
                    bl2 = bl = (boolean)textureInput.bilinear();
                    boolean bilinear = bl;
                    AbstractTexture texture = textureManager.getTexture(((Identifier)location).withPath(path -> "textures/effect/" + path + ".png"));
                    inputs.add(new PostPass.TextureInput(samplerName, texture, width ? 1 : 0, height ? 1 : 0, bilinear));
                    continue block9;
                }
                case 1: {
                    boolean useDepthBuffer;
                    boolean bl;
                    Object targetId;
                    String samplerName;
                    PostChainConfig.TargetInput targetInput = (PostChainConfig.TargetInput)input;
                    try {
                        Object object = targetInput.samplerName();
                        samplerName = object;
                        targetId = object = targetInput.targetId();
                        boolean bl3 = bl = targetInput.useDepthBuffer();
                        useDepthBuffer = bl;
                        bl3 = bl = targetInput.bilinear();
                    }
                    catch (Throwable throwable) {
                        throw new MatchException(throwable.toString(), throwable);
                    }
                    boolean bilinear = bl;
                    inputs.add(new PostPass.TargetInput(samplerName, (Identifier)targetId, useDepthBuffer, bilinear));
                    continue block9;
                }
            }
            break;
        }
        throw new MatchException(null, null);
    }

    public void addToFrame(FrameGraphBuilder frame, int screenWidth, int screenHeight, TargetBundle providedTargets) {
        this.projection.setSize(screenWidth, screenHeight);
        GpuBufferSlice projectionBuffer = this.projectionMatrixBuffer.getBuffer(this.projection);
        HashMap<Identifier, ResourceHandle<RenderTarget>> targets = new HashMap<Identifier, ResourceHandle<RenderTarget>>(this.internalTargets.size() + this.externalTargets.size());
        for (Identifier identifier : this.externalTargets) {
            targets.put(identifier, providedTargets.getOrThrow(identifier));
        }
        for (Map.Entry entry : this.internalTargets.entrySet()) {
            Identifier id = (Identifier)entry.getKey();
            PostChainConfig.InternalTarget target = (PostChainConfig.InternalTarget)entry.getValue();
            RenderTargetDescriptor descriptor = new RenderTargetDescriptor(target.width().orElse(screenWidth), target.height().orElse(screenHeight), true, (Vector4fc)ARGB.vector4fFromARGB32(target.clearColor()), GpuFormat.RGBA8_UNORM);
            if (target.persistent()) {
                RenderTarget persistentTarget = this.getOrCreatePersistentTarget(id, descriptor);
                targets.put(id, frame.importExternal(id.toString(), persistentTarget));
                continue;
            }
            targets.put(id, frame.createInternal(id.toString(), descriptor));
        }
        for (PostPass postPass : this.passes) {
            postPass.addToFrame(frame, targets, projectionBuffer);
        }
        for (Identifier identifier : this.externalTargets) {
            providedTargets.replace(identifier, (ResourceHandle)targets.get(identifier));
        }
    }

    @Deprecated
    public void process(RenderTarget mainTarget, GraphicsResourceAllocator resourceAllocator) {
        FrameGraphBuilder frame = new FrameGraphBuilder();
        TargetBundle targets = TargetBundle.of(MAIN_TARGET_ID, frame.importExternal("main", mainTarget));
        this.addToFrame(frame, mainTarget.width, mainTarget.height, targets);
        frame.execute(resourceAllocator);
    }

    private RenderTarget getOrCreatePersistentTarget(Identifier id, RenderTargetDescriptor descriptor) {
        RenderTarget target = this.persistentTargets.get(id);
        if (target == null || target.width != descriptor.width() || target.height != descriptor.height()) {
            if (target != null) {
                target.destroyBuffers();
            }
            target = descriptor.allocate();
            descriptor.prepare(target);
            this.persistentTargets.put(id, target);
        }
        return target;
    }

    @Override
    public void close() {
        this.persistentTargets.values().forEach(RenderTarget::destroyBuffers);
        this.persistentTargets.clear();
        for (PostPass pass : this.passes) {
            pass.close();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static interface TargetBundle {
        public static TargetBundle of(final Identifier targetId, final ResourceHandle<RenderTarget> target) {
            return new TargetBundle(){
                private ResourceHandle<RenderTarget> handle;
                {
                    this.handle = target;
                }

                @Override
                public void replace(Identifier id, ResourceHandle<RenderTarget> handle) {
                    if (!id.equals(targetId)) {
                        throw new IllegalArgumentException("No target with id " + String.valueOf(id));
                    }
                    this.handle = handle;
                }

                @Override
                public @Nullable ResourceHandle<RenderTarget> get(Identifier id) {
                    return id.equals(targetId) ? this.handle : null;
                }
            };
        }

        public void replace(Identifier var1, ResourceHandle<RenderTarget> var2);

        public @Nullable ResourceHandle<RenderTarget> get(Identifier var1);

        default public ResourceHandle<RenderTarget> getOrThrow(Identifier id) {
            ResourceHandle<RenderTarget> handle = this.get(id);
            if (handle == null) {
                throw new IllegalArgumentException("Missing target with id " + String.valueOf(id));
            }
            return handle;
        }
    }
}

