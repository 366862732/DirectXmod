/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public record JukeboxPlayable(Holder<JukeboxSong> song) implements TooltipProvider
{
    public static final Codec<JukeboxPlayable> CODEC = JukeboxSong.CODEC.xmap(JukeboxPlayable::new, JukeboxPlayable::song);
    public static final StreamCodec<RegistryFriendlyByteBuf, JukeboxPlayable> STREAM_CODEC = StreamCodec.composite(JukeboxSong.STREAM_CODEC, JukeboxPlayable::song, JukeboxPlayable::new);

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        consumer.accept(ComponentUtils.mergeStyles(this.song.value().description(), Style.EMPTY.withColor(ChatFormatting.GRAY)));
    }

    public static InteractionResult tryInsertIntoJukebox(Level level, BlockPos pos, ItemStack toInsert, Player player) {
        JukeboxPlayable jukeboxPlayable = toInsert.get(DataComponents.JUKEBOX_PLAYABLE);
        if (jukeboxPlayable == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.JUKEBOX) || state.getValue(JukeboxBlock.HAS_RECORD).booleanValue()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            ItemStack inserted = toInsert.consumeAndReturn(1, player);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof JukeboxBlockEntity) {
                JukeboxBlockEntity jukebox = (JukeboxBlockEntity)blockEntity;
                jukebox.setTheItem(inserted);
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
            }
            player.awardStat(Stats.PLAY_RECORD);
        }
        return InteractionResult.SUCCESS;
    }
}

