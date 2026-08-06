/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.chunk;

import java.util.Arrays;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import org.jspecify.annotations.Nullable;

public class DataLayer {
    public static final int LAYER_COUNT = 16;
    public static final int LAYER_SIZE = 128;
    public static final int SIZE = 2048;
    private static final int NIBBLE_SIZE = 4;
    protected byte @Nullable [] data;
    private int defaultValue;

    public DataLayer() {
        this(0);
    }

    public DataLayer(int defaultValue) {
        this.defaultValue = defaultValue;
    }

    public DataLayer(byte[] data) {
        this.data = data;
        this.defaultValue = 0;
        if (data.length != 2048) {
            throw Util.pauseInIde(new IllegalArgumentException("DataLayer should be 2048 bytes not: " + data.length));
        }
    }

    public int get(int x, int y, int z) {
        return this.get(DataLayer.getIndex(x, y, z));
    }

    public void set(int x, int y, int z, int val) {
        this.set(DataLayer.getIndex(x, y, z), val);
    }

    private static int getIndex(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    private int get(int index) {
        if (this.data == null) {
            return this.defaultValue;
        }
        int position = DataLayer.getByteIndex(index);
        int nibble = DataLayer.getNibbleIndex(index);
        return this.data[position] >> 4 * nibble & 0xF;
    }

    private void set(int index, int val) {
        byte[] data = this.getData();
        int position = DataLayer.getByteIndex(index);
        int nibble = DataLayer.getNibbleIndex(index);
        int mask = ~(15 << 4 * nibble);
        int valueToSet = (val & 0xF) << 4 * nibble;
        data[position] = (byte)(data[position] & mask | valueToSet);
    }

    private static int getNibbleIndex(int index) {
        return index & 1;
    }

    private static int getByteIndex(int position) {
        return position >> 1;
    }

    public void fill(int value) {
        this.defaultValue = value;
        this.data = null;
    }

    private static byte packFilled(int value) {
        byte packed = (byte)value;
        for (int i = 4; i < 8; i += 4) {
            packed = (byte)(packed | value << i);
        }
        return packed;
    }

    public byte[] getData() {
        if (this.data == null) {
            this.data = new byte[2048];
            if (this.defaultValue != 0) {
                Arrays.fill(this.data, DataLayer.packFilled(this.defaultValue));
            }
        }
        return this.data;
    }

    public DataLayer copy() {
        if (this.data == null) {
            return new DataLayer(this.defaultValue);
        }
        return new DataLayer((byte[])this.data.clone());
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4096; ++i) {
            builder.append(Integer.toHexString(this.get(i)));
            if ((i & 0xF) == 15) {
                builder.append("\n");
            }
            if ((i & 0xFF) != 255) continue;
            builder.append("\n");
        }
        return builder.toString();
    }

    @VisibleForDebug
    public String layerToString(int layer) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 256; ++i) {
            builder.append(Integer.toHexString(this.get(i)));
            if ((i & 0xF) != 15) continue;
            builder.append("\n");
        }
        return builder.toString();
    }

    public boolean isDefinitelyHomogenous() {
        return this.data == null;
    }

    public boolean isDefinitelyFilledWith(int value) {
        return this.data == null && this.defaultValue == value;
    }

    public boolean isEmpty() {
        return this.data == null && this.defaultValue == 0;
    }
}

