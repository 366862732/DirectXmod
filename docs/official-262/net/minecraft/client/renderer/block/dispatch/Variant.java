/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.block.dispatch;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.math.Quadrant;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public record Variant(Identifier modelLocation, SimpleModelState modelState) implements BlockStateModelPart.Unbaked
{
    public static final MapCodec<Variant> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.fieldOf("model").forGetter(Variant::modelLocation), (App)SimpleModelState.MAP_CODEC.forGetter(Variant::modelState)).apply((Applicative)i, Variant::new));
    public static final Codec<Variant> CODEC = MAP_CODEC.codec();

    public Variant(Identifier modelLocation) {
        this(modelLocation, SimpleModelState.DEFAULT);
    }

    public Variant withXRot(Quadrant x) {
        return this.withState(this.modelState.withX(x));
    }

    public Variant withYRot(Quadrant y) {
        return this.withState(this.modelState.withY(y));
    }

    public Variant withZRot(Quadrant z) {
        return this.withState(this.modelState.withZ(z));
    }

    public Variant withUvLock(boolean uvLock) {
        return this.withState(this.modelState.withUvLock(uvLock));
    }

    public Variant withModel(Identifier modelLocation) {
        return new Variant(modelLocation, this.modelState);
    }

    public Variant withState(SimpleModelState modelState) {
        return new Variant(this.modelLocation, modelState);
    }

    public Variant with(VariantMutator mutator) {
        return (Variant)mutator.apply(this);
    }

    @Override
    public BlockStateModelPart bake(ModelBaker modelBakery) {
        return SimpleModelWrapper.bake(modelBakery, this.modelLocation, this.modelState.asModelState());
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
        resolver.markDependency(this.modelLocation);
    }

    @Environment(value=EnvType.CLIENT)
    public record SimpleModelState(Quadrant x, Quadrant y, Quadrant z, boolean uvLock) {
        public static final MapCodec<SimpleModelState> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Quadrant.CODEC.optionalFieldOf("x", (Object)Quadrant.R0).forGetter(SimpleModelState::x), (App)Quadrant.CODEC.optionalFieldOf("y", (Object)Quadrant.R0).forGetter(SimpleModelState::y), (App)Quadrant.CODEC.optionalFieldOf("z", (Object)Quadrant.R0).forGetter(SimpleModelState::z), (App)Codec.BOOL.optionalFieldOf("uvlock", (Object)false).forGetter(SimpleModelState::uvLock)).apply((Applicative)i, SimpleModelState::new));
        public static final SimpleModelState DEFAULT = new SimpleModelState(Quadrant.R0, Quadrant.R0, Quadrant.R0, false);

        public ModelState asModelState() {
            BlockModelRotation rotation = BlockModelRotation.get(Quadrant.fromXYZAngles(this.x, this.y, this.z));
            return this.uvLock ? rotation.withUvLock() : rotation;
        }

        public SimpleModelState withX(Quadrant x) {
            return new SimpleModelState(x, this.y, this.z, this.uvLock);
        }

        public SimpleModelState withY(Quadrant y) {
            return new SimpleModelState(this.x, y, this.z, this.uvLock);
        }

        public SimpleModelState withZ(Quadrant z) {
            return new SimpleModelState(this.x, this.y, z, this.uvLock);
        }

        public SimpleModelState withUvLock(boolean uvLock) {
            return new SimpleModelState(this.x, this.y, this.z, uvLock);
        }
    }
}

