/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.biome;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;

public record BiomeSpecialEffects(int waterColor, Optional<Integer> foliageColorOverride, Optional<Integer> dryFoliageColorOverride, Optional<Integer> grassColorOverride, GrassColorModifier grassColorModifier) {
    public static final Codec<BiomeSpecialEffects> CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.STRING_RGB_COLOR.fieldOf("water_color").forGetter(BiomeSpecialEffects::waterColor), (App)ExtraCodecs.STRING_RGB_COLOR.optionalFieldOf("foliage_color").forGetter(BiomeSpecialEffects::foliageColorOverride), (App)ExtraCodecs.STRING_RGB_COLOR.optionalFieldOf("dry_foliage_color").forGetter(BiomeSpecialEffects::dryFoliageColorOverride), (App)ExtraCodecs.STRING_RGB_COLOR.optionalFieldOf("grass_color").forGetter(BiomeSpecialEffects::grassColorOverride), (App)GrassColorModifier.CODEC.optionalFieldOf("grass_color_modifier", (Object)GrassColorModifier.NONE).forGetter(BiomeSpecialEffects::grassColorModifier)).apply((Applicative)i, BiomeSpecialEffects::new));

    public static enum GrassColorModifier implements StringRepresentable
    {
        NONE("none"){

            @Override
            public int modifyColor(double x, double z, int baseColor) {
                return baseColor;
            }
        }
        ,
        DARK_FOREST("dark_forest"){

            @Override
            public int modifyColor(double x, double z, int baseColor) {
                return ARGB.opaque((baseColor & 0xFEFEFE) + 2634762 >> 1);
            }
        }
        ,
        SWAMP("swamp"){

            @Override
            public int modifyColor(double x, double z, int baseColor) {
                double groundValue = Biome.BIOME_INFO_NOISE.getValue(x * 0.0225, z * 0.0225, false);
                if (groundValue < -0.1) {
                    return -11766212;
                }
                return -9801671;
            }
        };

        private final String name;
        public static final Codec<GrassColorModifier> CODEC;

        public abstract int modifyColor(double var1, double var3, int var5);

        private GrassColorModifier(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(GrassColorModifier::values);
        }
    }

    public static class Builder {
        private OptionalInt waterColor = OptionalInt.empty();
        private Optional<Integer> foliageColorOverride = Optional.empty();
        private Optional<Integer> dryFoliageColorOverride = Optional.empty();
        private Optional<Integer> grassColorOverride = Optional.empty();
        private GrassColorModifier grassColorModifier = GrassColorModifier.NONE;

        public Builder waterColor(int waterColor) {
            this.waterColor = OptionalInt.of(waterColor);
            return this;
        }

        public Builder foliageColorOverride(int foliageColor) {
            this.foliageColorOverride = Optional.of(foliageColor);
            return this;
        }

        public Builder dryFoliageColorOverride(int dryFoliageColor) {
            this.dryFoliageColorOverride = Optional.of(dryFoliageColor);
            return this;
        }

        public Builder grassColorOverride(int grassColor) {
            this.grassColorOverride = Optional.of(grassColor);
            return this;
        }

        public Builder grassColorModifier(GrassColorModifier grassModifier) {
            this.grassColorModifier = grassModifier;
            return this;
        }

        public BiomeSpecialEffects build() {
            return new BiomeSpecialEffects(this.waterColor.orElseThrow(() -> new IllegalStateException("Missing 'water' color.")), this.foliageColorOverride, this.dryFoliageColorOverride, this.grassColorOverride, this.grassColorModifier);
        }
    }
}

