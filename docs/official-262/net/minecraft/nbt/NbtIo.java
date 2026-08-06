/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import net.minecraft.nbt.TagTypes;
import net.minecraft.util.DelegateDataOutput;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class NbtIo {
    private static final OpenOption[] SYNC_OUTPUT_OPTIONS = new OpenOption[]{StandardOpenOption.SYNC, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING};

    public static CompoundTag readCompressed(Path file, NbtAccounter accounter) throws IOException {
        try (InputStream rawInput = Files.newInputStream(file, new OpenOption[0]);){
            CompoundTag compoundTag;
            try (FastBufferedInputStream input = new FastBufferedInputStream(rawInput);){
                compoundTag = NbtIo.readCompressed(input, accounter);
            }
            return compoundTag;
        }
    }

    private static DataInputStream createDecompressorStream(InputStream in) throws IOException {
        return new DataInputStream(new FastBufferedInputStream(new GZIPInputStream(in)));
    }

    private static DataOutputStream createCompressorStream(OutputStream out) throws IOException {
        return new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(out)));
    }

    public static CompoundTag readCompressed(InputStream in, NbtAccounter accounter) throws IOException {
        try (DataInputStream dis = NbtIo.createDecompressorStream(in);){
            CompoundTag compoundTag = NbtIo.read(dis, accounter);
            return compoundTag;
        }
    }

    public static void parseCompressed(Path file, StreamTagVisitor output, NbtAccounter accounter) throws IOException {
        try (InputStream rawInput = Files.newInputStream(file, new OpenOption[0]);
             FastBufferedInputStream input = new FastBufferedInputStream(rawInput);){
            NbtIo.parseCompressed(input, output, accounter);
        }
    }

    public static void parseCompressed(InputStream in, StreamTagVisitor output, NbtAccounter accounter) throws IOException {
        try (DataInputStream dis = NbtIo.createDecompressorStream(in);){
            NbtIo.parse(dis, output, accounter);
        }
    }

    public static void writeCompressed(CompoundTag tag, Path file) throws IOException {
        try (OutputStream out = Files.newOutputStream(file, SYNC_OUTPUT_OPTIONS);
             BufferedOutputStream bufferedOut = new BufferedOutputStream(out);){
            NbtIo.writeCompressed(tag, bufferedOut);
        }
    }

    public static void writeCompressed(CompoundTag tag, OutputStream out) throws IOException {
        try (DataOutputStream dos = NbtIo.createCompressorStream(out);){
            NbtIo.write(tag, dos);
        }
    }

    public static void write(CompoundTag tag, Path file) throws IOException {
        try (OutputStream out = Files.newOutputStream(file, SYNC_OUTPUT_OPTIONS);
             BufferedOutputStream bufferedOut = new BufferedOutputStream(out);
             DataOutputStream dos = new DataOutputStream(bufferedOut);){
            NbtIo.write(tag, dos);
        }
    }

    public static @Nullable CompoundTag read(Path file) throws IOException {
        if (!Files.exists(file, new LinkOption[0])) {
            return null;
        }
        try (InputStream in = Files.newInputStream(file, new OpenOption[0]);){
            CompoundTag compoundTag;
            try (DataInputStream dis = new DataInputStream(in);){
                compoundTag = NbtIo.read(dis, NbtAccounter.unlimitedHeap());
            }
            return compoundTag;
        }
    }

    public static CompoundTag read(DataInput input) throws IOException {
        return NbtIo.read(input, NbtAccounter.unlimitedHeap());
    }

    public static CompoundTag read(DataInput input, NbtAccounter accounter) throws IOException {
        Tag tag = NbtIo.readUnnamedTag(input, accounter);
        if (tag instanceof CompoundTag) {
            CompoundTag compoundTag = (CompoundTag)tag;
            return compoundTag;
        }
        throw new IOException("Root tag must be a named compound tag");
    }

    public static void write(CompoundTag tag, DataOutput output) throws IOException {
        NbtIo.writeUnnamedTagWithFallback(tag, output);
    }

    public static void parse(DataInput input, StreamTagVisitor output, NbtAccounter accounter) throws IOException {
        TagType<?> type = TagTypes.getType(input.readByte());
        if (type == EndTag.TYPE) {
            if (output.visitRootEntry(EndTag.TYPE) == StreamTagVisitor.ValueResult.CONTINUE) {
                output.visitEnd();
            }
            return;
        }
        switch (output.visitRootEntry(type)) {
            case HALT: {
                break;
            }
            case BREAK: {
                StringTag.skipString(input);
                type.skip(input, accounter);
                break;
            }
            case CONTINUE: {
                StringTag.skipString(input);
                type.parse(input, output, accounter);
            }
        }
    }

    public static Tag readAnyTag(DataInput input, NbtAccounter accounter) throws IOException {
        byte type = input.readByte();
        if (type == 0) {
            return EndTag.INSTANCE;
        }
        return NbtIo.readTagSafe(input, accounter, type);
    }

    public static void writeAnyTag(Tag tag, DataOutput output) throws IOException {
        output.writeByte(tag.getId());
        if (tag.getId() == 0) {
            return;
        }
        tag.write(output);
    }

    public static void writeUnnamedTag(Tag tag, DataOutput output) throws IOException {
        output.writeByte(tag.getId());
        if (tag.getId() == 0) {
            return;
        }
        output.writeUTF("");
        tag.write(output);
    }

    public static void writeUnnamedTagWithFallback(Tag tag, DataOutput output) throws IOException {
        NbtIo.writeUnnamedTag(tag, new StringFallbackDataOutput(output));
    }

    @VisibleForTesting
    public static Tag readUnnamedTag(DataInput input, NbtAccounter accounter) throws IOException {
        byte type = input.readByte();
        if (type == 0) {
            return EndTag.INSTANCE;
        }
        StringTag.skipString(input);
        return NbtIo.readTagSafe(input, accounter, type);
    }

    private static Tag readTagSafe(DataInput input, NbtAccounter accounter, byte type) {
        try {
            return TagTypes.getType(type).load(input, accounter);
        }
        catch (IOException e) {
            CrashReport report = CrashReport.forThrowable(e, "Loading NBT data");
            CrashReportCategory category = report.addCategory("NBT Tag");
            category.setDetail("Tag type", type);
            throw new ReportedNbtException(report);
        }
    }

    public static class StringFallbackDataOutput
    extends DelegateDataOutput {
        public StringFallbackDataOutput(DataOutput parent) {
            super(parent);
        }

        @Override
        public void writeUTF(String s) throws IOException {
            try {
                super.writeUTF(s);
            }
            catch (UTFDataFormatException exception) {
                Util.logAndPauseIfInIde("Failed to write NBT String", exception);
                super.writeUTF("");
            }
        }
    }
}

