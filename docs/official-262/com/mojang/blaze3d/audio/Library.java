/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.openal.AL
 *  org.lwjgl.openal.AL10
 *  org.lwjgl.openal.ALC
 *  org.lwjgl.openal.ALC10
 *  org.lwjgl.openal.ALC11
 *  org.lwjgl.openal.ALCCapabilities
 *  org.lwjgl.openal.ALCapabilities
 *  org.lwjgl.system.MemoryStack
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.audio;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.audio.CallbackDeviceTracker;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.DeviceList;
import com.mojang.blaze3d.audio.DeviceTracker;
import com.mojang.blaze3d.audio.Listener;
import com.mojang.blaze3d.audio.OpenAlUtil;
import com.mojang.blaze3d.audio.PollingDeviceTracker;
import com.mojang.logging.LogUtils;
import java.nio.IntBuffer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class Library {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int NO_DEVICE = 0;
    private static final String NO_DEVICE_NAME = "(None)";
    private static final int DEFAULT_CHANNEL_COUNT = 30;
    private long currentDevice;
    private String currentDeviceName = "(None)";
    private long context;
    private boolean supportsDisconnections;
    private static final ChannelPool EMPTY = new ChannelPool(){

        @Override
        public @Nullable Channel acquire() {
            return null;
        }

        @Override
        public boolean release(Channel channel) {
            return false;
        }

        @Override
        public void cleanup() {
        }

        @Override
        public int getMaxCount() {
            return 0;
        }

        @Override
        public int getUsedCount() {
            return 0;
        }
    };
    private ChannelPool staticChannels = EMPTY;
    private ChannelPool streamingChannels = EMPTY;
    private final Listener listener = new Listener();

    public void init(@Nullable String preferredDevice, DeviceList currentDevices, boolean useHrtf) {
        this.currentDeviceName = NO_DEVICE_NAME;
        this.currentDevice = Library.openDeviceOrFallback(preferredDevice, currentDevices.defaultDevice());
        this.currentDeviceName = Library.queryDeviceName(this.currentDevice);
        this.supportsDisconnections = false;
        ALCCapabilities alcCapabilities = ALC.createCapabilities((long)this.currentDevice);
        if (OpenAlUtil.checkALCError(this.currentDevice, "Get capabilities")) {
            throw new IllegalStateException("Failed to get OpenAL capabilities");
        }
        if (!alcCapabilities.OpenALC11) {
            throw new IllegalStateException("OpenAL 1.1 not supported");
        }
        try (MemoryStack stack = MemoryStack.stackPush();){
            IntBuffer attr = this.createAttributes(stack, alcCapabilities.ALC_SOFT_HRTF && useHrtf);
            this.context = ALC10.alcCreateContext((long)this.currentDevice, (IntBuffer)attr);
        }
        if (OpenAlUtil.checkALCError(this.currentDevice, "Create context")) {
            throw new IllegalStateException("Unable to create OpenAL context");
        }
        ALC10.alcMakeContextCurrent((long)this.context);
        int totalChannelCount = this.getChannelCount();
        int streamingChannelCount = Mth.clamp((int)Mth.sqrt(totalChannelCount), 2, 8);
        int staticChannelCount = Mth.clamp(totalChannelCount - streamingChannelCount, 8, 255);
        this.staticChannels = new CountingChannelPool(staticChannelCount);
        this.streamingChannels = new CountingChannelPool(streamingChannelCount);
        ALCapabilities alCapabilities = AL.createCapabilities((ALCCapabilities)alcCapabilities);
        OpenAlUtil.checkALError("Initialization");
        if (!alCapabilities.AL_EXT_source_distance_model) {
            throw new IllegalStateException("AL_EXT_source_distance_model is not supported");
        }
        AL10.alEnable((int)512);
        if (!alCapabilities.AL_EXT_LINEAR_DISTANCE) {
            throw new IllegalStateException("AL_EXT_LINEAR_DISTANCE is not supported");
        }
        OpenAlUtil.checkALError("Enable per-source distance models");
        LOGGER.info("OpenAL initialized on device {}", (Object)this.currentDeviceName);
        this.supportsDisconnections = ALC10.alcIsExtensionPresent((long)this.currentDevice, (CharSequence)"ALC_EXT_disconnect");
    }

    private IntBuffer createAttributes(MemoryStack stack, boolean enableHrtf) {
        int maxAttributes = 5;
        IntBuffer attr = stack.callocInt(11);
        int numHrtf = ALC10.alcGetInteger((long)this.currentDevice, (int)6548);
        if (numHrtf > 0) {
            attr.put(6546).put(enableHrtf ? 1 : 0);
            attr.put(6550).put(0);
        }
        attr.put(6554).put(1);
        return attr.put(0).flip();
    }

    private int getChannelCount() {
        try (MemoryStack stack = MemoryStack.stackPush();){
            int size = ALC10.alcGetInteger((long)this.currentDevice, (int)4098);
            if (OpenAlUtil.checkALCError(this.currentDevice, "Get attributes size")) {
                throw new IllegalStateException("Failed to get OpenAL attributes");
            }
            IntBuffer attributes = stack.mallocInt(size);
            ALC10.alcGetIntegerv((long)this.currentDevice, (int)4099, (IntBuffer)attributes);
            if (OpenAlUtil.checkALCError(this.currentDevice, "Get attributes")) {
                throw new IllegalStateException("Failed to get OpenAL attributes");
            }
            int pos = 0;
            while (pos < size) {
                int attribute;
                if ((attribute = attributes.get(pos++)) == 0) {
                    break;
                }
                int attributeValue = attributes.get(pos++);
                if (attribute != 4112) continue;
                int n = attributeValue;
                return n;
            }
        }
        return 30;
    }

    public @Nullable String currentDeviceName() {
        return this.currentDeviceName;
    }

    private static String queryDeviceName(long deviceId) {
        Object name = ALC10.alcGetString((long)deviceId, (int)4115);
        if (name == null) {
            name = ALC10.alcGetString((long)deviceId, (int)4101);
        }
        if (name == null) {
            name = "Unknown (0x" + HexFormat.of().toHexDigits(deviceId) + ")";
        }
        return name;
    }

    private static long openDeviceOrFallback(@Nullable String preferredDevice, @Nullable String systemDefaultDevice) {
        OptionalLong device = OptionalLong.empty();
        if (preferredDevice != null) {
            device = Library.tryOpenDevice(preferredDevice);
        }
        if (device.isEmpty() && systemDefaultDevice != null) {
            device = Library.tryOpenDevice(systemDefaultDevice);
        }
        if (device.isEmpty()) {
            device = Library.tryOpenDevice(null);
        }
        if (device.isEmpty()) {
            throw new IllegalStateException("Failed to open OpenAL device");
        }
        return device.getAsLong();
    }

    private static OptionalLong tryOpenDevice(@Nullable String name) {
        long device = ALC10.alcOpenDevice((CharSequence)name);
        if (device != 0L && !OpenAlUtil.checkALCError(device, "Open device")) {
            return OptionalLong.of(device);
        }
        return OptionalLong.empty();
    }

    public void cleanup() {
        this.staticChannels.cleanup();
        this.streamingChannels.cleanup();
        ALC10.alcDestroyContext((long)this.context);
        if (this.currentDevice != 0L) {
            ALC10.alcCloseDevice((long)this.currentDevice);
        }
    }

    public Listener getListener() {
        return this.listener;
    }

    public @Nullable Channel acquireChannel(Pool pool) {
        return (pool == Pool.STREAMING ? this.streamingChannels : this.staticChannels).acquire();
    }

    public void releaseChannel(Channel channel) {
        if (!this.staticChannels.release(channel) && !this.streamingChannels.release(channel)) {
            throw new IllegalStateException("Tried to release unknown channel");
        }
    }

    public String getChannelDebugString() {
        return String.format(Locale.ROOT, "Sounds: %d/%d + %d/%d", this.staticChannels.getUsedCount(), this.staticChannels.getMaxCount(), this.streamingChannels.getUsedCount(), this.streamingChannels.getMaxCount());
    }

    public boolean isCurrentDeviceDisconnected() {
        return this.supportsDisconnections && ALC11.alcGetInteger((long)this.currentDevice, (int)787) == 0;
    }

    public static DeviceTracker createDeviceTracker() {
        DeviceList deviceList = DeviceList.query();
        if (CallbackDeviceTracker.isSupported()) {
            LOGGER.debug("Using SOFT_system_events callback for tracking audio device changes");
            return CallbackDeviceTracker.createAndInstall(deviceList);
        }
        LOGGER.debug("Using polling for tracking audio device changes");
        return new PollingDeviceTracker(deviceList);
    }

    @Environment(value=EnvType.CLIENT)
    private static interface ChannelPool {
        public @Nullable Channel acquire();

        public boolean release(Channel var1);

        public void cleanup();

        public int getMaxCount();

        public int getUsedCount();
    }

    @Environment(value=EnvType.CLIENT)
    private static class CountingChannelPool
    implements ChannelPool {
        private final int limit;
        private final Set<Channel> activeChannels = Sets.newIdentityHashSet();

        public CountingChannelPool(int limit) {
            this.limit = limit;
        }

        @Override
        public @Nullable Channel acquire() {
            if (this.activeChannels.size() >= this.limit) {
                if (SharedConstants.IS_RUNNING_IN_IDE) {
                    LOGGER.warn("Maximum sound pool size {} reached", (Object)this.limit);
                }
                return null;
            }
            Channel channel = Channel.create();
            if (channel != null) {
                this.activeChannels.add(channel);
            }
            return channel;
        }

        @Override
        public boolean release(Channel channel) {
            if (!this.activeChannels.remove(channel)) {
                return false;
            }
            channel.destroy();
            return true;
        }

        @Override
        public void cleanup() {
            this.activeChannels.forEach(Channel::destroy);
            this.activeChannels.clear();
        }

        @Override
        public int getMaxCount() {
            return this.limit;
        }

        @Override
        public int getUsedCount() {
            return this.activeChannels.size();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Pool {
        STATIC,
        STREAMING;

    }
}

