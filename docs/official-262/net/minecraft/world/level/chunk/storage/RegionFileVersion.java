/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  net.jpountz.lz4.LZ4BlockInputStream
 *  net.jpountz.lz4.LZ4BlockOutputStream
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.chunk.storage;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.minecraft.util.FastBufferedInputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RegionFileVersion {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Int2ObjectMap<RegionFileVersion> VERSIONS = new Int2ObjectOpenHashMap();
    private static final Object2ObjectMap<String, RegionFileVersion> VERSIONS_BY_NAME = new Object2ObjectOpenHashMap();
    public static final RegionFileVersion VERSION_GZIP = RegionFileVersion.register(new RegionFileVersion(1, null, in -> new FastBufferedInputStream(new GZIPInputStream((InputStream)in)), out -> new BufferedOutputStream(new GZIPOutputStream((OutputStream)out))));
    public static final RegionFileVersion VERSION_DEFLATE = RegionFileVersion.register(new RegionFileVersion(2, "deflate", in -> new FastBufferedInputStream(new InflaterInputStream((InputStream)in)), out -> new BufferedOutputStream(new DeflaterOutputStream((OutputStream)out))));
    public static final RegionFileVersion VERSION_NONE = RegionFileVersion.register(new RegionFileVersion(3, "none", FastBufferedInputStream::new, BufferedOutputStream::new));
    public static final RegionFileVersion VERSION_LZ4 = RegionFileVersion.register(new RegionFileVersion(4, "lz4", in -> new FastBufferedInputStream((InputStream)new LZ4BlockInputStream(in)), out -> new BufferedOutputStream((OutputStream)new LZ4BlockOutputStream(out))));
    public static final RegionFileVersion VERSION_CUSTOM = RegionFileVersion.register(new RegionFileVersion(127, null, in -> {
        throw new UnsupportedOperationException();
    }, out -> {
        throw new UnsupportedOperationException();
    }));
    public static final RegionFileVersion DEFAULT;
    private static volatile RegionFileVersion selected;
    private final int id;
    private final @Nullable String optionName;
    private final StreamWrapper<InputStream> inputWrapper;
    private final StreamWrapper<OutputStream> outputWrapper;

    private RegionFileVersion(int id, @Nullable String optionName, StreamWrapper<InputStream> inputWrapper, StreamWrapper<OutputStream> outputWrapper) {
        this.id = id;
        this.optionName = optionName;
        this.inputWrapper = inputWrapper;
        this.outputWrapper = outputWrapper;
    }

    private static RegionFileVersion register(RegionFileVersion version) {
        VERSIONS.put(version.id, (Object)version);
        if (version.optionName != null) {
            VERSIONS_BY_NAME.put((Object)version.optionName, (Object)version);
        }
        return version;
    }

    public static @Nullable RegionFileVersion fromId(int id) {
        return (RegionFileVersion)VERSIONS.get(id);
    }

    public static void configure(String optionName) {
        RegionFileVersion version = (RegionFileVersion)VERSIONS_BY_NAME.get((Object)optionName);
        if (version != null) {
            selected = version;
        } else {
            LOGGER.error("Invalid `region-file-compression` value `{}` in server.properties. Please use one of: {}", (Object)optionName, (Object)String.join((CharSequence)", ", (Iterable<? extends CharSequence>)VERSIONS_BY_NAME.keySet()));
        }
    }

    public static RegionFileVersion getSelected() {
        return selected;
    }

    public static boolean isValidVersion(int version) {
        return VERSIONS.containsKey(version);
    }

    public int getId() {
        return this.id;
    }

    public OutputStream wrap(OutputStream is) throws IOException {
        return this.outputWrapper.wrap(is);
    }

    public InputStream wrap(InputStream is) throws IOException {
        return this.inputWrapper.wrap(is);
    }

    static {
        selected = DEFAULT = VERSION_DEFLATE;
    }

    @FunctionalInterface
    private static interface StreamWrapper<O> {
        public O wrap(O var1) throws IOException;
    }
}

