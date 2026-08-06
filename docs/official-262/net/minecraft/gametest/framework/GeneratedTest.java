/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.gametest.framework;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public record GeneratedTest(Map<Identifier, TestData<ResourceKey<TestEnvironmentDefinition<?>>>> tests, ResourceKey<Consumer<GameTestHelper>> functionKey, Consumer<GameTestHelper> function) {
    public GeneratedTest(Map<Identifier, TestData<ResourceKey<TestEnvironmentDefinition<?>>>> tests, Identifier functionId, Consumer<GameTestHelper> function) {
        this(tests, ResourceKey.create(Registries.TEST_FUNCTION, functionId), function);
    }

    public GeneratedTest(Identifier id, TestData<ResourceKey<TestEnvironmentDefinition<?>>> testData, Consumer<GameTestHelper> function) {
        this(Map.of(id, testData), id, function);
    }
}

