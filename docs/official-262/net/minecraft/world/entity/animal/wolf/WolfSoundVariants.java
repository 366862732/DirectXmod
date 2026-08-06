/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.animal.wolf;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;

public class WolfSoundVariants {
    public static final ResourceKey<WolfSoundVariant> CLASSIC = WolfSoundVariants.createKey(SoundSet.CLASSIC);
    public static final ResourceKey<WolfSoundVariant> PUGLIN = WolfSoundVariants.createKey(SoundSet.PUGLIN);
    public static final ResourceKey<WolfSoundVariant> SAD = WolfSoundVariants.createKey(SoundSet.SAD);
    public static final ResourceKey<WolfSoundVariant> ANGRY = WolfSoundVariants.createKey(SoundSet.ANGRY);
    public static final ResourceKey<WolfSoundVariant> GRUMPY = WolfSoundVariants.createKey(SoundSet.GRUMPY);
    public static final ResourceKey<WolfSoundVariant> BIG = WolfSoundVariants.createKey(SoundSet.BIG);
    public static final ResourceKey<WolfSoundVariant> CUTE = WolfSoundVariants.createKey(SoundSet.CUTE);

    private static ResourceKey<WolfSoundVariant> createKey(SoundSet wolfSoundVariant) {
        return ResourceKey.create(Registries.WOLF_SOUND_VARIANT, Identifier.withDefaultNamespace(wolfSoundVariant.getIdentifier()));
    }

    public static void bootstrap(BootstrapContext<WolfSoundVariant> context) {
        WolfSoundVariants.register(context, CLASSIC, SoundSet.CLASSIC);
        WolfSoundVariants.register(context, PUGLIN, SoundSet.PUGLIN);
        WolfSoundVariants.register(context, SAD, SoundSet.SAD);
        WolfSoundVariants.register(context, ANGRY, SoundSet.ANGRY);
        WolfSoundVariants.register(context, GRUMPY, SoundSet.GRUMPY);
        WolfSoundVariants.register(context, BIG, SoundSet.BIG);
        WolfSoundVariants.register(context, CUTE, SoundSet.CUTE);
    }

    private static void register(BootstrapContext<WolfSoundVariant> context, ResourceKey<WolfSoundVariant> key, SoundSet wolfSoundVariant) {
        context.register(key, SoundEvents.WOLF_SOUNDS.get((Object)wolfSoundVariant));
    }

    public static Holder<WolfSoundVariant> pickRandomSoundVariant(RegistryAccess registryAccess, RandomSource random) {
        return registryAccess.lookupOrThrow(Registries.WOLF_SOUND_VARIANT).getRandom(random).orElseThrow();
    }

    public static enum SoundSet {
        CLASSIC("classic", "wolf"),
        PUGLIN("puglin", "wolf_puglin"),
        SAD("sad", "wolf_sad"),
        ANGRY("angry", "wolf_angry"),
        GRUMPY("grumpy", "wolf_grumpy"),
        BIG("big", "wolf_big"),
        CUTE("cute", "wolf_cute");

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

