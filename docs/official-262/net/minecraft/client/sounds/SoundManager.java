/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.reflect.TypeToken
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.sounds;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.audio.ListenerTransform;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.resources.sounds.SoundEventRegistrationSerializer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.MultipliedFloats;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class SoundManager
extends SimplePreparableReloadListener<Preparations> {
    public static final Identifier EMPTY_SOUND_LOCATION = Identifier.withDefaultNamespace("empty");
    public static final Sound EMPTY_SOUND = new Sound(EMPTY_SOUND_LOCATION, ConstantFloat.of(1.0f), ConstantFloat.of(1.0f), 1, Sound.Type.FILE, false, false, 16);
    public static final Identifier INTENTIONALLY_EMPTY_SOUND_LOCATION = Identifier.withDefaultNamespace("intentionally_empty");
    public static final WeighedSoundEvents INTENTIONALLY_EMPTY_SOUND_EVENT = new WeighedSoundEvents(INTENTIONALLY_EMPTY_SOUND_LOCATION, null);
    public static final Sound INTENTIONALLY_EMPTY_SOUND = new Sound(INTENTIONALLY_EMPTY_SOUND_LOCATION, ConstantFloat.of(1.0f), ConstantFloat.of(1.0f), 1, Sound.Type.FILE, false, false, 16);
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SOUNDS_PATH = "sounds.json";
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(SoundEventRegistration.class, (Object)new SoundEventRegistrationSerializer()).create();
    private static final TypeToken<Map<String, SoundEventRegistration>> SOUND_EVENT_REGISTRATION_TYPE = new TypeToken<Map<String, SoundEventRegistration>>(){};
    private final Map<Identifier, WeighedSoundEvents> registry = Maps.newHashMap();
    private final SoundEngine soundEngine;
    private final Map<Identifier, Resource> soundCache = new HashMap<Identifier, Resource>();

    public SoundManager(Options options) {
        this.soundEngine = new SoundEngine(this, options, ResourceProvider.fromMap(this.soundCache));
    }

    @Override
    protected Preparations prepare(ResourceManager manager, ProfilerFiller profiler) {
        Preparations preparations = new Preparations();
        try (Zone ignored = profiler.zone("list");){
            preparations.listResources(manager);
        }
        for (String namespace : manager.getNamespaces()) {
            try {
                Zone ignored = profiler.zone(namespace);
                try {
                    List<Resource> resources = manager.getResourceStack(Identifier.fromNamespaceAndPath(namespace, SOUNDS_PATH));
                    for (Resource resource : resources) {
                        profiler.push(resource.sourcePackId());
                        try (BufferedReader reader = resource.openAsReader();){
                            profiler.push("parse");
                            Map<String, SoundEventRegistration> map = GsonHelper.fromJson(GSON, (Reader)reader, SOUND_EVENT_REGISTRATION_TYPE);
                            profiler.popPush("register");
                            for (Map.Entry<String, SoundEventRegistration> entry : map.entrySet()) {
                                preparations.handleRegistration(Identifier.fromNamespaceAndPath(namespace, entry.getKey()), entry.getValue());
                            }
                            profiler.pop();
                        }
                        catch (RuntimeException e) {
                            LOGGER.warn("Invalid {} in resourcepack: '{}'", new Object[]{SOUNDS_PATH, resource.sourcePackId(), e});
                        }
                        profiler.pop();
                    }
                }
                finally {
                    if (ignored == null) continue;
                    ignored.close();
                }
            }
            catch (IOException iOException) {}
        }
        return preparations;
    }

    @Override
    protected void apply(Preparations preparations, ResourceManager manager, ProfilerFiller profiler) {
        preparations.apply(this.registry, this.soundCache, this.soundEngine);
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            for (Identifier location : this.registry.keySet()) {
                WeighedSoundEvents event = this.registry.get(location);
                if (ComponentUtils.isTranslationResolvable(event.getSubtitle()) || !BuiltInRegistries.SOUND_EVENT.containsKey(location)) continue;
                LOGGER.error("Missing subtitle {} for sound event: {}", (Object)event.getSubtitle(), (Object)location);
            }
        }
        if (LOGGER.isDebugEnabled()) {
            for (Identifier location : this.registry.keySet()) {
                if (BuiltInRegistries.SOUND_EVENT.containsKey(location)) continue;
                LOGGER.debug("Not having sound event for: {}", (Object)location);
            }
        }
        this.soundEngine.reload();
    }

    public List<String> getAvailableSoundDevices() {
        return this.soundEngine.getAvailableSoundDevices();
    }

    public ListenerTransform getListenerTransform() {
        return this.soundEngine.getListenerTransform();
    }

    private static boolean validateSoundResource(Sound sound, Identifier eventLocation, ResourceProvider resourceProvider) {
        Identifier soundPath = sound.getPath();
        if (resourceProvider.getResource(soundPath).isEmpty()) {
            LOGGER.warn("File {} does not exist, cannot add it to event {}", (Object)soundPath, (Object)eventLocation);
            return false;
        }
        return true;
    }

    public @Nullable WeighedSoundEvents getSoundEvent(Identifier location) {
        return this.registry.get(location);
    }

    public Collection<Identifier> getAvailableSounds() {
        return this.registry.keySet();
    }

    public void queueTickingSound(TickableSoundInstance instance) {
        this.soundEngine.queueTickingSound(instance);
    }

    public SoundEngine.PlayResult play(SoundInstance instance) {
        return this.soundEngine.play(instance);
    }

    public void playDelayed(SoundInstance instance, int delay) {
        this.soundEngine.playDelayed(instance, delay);
    }

    public void updateSource(Camera camera) {
        this.soundEngine.updateSource(camera);
    }

    public void pauseAllExcept(SoundSource ... ignoredSources) {
        this.soundEngine.pauseAllExcept(ignoredSources);
    }

    public void stop() {
        this.soundEngine.stopAll();
    }

    public void destroy() {
        this.soundEngine.destroy();
    }

    public void emergencyShutdown() {
        this.soundEngine.emergencyShutdown();
    }

    public void tick(boolean paused) {
        this.soundEngine.tick(paused);
    }

    public void resume() {
        this.soundEngine.resume();
    }

    public void refreshCategoryVolume(SoundSource category) {
        this.soundEngine.refreshCategoryVolume(category);
    }

    public void stop(SoundInstance soundInstance) {
        this.soundEngine.stop(soundInstance);
    }

    public void updateCategoryVolume(SoundSource source, float gain) {
        this.soundEngine.updateCategoryVolume(source, gain);
    }

    public boolean isActive(SoundInstance instance) {
        return this.soundEngine.isActive(instance);
    }

    public void addListener(SoundEventListener listener) {
        this.soundEngine.addEventListener(listener);
    }

    public void removeListener(SoundEventListener listener) {
        this.soundEngine.removeEventListener(listener);
    }

    public void stop(@Nullable Identifier sound, @Nullable SoundSource source) {
        this.soundEngine.stop(sound, source);
    }

    public String getChannelDebugString() {
        return this.soundEngine.getChannelDebugString();
    }

    public void getSoundCacheDebugStats(SoundBufferLibrary.DebugOutput output) {
        this.soundEngine.getSoundCacheDebugStats(output);
    }

    public void reload() {
        this.soundEngine.reload();
    }

    @Environment(value=EnvType.CLIENT)
    protected static class Preparations {
        private final Map<Identifier, WeighedSoundEvents> registry = Maps.newHashMap();
        private Map<Identifier, Resource> soundCache = Map.of();

        protected Preparations() {
        }

        private void listResources(ResourceManager resourceManager) {
            this.soundCache = Sound.SOUND_LISTER.listMatchingResources(resourceManager);
        }

        private void handleRegistration(Identifier eventLocation, SoundEventRegistration soundEventRegistration) {
            boolean missesRegistration;
            WeighedSoundEvents registration = this.registry.get(eventLocation);
            boolean bl = missesRegistration = registration == null;
            if (missesRegistration || soundEventRegistration.isReplace()) {
                if (!missesRegistration) {
                    LOGGER.debug("Replaced sound event location {}", (Object)eventLocation);
                }
                registration = new WeighedSoundEvents(eventLocation, soundEventRegistration.getSubtitle());
                this.registry.put(eventLocation, registration);
            }
            ResourceProvider cachedProvider = ResourceProvider.fromMap(this.soundCache);
            block4: for (final Sound sound : soundEventRegistration.getSounds()) {
                final Identifier soundLocation = sound.getLocation();
                registration.addSound(switch (sound.getType()) {
                    case Sound.Type.FILE -> {
                        if (!SoundManager.validateSoundResource(sound, eventLocation, cachedProvider)) continue block4;
                        yield sound;
                    }
                    case Sound.Type.SOUND_EVENT -> new Weighted<Sound>(this){
                        final /* synthetic */ Preparations this$0;
                        {
                            Preparations preparations = this$0;
                            Objects.requireNonNull(preparations);
                            this.this$0 = preparations;
                        }

                        @Override
                        public int getWeight() {
                            WeighedSoundEvents registration = this.this$0.registry.get(soundLocation);
                            return registration == null ? 0 : registration.getWeight();
                        }

                        @Override
                        public Sound getSound(RandomSource random) {
                            WeighedSoundEvents registration = this.this$0.registry.get(soundLocation);
                            if (registration == null) {
                                return EMPTY_SOUND;
                            }
                            Sound wrappedSound = registration.getSound(random);
                            return new Sound(wrappedSound.getLocation(), new MultipliedFloats(wrappedSound.getVolume(), sound.getVolume()), new MultipliedFloats(wrappedSound.getPitch(), sound.getPitch()), sound.getWeight(), Sound.Type.FILE, wrappedSound.shouldStream() || sound.shouldStream(), wrappedSound.shouldPreload(), wrappedSound.getAttenuationDistance());
                        }

                        @Override
                        public void preloadIfRequired(SoundEngine soundEngine) {
                            WeighedSoundEvents registration = this.this$0.registry.get(soundLocation);
                            if (registration == null) {
                                return;
                            }
                            registration.preloadIfRequired(soundEngine);
                        }
                    };
                    default -> throw new IllegalStateException("Unknown SoundEventRegistration type: " + String.valueOf((Object)sound.getType()));
                });
            }
        }

        public void apply(Map<Identifier, WeighedSoundEvents> registry, Map<Identifier, Resource> soundCache, SoundEngine engine) {
            registry.clear();
            soundCache.clear();
            soundCache.putAll(this.soundCache);
            for (Map.Entry<Identifier, WeighedSoundEvents> entry : this.registry.entrySet()) {
                registry.put(entry.getKey(), entry.getValue());
                entry.getValue().preloadIfRequired(engine);
            }
        }
    }
}

