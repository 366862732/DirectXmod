/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.nbt;

import java.io.DataInput;
import java.io.IOException;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.Tag;

public interface TagType<T extends Tag> {
    public T load(DataInput var1, NbtAccounter var2) throws IOException;

    public StreamTagVisitor.ValueResult parse(DataInput var1, StreamTagVisitor var2, NbtAccounter var3) throws IOException;

    default public void parseRoot(DataInput input, StreamTagVisitor output, NbtAccounter accounter) throws IOException {
        switch (output.visitRootEntry(this)) {
            case CONTINUE: {
                this.parse(input, output, accounter);
                break;
            }
            case HALT: {
                break;
            }
            case BREAK: {
                this.skip(input, accounter);
            }
        }
    }

    public void skip(DataInput var1, int var2, NbtAccounter var3) throws IOException;

    public void skip(DataInput var1, NbtAccounter var2) throws IOException;

    public String getName();

    public String getPrettyName();

    public static TagType<EndTag> createInvalid(final int id) {
        return new TagType<EndTag>(){

            private IOException createException() {
                return new IOException("Invalid tag id: " + id);
            }

            @Override
            public EndTag load(DataInput input, NbtAccounter accounter) throws IOException {
                throw this.createException();
            }

            @Override
            public StreamTagVisitor.ValueResult parse(DataInput input, StreamTagVisitor output, NbtAccounter accounter) throws IOException {
                throw this.createException();
            }

            @Override
            public void skip(DataInput input, int count, NbtAccounter accounter) throws IOException {
                throw this.createException();
            }

            @Override
            public void skip(DataInput input, NbtAccounter accounter) throws IOException {
                throw this.createException();
            }

            @Override
            public String getName() {
                return "INVALID[" + id + "]";
            }

            @Override
            public String getPrettyName() {
                return "UNKNOWN_" + id;
            }
        };
    }

    public static interface VariableSize<T extends Tag>
    extends TagType<T> {
        @Override
        default public void skip(DataInput input, int count, NbtAccounter accounter) throws IOException {
            for (int i = 0; i < count; ++i) {
                this.skip(input, accounter);
            }
        }
    }

    public static interface StaticSize<T extends Tag>
    extends TagType<T> {
        @Override
        default public void skip(DataInput input, NbtAccounter accounter) throws IOException {
            input.skipBytes(this.size());
        }

        @Override
        default public void skip(DataInput input, int count, NbtAccounter accounter) throws IOException {
            input.skipBytes(this.size() * count);
        }

        public int size();
    }
}

