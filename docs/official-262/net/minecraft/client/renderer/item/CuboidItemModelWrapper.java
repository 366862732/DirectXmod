/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Suppliers
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.ints.IntList
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
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class CuboidItemModelWrapper
implements ItemModel {
    private final List<ItemTintSource> tints;
    private final QuadCollection quads;
    private final Supplier<Vector3fc[]> extents;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;

    private CuboidItemModelWrapper(List<ItemTintSource> tints, QuadCollection quads, ModelRenderProperties properties, Matrix4fc transformation) {
        this.tints = tints;
        this.quads = quads;
        this.properties = properties;
        this.transformation = transformation;
        this.extents = Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(quads.getAll()));
    }

    public static Vector3fc[] computeExtents(List<BakedQuad> quads) {
        HashSet<Vector3fc> result = new HashSet<Vector3fc>();
        for (BakedQuad quad : quads) {
            for (int vertex = 0; vertex < 4; ++vertex) {
                result.add(quad.position(vertex));
            }
        }
        return (Vector3fc[])result.toArray(Vector3fc[]::new);
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        output.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        if (item.hasFoil()) {
            ItemStackRenderState.FoilType foilType = CuboidItemModelWrapper.hasSpecialAnimatedTexture(item) ? ItemStackRenderState.FoilType.SPECIAL : ItemStackRenderState.FoilType.STANDARD;
            layer.setFoilType(foilType);
            output.setAnimated();
            output.appendModelIdentityElement((Object)foilType);
        }
        if (!this.tints.isEmpty()) {
            IntList tintLayers = layer.tintLayers();
            for (ItemTintSource tintSource : this.tints) {
                int tint = tintSource.calculate(item, level, owner == null ? null : owner.asLivingEntity());
                tintLayers.add(tint);
                output.appendModelIdentityElement(tint);
            }
        }
        layer.setExtents(this.extents);
        layer.setLocalTransform(this.transformation);
        this.properties.applyToLayer(layer, displayContext);
        layer.prepareQuadList().addAll(this.quads.getAll());
        if (this.quads.hasMaterialFlag(2)) {
            output.setAnimated();
        }
    }

    private static void validateAtlasUsage(List<BakedQuad> quads) {
        Iterator<BakedQuad> quadIterator = quads.iterator();
        if (!quadIterator.hasNext()) {
            return;
        }
        Identifier expectedAtlas = quadIterator.next().materialInfo().sprite().atlasLocation();
        while (quadIterator.hasNext()) {
            BakedQuad quad = quadIterator.next();
            Identifier quadAtlas = quad.materialInfo().sprite().atlasLocation();
            if (quadAtlas.equals(expectedAtlas)) continue;
            throw new IllegalStateException("Multiple atlases used in model, expected " + String.valueOf(expectedAtlas) + ", but also got " + String.valueOf(quadAtlas));
        }
        if (!expectedAtlas.equals(TextureAtlas.LOCATION_ITEMS) && !expectedAtlas.equals(TextureAtlas.LOCATION_BLOCKS)) {
            throw new IllegalArgumentException("Atlas " + String.valueOf(expectedAtlas) + " can't be usef for item models");
        }
    }

    private static boolean hasSpecialAnimatedTexture(ItemStack itemStack) {
        return itemStack.is(ItemTags.COMPASSES) || itemStack.is(Items.CLOCK);
    }

    @Environment(value=EnvType.CLIENT)
    public record Unbaked(Identifier model, Optional<Transformation> transformation, List<ItemTintSource> tints) implements ItemModel.Unbaked
    {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model), (App)Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(Unbaked::transformation), (App)ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(Unbaked::tints)).apply((Applicative)i, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(this.model);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            ResolvedModel resolvedModel = baker.getModel(this.model);
            TextureSlots textureSlots = resolvedModel.getTopTextureSlots();
            QuadCollection quads = resolvedModel.bakeTopGeometry(textureSlots, baker, BlockModelRotation.IDENTITY);
            ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolvedModel, textureSlots);
            CuboidItemModelWrapper.validateAtlasUsage(quads.getAll());
            Matrix4fc modelTransform = Transformation.compose(transformation, this.transformation);
            return new CuboidItemModelWrapper(this.tints, quads, properties, modelTransform);
        }

        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}

