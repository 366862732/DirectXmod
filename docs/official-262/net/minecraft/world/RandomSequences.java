/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 */
package net.minecraft.world;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class RandomSequences
extends SavedData {
    public static final Codec<RandomSequences> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.INT.fieldOf("salt").forGetter(RandomSequences::salt), (App)Codec.BOOL.optionalFieldOf("include_world_seed", (Object)true).forGetter(RandomSequences::includeWorldSeed), (App)Codec.BOOL.optionalFieldOf("include_sequence_id", (Object)true).forGetter(RandomSequences::includeSequenceId), (App)Codec.unboundedMap(Identifier.CODEC, RandomSequence.CODEC).fieldOf("sequences").forGetter(rs -> rs.sequences)).apply((Applicative)i, RandomSequences::new));
    public static final SavedDataType<RandomSequences> TYPE = new SavedDataType<RandomSequences>(Identifier.withDefaultNamespace("random_sequences"), RandomSequences::new, CODEC, DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);
    private int salt;
    private boolean includeWorldSeed = true;
    private boolean includeSequenceId = true;
    private final Map<Identifier, RandomSequence> sequences = new Object2ObjectOpenHashMap();

    public RandomSequences() {
    }

    private RandomSequences(int salt, boolean includeWorldSeed, boolean includeSequenceId, Map<Identifier, RandomSequence> sequences) {
        this.salt = salt;
        this.includeWorldSeed = includeWorldSeed;
        this.includeSequenceId = includeSequenceId;
        this.sequences.putAll(sequences);
    }

    public RandomSource get(Identifier key, long worldSeed) {
        RandomSource random = this.sequences.computeIfAbsent(key, rl -> this.createSequence((Identifier)rl, worldSeed)).random();
        return new DirtyMarkingRandomSource(this, random);
    }

    private RandomSequence createSequence(Identifier key, long worldSeed) {
        return this.createSequence(key, worldSeed, this.salt, this.includeWorldSeed, this.includeSequenceId);
    }

    private RandomSequence createSequence(Identifier key, long worldSeed, int salt, boolean includeWorldSeed, boolean includeSequenceId) {
        long seed = (includeWorldSeed ? worldSeed : 0L) ^ (long)salt;
        return new RandomSequence(seed, includeSequenceId ? Optional.of(key) : Optional.empty());
    }

    public void forAllSequences(BiConsumer<Identifier, RandomSequence> consumer) {
        this.sequences.forEach(consumer);
    }

    public void setSeedDefaults(int salt, boolean includeWorldSeed, boolean includeSequenceId) {
        this.salt = salt;
        this.includeWorldSeed = includeWorldSeed;
        this.includeSequenceId = includeSequenceId;
    }

    public int clear() {
        int count = this.sequences.size();
        this.sequences.clear();
        return count;
    }

    public void reset(Identifier id, long worldSeed) {
        this.sequences.put(id, this.createSequence(id, worldSeed));
    }

    public void reset(Identifier id, long worldSeed, int salt, boolean includeWorldSeed, boolean includeSequenceId) {
        this.sequences.put(id, this.createSequence(id, worldSeed, salt, includeWorldSeed, includeSequenceId));
    }

    private int salt() {
        return this.salt;
    }

    private boolean includeWorldSeed() {
        return this.includeWorldSeed;
    }

    private boolean includeSequenceId() {
        return this.includeSequenceId;
    }

    private class DirtyMarkingRandomSource
    implements RandomSource {
        private final RandomSource random;
        final /* synthetic */ RandomSequences this$0;

        private DirtyMarkingRandomSource(RandomSequences randomSequences, RandomSource random) {
            RandomSequences randomSequences2 = randomSequences;
            Objects.requireNonNull(randomSequences2);
            this.this$0 = randomSequences2;
            this.random = random;
        }

        @Override
        public RandomSource fork() {
            this.this$0.setDirty();
            return this.random.fork();
        }

        @Override
        public PositionalRandomFactory forkPositional() {
            this.this$0.setDirty();
            return this.random.forkPositional();
        }

        @Override
        public void setSeed(long seed) {
            this.this$0.setDirty();
            this.random.setSeed(seed);
        }

        @Override
        public int nextInt() {
            this.this$0.setDirty();
            return this.random.nextInt();
        }

        @Override
        public int nextInt(int bound) {
            this.this$0.setDirty();
            return this.random.nextInt(bound);
        }

        @Override
        public long nextLong() {
            this.this$0.setDirty();
            return this.random.nextLong();
        }

        @Override
        public boolean nextBoolean() {
            this.this$0.setDirty();
            return this.random.nextBoolean();
        }

        @Override
        public float nextFloat() {
            this.this$0.setDirty();
            return this.random.nextFloat();
        }

        @Override
        public double nextDouble() {
            this.this$0.setDirty();
            return this.random.nextDouble();
        }

        @Override
        public double nextGaussian() {
            this.this$0.setDirty();
            return this.random.nextGaussian();
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof DirtyMarkingRandomSource) {
                DirtyMarkingRandomSource other = (DirtyMarkingRandomSource)obj;
                return this.random.equals(other.random);
            }
            return false;
        }

        public int hashCode() {
            return Objects.hashCode(this.random);
        }
    }
}

