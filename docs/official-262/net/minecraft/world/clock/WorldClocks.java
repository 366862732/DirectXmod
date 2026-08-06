/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.clock;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.clock.WorldClock;

public interface WorldClocks {
    public static final ResourceKey<WorldClock> OVERWORLD = WorldClocks.key("overworld");
    public static final ResourceKey<WorldClock> THE_END = WorldClocks.key("the_end");

    public static void bootstrap(BootstrapContext<WorldClock> context) {
        context.register(OVERWORLD, new WorldClock());
        context.register(THE_END, new WorldClock());
    }

    private static ResourceKey<WorldClock> key(String id) {
        return ResourceKey.create(Registries.WORLD_CLOCK, Identifier.withDefaultNamespace(id));
    }
}

