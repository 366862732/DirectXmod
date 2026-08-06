/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.apache.commons.lang3.mutable.MutableBoolean
 */
package net.minecraft.world.level.levelgen.placement;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.FeatureCountTracker;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import org.apache.commons.lang3.mutable.MutableBoolean;

public record PlacedFeature(Holder<ConfiguredFeature<?, ?>> feature, List<PlacementModifier> placement) {
    public static final Codec<PlacedFeature> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group((App)ConfiguredFeature.CODEC.fieldOf("feature").forGetter(c -> c.feature), (App)PlacementModifier.CODEC.listOf().fieldOf("placement").forGetter(c -> c.placement)).apply((Applicative)i, PlacedFeature::new));
    public static final Codec<Holder<PlacedFeature>> CODEC = RegistryFileCodec.create(Registries.PLACED_FEATURE, DIRECT_CODEC);
    public static final Codec<HolderSet<PlacedFeature>> LIST_CODEC = RegistryCodecs.homogeneousList(Registries.PLACED_FEATURE, DIRECT_CODEC);
    public static final Codec<List<HolderSet<PlacedFeature>>> LIST_OF_LISTS_CODEC = RegistryCodecs.homogeneousList(Registries.PLACED_FEATURE, DIRECT_CODEC, true).listOf();

    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        return this.placeWithContext(new PlacementContext(level, generator, Optional.empty()), random, origin);
    }

    public boolean placeWithBiomeCheck(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        return this.placeWithContext(new PlacementContext(level, generator, Optional.of(this)), random, origin);
    }

    private boolean placeWithContext(PlacementContext context, RandomSource random, BlockPos origin) {
        Stream<BlockPos> placements = Stream.of(origin);
        for (PlacementModifier placementModifier : this.placement) {
            placements = placements.flatMap(p -> placementModifier.getPositions(context, random, (BlockPos)p));
        }
        ConfiguredFeature<?, ?> feature = this.feature.value();
        MutableBoolean placedAny = new MutableBoolean();
        placements.forEach(pos -> {
            if (feature.place(context.getLevel(), context.generator(), random, (BlockPos)pos)) {
                placedAny.setTrue();
                if (SharedConstants.DEBUG_FEATURE_COUNT) {
                    FeatureCountTracker.featurePlaced(context.getLevel().getLevel(), feature, context.topFeature());
                }
            }
        });
        return placedAny.isTrue();
    }

    public Stream<Holder<ConfiguredFeature<?, ?>>> getFeatures() {
        return Stream.concat(Stream.of(this.feature), this.feature.value().getSubFeatures());
    }

    @Override
    public String toString() {
        return "Placed " + String.valueOf(this.feature);
    }
}

