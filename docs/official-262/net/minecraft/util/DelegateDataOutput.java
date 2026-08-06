/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import java.io.DataOutput;
import java.io.IOException;
import net.minecraft.SuppressForbidden;

public class DelegateDataOutput
implements DataOutput {
    private final DataOutput parent;

    public DelegateDataOutput(DataOutput parent) {
        this.parent = parent;
    }

    @Override
    public void write(int b) throws IOException {
        this.parent.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.parent.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.parent.write(b, off, len);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        this.parent.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        this.parent.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        this.parent.writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        this.parent.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        this.parent.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        this.parent.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        this.parent.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        this.parent.writeDouble(v);
    }

    @Override
    @SuppressForbidden(reason="Delegation is not use")
    public void writeBytes(String s) throws IOException {
        this.parent.writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        this.parent.writeChars(s);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        this.parent.writeUTF(s);
    }
}

