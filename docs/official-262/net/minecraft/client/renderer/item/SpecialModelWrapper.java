/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Suppliers
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4fc
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.item;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SpecialModelWrapper<T>
implements ItemModel {
    private final SpecialModelRenderer<T> specialRenderer;
    private final ModelRenderProperties properties;
    private final Supplier<Vector3fc[]> extents;
    private final Matrix4fc transformation;

    public SpecialModelWrapper(SpecialModelRenderer<T> specialRenderer, ModelRenderProperties properties, Matrix4fc transformation) {
        this.specialRenderer = specialRenderer;
        this.properties = properties;
        this.extents = Suppliers.memoize(() -> {
            HashSet results = new HashSet();
            specialRenderer.getExtents(results::add);
            return results.toArray(new Vector3fc[0]);
        });
        this.transformation = transformation;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        output.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        if (item.hasFoil()) {
            ItemStackRenderState.FoilType foilType = ItemStackRenderState.FoilType.STANDARD;
            layer.setFoilType(foilType);
            output.setAnimated();
            output.appendModelIdentityElement((Object)foilType);
        }
        T argument = this.specialRenderer.extractArgument(item);
        layer.setExtents(this.extents);
        layer.setLocalTransform(this.transformation);
        layer.setupSpecialModel(this.specialRenderer, argument);
        if (argument != null) {
            output.appendModelIdentityElement(argument);
        }
        this.properties.applyToLayer(layer, displayContext);
    }

    @Environment(value=EnvType.CLIENT)
    public record Unbaked(Identifier base, Optional<Transformation> transformation, SpecialModelRenderer.Unbaked<?> specialModel) implements ItemModel.Unbaked
    {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.fieldOf("base").forGetter(Unbaked::base), (App)Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(Unbaked::transformation), (App)SpecialModelRenderers.CODEC.fieldOf("model").forGetter(Unbaked::specialModel)).apply((Applicative)i, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(this.base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            Matrix4fc modelTransform = Transformation.compose(transformation, this.transformation);
            SpecialModelRenderer<?> bakedSpecialModel = this.specialModel.bake(context);
            if (bakedSpecialModel == null) {
                return context.missingItemModel(modelTransform);
            }
            ModelRenderProperties properties = this.getProperties(context);
            return new SpecialModelWrapper(bakedSpecialModel, properties, modelTransform);
        }

        private ModelRenderProperties getProperties(ItemModel.BakingContext context) {
            ModelBaker baker = context.blockModelBaker();
            ResolvedModel model = baker.getModel(this.base);
            TextureSlots textureSlots = model.getTopTextureSlots();
            return ModelRenderProperties.fromResolvedModel(baker, model, textureSlots);
        }

        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}

