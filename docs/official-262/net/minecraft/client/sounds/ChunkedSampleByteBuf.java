/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  it.unimi.dsi.fastutil.floats.FloatConsumer
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.BufferUtils
 */
package net.minecraft.client.sounds;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.nio.ByteBuffer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import org.lwjgl.BufferUtils;

@Environment(value=EnvType.CLIENT)
public class ChunkedSampleByteBuf
implements FloatConsumer {
    private final List<ByteBuffer> buffers = Lists.newArrayList();
    private final int bufferSize;
    private int byteCount;
    private ByteBuffer currentBuffer;

    public ChunkedSampleByteBuf(int bufferSize) {
        this.bufferSize = bufferSize + 1 & 0xFFFFFFFE;
        this.currentBuffer = BufferUtils.createByteBuffer((int)bufferSize);
    }

    public void accept(float sample) {
        if (this.currentBuffer.remaining() == 0) {
            this.currentBuffer.flip();
            this.buffers.add(this.currentBuffer);
            this.currentBuffer = BufferUtils.createByteBuffer((int)this.bufferSize);
        }
        int intVal = Mth.clamp((int)(sample * 32767.5f - 0.5f), Short.MIN_VALUE, Short.MAX_VALUE);
        this.currentBuffer.putShort((short)intVal);
        this.byteCount += 2;
    }

    public ByteBuffer get() {
        this.currentBuffer.flip();
        if (this.buffers.isEmpty()) {
            return this.currentBuffer;
        }
        ByteBuffer result = BufferUtils.createByteBuffer((int)this.byteCount);
        this.buffers.forEach(result::put);
        result.put(this.currentBuffer);
        result.flip();
        return result;
    }

    public int size() {
        return this.byteCount;
    }
}

