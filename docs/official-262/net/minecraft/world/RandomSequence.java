/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

public class RandomSequence {
    public static final Codec<RandomSequence> CODEC = RecordCodecBuilder.create(i -> i.group((App)XoroshiroRandomSource.CODEC.fieldOf("source").forGetter(r -> r.source)).apply((Applicative)i, RandomSequence::new));
    private final XoroshiroRandomSource source;

    public RandomSequence(XoroshiroRandomSource source) {
        this.source = source;
    }

    public RandomSequence(long seed, Identifier key) {
        this(RandomSequence.createSequence(seed, Optional.of(key)));
    }

    public RandomSequence(long seed, Optional<Identifier> key) {
        this(RandomSequence.createSequence(seed, key));
    }

    private static XoroshiroRandomSource createSequence(long seed, Optional<Identifier> key) {
        RandomSupport.Seed128bit seed128bit = RandomSupport.upgradeSeedTo128bitUnmixed(seed);
        if (key.isPresent()) {
            seed128bit = seed128bit.xor(RandomSequence.seedForKey(key.get()));
        }
        return new XoroshiroRandomSource(seed128bit.mixed());
    }

    public static RandomSupport.Seed128bit seedForKey(Identifier key) {
        return RandomSupport.seedFromHashOf(key.toString());
    }

    public RandomSource random() {
        return this.source;
    }
}

