/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.animal.feline;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.feline.CatSoundVariant;

public class CatSoundVariants {
    public static final ResourceKey<CatSoundVariant> CLASSIC = CatSoundVariants.createKey(SoundSet.CLASSIC);
    public static final ResourceKey<CatSoundVariant> ROYAL = CatSoundVariants.createKey(SoundSet.ROYAL);

    private static ResourceKey<CatSoundVariant> createKey(SoundSet catSoundVariant) {
        return ResourceKey.create(Registries.CAT_SOUND_VARIANT, Identifier.withDefaultNamespace(catSoundVariant.getIdentifier()));
    }

    public static void bootstrap(BootstrapContext<CatSoundVariant> context) {
        CatSoundVariants.register(context, CLASSIC, SoundSet.CLASSIC);
        CatSoundVariants.register(context, ROYAL, SoundSet.ROYAL);
    }

    private static void register(BootstrapContext<CatSoundVariant> context, ResourceKey<CatSoundVariant> key, SoundSet CatSoundVariant2) {
        context.register(key, SoundEvents.CAT_SOUNDS.get((Object)CatSoundVariant2));
    }

    public static Holder<CatSoundVariant> pickRandomSoundVariant(RegistryAccess registryAccess, RandomSource random) {
        return registryAccess.lookupOrThrow(Registries.CAT_SOUND_VARIANT).getRandom(random).orElseThrow();
    }

    public static enum SoundSet {
        CLASSIC("classic", "cat"),
        ROYAL("royal", "cat_royal");

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

