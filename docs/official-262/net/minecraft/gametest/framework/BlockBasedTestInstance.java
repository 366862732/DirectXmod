/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.gametest.framework;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TestBlock;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.TestBlockMode;

public class BlockBasedTestInstance
extends GameTestInstance {
    public static final MapCodec<BlockBasedTestInstance> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)TestData.CODEC.forGetter(GameTestInstance::info)).apply((Applicative)i, BlockBasedTestInstance::new));

    public BlockBasedTestInstance(TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
        super(testData);
    }

    @Override
    public void run(GameTestHelper helper) {
        BlockPos startPos = this.findStartBlock(helper);
        TestBlockEntity blockEntity = helper.getBlockEntity(startPos, TestBlockEntity.class);
        blockEntity.trigger();
        helper.onEachTick(() -> {
            boolean acceptTriggered;
            List<BlockPos> acceptBlocks = this.findTestBlocks(helper, TestBlockMode.ACCEPT);
            if (acceptBlocks.isEmpty()) {
                helper.fail(Component.translatable("test_block.error.missing", TestBlockMode.ACCEPT.getDisplayName()));
            }
            if (acceptTriggered = acceptBlocks.stream().map(pos -> helper.getBlockEntity((BlockPos)pos, TestBlockEntity.class)).anyMatch(TestBlockEntity::hasTriggered)) {
                helper.succeed();
            } else {
                this.forAllTriggeredTestBlocks(helper, TestBlockMode.FAIL, failEntity -> helper.fail(Component.literal(failEntity.getMessage())));
                this.forAllTriggeredTestBlocks(helper, TestBlockMode.LOG, TestBlockEntity::trigger);
            }
        });
    }

    private void forAllTriggeredTestBlocks(GameTestHelper helper, TestBlockMode mode, Consumer<TestBlockEntity> action) {
        List<BlockPos> failBlocks = this.findTestBlocks(helper, mode);
        for (BlockPos failBlock : failBlocks) {
            TestBlockEntity blockEntity = helper.getBlockEntity(failBlock, TestBlockEntity.class);
            if (!blockEntity.hasTriggered()) continue;
            action.accept(blockEntity);
            blockEntity.reset();
        }
    }

    private BlockPos findStartBlock(GameTestHelper helper) {
        List<BlockPos> testBlocks = this.findTestBlocks(helper, TestBlockMode.START);
        if (testBlocks.isEmpty()) {
            helper.fail(Component.translatable("test_block.error.missing", TestBlockMode.START.getDisplayName()));
        }
        if (testBlocks.size() != 1) {
            helper.fail(Component.translatable("test_block.error.too_many", TestBlockMode.START.getDisplayName()));
        }
        return testBlocks.getFirst();
    }

    private List<BlockPos> findTestBlocks(GameTestHelper helper, TestBlockMode mode) {
        ArrayList<BlockPos> blocks = new ArrayList<BlockPos>();
        helper.forEveryBlockInStructure(pos -> {
            BlockState state = helper.getBlockState((BlockPos)pos);
            if (state.is(Blocks.TEST_BLOCK) && state.getValue(TestBlock.MODE) == mode) {
                blocks.add(pos.immutable());
            }
        });
        return blocks;
    }

    public MapCodec<BlockBasedTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.translatable("test_instance.type.block_based");
    }
}

