/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.equipment.trim;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.equipment.trim.TrimPattern;

public class TrimPatterns {
    public static final ResourceKey<TrimPattern> SENTRY = TrimPatterns.registryKey("sentry");
    public static final ResourceKey<TrimPattern> DUNE = TrimPatterns.registryKey("dune");
    public static final ResourceKey<TrimPattern> COAST = TrimPatterns.registryKey("coast");
    public static final ResourceKey<TrimPattern> WILD = TrimPatterns.registryKey("wild");
    public static final ResourceKey<TrimPattern> WARD = TrimPatterns.registryKey("ward");
    public static final ResourceKey<TrimPattern> EYE = TrimPatterns.registryKey("eye");
    public static final ResourceKey<TrimPattern> VEX = TrimPatterns.registryKey("vex");
    public static final ResourceKey<TrimPattern> TIDE = TrimPatterns.registryKey("tide");
    public static final ResourceKey<TrimPattern> SNOUT = TrimPatterns.registryKey("snout");
    public static final ResourceKey<TrimPattern> RIB = TrimPatterns.registryKey("rib");
    public static final ResourceKey<TrimPattern> SPIRE = TrimPatterns.registryKey("spire");
    public static final ResourceKey<TrimPattern> WAYFINDER = TrimPatterns.registryKey("wayfinder");
    public static final ResourceKey<TrimPattern> SHAPER = TrimPatterns.registryKey("shaper");
    public static final ResourceKey<TrimPattern> SILENCE = TrimPatterns.registryKey("silence");
    public static final ResourceKey<TrimPattern> RAISER = TrimPatterns.registryKey("raiser");
    public static final ResourceKey<TrimPattern> HOST = TrimPatterns.registryKey("host");
    public static final ResourceKey<TrimPattern> FLOW = TrimPatterns.registryKey("flow");
    public static final ResourceKey<TrimPattern> BOLT = TrimPatterns.registryKey("bolt");

    public static void bootstrap(BootstrapContext<TrimPattern> context) {
        TrimPatterns.register(context, SENTRY);
        TrimPatterns.register(context, DUNE);
        TrimPatterns.register(context, COAST);
        TrimPatterns.register(context, WILD);
        TrimPatterns.register(context, WARD);
        TrimPatterns.register(context, EYE);
        TrimPatterns.register(context, VEX);
        TrimPatterns.register(context, TIDE);
        TrimPatterns.register(context, SNOUT);
        TrimPatterns.register(context, RIB);
        TrimPatterns.register(context, SPIRE);
        TrimPatterns.register(context, WAYFINDER);
        TrimPatterns.register(context, SHAPER);
        TrimPatterns.register(context, SILENCE);
        TrimPatterns.register(context, RAISER);
        TrimPatterns.register(context, HOST);
        TrimPatterns.register(context, FLOW);
        TrimPatterns.register(context, BOLT);
    }

    public static void register(BootstrapContext<TrimPattern> context, ResourceKey<TrimPattern> registryKey) {
        TrimPattern pattern = new TrimPattern(TrimPatterns.defaultAssetId(registryKey), Component.translatable(Util.makeDescriptionId("trim_pattern", registryKey.identifier())), false);
        context.register(registryKey, pattern);
    }

    private static ResourceKey<TrimPattern> registryKey(String id) {
        return ResourceKey.create(Registries.TRIM_PATTERN, Identifier.withDefaultNamespace(id));
    }

    public static Identifier defaultAssetId(ResourceKey<TrimPattern> registryKey) {
        return registryKey.identifier();
    }
}

