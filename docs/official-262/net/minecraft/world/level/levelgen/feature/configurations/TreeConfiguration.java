/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.feature.configurations;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;

public class TreeConfiguration
implements FeatureConfiguration {
    public static final BlockPredicate CAN_PLACE_BELOW_TREE_TRUNKS = BlockPredicate.not(BlockPredicate.matchesTag(BlockTags.CANNOT_REPLACE_BELOW_TREE_TRUNK));
    public static final Codec<TreeConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)BlockStateProvider.CODEC.fieldOf("trunk_provider").forGetter(c -> c.trunkProvider), (App)TrunkPlacer.CODEC.fieldOf("trunk_placer").forGetter(c -> c.trunkPlacer), (App)BlockStateProvider.CODEC.fieldOf("foliage_provider").forGetter(c -> c.foliageProvider), (App)FoliagePlacer.CODEC.fieldOf("foliage_placer").forGetter(c -> c.foliagePlacer), (App)RootPlacer.CODEC.optionalFieldOf("root_placer").forGetter(c -> c.rootPlacer), (App)FeatureSize.CODEC.fieldOf("minimum_size").forGetter(c -> c.minimumSize), (App)TreeDecorator.CODEC.listOf().fieldOf("decorators").forGetter(c -> c.decorators), (App)Codec.BOOL.fieldOf("ignore_vines").orElse((Object)false).forGetter(c -> c.ignoreVines), (App)BlockStateProvider.CODEC.fieldOf("below_trunk_provider").forGetter(c -> c.belowTrunkProvider)).apply((Applicative)i, TreeConfiguration::new));
    public final BlockStateProvider trunkProvider;
    public final TrunkPlacer trunkPlacer;
    public final BlockStateProvider foliageProvider;
    public final FoliagePlacer foliagePlacer;
    public final Optional<RootPlacer> rootPlacer;
    public final FeatureSize minimumSize;
    public final List<TreeDecorator> decorators;
    public final boolean ignoreVines;
    public final BlockStateProvider belowTrunkProvider;

    protected TreeConfiguration(BlockStateProvider trunkProvider, TrunkPlacer trunkPlacer, BlockStateProvider foliageProvider, FoliagePlacer foliagePlacer, Optional<RootPlacer> rootPlacer, FeatureSize minimumSize, List<TreeDecorator> decorators, boolean ignoreVines, BlockStateProvider belowTrunkProvider) {
        this.trunkProvider = trunkProvider;
        this.trunkPlacer = trunkPlacer;
        this.foliageProvider = foliageProvider;
        this.foliagePlacer = foliagePlacer;
        this.rootPlacer = rootPlacer;
        this.minimumSize = minimumSize;
        this.decorators = decorators;
        this.ignoreVines = ignoreVines;
        this.belowTrunkProvider = belowTrunkProvider;
    }

    public static BlockStateProvider defaultPlaceBelowTreeTrunkProvider(HolderGetter<Biome> biomes) {
        return RuleBasedStateProvider.ifTrueThenProvide(CAN_PLACE_BELOW_TREE_TRUNKS, Blocks.DIRT);
    }

    public static class TreeConfigurationBuilder {
        public final BlockStateProvider trunkProvider;
        private final TrunkPlacer trunkPlacer;
        public final BlockStateProvider foliageProvider;
        private final FoliagePlacer foliagePlacer;
        private final Optional<RootPlacer> rootPlacer;
        private final FeatureSize minimumSize;
        private List<TreeDecorator> decorators = ImmutableList.of();
        private boolean ignoreVines;
        private BlockStateProvider belowTrunkProvider;

        public TreeConfigurationBuilder(BlockStateProvider trunkProvider, TrunkPlacer trunkPlacer, BlockStateProvider foliageProvider, FoliagePlacer foliagePlacer, Optional<RootPlacer> rootPlacer, FeatureSize minimumSize, BlockStateProvider belowTrunkProvider) {
            this.trunkProvider = trunkProvider;
            this.trunkPlacer = trunkPlacer;
            this.foliageProvider = foliageProvider;
            this.foliagePlacer = foliagePlacer;
            this.rootPlacer = rootPlacer;
            this.minimumSize = minimumSize;
            this.belowTrunkProvider = belowTrunkProvider;
        }

        public TreeConfigurationBuilder(BlockStateProvider trunkProvider, TrunkPlacer trunkPlacer, BlockStateProvider foliageProvider, FoliagePlacer foliagePlacer, FeatureSize minimumSize, BlockStateProvider belowTrunkProvider) {
            this(trunkProvider, trunkPlacer, foliageProvider, foliagePlacer, Optional.empty(), minimumSize, belowTrunkProvider);
        }

        public TreeConfigurationBuilder belowTrunkProvider(BlockStateProvider belowTrunkProvider) {
            this.belowTrunkProvider = belowTrunkProvider;
            return this;
        }

        public TreeConfigurationBuilder decorators(List<TreeDecorator> decorators) {
            this.decorators = decorators;
            return this;
        }

        public TreeConfigurationBuilder ignoreVines() {
            this.ignoreVines = true;
            return this;
        }

        public TreeConfiguration build() {
            return new TreeConfiguration(this.trunkProvider, this.trunkPlacer, this.foliageProvider, this.foliagePlacer, this.rootPlacer, this.minimumSize, this.decorators, this.ignoreVines, this.belowTrunkProvider);
        }
    }
}

