/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.advancements.predicates;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.FluidPredicate;
import net.minecraft.advancements.predicates.LightPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.levelgen.structure.Structure;

public record LocationPredicate(Optional<PositionPredicate> position, Optional<HolderSet<Biome>> biomes, Optional<HolderSet<Structure>> structures, Optional<ResourceKey<Level>> dimension, Optional<Boolean> smokey, Optional<LightPredicate> light, Optional<BlockPredicate> block, Optional<FluidPredicate> fluid, Optional<Boolean> canSeeSky) {
    public static final Codec<LocationPredicate> CODEC = RecordCodecBuilder.create(i -> i.group((App)PositionPredicate.CODEC.optionalFieldOf("position").forGetter(LocationPredicate::position), (App)RegistryCodecs.homogeneousList(Registries.BIOME).optionalFieldOf("biomes").forGetter(LocationPredicate::biomes), (App)RegistryCodecs.homogeneousList(Registries.STRUCTURE).optionalFieldOf("structures").forGetter(LocationPredicate::structures), (App)ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(LocationPredicate::dimension), (App)Codec.BOOL.optionalFieldOf("smokey").forGetter(LocationPredicate::smokey), (App)LightPredicate.CODEC.optionalFieldOf("light").forGetter(LocationPredicate::light), (App)BlockPredicate.CODEC.optionalFieldOf("block").forGetter(LocationPredicate::block), (App)FluidPredicate.CODEC.optionalFieldOf("fluid").forGetter(LocationPredicate::fluid), (App)Codec.BOOL.optionalFieldOf("can_see_sky").forGetter(LocationPredicate::canSeeSky)).apply((Applicative)i, LocationPredicate::new));

    public boolean matches(ServerLevel level, double x, double y, double z) {
        if (this.position.isPresent() && !this.position.get().matches(x, y, z)) {
            return false;
        }
        if (this.dimension.isPresent() && this.dimension.get() != level.dimension()) {
            return false;
        }
        BlockPos pos = BlockPos.containing(x, y, z);
        boolean loaded = level.isLoaded(pos);
        if (!(!this.biomes.isPresent() || loaded && this.biomes.get().contains(level.getBiome(pos)))) {
            return false;
        }
        if (!(!this.structures.isPresent() || loaded && level.structureManager().getStructureWithPieceAt(pos, this.structures.get()).isValid())) {
            return false;
        }
        if (this.smokey.isPresent() && (!loaded || this.smokey.get() != CampfireBlock.isSmokeyPos(level, pos))) {
            return false;
        }
        if (this.light.isPresent() && !this.light.get().matches(level, pos)) {
            return false;
        }
        if (this.block.isPresent() && !this.block.get().matches(level, pos)) {
            return false;
        }
        if (this.fluid.isPresent() && !this.fluid.get().matches(level, pos)) {
            return false;
        }
        return !this.canSeeSky.isPresent() || this.canSeeSky.get().booleanValue() == level.canSeeSky(pos);
    }

    private record PositionPredicate(MinMaxBounds.Doubles x, MinMaxBounds.Doubles y, MinMaxBounds.Doubles z) {
        public static final Codec<PositionPredicate> CODEC = RecordCodecBuilder.create(i -> i.group((App)MinMaxBounds.Doubles.CODEC.optionalFieldOf("x", (Object)MinMaxBounds.Doubles.ANY).forGetter(PositionPredicate::x), (App)MinMaxBounds.Doubles.CODEC.optionalFieldOf("y", (Object)MinMaxBounds.Doubles.ANY).forGetter(PositionPredicate::y), (App)MinMaxBounds.Doubles.CODEC.optionalFieldOf("z", (Object)MinMaxBounds.Doubles.ANY).forGetter(PositionPredicate::z)).apply((Applicative)i, PositionPredicate::new));

        private static Optional<PositionPredicate> of(MinMaxBounds.Doubles x, MinMaxBounds.Doubles y, MinMaxBounds.Doubles z) {
            if (x.isAny() && y.isAny() && z.isAny()) {
                return Optional.empty();
            }
            return Optional.of(new PositionPredicate(x, y, z));
        }

        public boolean matches(double x, double y, double z) {
            return this.x.matches(x) && this.y.matches(y) && this.z.matches(z);
        }
    }

    public static class Builder {
        private MinMaxBounds.Doubles x = MinMaxBounds.Doubles.ANY;
        private MinMaxBounds.Doubles y = MinMaxBounds.Doubles.ANY;
        private MinMaxBounds.Doubles z = MinMaxBounds.Doubles.ANY;
        private Optional<HolderSet<Biome>> biomes = Optional.empty();
        private Optional<HolderSet<Structure>> structures = Optional.empty();
        private Optional<ResourceKey<Level>> dimension = Optional.empty();
        private Optional<Boolean> smokey = Optional.empty();
        private Optional<LightPredicate> light = Optional.empty();
        private Optional<BlockPredicate> block = Optional.empty();
        private Optional<FluidPredicate> fluid = Optional.empty();
        private Optional<Boolean> canSeeSky = Optional.empty();

        public static Builder location() {
            return new Builder();
        }

        public static Builder inBiome(Holder<Biome> biome) {
            return Builder.location().setBiomes(HolderSet.direct(biome));
        }

        public static Builder inDimension(ResourceKey<Level> dimension) {
            return Builder.location().setDimension(dimension);
        }

        public static Builder inStructure(Holder<Structure> structure) {
            return Builder.location().setStructures(HolderSet.direct(structure));
        }

        public static Builder atYLocation(MinMaxBounds.Doubles yLocation) {
            return Builder.location().setY(yLocation);
        }

        public Builder setX(MinMaxBounds.Doubles x) {
            this.x = x;
            return this;
        }

        public Builder setY(MinMaxBounds.Doubles y) {
            this.y = y;
            return this;
        }

        public Builder setZ(MinMaxBounds.Doubles z) {
            this.z = z;
            return this;
        }

        public Builder setBiomes(HolderSet<Biome> biomes) {
            this.biomes = Optional.of(biomes);
            return this;
        }

        public Builder setStructures(HolderSet<Structure> structures) {
            this.structures = Optional.of(structures);
            return this;
        }

        public Builder setDimension(ResourceKey<Level> dimension) {
            this.dimension = Optional.of(dimension);
            return this;
        }

        public Builder setLight(LightPredicate.Builder light) {
            this.light = Optional.of(light.build());
            return this;
        }

        public Builder setBlock(BlockPredicate.Builder block) {
            this.block = Optional.of(block.build());
            return this;
        }

        public Builder setFluid(FluidPredicate.Builder fluid) {
            this.fluid = Optional.of(fluid.build());
            return this;
        }

        public Builder setSmokey(boolean smokey) {
            this.smokey = Optional.of(smokey);
            return this;
        }

        public Builder setCanSeeSky(boolean canSeeSky) {
            this.canSeeSky = Optional.of(canSeeSky);
            return this;
        }

        public LocationPredicate build() {
            Optional<PositionPredicate> position = PositionPredicate.of(this.x, this.y, this.z);
            return new LocationPredicate(position, this.biomes, this.structures, this.dimension, this.smokey, this.light, this.block, this.fluid, this.canSeeSky);
        }
    }
}

