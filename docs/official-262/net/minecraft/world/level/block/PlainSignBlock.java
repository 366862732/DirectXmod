/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface PlainSignBlock {
    public Attachment attachmentPoint(BlockState var1);

    public static Attachment getAttachmentPoint(BlockState blockState) {
        Attachment attachment;
        Block block = blockState.getBlock();
        if (block instanceof PlainSignBlock) {
            PlainSignBlock plainSignBlock = (PlainSignBlock)((Object)block);
            attachment = plainSignBlock.attachmentPoint(blockState);
        } else {
            attachment = Attachment.GROUND;
        }
        return attachment;
    }

    public static enum Attachment implements StringRepresentable
    {
        WALL("wall"),
        GROUND("ground");

        public static final Codec<Attachment> CODEC;
        private final String name;

        private Attachment(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(Attachment::values);
        }
    }
}

