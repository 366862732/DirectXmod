/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.data.tags;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.references.BlockItemId;
import net.minecraft.tags.BlockItemTagId;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public abstract class BlockItemTagsProvider {
    private final Function<BlockItemTagId, CombinedAppender> tagSupplier;

    protected BlockItemTagsProvider(Function<BlockItemTagId, CombinedAppender> tagSupplier) {
        this.tagSupplier = tagSupplier;
    }

    protected CombinedAppender tag(BlockItemTagId tag) {
        return this.tagSupplier.apply(tag);
    }

    protected abstract void run();

    public static CombinedAppender wrapForBlocks(final TagAppender<Block> appender) {
        return new CombinedAppender(){

            @Override
            public CombinedAppender addAll(Stream<BlockItemId> ids) {
                appender.addAll(ids.map(BlockItemId::block));
                return this;
            }

            @Override
            public CombinedAppender addTag(BlockItemTagId id) {
                appender.addTag(id.block());
                return this;
            }
        };
    }

    public static CombinedAppender wrapForItems(final TagAppender<Item> appender) {
        return new CombinedAppender(){

            @Override
            public CombinedAppender addAll(Stream<BlockItemId> ids) {
                appender.addAll(ids.map(BlockItemId::item));
                return this;
            }

            @Override
            public CombinedAppender addTag(BlockItemTagId id) {
                appender.addTag(id.item());
                return this;
            }
        };
    }

    public static interface CombinedAppender {
        public CombinedAppender addAll(Stream<BlockItemId> var1);

        public CombinedAppender addTag(BlockItemTagId var1);

        default public CombinedAppender add(BlockItemId ... ids) {
            this.addAll(Arrays.stream(ids));
            return this;
        }

        default public CombinedAppender addAll(Collection<BlockItemId> ids) {
            this.addAll(ids.stream());
            return this;
        }
    }
}

