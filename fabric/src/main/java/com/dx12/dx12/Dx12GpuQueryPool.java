package com.dx12.dx12;

import com.mojang.blaze3d.systems.GpuQueryPool;
import java.util.OptionalLong;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * D3D12-backed {@link GpuQueryPool} (timestamp queries).
 *
 * Native counterpart: {@code dx12CreateQueryPool} / {@code dx12ReadQueryValue} /
 * {@code dx12ReadQueryValues}. Reads are blocking: the native side resolves the
 * heap into a readback buffer, submits it and waits for completion.
 */
@Environment(EnvType.CLIENT)
public class Dx12GpuQueryPool implements GpuQueryPool {
    private final long handle;
    private final int size;
    private boolean closed;

    public Dx12GpuQueryPool(int size) {
        this.handle = Dx12Native.dx12CreateQueryPool(size);
        if (this.handle == 0) {
            throw new IllegalStateException("dx12CreateQueryPool returned a null handle");
        }
        this.size = size;
    }

    /** Native handle ({@code QueryPool*} as long). */
    public long nativeHandle() {
        return this.handle;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public OptionalLong getValue(int index) {
        this.ensureRange(index, 1);
        return OptionalLong.of(Dx12Native.dx12ReadQueryValue(this.handle, index));
    }

    @Override
    public OptionalLong[] getValues(int index, int count) {
        this.ensureRange(index, count);
        long[] values = new long[count];
        Dx12Native.dx12ReadQueryValues(this.handle, index, count, values);
        OptionalLong[] result = new OptionalLong[count];
        for (int i = 0; i < count; ++i) {
            result[i] = OptionalLong.of(values[i]);
        }
        return result;
    }

    private void ensureRange(int index, int count) {
        if (index < 0 || count < 0 || index + count > this.size) {
            throw new IndexOutOfBoundsException(
                "Query range " + index + ".." + (index + count) + " exceeds pool size " + this.size);
        }
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        Dx12Native.dx12DestroyQueryPool(this.handle);
    }
}
