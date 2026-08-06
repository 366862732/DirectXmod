/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.context.CommandContext
 */
package net.minecraft.gametest.framework;

import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.FailedTestTracker;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestInstanceFinder;
import net.minecraft.gametest.framework.TestPosFinder;

public class TestFinder
implements TestInstanceFinder,
TestPosFinder {
    private static final TestInstanceFinder NO_FUNCTIONS = Stream::empty;
    private static final TestPosFinder NO_STRUCTURES = Stream::empty;
    private final TestInstanceFinder testInstanceFinder;
    private final TestPosFinder testPosFinder;
    private final CommandSourceStack source;

    @Override
    public Stream<BlockPos> findTestPos() {
        return this.testPosFinder.findTestPos();
    }

    public static Builder builder() {
        return new Builder();
    }

    private TestFinder(CommandSourceStack source, TestInstanceFinder testInstanceFinder, TestPosFinder testPosFinder) {
        this.source = source;
        this.testInstanceFinder = testInstanceFinder;
        this.testPosFinder = testPosFinder;
    }

    public CommandSourceStack source() {
        return this.source;
    }

    @Override
    public Stream<Holder.Reference<GameTestInstance>> findTests() {
        return this.testInstanceFinder.findTests();
    }

    public static class Builder {
        private final UnaryOperator<Supplier<Stream<Holder.Reference<GameTestInstance>>>> testFinderWrapper;
        private final UnaryOperator<Supplier<Stream<BlockPos>>> structureBlockPosFinderWrapper;

        public Builder() {
            this.testFinderWrapper = f -> f;
            this.structureBlockPosFinderWrapper = f -> f;
        }

        private Builder(UnaryOperator<Supplier<Stream<Holder.Reference<GameTestInstance>>>> testFinderWrapper, UnaryOperator<Supplier<Stream<BlockPos>>> structureBlockPosFinderWrapper) {
            this.testFinderWrapper = testFinderWrapper;
            this.structureBlockPosFinderWrapper = structureBlockPosFinderWrapper;
        }

        public Builder createMultipleCopies(int amount) {
            return new Builder(Builder.createCopies(amount), Builder.createCopies(amount));
        }

        private static <Q> UnaryOperator<Supplier<Stream<Q>>> createCopies(int amount) {
            return source -> {
                LinkedList copyList = new LinkedList();
                List sourceList = ((Stream)source.get()).toList();
                for (int i = 0; i < amount; ++i) {
                    copyList.addAll(sourceList);
                }
                return copyList::stream;
            };
        }

        private TestFinder build(CommandSourceStack source, TestInstanceFinder testInstanceFinder, TestPosFinder testPosFinder) {
            return new TestFinder(source, ((Supplier)((Supplier)this.testFinderWrapper.apply(testInstanceFinder::findTests)))::get, ((Supplier)((Supplier)this.structureBlockPosFinderWrapper.apply(testPosFinder::findTestPos)))::get);
        }

        public TestFinder radius(CommandContext<CommandSourceStack> sourceStack, int radius) {
            CommandSourceStack source = (CommandSourceStack)sourceStack.getSource();
            BlockPos pos = BlockPos.containing(source.getPosition());
            return this.build(source, NO_FUNCTIONS, () -> StructureUtils.findTestBlocks(pos, radius, source.getLevel()));
        }

        public TestFinder nearest(CommandContext<CommandSourceStack> sourceStack) {
            CommandSourceStack source = (CommandSourceStack)sourceStack.getSource();
            BlockPos pos = BlockPos.containing(source.getPosition());
            return this.build(source, NO_FUNCTIONS, () -> StructureUtils.findNearestTest(pos, 15, source.getLevel()).stream());
        }

        public TestFinder allNearby(CommandContext<CommandSourceStack> sourceStack) {
            CommandSourceStack source = (CommandSourceStack)sourceStack.getSource();
            BlockPos pos = BlockPos.containing(source.getPosition());
            return this.build(source, NO_FUNCTIONS, () -> StructureUtils.findTestBlocks(pos, 250, source.getLevel()));
        }

        public TestFinder lookedAt(CommandContext<CommandSourceStack> sourceStack) {
            CommandSourceStack source = (CommandSourceStack)sourceStack.getSource();
            return this.build(source, NO_FUNCTIONS, () -> StructureUtils.lookedAtTestPos(BlockPos.containing(source.getPosition()), source.getPlayer().getCamera(), source.getLevel()));
        }

        public TestFinder failedTests(CommandContext<CommandSourceStack> sourceStack, boolean onlyRequiredTests) {
            return this.build((CommandSourceStack)sourceStack.getSource(), () -> FailedTestTracker.getLastFailedTests().filter(test -> !onlyRequiredTests || ((GameTestInstance)test.value()).required()), NO_STRUCTURES);
        }

        public TestFinder byResourceSelection(CommandContext<CommandSourceStack> sourceStack, Collection<Holder.Reference<GameTestInstance>> holders) {
            return this.build((CommandSourceStack)sourceStack.getSource(), holders::stream, NO_STRUCTURES);
        }

        public TestFinder failedTests(CommandContext<CommandSourceStack> sourceStack) {
            return this.failedTests(sourceStack, false);
        }
    }
}

