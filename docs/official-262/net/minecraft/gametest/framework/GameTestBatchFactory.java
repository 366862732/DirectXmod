/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Streams
 */
package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.RetryOptions;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;

public class GameTestBatchFactory {
    private static final int MAX_TESTS_PER_BATCH = 50;
    public static final TestDecorator DIRECT = (test, level) -> Stream.of(new GameTestInfo(test, Rotation.NONE, level, RetryOptions.noRetries()));

    public static List<GameTestBatch> divideIntoBatches(Collection<Holder.Reference<GameTestInstance>> allTests, TestDecorator decorator, ServerLevel level) {
        Map<Holder, List<GameTestInfo>> testsPerBatch = allTests.stream().flatMap(test -> decorator.decorate((Holder.Reference<GameTestInstance>)test, level)).collect(Collectors.groupingBy(info -> info.getTest().batch()));
        return testsPerBatch.entrySet().stream().flatMap(e -> {
            Holder batchKey = (Holder)e.getKey();
            List testsInBatch = (List)e.getValue();
            return Streams.mapWithIndex(Lists.partition((List)testsInBatch, (int)50).stream(), (tests, index) -> GameTestBatchFactory.toGameTestBatch(tests, batchKey, (int)index));
        }).toList();
    }

    public static GameTestRunner.GameTestBatcher fromGameTestInfo() {
        return GameTestBatchFactory.fromGameTestInfo(50);
    }

    public static GameTestRunner.GameTestBatcher fromGameTestInfo(int maxTestsPerBatch) {
        return gameTestInfos -> {
            Map<Holder, List<GameTestInfo>> testFunctionsPerBatch = gameTestInfos.stream().filter(Objects::nonNull).collect(Collectors.groupingBy(gameTestInfo -> gameTestInfo.getTest().batch()));
            return testFunctionsPerBatch.entrySet().stream().flatMap(e -> {
                Holder batchKey = (Holder)e.getKey();
                List testsInBatch = (List)e.getValue();
                return Streams.mapWithIndex(Lists.partition((List)testsInBatch, (int)maxTestsPerBatch).stream(), (tests, index) -> GameTestBatchFactory.toGameTestBatch(List.copyOf(tests), batchKey, (int)index));
            }).toList();
        };
    }

    public static GameTestBatch toGameTestBatch(Collection<GameTestInfo> tests, Holder<TestEnvironmentDefinition<?>> batch, int counter) {
        return new GameTestBatch(counter, tests, batch);
    }

    @FunctionalInterface
    public static interface TestDecorator {
        public Stream<GameTestInfo> decorate(Holder.Reference<GameTestInstance> var1, ServerLevel var2);
    }
}

