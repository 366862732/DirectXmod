/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.animal.cow;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.cow.CowSoundVariant;

public class CowSoundVariants {
    public static final ResourceKey<CowSoundVariant> CLASSIC = CowSoundVariants.createKey(SoundSet.CLASSIC);
    public static final ResourceKey<CowSoundVariant> MOODY = CowSoundVariants.createKey(SoundSet.MOODY);

    private static ResourceKey<CowSoundVariant> createKey(SoundSet cowSoundVariant) {
        return ResourceKey.create(Registries.COW_SOUND_VARIANT, Identifier.withDefaultNamespace(cowSoundVariant.getIdentifier()));
    }

    public static void bootstrap(BootstrapContext<CowSoundVariant> context) {
        CowSoundVariants.register(context, CLASSIC, SoundSet.CLASSIC);
        CowSoundVariants.register(context, MOODY, SoundSet.MOODY);
    }

    private static void register(BootstrapContext<CowSoundVariant> context, ResourceKey<CowSoundVariant> key, SoundSet CowSoundVariant2) {
        context.register(key, SoundEvents.COW_SOUNDS.get((Object)CowSoundVariant2));
    }

    public static Holder<CowSoundVariant> pickRandomSoundVariant(RegistryAccess registryAccess, RandomSource random) {
        return registryAccess.lookupOrThrow(Registries.COW_SOUND_VARIANT).getRandom(random).orElseThrow();
    }

    public static enum SoundSet {
        CLASSIC("classic", "cow"),
        MOODY("moody", "cow_moody");

        private final String identifier;
        private final String soundEventIdentifier;

        private SoundSet(String identifier, String soundEventIdentifier) {
            this.identifier = identifier;
            this.soundEventIdentifier = soundEventIdentifier;
        }

        public String getIdentifier() {
            return this.identifier;
        }

        public String getSoundEventIdentifier() {
            return this.soundEventIdentifier;
        }
    }
}

