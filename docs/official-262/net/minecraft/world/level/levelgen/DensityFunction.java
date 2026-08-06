/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import java.lang.runtime.SwitchBootstraps;
import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jspecify.annotations.Nullable;

public interface DensityFunction {
    public static final Codec<DensityFunction> CODEC = RegistryFileCodec.create(Registries.DENSITY_FUNCTION, DensityFunctions.DIRECT_CODEC).xmap(holder -> {
        Holder holder2 = holder;
        Objects.requireNonNull(holder2);
        Holder selector0$temp = holder2;
        int index$1 = 0;
        return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{Holder.Direct.class, Holder.Reference.class}, (Holder)selector0$temp, index$1)) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                Holder.Direct direct = (Holder.Direct)selector0$temp;
                yield (DensityFunction)direct.value();
            }
            case 1 -> {
                Holder.Reference reference = (Holder.Reference)selector0$temp;
                yield new DensityFunctions.HolderHolder(reference);
            }
        };
    }, value -> {
        Holder<DensityFunction> holder;
        DensityFunction densityFunction = value;
        Objects.requireNonNull(densityFunction);
        DensityFunction selector1$temp = densityFunction;
        int index$2 = 0;
        switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{DensityFunctions.HolderHolder.class}, (DensityFunction)selector1$temp, index$2)) {
            case 0: {
                DensityFunctions.HolderHolder $b$0 = (DensityFunctions.HolderHolder)selector1$temp;
                try {
                    Holder<DensityFunction> patt3$temp;
                    Holder<DensityFunction> function = patt3$temp = $b$0.function();
                    holder = function;
                    return holder;
                }
                catch (Throwable throwable) {
                    throw new MatchException(throwable.toString(), throwable);
                }
            }
        }
        holder = Holder.direct(value);
        return holder;
    });

    public double compute(FunctionContext var1);

    public void fillArray(double[] var1, ContextProvider var2);

    public DensityFunction mapChildren(Visitor var1);

    default public DensityFunction mapAll(final Visitor visitor) {
        class RecursiveVisitor
        implements Visitor {
            RecursiveVisitor() {
                Objects.requireNonNull(this$0);
            }

            @Override
            public DensityFunction apply(DensityFunction input) {
                return visitor.apply(input.mapChildren(this));
            }

            @Override
            public NoiseHolder visitNoise(NoiseHolder noise) {
                return visitor.visitNoise(noise);
            }
        }
        return new RecursiveVisitor().apply(this);
    }

    public double minValue();

    public double maxValue();

    public KeyDispatchDataCodec<? extends DensityFunction> codec();

    default public DensityFunction clamp(double min, double max) {
        return new DensityFunctions.Clamp(this, min, max);
    }

    default public DensityFunction abs() {
        return DensityFunctions.map(this, DensityFunctions.Mapped.Type.ABS);
    }

    default public DensityFunction square() {
        return DensityFunctions.map(this, DensityFunctions.Mapped.Type.SQUARE);
    }

    default public DensityFunction cube() {
        return DensityFunctions.map(this, DensityFunctions.Mapped.Type.CUBE);
    }

    default public DensityFunction halfNegative() {
        return DensityFunctions.map(this, DensityFunctions.Mapped.Type.HALF_NEGATIVE);
    }

    default public DensityFunction quarterNegative() {
        return DensityFunctions.map(this, DensityFunctions.Mapped.Type.QUARTER_NEGATIVE);
    }

    default public DensityFunction invert() {
        return DensityFunctions.map(this, DensityFunctions.Mapped.Type.INVERT);
    }

    default public DensityFunction squeeze() {
        return DensityFunctions.map(this, DensityFunctions.Mapped.Type.SQUEEZE);
    }

    public static interface Visitor {
        public DensityFunction apply(DensityFunction var1);

        default public NoiseHolder visitNoise(NoiseHolder noise) {
            return noise;
        }
    }

    public record SinglePointContext(int blockX, int blockY, int blockZ) implements FunctionContext
    {
    }

    public static interface FunctionContext {
        public int blockX();

        public int blockY();

        public int blockZ();
    }

    public static interface SimpleFunction
    extends DensityFunction {
        @Override
        default public void fillArray(double[] output, ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(output, this);
        }

        @Override
        default public DensityFunction mapChildren(Visitor visitor) {
            return this;
        }
    }

    public record NoiseHolder(Holder<NormalNoise.NoiseParameters> noiseData, @Nullable NormalNoise noise) {
        public static final Codec<NoiseHolder> CODEC = NormalNoise.NoiseParameters.CODEC.xmap(data -> new NoiseHolder((Holder<NormalNoise.NoiseParameters>)data, null), NoiseHolder::noiseData);

        public NoiseHolder(Holder<NormalNoise.NoiseParameters> noiseData) {
            this(noiseData, null);
        }

        public double getValue(double x, double y, double z) {
            return this.noise == null ? 0.0 : this.noise.getValue(x, y, z);
        }

        public double maxValue() {
            return this.noise == null ? 2.0 : this.noise.maxValue();
        }
    }

    public static interface ContextProvider {
        public FunctionContext forIndex(int var1);

        public void fillAllDirectly(double[] var1, DensityFunction var2);
    }
}

