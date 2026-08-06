/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.objects.Object2ObjectFunction
 *  it.unimi.dsi.fastutil.objects.Object2ObjectMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.resources.model;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.cuboid.MissingCuboidModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class ModelDiscovery {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Object2ObjectMap<Identifier, ModelWrapper> modelWrappers = new Object2ObjectOpenHashMap();
    private final ModelWrapper missingModel;
    private final Object2ObjectFunction<Identifier, ModelWrapper> uncachedResolver;
    private final ResolvableModel.Resolver resolver;
    private final Queue<ModelWrapper> parentDiscoveryQueue = new ArrayDeque<ModelWrapper>();

    public ModelDiscovery(Map<Identifier, UnbakedModel> unbakedModels, UnbakedModel missingUnbakedModel) {
        this.missingModel = new ModelWrapper(MissingCuboidModel.LOCATION, missingUnbakedModel, true);
        this.modelWrappers.put((Object)MissingCuboidModel.LOCATION, (Object)this.missingModel);
        this.uncachedResolver = rawId -> {
            Identifier id = (Identifier)rawId;
            UnbakedModel rawModel = (UnbakedModel)unbakedModels.get(id);
            if (rawModel == null) {
                LOGGER.warn("Missing block model: {}", (Object)id);
                return this.missingModel;
            }
            return this.createAndQueueWrapper(id, rawModel);
        };
        this.resolver = this::getOrCreateModel;
    }

    private static boolean isRoot(UnbakedModel model) {
        return model.parent() == null;
    }

    private ModelWrapper getOrCreateModel(Identifier id) {
        return (ModelWrapper)this.modelWrappers.computeIfAbsent((Object)id, this.uncachedResolver);
    }

    private ModelWrapper createAndQueueWrapper(Identifier id, UnbakedModel rawModel) {
        boolean isRoot = ModelDiscovery.isRoot(rawModel);
        ModelWrapper result = new ModelWrapper(id, rawModel, isRoot);
        if (!isRoot) {
            this.parentDiscoveryQueue.add(result);
        }
        return result;
    }

    public void addRoot(ResolvableModel model) {
        model.resolveDependencies(this.resolver);
    }

    public void addSpecialModel(Identifier id, UnbakedModel model) {
        if (!ModelDiscovery.isRoot(model)) {
            LOGGER.warn("Trying to add non-root special model {}, ignoring", (Object)id);
            return;
        }
        ModelWrapper previous = (ModelWrapper)this.modelWrappers.put((Object)id, (Object)this.createAndQueueWrapper(id, model));
        if (previous != null) {
            LOGGER.warn("Duplicate special model {}", (Object)id);
        }
    }

    public ResolvedModel missingModel() {
        return this.missingModel;
    }

    public Map<Identifier, ResolvedModel> resolve() {
        ArrayList<ModelWrapper> toValidate = new ArrayList<ModelWrapper>();
        this.discoverDependencies(toValidate);
        ModelDiscovery.propagateValidity(toValidate);
        ImmutableMap.Builder result = ImmutableMap.builder();
        this.modelWrappers.forEach((location, model) -> {
            if (model.valid) {
                result.put(location, model);
            } else {
                LOGGER.warn("Model {} ignored due to cyclic dependency", location);
            }
        });
        return result.build();
    }

    private void discoverDependencies(List<ModelWrapper> toValidate) {
        ModelWrapper current;
        while ((current = this.parentDiscoveryQueue.poll()) != null) {
            ModelWrapper parent;
            Identifier parentLocation = Objects.requireNonNull(current.wrapped.parent());
            current.parent = parent = this.getOrCreateModel(parentLocation);
            if (parent.valid) {
                current.valid = true;
                continue;
            }
            toValidate.add(current);
        }
    }

    private static void propagateValidity(List<ModelWrapper> toValidate) {
        boolean progressed = true;
        while (progressed) {
            progressed = false;
            Iterator<ModelWrapper> iterator = toValidate.iterator();
            while (iterator.hasNext()) {
                ModelWrapper model = iterator.next();
                if (!Objects.requireNonNull(model.parent).valid) continue;
                model.valid = true;
                iterator.remove();
                progressed = true;
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class ModelWrapper
    implements ResolvedModel {
        private static final Slot<Boolean> KEY_AMBIENT_OCCLUSION = ModelWrapper.slot(0);
        private static final Slot<UnbakedModel.GuiLight> KEY_GUI_LIGHT = ModelWrapper.slot(1);
        private static final Slot<UnbakedGeometry> KEY_GEOMETRY = ModelWrapper.slot(2);
        private static final Slot<ItemTransforms> KEY_TRANSFORMS = ModelWrapper.slot(3);
        private static final Slot<TextureSlots> KEY_TEXTURE_SLOTS = ModelWrapper.slot(4);
        private static final Slot<Material.Baked> KEY_PARTICLE_SPRITE = ModelWrapper.slot(5);
        private static final Slot<QuadCollection> KEY_DEFAULT_GEOMETRY = ModelWrapper.slot(6);
        private static final int SLOT_COUNT = 7;
        private final Identifier id;
        private boolean valid;
        private @Nullable ModelWrapper parent;
        private final UnbakedModel wrapped;
        private final AtomicReferenceArray<@Nullable Object> fixedSlots = new AtomicReferenceArray(7);
        private final Map<ModelState, QuadCollection> modelBakeCache = new ConcurrentHashMap<ModelState, QuadCollection>();

        private static <T> Slot<T> slot(int index) {
            Objects.checkIndex(index, 7);
            return new Slot(index);
        }

        private ModelWrapper(Identifier id, UnbakedModel wrapped, boolean valid) {
            this.id = id;
            this.wrapped = wrapped;
            this.valid = valid;
        }

        @Override
        public UnbakedModel wrapped() {
            return this.wrapped;
        }

        @Override
        public @Nullable ResolvedModel parent() {
            return this.parent;
        }

        @Override
        public String debugName() {
            return this.id.toString();
        }

        private <T> @Nullable T getSlot(Slot<T> key) {
            return (T)this.fixedSlots.get(key.index);
        }

        private <T> T updateSlot(Slot<T> key, T value) {
            T currentValue = this.fixedSlots.compareAndExchange(key.index, null, value);
            if (currentValue == null) {
                return value;
            }
            return currentValue;
        }

        private <T> T getSimpleProperty(Slot<T> key, Function<ResolvedModel, T> getter) {
            T result = this.getSlot(key);
            if (result != null) {
                return result;
            }
            return this.updateSlot(key, getter.apply(this));
        }

        @Override
        public boolean getTopAmbientOcclusion() {
            return this.getSimpleProperty(KEY_AMBIENT_OCCLUSION, ResolvedModel::findTopAmbientOcclusion);
        }

        @Override
        public UnbakedModel.GuiLight getTopGuiLight() {
            return this.getSimpleProperty(KEY_GUI_LIGHT, ResolvedModel::findTopGuiLight);
        }

        @Override
        public ItemTransforms getTopTransforms() {
            return this.getSimpleProperty(KEY_TRANSFORMS, ResolvedModel::findTopTransforms);
        }

        @Override
        public UnbakedGeometry getTopGeometry() {
            return this.getSimpleProperty(KEY_GEOMETRY, ResolvedModel::findTopGeometry);
        }

        @Override
        public TextureSlots getTopTextureSlots() {
            return this.getSimpleProperty(KEY_TEXTURE_SLOTS, ResolvedModel::findTopTextureSlots);
        }

        @Override
        public Material.Baked resolveParticleMaterial(TextureSlots textureSlots, ModelBaker baker) {
            Material.Baked result = this.getSlot(KEY_PARTICLE_SPRITE);
            if (result != null) {
                return result;
            }
            return this.updateSlot(KEY_PARTICLE_SPRITE, ResolvedModel.resolveParticleMaterial(textureSlots, baker, this));
        }

        private QuadCollection bakeDefaultState(TextureSlots textureSlots, ModelBaker baker, ModelState state) {
            QuadCollection result = this.getSlot(KEY_DEFAULT_GEOMETRY);
            if (result != null) {
                return result;
            }
            return this.updateSlot(KEY_DEFAULT_GEOMETRY, this.getTopGeometry().bake(textureSlots, baker, state, this));
        }

        @Override
        public QuadCollection bakeTopGeometry(TextureSlots textureSlots, ModelBaker baker, ModelState state) {
            if (state == BlockModelRotation.IDENTITY) {
                return this.bakeDefaultState(textureSlots, baker, state);
            }
            return this.modelBakeCache.computeIfAbsent(state, s -> {
                UnbakedGeometry topGeometry = this.getTopGeometry();
                return topGeometry.bake(textureSlots, baker, (ModelState)s, this);
            });
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record Slot<T>(int index) {
    }
}

