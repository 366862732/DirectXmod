package com.xgdt.dx11.dx11;

import com.mojang.blaze3d.systems.GpuQuery;
import java.util.OptionalLong;

/** {@link GpuQuery} backed by D3D11 timestamp queries. */
final class Dx11Query implements GpuQuery, Dx11Resource {
    private final Dx11Device device;
    private final long handle;
    private boolean closed;

    Dx11Query(Dx11Device device) {
        this.device = device;
        this.handle = Dx11Native.nativeCreateTimerQuery(device.nativeHandle());
        if (this.handle == 0L) {
            throw new IllegalStateException("Could not create D3D11 timer query");
        }
        device.register(this);
    }

    long handle() {
        return this.handle;
    }

    void end() {
        if (!this.closed) {
            Dx11Native.nativeTimerQueryEnd(this.handle);
        }
    }

    @Override
    public OptionalLong getValue() {
        if (this.closed) {
            return OptionalLong.empty();
        }
        long value = Dx11Native.nativeQueryGetValue(this.handle);
        return value == Long.MIN_VALUE ? OptionalLong.empty() : OptionalLong.of(value);
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            this.device.unregister(this);
            Dx11Native.nativeDestroyQuery(this.handle);
        }
    }

    @Override
    public long nativeHandle() {
        return this.handle;
    }

    @Override
    public void destroyNative() {
        close();
    }
}
