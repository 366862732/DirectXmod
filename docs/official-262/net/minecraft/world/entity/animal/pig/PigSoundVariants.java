/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.animal.pig;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.pig.PigSoundVariant;

public class PigSoundVariants {
    public static final ResourceKey<PigSoundVariant> CLASSIC = PigSoundVariants.createKey(SoundSet.CLASSIC);
    public static final ResourceKey<PigSoundVariant> MINI = PigSoundVariants.createKey(SoundSet.MINI);
    public static final ResourceKey<PigSoundVariant> BIG = PigSoundVariants.createKey(SoundSet.BIG);

    private static ResourceKey<PigSoundVariant> createKey(SoundSet pigSoundVariant) {
        return ResourceKey.create(Registries.PIG_SOUND_VARIANT, Identifier.withDefaultNamespace(pigSoundVariant.getIdentifier()));
    }

    public static void bootstrap(BootstrapContext<PigSoundVariant> context) {
        PigSoundVariants.register(context, CLASSIC, SoundSet.CLASSIC);
        PigSoundVariants.register(context, BIG, SoundSet.BIG);
        PigSoundVariants.register(context, MINI, SoundSet.MINI);
    }

    private static void register(BootstrapContext<PigSoundVariant> context, ResourceKey<PigSoundVariant> key, SoundSet PigSoundVariant2) {
        context.register(key, SoundEvents.PIG_SOUNDS.get((Object)PigSoundVariant2));
    }

    public static Holder<PigSoundVariant> pickRandomSoundVariant(RegistryAccess registryAccess, RandomSource random) {
        return registryAccess.lookupOrThrow(Registries.PIG_SOUND_VARIANT).getRandom(random).orElseThrow();
    }

    public static enum SoundSet {
        CLASSIC("classic", "pig"),
        MINI("mini", "pig_mini"),
        BIG("big", "pig_big");

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

