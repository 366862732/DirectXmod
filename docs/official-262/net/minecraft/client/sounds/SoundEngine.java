/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashMultimap
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Multimap
 *  com.google.common.collect.Sets
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.objects.Object2FloatMap
 *  it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 *  org.slf4j.Marker
 *  org.slf4j.MarkerFactory
 */
package net.minecraft.client.sounds;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.DeviceList;
import com.mojang.blaze3d.audio.DeviceTracker;
import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.Listener;
import com.mojang.blaze3d.audio.ListenerTransform;
import com.mojang.blaze3d.audio.SoundBuffer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundEngineExecutor;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@Environment(value=EnvType.CLIENT)
public class SoundEngine {
    private static final Marker MARKER = MarkerFactory.getMarker((String)"SOUNDS");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float PITCH_MIN = 0.5f;
    private static final float PITCH_MAX = 2.0f;
    private static final float VOLUME_MIN = 0.0f;
    private static final float VOLUME_MAX = 1.0f;
    private static final int MIN_SOURCE_LIFETIME = 20;
    private static final Set<Identifier> ONLY_WARN_ONCE = Sets.newHashSet();
    public static final String MISSING_SOUND = "FOR THE DEBUG!";
    public static final String OPEN_AL_SOFT_PREFIX = "OpenAL Soft on ";
    public static final int OPEN_AL_SOFT_PREFIX_LENGTH = "OpenAL Soft on ".length();
    private final SoundManager soundManager;
    private final Options options;
    private boolean loaded;
    private final Library library = new Library();
    private final Listener listener = this.library.getListener();
    private final SoundBufferLibrary soundBuffers;
    private final SoundEngineExecutor executor = new SoundEngineExecutor();
    private final ChannelAccess channelAccess = new ChannelAccess(this.library, this.executor);
    private int tickCount;
    private DeviceList lastSeenDevices;
    private final DeviceTracker deviceTracker = Library.createDeviceTracker();
    private final Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel = Maps.newHashMap();
    private final Multimap<SoundSource, SoundInstance> instanceBySource = HashMultimap.create();
    private final Object2FloatMap<SoundSource> gainBySource = (Object2FloatMap)Util.make(new Object2FloatOpenHashMap(), map -> map.defaultReturnValue(1.0f));
    private final List<TickableSoundInstance> tickingSounds = Lists.newArrayList();
    private final Map<SoundInstance, Integer> queuedSounds = Maps.newHashMap();
    private final Map<SoundInstance, Integer> soundDeleteTime = Maps.newHashMap();
    private final List<SoundEventListener> listeners = Lists.newArrayList();
    private final List<TickableSoundInstance> queuedTickableSounds = Lists.newArrayList();
    private final List<Sound> preloadQueue = Lists.newArrayList();

    public SoundEngine(SoundManager soundManager, Options options, ResourceProvider resourceProvider) {
        this.soundManager = soundManager;
        this.options = options;
        this.soundBuffers = new SoundBufferLibrary(resourceProvider);
        this.lastSeenDevices = this.deviceTracker.currentDevices();
    }

    public void reload() {
        ONLY_WARN_ONCE.clear();
        for (SoundEvent sound : BuiltInRegistries.SOUND_EVENT) {
            Identifier location;
            if (sound == SoundEvents.EMPTY || this.soundManager.getSoundEvent(location = sound.location()) != null) continue;
            LOGGER.warn("Missing sound for event: {}", (Object)BuiltInRegistries.SOUND_EVENT.getKey(sound));
            ONLY_WARN_ONCE.add(location);
        }
        this.destroy();
        this.loadLibrary();
    }

    private synchronized void loadLibrary() {
        if (this.loaded) {
            return;
        }
        try {
            String soundDevice = this.options.soundDevice().get();
            DeviceList currentDevices = this.deviceTracker.currentDevices();
            this.library.init(Options.isSoundDeviceDefault(soundDevice) ? null : soundDevice, currentDevices, this.options.directionalAudio().get());
            this.listener.reset();
            this.soundBuffers.preload(this.preloadQueue).thenRun(this.preloadQueue::clear);
            this.loaded = true;
            LOGGER.info(MARKER, "Sound engine started");
        }
        catch (RuntimeException e) {
            LOGGER.error(MARKER, "Error starting SoundSystem. Turning off sounds & music", (Throwable)e);
        }
    }

    public void refreshCategoryVolume(SoundSource source) {
        if (!this.loaded) {
            return;
        }
        this.instanceToChannel.forEach((soundInstance, channelHandle) -> {
            if (source == soundInstance.getSource() || source == SoundSource.MASTER) {
                float newVolume = this.calculateVolume((SoundInstance)soundInstance);
                channelHandle.execute(channel -> channel.setVolume(newVolume));
            }
        });
    }

    public void destroy() {
        if (this.loaded) {
            this.stopAll();
            this.soundBuffers.clear();
            this.library.cleanup();
            this.loaded = false;
        }
    }

    public void emergencyShutdown() {
        if (this.loaded) {
            this.library.cleanup();
        }
    }

    public void stop(SoundInstance soundInstance) {
        ChannelAccess.ChannelHandle handle;
        if (this.loaded && (handle = this.instanceToChannel.get(soundInstance)) != null) {
            handle.execute(Channel::stop);
        }
    }

    public void updateCategoryVolume(SoundSource source, float gain) {
        this.gainBySource.put((Object)source, Mth.clamp(gain, 0.0f, 1.0f));
        this.refreshCategoryVolume(source);
    }

    public void stopAll() {
        if (this.loaded) {
            this.executor.shutDown();
            this.instanceToChannel.clear();
            this.channelAccess.clear();
            this.queuedSounds.clear();
            this.tickingSounds.clear();
            this.instanceBySource.clear();
            this.soundDeleteTime.clear();
            this.queuedTickableSounds.clear();
            this.gainBySource.clear();
            this.executor.startUp();
        }
    }

    public void addEventListener(SoundEventListener listener) {
        this.listeners.add(listener);
    }

    public void removeEventListener(SoundEventListener listener) {
        this.listeners.remove(listener);
    }

    private boolean shouldChangeDevice() {
        if (this.library.isCurrentDeviceDisconnected()) {
            LOGGER.info("Audio device was lost!");
            this.deviceTracker.forceRefresh();
            return true;
        }
        this.deviceTracker.tick();
        boolean shouldChangeDevice = false;
        DeviceList currentDevices = this.deviceTracker.currentDevices();
        if (!currentDevices.equals(this.lastSeenDevices)) {
            String userSelectedDevice;
            String currentDeviceName = this.library.currentDeviceName();
            if (!currentDevices.allDevices().contains(currentDeviceName)) {
                LOGGER.info("Current audio device has disapeared!");
                shouldChangeDevice = true;
            }
            if (Options.isSoundDeviceDefault(userSelectedDevice = this.options.soundDevice().get())) {
                String newDefault = currentDevices.defaultDevice();
                if (!Objects.equals(currentDeviceName, newDefault)) {
                    LOGGER.info("System default audio device has changed!");
                    shouldChangeDevice = true;
                }
            } else if (!Objects.equals(currentDeviceName, userSelectedDevice) && currentDevices.allDevices().contains(userSelectedDevice)) {
                LOGGER.info("Preferred audio device has become available!");
                shouldChangeDevice = true;
            }
            this.lastSeenDevices = currentDevices;
        }
        return shouldChangeDevice;
    }

    public void tick(boolean paused) {
        if (this.shouldChangeDevice()) {
            this.reload();
        }
        if (!paused) {
            this.tickInGameSound();
        } else {
            this.tickMusicWhenPaused();
        }
        this.channelAccess.scheduleTick();
    }

    private void tickInGameSound() {
        ++this.tickCount;
        this.queuedTickableSounds.stream().filter(SoundInstance::canPlaySound).forEach(this::play);
        this.queuedTickableSounds.clear();
        for (TickableSoundInstance instance : this.tickingSounds) {
            if (!instance.canPlaySound()) {
                this.stop(instance);
            }
            instance.tick();
            if (instance.isStopped()) {
                this.stop(instance);
                continue;
            }
            float volume = this.calculateVolume(instance);
            float pitch = this.calculatePitch(instance);
            Vec3 position = new Vec3(instance.getX(), instance.getY(), instance.getZ());
            ChannelAccess.ChannelHandle handle = this.instanceToChannel.get(instance);
            if (handle == null) continue;
            handle.execute(channel -> {
                channel.setVolume(volume);
                channel.setPitch(pitch);
                channel.setSelfPosition(position);
            });
        }
        Iterator<Map.Entry<SoundInstance, ChannelAccess.ChannelHandle>> iterator = this.instanceToChannel.entrySet().iterator();
        while (iterator.hasNext()) {
            int minDeleteTime;
            Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry = iterator.next();
            ChannelAccess.ChannelHandle handle = entry.getValue();
            SoundInstance instance = entry.getKey();
            if (!handle.isStopped() || (minDeleteTime = this.soundDeleteTime.get(instance).intValue()) > this.tickCount) continue;
            if (SoundEngine.shouldLoopManually(instance)) {
                this.queuedSounds.put(instance, this.tickCount + instance.getDelay());
            }
            iterator.remove();
            LOGGER.debug(MARKER, "Removed channel {} because it's not playing anymore", (Object)handle);
            this.soundDeleteTime.remove(instance);
            try {
                this.instanceBySource.remove((Object)instance.getSource(), (Object)instance);
            }
            catch (RuntimeException runtimeException) {
                // empty catch block
            }
            if (!(instance instanceof TickableSoundInstance)) continue;
            this.tickingSounds.remove(instance);
        }
        Iterator<Map.Entry<SoundInstance, Integer>> queueIterator = this.queuedSounds.entrySet().iterator();
        while (queueIterator.hasNext()) {
            Map.Entry<SoundInstance, Integer> next = queueIterator.next();
            if (this.tickCount < next.getValue()) continue;
            SoundInstance instance = next.getKey();
            if (instance instanceof TickableSoundInstance) {
                TickableSoundInstance tickableSoundInstance = (TickableSoundInstance)instance;
                tickableSoundInstance.tick();
            }
            this.play(instance);
            queueIterator.remove();
        }
    }

    private void tickMusicWhenPaused() {
        Iterator<Map.Entry<SoundInstance, ChannelAccess.ChannelHandle>> iterator = this.instanceToChannel.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry = iterator.next();
            ChannelAccess.ChannelHandle handle = entry.getValue();
            SoundInstance instance = entry.getKey();
            if (instance.getSource() != SoundSource.MUSIC || !handle.isStopped()) continue;
            iterator.remove();
            LOGGER.debug(MARKER, "Removed channel {} because it's not playing anymore", (Object)handle);
            this.soundDeleteTime.remove(instance);
            this.instanceBySource.remove((Object)instance.getSource(), (Object)instance);
        }
    }

    private static boolean requiresManualLooping(SoundInstance instance) {
        return instance.getDelay() > 0;
    }

    private static boolean shouldLoopManually(SoundInstance instance) {
        return instance.isLooping() && SoundEngine.requiresManualLooping(instance);
    }

    private static boolean shouldLoopAutomatically(SoundInstance instance) {
        return instance.isLooping() && !SoundEngine.requiresManualLooping(instance);
    }

    public boolean isActive(SoundInstance instance) {
        if (!this.loaded) {
            return false;
        }
        if (this.soundDeleteTime.containsKey(instance) && this.soundDeleteTime.get(instance) <= this.tickCount) {
            return true;
        }
        return this.instanceToChannel.containsKey(instance);
    }

    public PlayResult play(SoundInstance instance) {
        Sound sound;
        if (!this.loaded) {
            return PlayResult.NOT_STARTED;
        }
        if (!instance.canPlaySound()) {
            return PlayResult.NOT_STARTED;
        }
        WeighedSoundEvents soundEvent = instance.resolve(this.soundManager);
        Identifier eventLocation = instance.getIdentifier();
        if (soundEvent == null) {
            if (ONLY_WARN_ONCE.add(eventLocation)) {
                LOGGER.warn(MARKER, "Unable to play unknown soundEvent: {}", (Object)eventLocation);
            }
            if (!SharedConstants.DEBUG_SUBTITLES) {
                return PlayResult.NOT_STARTED;
            }
            soundEvent = new WeighedSoundEvents(eventLocation, MISSING_SOUND);
        }
        if ((sound = instance.getSound()) == SoundManager.INTENTIONALLY_EMPTY_SOUND) {
            return PlayResult.NOT_STARTED;
        }
        if (sound == SoundManager.EMPTY_SOUND) {
            if (ONLY_WARN_ONCE.add(eventLocation)) {
                LOGGER.warn(MARKER, "Unable to play empty soundEvent: {}", (Object)eventLocation);
            }
            return PlayResult.NOT_STARTED;
        }
        float instanceVolume = instance.getVolume();
        float attenuationDistance = Math.max(instanceVolume, 1.0f) * (float)sound.getAttenuationDistance();
        SoundSource soundSource = instance.getSource();
        float volume = this.calculateVolume(instanceVolume, soundSource);
        float pitch = this.calculatePitch(instance);
        SoundInstance.Attenuation attenuation = instance.getAttenuation();
        boolean isRelative = instance.isRelative();
        if (!this.listeners.isEmpty()) {
            float range = isRelative || attenuation == SoundInstance.Attenuation.NONE ? Float.POSITIVE_INFINITY : attenuationDistance;
            for (SoundEventListener listener : this.listeners) {
                listener.onPlaySound(instance, soundEvent, range);
            }
        }
        boolean startedSilently = false;
        if (volume == 0.0f) {
            if (instance.canStartSilent() || soundSource == SoundSource.MUSIC) {
                startedSilently = true;
            } else {
                LOGGER.debug(MARKER, "Skipped playing sound {}, volume was zero.", (Object)sound.getLocation());
                return PlayResult.NOT_STARTED;
            }
        }
        Vec3 position = new Vec3(instance.getX(), instance.getY(), instance.getZ());
        boolean isLooping = SoundEngine.shouldLoopAutomatically(instance);
        boolean isStreaming = sound.shouldStream();
        CompletableFuture<@Nullable ChannelAccess.ChannelHandle> handleFuture = this.channelAccess.createHandle(sound.shouldStream() ? Library.Pool.STREAMING : Library.Pool.STATIC);
        ChannelAccess.ChannelHandle handle = handleFuture.join();
        if (handle == null) {
            if (SharedConstants.IS_RUNNING_IN_IDE) {
                LOGGER.warn("Failed to create new sound handle");
            }
            return PlayResult.NOT_STARTED;
        }
        LOGGER.debug(MARKER, "Playing sound {} for event {}", (Object)sound.getLocation(), (Object)eventLocation);
        this.soundDeleteTime.put(instance, this.tickCount + 20);
        this.instanceToChannel.put(instance, handle);
        this.instanceBySource.put((Object)soundSource, (Object)instance);
        handle.execute(channel -> {
            channel.setPitch(pitch);
            channel.setVolume(volume);
            if (attenuation == SoundInstance.Attenuation.LINEAR) {
                channel.linearAttenuation(attenuationDistance);
            } else {
                channel.disableAttenuation();
            }
            channel.setLooping(isLooping && !isStreaming);
            channel.setSelfPosition(position);
            channel.setRelative(isRelative);
        });
        if (!isStreaming) {
            this.soundBuffers.getCompleteBuffer(sound.getPath()).thenAccept(soundBuffer -> handle.execute(channel -> {
                channel.attachStaticBuffer((SoundBuffer)soundBuffer);
                channel.play();
            }));
        } else {
            this.soundBuffers.getStream(sound.getPath(), isLooping).thenAccept(stream -> handle.execute(channel -> {
                channel.attachBufferStream((AudioStream)stream);
                channel.play();
            }));
        }
        if (instance instanceof TickableSoundInstance) {
            TickableSoundInstance tickableSoundInstance = (TickableSoundInstance)instance;
            this.tickingSounds.add(tickableSoundInstance);
        }
        if (startedSilently) {
            return PlayResult.STARTED_SILENTLY;
        }
        return PlayResult.STARTED;
    }

    public void queueTickingSound(TickableSoundInstance tickableSoundInstance) {
        this.queuedTickableSounds.add(tickableSoundInstance);
    }

    public void requestPreload(Sound sound) {
        this.preloadQueue.add(sound);
    }

    private float calculatePitch(SoundInstance instance) {
        return Mth.clamp(instance.getPitch(), 0.5f, 2.0f);
    }

    private float calculateVolume(SoundInstance instance) {
        return this.calculateVolume(instance.getVolume(), instance.getSource());
    }

    private float calculateVolume(float volume, SoundSource source) {
        return Mth.clamp(volume, 0.0f, 1.0f) * Mth.clamp(this.options.getFinalSoundSourceVolume(source), 0.0f, 1.0f) * this.gainBySource.getFloat((Object)source);
    }

    public void pauseAllExcept(SoundSource ... ignoredSources) {
        if (!this.loaded) {
            return;
        }
        for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> instance : this.instanceToChannel.entrySet()) {
            if (List.of(ignoredSources).contains((Object)instance.getKey().getSource())) continue;
            instance.getValue().execute(Channel::pause);
        }
    }

    public void resume() {
        if (this.loaded) {
            this.channelAccess.executeOnChannels(channels -> channels.forEach(Channel::unpause));
        }
    }

    public void playDelayed(SoundInstance instance, int delay) {
        this.queuedSounds.put(instance, this.tickCount + delay);
    }

    public void updateSource(Camera camera) {
        if (!this.loaded || !camera.isInitialized()) {
            return;
        }
        ListenerTransform transform = new ListenerTransform(camera.position(), new Vec3(camera.forwardVector()), new Vec3(camera.upVector()));
        this.executor.execute(() -> this.listener.setTransform(transform));
    }

    public void stop(@Nullable Identifier sound, @Nullable SoundSource source) {
        if (source != null) {
            for (SoundInstance instance : this.instanceBySource.get((Object)source)) {
                if (sound != null && !instance.getIdentifier().equals(sound)) continue;
                this.stop(instance);
            }
        } else if (sound == null) {
            this.stopAll();
        } else {
            for (SoundInstance instance : this.instanceToChannel.keySet()) {
                if (!instance.getIdentifier().equals(sound)) continue;
                this.stop(instance);
            }
        }
    }

    public String getChannelDebugString() {
        return this.library.getChannelDebugString();
    }

    public void getSoundCacheDebugStats(SoundBufferLibrary.DebugOutput output) {
        this.soundBuffers.enumerate(output);
    }

    public List<String> getAvailableSoundDevices() {
        return this.deviceTracker.currentDevices().allDevices();
    }

    public ListenerTransform getListenerTransform() {
        return this.listener.getTransform();
    }

    @Environment(value=EnvType.CLIENT)
    public static enum PlayResult {
        STARTED,
        STARTED_SILENTLY,
        NOT_STARTED;

    }
}

