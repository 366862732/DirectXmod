/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.gametest.framework;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public interface GameTestEnvironments {
    public static final String DEFAULT = "default";
    public static final ResourceKey<TestEnvironmentDefinition<?>> DEFAULT_KEY = GameTestEnvironments.create("default");

    private static ResourceKey<TestEnvironmentDefinition<?>> create(String name) {
        return ResourceKey.create(Registries.TEST_ENVIRONMENT, Identifier.withDefaultNamespace(name));
    }

    public static void bootstrap(BootstrapContext<TestEnvironmentDefinition<?>> context) {
        context.register(DEFAULT_KEY, new TestEnvironmentDefinition.AllOf(List.of()));
    }
}

