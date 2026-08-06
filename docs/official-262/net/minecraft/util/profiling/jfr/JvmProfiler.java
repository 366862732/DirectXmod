/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.util.profiling.jfr;

import com.mojang.logging.LogUtils;
import java.net.SocketAddress;
import java.nio.file.Path;
import jdk.jfr.FlightRecorder;
import net.minecraft.core.Holder;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JfrProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public interface JvmProfiler {
    public static final JvmProfiler INSTANCE = Runtime.class.getModule().getLayer().findModule("jdk.jfr").isPresent() && FlightRecorder.isAvailable() ? JfrProfiler.getInstance() : new NoOpProfiler();

    public boolean start(Environment var1);

    public Path stop();

    public boolean isRunning();

    public boolean isAvailable();

    public void onServerTick(float var1);

    public void onClientTick(int var1);

    public void onPacketReceived(ConnectionProtocol var1, PacketType<?> var2, SocketAddress var3, int var4);

    public void onPacketSent(ConnectionProtocol var1, PacketType<?> var2, SocketAddress var3, int var4);

    public void onRegionFileRead(RegionStorageInfo var1, ChunkPos var2, RegionFileVersion var3, int var4);

    public void onRegionFileWrite(RegionStorageInfo var1, ChunkPos var2, RegionFileVersion var3, int var4);

    public @Nullable ProfiledDuration onWorldLoadedStarted();

    public @Nullable ProfiledDuration onChunkGenerate(ChunkPos var1, ResourceKey<Level> var2, String var3);

    public @Nullable ProfiledDuration onStructureGenerate(ChunkPos var1, ResourceKey<Level> var2, Holder<Structure> var3);

    public static class NoOpProfiler
    implements JvmProfiler {
        private static final Logger LOGGER = LogUtils.getLogger();
        private static final ProfiledDuration NO_OP_COMMIT = ignored -> {};

        @Override
        public boolean start(Environment environment) {
            LOGGER.warn("Attempted to start Flight Recorder, but it's not supported on this JVM");
            return false;
        }

        @Override
        public Path stop() {
            throw new IllegalStateException("Attempted to stop Flight Recorder, but it's not supported on this JVM");
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void onPacketReceived(ConnectionProtocol protocol, PacketType<?> packetId, SocketAddress remoteAddress, int readableBytes) {
        }

        @Override
        public void onPacketSent(ConnectionProtocol protocol, PacketType<?> packetId, SocketAddress remoteAddress, int writtenBytes) {
        }

        @Override
        public void onRegionFileRead(RegionStorageInfo info, ChunkPos pos, RegionFileVersion version, int readBytes) {
        }

        @Override
        public void onRegionFileWrite(RegionStorageInfo info, ChunkPos pos, RegionFileVersion version, int writtenBytes) {
        }

        @Override
        public void onServerTick(float averageTickTime) {
        }

        @Override
        public void onClientTick(int fps) {
        }

        @Override
        public ProfiledDuration onWorldLoadedStarted() {
            return NO_OP_COMMIT;
        }

        @Override
        public @Nullable ProfiledDuration onChunkGenerate(ChunkPos pos, ResourceKey<Level> dimension, String name) {
            return null;
        }

        @Override
        public ProfiledDuration onStructureGenerate(ChunkPos sourceChunkPos, ResourceKey<Level> dimension, Holder<Structure> structure) {
            return NO_OP_COMMIT;
        }
    }
}

