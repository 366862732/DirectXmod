/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.hash.HashFunction
 *  com.google.common.hash.Hashing
 *  com.google.common.primitives.Longs
 */
package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Longs;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

public final class RandomSupport {
    public static final long GOLDEN_RATIO_64 = -7046029254386353131L;
    public static final long SILVER_RATIO_64 = 7640891576956012809L;
    private static final HashFunction MD5_128 = Hashing.md5();
    private static final AtomicLong SEED_UNIQUIFIER = new AtomicLong(8682522807148012L);

    @VisibleForTesting
    public static long mixStafford13(long z) {
        z = (z ^ z >>> 30) * -4658895280553007687L;
        z = (z ^ z >>> 27) * -7723592293110705685L;
        return z ^ z >>> 31;
    }

    public static Seed128bit upgradeSeedTo128bitUnmixed(long legacySeed) {
        long lowBits = legacySeed ^ 0x6A09E667F3BCC909L;
        long highBits = lowBits + -7046029254386353131L;
        return new Seed128bit(lowBits, highBits);
    }

    public static Seed128bit upgradeSeedTo128bit(long legacySeed) {
        return RandomSupport.upgradeSeedTo128bitUnmixed(legacySeed).mixed();
    }

    public static Seed128bit seedFromHashOf(String input) {
        byte[] hashCode = MD5_128.hashString((CharSequence)input, StandardCharsets.UTF_8).asBytes();
        long hashLo = Longs.fromBytes((byte)hashCode[0], (byte)hashCode[1], (byte)hashCode[2], (byte)hashCode[3], (byte)hashCode[4], (byte)hashCode[5], (byte)hashCode[6], (byte)hashCode[7]);
        long hashHi = Longs.fromBytes((byte)hashCode[8], (byte)hashCode[9], (byte)hashCode[10], (byte)hashCode[11], (byte)hashCode[12], (byte)hashCode[13], (byte)hashCode[14], (byte)hashCode[15]);
        return new Seed128bit(hashLo, hashHi);
    }

    public static long generateUniqueSeed() {
        return SEED_UNIQUIFIER.updateAndGet(current -> current * 1181783497276652981L) ^ System.nanoTime();
    }

    public record Seed128bit(long seedLo, long seedHi) {
        public Seed128bit xor(long lo, long hi) {
            return new Seed128bit(this.seedLo ^ lo, this.seedHi ^ hi);
        }

        public Seed128bit xor(Seed128bit other) {
            return this.xor(other.seedLo, other.seedHi);
        }

        public Seed128bit mixed() {
            return new Seed128bit(RandomSupport.mixStafford13(this.seedLo), RandomSupport.mixStafford13(this.seedHi));
        }
    }
}

