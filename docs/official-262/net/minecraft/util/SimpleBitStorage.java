/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.Validate
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util;

import java.util.function.IntConsumer;
import net.minecraft.util.BitStorage;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

public class SimpleBitStorage
implements BitStorage {
    private static final int[] MAGIC = new int[]{-1, -1, 0, Integer.MIN_VALUE, 0, 0, 0x55555555, 0x55555555, 0, Integer.MIN_VALUE, 0, 1, 0x33333333, 0x33333333, 0, 0x2AAAAAAA, 0x2AAAAAAA, 0, 0x24924924, 0x24924924, 0, Integer.MIN_VALUE, 0, 2, 0x1C71C71C, 0x1C71C71C, 0, 0x19999999, 0x19999999, 0, 390451572, 390451572, 0, 0x15555555, 0x15555555, 0, 0x13B13B13, 0x13B13B13, 0, 306783378, 306783378, 0, 0x11111111, 0x11111111, 0, Integer.MIN_VALUE, 0, 3, 0xF0F0F0F, 0xF0F0F0F, 0, 0xE38E38E, 0xE38E38E, 0, 226050910, 226050910, 0, 0xCCCCCCC, 0xCCCCCCC, 0, 0xC30C30C, 0xC30C30C, 0, 195225786, 195225786, 0, 186737708, 186737708, 0, 0xAAAAAAA, 0xAAAAAAA, 0, 171798691, 171798691, 0, 0x9D89D89, 0x9D89D89, 0, 159072862, 159072862, 0, 0x9249249, 0x9249249, 0, 148102320, 148102320, 0, 0x8888888, 0x8888888, 0, 138547332, 138547332, 0, Integer.MIN_VALUE, 0, 4, 130150524, 130150524, 0, 0x7878787, 0x7878787, 0, 0x7507507, 0x7507507, 0, 0x71C71C7, 0x71C71C7, 0, 116080197, 116080197, 0, 113025455, 113025455, 0, 0x6906906, 0x6906906, 0, 0x6666666, 0x6666666, 0, 104755299, 104755299, 0, 0x6186186, 0x6186186, 0, 99882960, 99882960, 0, 97612893, 97612893, 0, 0x5B05B05, 0x5B05B05, 0, 93368854, 93368854, 0, 91382282, 91382282, 0, 0x5555555, 0x5555555, 0, 87652393, 87652393, 0, 85899345, 85899345, 0, 0x5050505, 0x5050505, 0, 0x4EC4EC4, 0x4EC4EC4, 0, 81037118, 81037118, 0, 79536431, 79536431, 0, 78090314, 78090314, 0, 0x4924924, 0x4924924, 0, 75350303, 75350303, 0, 74051160, 74051160, 0, 72796055, 72796055, 0, 0x4444444, 0x4444444, 0, 70409299, 70409299, 0, 69273666, 69273666, 0, 0x4104104, 0x4104104, 0, Integer.MIN_VALUE, 0, 5};
    private final long[] data;
    private final int bits;
    private final long mask;
    private final int size;
    private final int valuesPerLong;
    private final int divideMul;
    private final int divideAdd;
    private final int divideShift;

    public SimpleBitStorage(int bits, int size, int[] values) {
        this(bits, size);
        int inputOffset;
        int outputIndex = 0;
        for (inputOffset = 0; inputOffset <= size - this.valuesPerLong; inputOffset += this.valuesPerLong) {
            long packedValue = 0L;
            for (int indexInLong = this.valuesPerLong - 1; indexInLong >= 0; --indexInLong) {
                packedValue <<= bits;
                packedValue |= (long)values[inputOffset + indexInLong] & this.mask;
            }
            this.data[outputIndex++] = packedValue;
        }
        int remainderCount = size - inputOffset;
        if (remainderCount > 0) {
            long lastPackedValue = 0L;
            for (int indexInLong = remainderCount - 1; indexInLong >= 0; --indexInLong) {
                lastPackedValue <<= bits;
                lastPackedValue |= (long)values[inputOffset + indexInLong] & this.mask;
            }
            this.data[outputIndex] = lastPackedValue;
        }
    }

    public SimpleBitStorage(int bits, int size) {
        this(bits, size, (long[])null);
    }

    public SimpleBitStorage(int bits, int size, long @Nullable [] data) {
        Validate.inclusiveBetween((long)1L, (long)32L, (long)bits);
        this.size = size;
        this.bits = bits;
        this.mask = (1L << bits) - 1L;
        this.valuesPerLong = (char)(64 / bits);
        int row = 3 * (this.valuesPerLong - 1);
        this.divideMul = MAGIC[row + 0];
        this.divideAdd = MAGIC[row + 1];
        this.divideShift = MAGIC[row + 2];
        int requiredLength = (size + this.valuesPerLong - 1) / this.valuesPerLong;
        if (data != null) {
            if (data.length != requiredLength) {
                throw new InitializationException("Invalid length given for storage, got: " + data.length + " but expected: " + requiredLength);
            }
            this.data = data;
        } else {
            this.data = new long[requiredLength];
        }
    }

    private int cellIndex(int bitIndex) {
        long mul = Integer.toUnsignedLong(this.divideMul);
        long add = Integer.toUnsignedLong(this.divideAdd);
        return (int)((long)bitIndex * mul + add >> 32 >> this.divideShift);
    }

    @Override
    public int getAndSet(int index, int value) {
        Validate.inclusiveBetween((long)0L, (long)(this.size - 1), (long)index);
        Validate.inclusiveBetween((long)0L, (long)this.mask, (long)value);
        int cellIndex = this.cellIndex(index);
        long cellValue = this.data[cellIndex];
        int bitIndex = (index - cellIndex * this.valuesPerLong) * this.bits;
        int oldValue = (int)(cellValue >> bitIndex & this.mask);
        this.data[cellIndex] = cellValue & (this.mask << bitIndex ^ 0xFFFFFFFFFFFFFFFFL) | ((long)value & this.mask) << bitIndex;
        return oldValue;
    }

    @Override
    public void set(int index, int value) {
        Validate.inclusiveBetween((long)0L, (long)(this.size - 1), (long)index);
        Validate.inclusiveBetween((long)0L, (long)this.mask, (long)value);
        int cellIndex = this.cellIndex(index);
        long cellValue = this.data[cellIndex];
        int bitIndex = (index - cellIndex * this.valuesPerLong) * this.bits;
        this.data[cellIndex] = cellValue & (this.mask << bitIndex ^ 0xFFFFFFFFFFFFFFFFL) | ((long)value & this.mask) << bitIndex;
    }

    @Override
    public int get(int index) {
        Validate.inclusiveBetween((long)0L, (long)(this.size - 1), (long)index);
        int cellIndex = this.cellIndex(index);
        long cellValue = this.data[cellIndex];
        int bitIndex = (index - cellIndex * this.valuesPerLong) * this.bits;
        return (int)(cellValue >> bitIndex & this.mask);
    }

    @Override
    public long[] getRaw() {
        return this.data;
    }

    @Override
    public int getSize() {
        return this.size;
    }

    @Override
    public int getBits() {
        return this.bits;
    }

    @Override
    public void getAll(IntConsumer output) {
        int count = 0;
        for (long cellValue : this.data) {
            for (int value = 0; value < this.valuesPerLong; ++value) {
                output.accept((int)(cellValue & this.mask));
                cellValue >>= this.bits;
                if (++count < this.size) continue;
                return;
            }
        }
    }

    @Override
    public void unpack(int[] output) {
        int indexInLong;
        long cellValue;
        int dataLength = this.data.length;
        int outputOffset = 0;
        for (int i = 0; i < dataLength - 1; ++i) {
            cellValue = this.data[i];
            for (indexInLong = 0; indexInLong < this.valuesPerLong; ++indexInLong) {
                output[outputOffset + indexInLong] = (int)(cellValue & this.mask);
                cellValue >>= this.bits;
            }
            outputOffset += this.valuesPerLong;
        }
        int remainder = this.size - outputOffset;
        if (remainder > 0) {
            cellValue = this.data[dataLength - 1];
            for (indexInLong = 0; indexInLong < remainder; ++indexInLong) {
                output[outputOffset + indexInLong] = (int)(cellValue & this.mask);
                cellValue >>= this.bits;
            }
        }
    }

    @Override
    public BitStorage copy() {
        return new SimpleBitStorage(this.bits, this.size, (long[])this.data.clone());
    }

    public static class InitializationException
    extends RuntimeException {
        private InitializationException(String message) {
            super(message);
        }
    }
}

