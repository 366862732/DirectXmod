/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 *  org.slf4j.Logger
 */
package net.minecraft.util.filefix.access;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.FileUtil;
import org.slf4j.Logger;

public abstract class CompressedNbt
implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path path;
    private final MissingSeverity missingSeverity;

    public CompressedNbt(Path path, MissingSeverity missingSeverity) {
        this.path = path;
        this.missingSeverity = missingSeverity;
    }

    public abstract Optional<Dynamic<Tag>> read() throws IOException;

    protected final Optional<Dynamic<Tag>> readFile() throws IOException {
        try {
            return Optional.of(new Dynamic((DynamicOps)NbtOps.INSTANCE, (Object)NbtIo.readCompressed(this.path, NbtAccounter.unlimitedHeap())));
        }
        catch (NoSuchFileException ignored) {
            this.missingSeverity.log("Missing file: {}", this.path);
            return Optional.empty();
        }
    }

    public abstract <T> void write(Dynamic<T> var1);

    protected final <T> void writeFile(Dynamic<T> data) {
        CompoundTag cast = (CompoundTag)data.cast((DynamicOps)NbtOps.INSTANCE);
        try {
            FileUtil.createDirectoriesSafe(this.path.getParent());
            NbtIo.writeCompressed(cast, this.path);
        }
        catch (IOException e) {
            LOGGER.error("Failed to write to {}: {}", (Object)this.path, (Object)e);
        }
    }

    public Path path() {
        return this.path;
    }

    @Override
    public void close() {
    }

    public static enum MissingSeverity {
        IMPORTANT((arg_0, arg_1) -> ((Logger)LOGGER).error(arg_0, arg_1)),
        NEUTRAL((arg_0, arg_1) -> ((Logger)LOGGER).info(arg_0, arg_1)),
        MINOR((arg_0, arg_1) -> ((Logger)LOGGER).debug(arg_0, arg_1));

        private final BiConsumer<String, Object> logFunction;

        private MissingSeverity(BiConsumer<String, Object> logFunction) {
            this.logFunction = logFunction;
        }

        public void log(String message, Path path) {
            this.logFunction.accept(message, path);
        }
    }
}

