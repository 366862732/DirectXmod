/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Queues
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.particle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ElderGuardianParticleGroup;
import net.minecraft.client.particle.ItemPickupParticleGroup;
import net.minecraft.client.particle.NoRenderParticleGroup;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ParticleEngine {
    private static final List<ParticleRenderType> RENDER_ORDER = List.of(ParticleRenderType.SINGLE_QUADS, ParticleRenderType.ITEM_PICKUP, ParticleRenderType.ELDER_GUARDIANS);
    protected ClientLevel level;
    private final Map<ParticleRenderType, ParticleGroup<?>> particles = Maps.newIdentityHashMap();
    private final Queue<TrackingEmitter> trackingEmitters = Queues.newArrayDeque();
    private final Queue<Particle> particlesToAdd = Queues.newArrayDeque();
    private final Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts = new Object2IntOpenHashMap();
    private final ParticleResources resourceManager;
    private final RandomSource random = RandomSource.create();

    public ParticleEngine(ClientLevel level, ParticleResources resourceManager) {
        this.level = level;
        this.resourceManager = resourceManager;
    }

    public void createTrackingEmitter(Entity entity, ParticleOptions particle) {
        this.trackingEmitters.add(new TrackingEmitter(this.level, entity, particle));
    }

    public void createTrackingEmitter(Entity entity, ParticleOptions particle, int lifeTime) {
        this.trackingEmitters.add(new TrackingEmitter(this.level, entity, particle, lifeTime));
    }

    public @Nullable Particle createParticle(ParticleOptions options, double x, double y, double z, double xa, double ya, double za) {
        Particle particle = this.makeParticle(options, x, y, z, xa, ya, za);
        if (particle != null) {
            this.add(particle);
            return particle;
        }
        return null;
    }

    private <T extends ParticleOptions> @Nullable Particle makeParticle(T options, double x, double y, double z, double xa, double ya, double za) {
        ParticleProvider provider = (ParticleProvider)this.resourceManager.getProviders().get(BuiltInRegistries.PARTICLE_TYPE.getId(options.getType()));
        if (provider == null) {
            return null;
        }
        return provider.createParticle(options, this.level, x, y, z, xa, ya, za, this.random);
    }

    public void add(Particle p) {
        Optional<ParticleLimit> limit = p.getParticleLimit();
        if (limit.isPresent()) {
            if (this.hasSpaceInParticleLimit(limit.get())) {
                this.particlesToAdd.add(p);
                this.updateCount(limit.get(), 1);
            }
        } else {
            this.particlesToAdd.add(p);
        }
    }

    public void tick() {
        this.particles.forEach((type, group) -> {
            Profiler.get().push(type.name());
            group.tickParticles();
            Profiler.get().pop();
        });
        if (!this.trackingEmitters.isEmpty()) {
            ArrayList removed = Lists.newArrayList();
            for (TrackingEmitter emitter : this.trackingEmitters) {
                emitter.tick();
                if (emitter.isAlive()) continue;
                removed.add(emitter);
            }
            this.trackingEmitters.removeAll(removed);
        }
        if (!this.particlesToAdd.isEmpty()) {
            Particle particle;
            while ((particle = this.particlesToAdd.poll()) != null) {
                if (this.particles.computeIfAbsent(particle.getGroup(), this::createParticleGroup).add(particle)) continue;
                particle.getParticleLimit().ifPresent(options -> this.updateCount((ParticleLimit)options, -1));
            }
        }
    }

    private ParticleGroup<?> createParticleGroup(ParticleRenderType type) {
        if (type == ParticleRenderType.ITEM_PICKUP) {
            return new ItemPickupParticleGroup(this);
        }
        if (type == ParticleRenderType.ELDER_GUARDIANS) {
            return new ElderGuardianParticleGroup(this);
        }
        if (type == ParticleRenderType.NO_RENDER) {
            return new NoRenderParticleGroup(this);
        }
        return new QuadParticleGroup(this, type);
    }

    protected void updateCount(ParticleLimit limit, int change) {
        this.trackedParticleCounts.addTo((Object)limit, change);
    }

    public void extract(ParticlesRenderState particlesRenderState, Frustum frustum, Camera camera, float partialTickTime) {
        for (ParticleRenderType particleType : RENDER_ORDER) {
            ParticleGroup<?> particles = this.particles.get(particleType);
            if (particles == null || particles.isEmpty()) continue;
            particlesRenderState.add(particles.extractRenderState(frustum, camera, partialTickTime));
        }
    }

    public void setLevel(@Nullable ClientLevel level) {
        this.level = level;
        this.clearParticles();
        this.trackingEmitters.clear();
    }

    public String countParticles() {
        StringBuilder builder = new StringBuilder();
        int total = 0;
        for (Map.Entry<ParticleRenderType, ParticleGroup<?>> group : this.particles.entrySet()) {
            builder.append(group.getKey().shorthand()).append(" ");
            int size = group.getValue().size();
            builder.append(size).append(" ");
            total += size;
        }
        builder.append("T ");
        builder.append(total);
        return builder.toString();
    }

    private boolean hasSpaceInParticleLimit(ParticleLimit limit) {
        return this.trackedParticleCounts.getInt((Object)limit) < limit.limit();
    }

    public void clearParticles() {
        this.particles.clear();
        this.particlesToAdd.clear();
        this.trackingEmitters.clear();
        this.trackedParticleCounts.clear();
    }

    public RandomSource getRandom() {
        return this.random;
    }
}

