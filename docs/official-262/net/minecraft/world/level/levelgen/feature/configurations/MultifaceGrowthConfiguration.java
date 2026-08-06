/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 */
package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MultifaceSpreadeableBlock;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class MultifaceGrowthConfiguration
implements FeatureConfiguration {
    public static final Codec<MultifaceGrowthConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)BuiltInRegistries.BLOCK.byNameCodec().validate(MultifaceGrowthConfiguration::validateBlock).fieldOf("block").forGetter(c -> c.placeBlock), (App)Codec.intRange((int)1, (int)64).optionalFieldOf("search_range", (Object)10).forGetter(c -> c.searchRange), (App)Codec.BOOL.optionalFieldOf("can_place_on_floor", (Object)false).forGetter(c -> c.canPlaceOnFloor), (App)Codec.BOOL.optionalFieldOf("can_place_on_ceiling", (Object)false).forGetter(c -> c.canPlaceOnCeiling), (App)Codec.BOOL.optionalFieldOf("can_place_on_wall", (Object)false).forGetter(c -> c.canPlaceOnWall), (App)Codec.floatRange((float)0.0f, (float)1.0f).optionalFieldOf("chance_of_spreading", (Object)Float.valueOf(0.5f)).forGetter(c -> Float.valueOf(c.chanceOfSpreading)), (App)RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("can_be_placed_on").forGetter(c -> c.canBePlacedOn)).apply((Applicative)i, MultifaceGrowthConfiguration::new));
    public final Block placeBlock;
    public final int searchRange;
    public final boolean canPlaceOnFloor;
    public final boolean canPlaceOnCeiling;
    public final boolean canPlaceOnWall;
    public final float chanceOfSpreading;
    public final HolderSet<Block> canBePlacedOn;
    private final ObjectArrayList<Direction> validDirections;

    private static DataResult<Block> validateBlock(Block block) {
        DataResult dataResult;
        if (block instanceof MultifaceSpreadeableBlock) {
            MultifaceSpreadeableBlock multifaceBlock = (MultifaceSpreadeableBlock)block;
            dataResult = DataResult.success((Object)multifaceBlock);
        } else {
            dataResult = DataResult.error(() -> "Growth block should be a multiface spreadeable block");
        }
        return dataResult;
    }

    public MultifaceGrowthConfiguration(Block placeBlock, int searchRange, boolean canPlaceOnFloor, boolean canPlaceOnCeiling, boolean canPlaceOnWall, float chanceOfSpreading, HolderSet<Block> canBePlacedOn) {
        this.placeBlock = placeBlock;
        this.searchRange = searchRange;
        this.canPlaceOnFloor = canPlaceOnFloor;
        this.canPlaceOnCeiling = canPlaceOnCeiling;
        this.canPlaceOnWall = canPlaceOnWall;
        this.chanceOfSpreading = chanceOfSpreading;
        this.canBePlacedOn = canBePlacedOn;
        this.validDirections = new ObjectArrayList(6);
        if (canPlaceOnCeiling) {
            this.validDirections.add((Object)Direction.UP);
        }
        if (canPlaceOnFloor) {
            this.validDirections.add((Object)Direction.DOWN);
        }
        if (canPlaceOnWall) {
            Direction.Plane.HORIZONTAL.forEach(arg_0 -> this.validDirections.add(arg_0));
        }
    }

    public List<Direction> getShuffledDirectionsExcept(RandomSource random, Direction excludeDirection) {
        return Util.toShuffledList(this.validDirections.stream().filter(direction -> direction != excludeDirection), random);
    }

    public List<Direction> getShuffledDirections(RandomSource random) {
        return Util.shuffledCopy(this.validDirections, random);
    }
}

