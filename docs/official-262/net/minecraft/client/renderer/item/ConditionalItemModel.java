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
 *  org.joml.Matrix4fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.item;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.CacheSlot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.ItemModelPropertyTest;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ConditionalItemModel
implements ItemModel {
    private final ItemModelPropertyTest property;
    private final ItemModel onTrue;
    private final ItemModel onFalse;

    public ConditionalItemModel(ItemModelPropertyTest property, ItemModel onTrue, ItemModel onFalse) {
        this.property = property;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        output.appendModelIdentityElement(this);
        (this.property.get(item, level, owner == null ? null : owner.asLivingEntity(), seed, displayContext) ? this.onTrue : this.onFalse).update(output, item, resolver, displayContext, level, owner, seed);
    }

    @Environment(value=EnvType.CLIENT)
    public record Unbaked(Optional<Transformation> transformation, ConditionalItemModelProperty property, ItemModel.Unbaked onTrue, ItemModel.Unbaked onFalse) implements ItemModel.Unbaked
    {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(Unbaked::transformation), (App)ConditionalItemModelProperties.MAP_CODEC.forGetter(Unbaked::property), (App)ItemModels.CODEC.fieldOf("on_true").forGetter(Unbaked::onTrue), (App)ItemModels.CODEC.fieldOf("on_false").forGetter(Unbaked::onFalse)).apply((Applicative)i, Unbaked::new));

        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            Matrix4fc childTransform = Transformation.compose(transformation, this.transformation);
            return new ConditionalItemModel(this.adaptProperty(this.property, context.contextSwapper()), this.onTrue.bake(context, childTransform), this.onFalse.bake(context, childTransform));
        }

        private ItemModelPropertyTest adaptProperty(ConditionalItemModelProperty originalProperty, @Nullable RegistryContextSwapper contextSwapper) {
            if (contextSwapper == null) {
                return originalProperty;
            }
            CacheSlot<ClientLevel, ItemModelPropertyTest> remappedModelCache = new CacheSlot<ClientLevel, ItemModelPropertyTest>(context -> Unbaked.swapContext(originalProperty, contextSwapper, context));
            return (itemStack, level, owner, seed, displayContext) -> {
                ConditionalItemModelProperty property = level == null ? originalProperty : (ItemModelPropertyTest)remappedModelCache.compute(level);
                return property.get(itemStack, level, owner, seed, displayContext);
            };
        }

        private static <T extends ConditionalItemModelProperty> T swapContext(T originalProperty, RegistryContextSwapper contextSwapper, ClientLevel context) {
            return contextSwapper.swapTo(originalProperty.type().codec(), originalProperty, context.registryAccess()).result().orElse(originalProperty);
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            this.onTrue.resolveDependencies(resolver);
            this.onFalse.resolveDependencies(resolver);
        }
    }
}

