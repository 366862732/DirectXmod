/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.BitSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class BelowZeroRetrogen {
    private static final BitSet EMPTY = new BitSet(0);
    private static final Codec<BitSet> BITSET_CODEC = Codec.LONG_STREAM.xmap(longStream -> BitSet.valueOf(longStream.toArray()), bitSet -> LongStream.of(bitSet.toLongArray()));
    private static final Codec<ChunkStatus> NON_EMPTY_CHUNK_STATUS = BuiltInRegistries.CHUNK_STATUS.byNameCodec().comapFlatMap(status -> status == ChunkStatus.EMPTY ? DataResult.error(() -> "target_status cannot be empty") : DataResult.success((Object)status), Function.identity());
    public static final Codec<BelowZeroRetrogen> CODEC = RecordCodecBuilder.create(i -> i.group((App)NON_EMPTY_CHUNK_STATUS.fieldOf("target_status").forGetter(BelowZeroRetrogen::targetStatus), (App)BITSET_CODEC.lenientOptionalFieldOf("missing_bedrock").forGetter(b -> b.missingBedrock.isEmpty() ? Optional.empty() : Optional.of(b.missingBedrock))).apply((Applicative)i, BelowZeroRetrogen::new));
    private static final Set<ResourceKey<Biome>> RETAINED_RETROGEN_BIOMES = Set.of(Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES, Biomes.DEEP_DARK);
    public static final LevelHeightAccessor UPGRADE_HEIGHT_ACCESSOR = new LevelHeightAccessor(){

        @Override
        public int getHeight() {
            return 64;
        }

        @Override
        public int getMinY() {
            return -64;
        }
    };
    private final ChunkStatus targetStatus;
    private final BitSet missingBedrock;

    private BelowZeroRetrogen(ChunkStatus targetStatus, Optional<BitSet> missingBedrock) {
        this.targetStatus = targetStatus;
        this.missingBedrock = missingBedrock.orElse(EMPTY);
    }

    public static void replaceOldBedrock(ProtoChunk chunk) {
        int maxGeneratedBedrockY = 4;
        BlockPos.betweenClosed(0, 0, 0, 15, 4, 15).forEach(pos -> {
            if (chunk.getBlockState((BlockPos)pos).is(Blocks.BEDROCK)) {
                chunk.setBlockState((BlockPos)pos, Blocks.DEEPSLATE.defaultBlockState());
            }
        });
    }

    public void applyBedrockMask(ProtoChunk chunk) {
        LevelHeightAccessor heightAccessor = chunk.getHeightAccessorForGeneration();
        int minY = heightAccessor.getMinY();
        int maxY = heightAccessor.getMaxY();
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                if (!this.hasBedrockHole(x, z)) continue;
                BlockPos.betweenClosed(x, minY, z, x, maxY, z).forEach(pos -> chunk.setBlockState((BlockPos)pos, Blocks.AIR.defaultBlockState()));
            }
        }
    }

    public ChunkStatus targetStatus() {
        return this.targetStatus;
    }

    public boolean hasBedrockHoles() {
        return !this.missingBedrock.isEmpty();
    }

    public boolean hasBedrockHole(int x, int z) {
        return this.missingBedrock.get((z & 0xF) * 16 + (x & 0xF));
    }

    public static BiomeResolver getBiomeResolver(BiomeResolver biomeResolver, ChunkAccess protoChunk) {
        if (!protoChunk.isUpgrading()) {
            return biomeResolver;
        }
        Predicate<ResourceKey> retainedBiomes = RETAINED_RETROGEN_BIOMES::contains;
        return (quartX, quartY, quartZ, sampler) -> {
            Holder<Biome> noiseBiome = biomeResolver.getNoiseBiome(quartX, quartY, quartZ, sampler);
            if (noiseBiome.is(retainedBiomes)) {
                return noiseBiome;
            }
            return protoChunk.getNoiseBiome(quartX, 0, quartZ);
        };
    }
}

