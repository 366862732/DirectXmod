package com.xgdt.dx11.dx11;

import com.mojang.blaze3d.buffers.GpuBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** A mapped slice of a {@link Dx11Buffer}. */
final class Dx11MappedView implements GpuBuffer.MappedView {
    private final Dx11Buffer buffer;
    private final ByteBuffer data;
    private boolean closed;

    Dx11MappedView(Dx11Buffer buffer, ByteBuffer data) {
        this.buffer = buffer;
        // JNI NewDirectByteBuffer() returns a BIG_ENDIAN buffer, while the GL
        // backend hands out native-order (little-endian) memory.  Vanilla's
        // Std140Builder uses ByteBuffer.putFloat/putInt without changing the
        // order, so restore native order or every dynamic uniform is swapped.
        this.data = data.order(ByteOrder.nativeOrder());
    }

    @Override
    public ByteBuffer data() {
        if (this.closed) {
            throw new IllegalStateException("Mapped view is closed");
        }
        return this.data;
    }

    boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            this.buffer.onUnmapped(this);
        }
    }
}
