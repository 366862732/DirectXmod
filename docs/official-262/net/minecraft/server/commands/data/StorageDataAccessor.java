/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.builder.ArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 */
package net.minecraft.server.commands.data;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.world.level.storage.CommandStorage;

public class StorageDataAccessor
implements DataAccessor {
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_STORAGE = (c, p) -> SharedSuggestionProvider.suggestResource(StorageDataAccessor.getGlobalTags((CommandContext<CommandSourceStack>)c).keys(), p);
    public static final Function<String, DataCommands.DataProvider> PROVIDER = arg -> new DataCommands.DataProvider((String)arg){
        final /* synthetic */ String val$arg;
        {
            this.val$arg = string;
        }

        @Override
        public DataAccessor access(CommandContext<CommandSourceStack> context) {
            return new StorageDataAccessor(StorageDataAccessor.getGlobalTags(context), IdentifierArgument.getId(context, this.val$arg));
        }

        @Override
        public ArgumentBuilder<CommandSourceStack, ?> wrap(ArgumentBuilder<CommandSourceStack, ?> parent, Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> function) {
            return parent.then(Commands.literal("storage").then(function.apply((ArgumentBuilder<CommandSourceStack, ?>)Commands.argument(this.val$arg, IdentifierArgument.id()).suggests(SUGGEST_STORAGE))));
        }
    };
    private final CommandStorage storage;
    private final Identifier id;

    private static CommandStorage getGlobalTags(CommandContext<CommandSourceStack> context) {
        return ((CommandSourceStack)context.getSource()).getServer().getCommandStorage();
    }

    private StorageDataAccessor(CommandStorage storage, Identifier id) {
        this.storage = storage;
        this.id = id;
    }

    @Override
    public void setData(CompoundTag tag) {
        this.storage.set(this.id, tag);
    }

    @Override
    public CompoundTag getData() {
        return this.storage.get(this.id);
    }

    @Override
    public Component getModifiedSuccess() {
        return Component.translatable("commands.data.storage.modified", Component.translationArg(this.id));
    }

    @Override
    public Component getPrintSuccess(Tag data) {
        return Component.translatable("commands.data.storage.query", Component.translationArg(this.id), NbtUtils.toPrettyComponent(data));
    }

    @Override
    public Component getPrintSuccess(NbtPathArgument.NbtPath path, double scale, int value) {
        return Component.translatable("commands.data.storage.get", path.asString(), Component.translationArg(this.id), String.format(Locale.ROOT, "%.2f", scale), value);
    }
}

