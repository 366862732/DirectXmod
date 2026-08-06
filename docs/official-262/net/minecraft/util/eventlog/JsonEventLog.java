/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonElement
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.eventlog;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.eventlog.JsonEventLogReader;
import org.jspecify.annotations.Nullable;

public class JsonEventLog<T>
implements Closeable {
    private static final Gson GSON = new Gson();
    private final Codec<T> codec;
    private final FileChannel channel;
    private final AtomicInteger referenceCount = new AtomicInteger(1);

    public JsonEventLog(Codec<T> codec, FileChannel channel) {
        this.codec = codec;
        this.channel = channel;
    }

    public static <T> JsonEventLog<T> open(Codec<T> codec, Path path) throws IOException {
        FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE);
        return new JsonEventLog<T>(codec, channel);
    }

    public void write(T event) throws IOException {
        JsonElement json = (JsonElement)this.codec.encodeStart((DynamicOps)JsonOps.INSTANCE, event).getOrThrow(IOException::new);
        this.channel.position(this.channel.size());
        Writer writer = Channels.newWriter((WritableByteChannel)this.channel, StandardCharsets.UTF_8);
        GSON.toJson(json, GSON.newJsonWriter(writer));
        writer.write(10);
        writer.flush();
    }

    public JsonEventLogReader<T> openReader() throws IOException {
        if (this.referenceCount.get() <= 0) {
            throw new IOException("Event log has already been closed");
        }
        this.referenceCount.incrementAndGet();
        final JsonEventLogReader<T> reader = JsonEventLogReader.create(this.codec, Channels.newReader((ReadableByteChannel)this.channel, StandardCharsets.UTF_8));
        return new JsonEventLogReader<T>(this){
            private volatile long position;
            final /* synthetic */ JsonEventLog this$0;
            {
                JsonEventLog jsonEventLog = this$0;
                Objects.requireNonNull(jsonEventLog);
                this.this$0 = jsonEventLog;
            }

            @Override
            public @Nullable T next() throws IOException {
                try {
                    this.this$0.channel.position(this.position);
                    Object t = reader.next();
                    return t;
                }
                finally {
                    this.position = this.this$0.channel.position();
                }
            }

            @Override
            public void close() throws IOException {
                this.this$0.releaseReference();
            }
        };
    }

    @Override
    public void close() throws IOException {
        this.releaseReference();
    }

    private void releaseReference() throws IOException {
        if (this.referenceCount.decrementAndGet() <= 0) {
            this.channel.close();
        }
    }
}

