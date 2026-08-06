/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.SignedArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.permissions.Permissions;
import org.jspecify.annotations.Nullable;

public class MessageArgument
implements SignedArgument<Message> {
    private static final Collection<String> EXAMPLES = Arrays.asList("Hello world!", "foo", "@e", "Hello @p :)");
    private static final Dynamic2CommandExceptionType TOO_LONG = new Dynamic2CommandExceptionType((length, maxLength) -> Component.translatableEscape("argument.message.too_long", length, maxLength));

    public static MessageArgument message() {
        return new MessageArgument();
    }

    public static Component getMessage(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        Message message = (Message)context.getArgument(name, Message.class);
        return message.resolveComponent((CommandSourceStack)context.getSource());
    }

    public static void resolveChatMessage(CommandContext<CommandSourceStack> context, String name, Consumer<PlayerChatMessage> task) throws CommandSyntaxException {
        Message message = (Message)context.getArgument(name, Message.class);
        CommandSourceStack sender = (CommandSourceStack)context.getSource();
        Component formatted = message.resolveComponent(sender);
        CommandSigningContext signingContext = sender.getSigningContext();
        PlayerChatMessage signedArgument = signingContext.getArgument(name);
        if (signedArgument != null) {
            MessageArgument.resolveSignedMessage(task, sender, signedArgument.withUnsignedContent(formatted));
        } else {
            MessageArgument.resolveDisguisedMessage(task, sender, PlayerChatMessage.system(message.text).withUnsignedContent(formatted));
        }
    }

    private static void resolveSignedMessage(Consumer<PlayerChatMessage> task, CommandSourceStack sender, PlayerChatMessage signedArgument) {
        MinecraftServer server = sender.getServer();
        CompletableFuture<FilteredText> filteredFuture = MessageArgument.filterPlainText(sender, signedArgument);
        Component decorated = server.getChatDecorator().decorate(sender.getPlayer(), signedArgument.decoratedContent());
        sender.getChatMessageChainer().append(filteredFuture, filtered -> {
            PlayerChatMessage filteredMessage = signedArgument.withUnsignedContent(decorated).filter(filtered.mask());
            task.accept(filteredMessage);
        });
    }

    private static void resolveDisguisedMessage(Consumer<PlayerChatMessage> task, CommandSourceStack sender, PlayerChatMessage argument) {
        ChatDecorator decorator = sender.getServer().getChatDecorator();
        Component decorated = decorator.decorate(sender.getPlayer(), argument.decoratedContent());
        task.accept(argument.withUnsignedContent(decorated));
    }

    private static CompletableFuture<FilteredText> filterPlainText(CommandSourceStack sender, PlayerChatMessage message) {
        ServerPlayer player = sender.getPlayer();
        if (player != null && message.hasSignatureFrom(player.getUUID())) {
            return player.getTextFilter().processStreamMessage(message.signedContent());
        }
        return CompletableFuture.completedFuture(FilteredText.passThrough(message.signedContent()));
    }

    public Message parse(StringReader reader) throws CommandSyntaxException {
        return Message.parseText(reader, true);
    }

    public <S> Message parse(StringReader reader, @Nullable S source) throws CommandSyntaxException {
        return Message.parseText(reader, EntitySelectorParser.allowSelectors(source));
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public record Message(String text, Part[] parts) {
        private Component resolveComponent(CommandSourceStack sender) throws CommandSyntaxException {
            return this.toComponent(sender, sender.permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS));
        }

        public Component toComponent(CommandSourceStack sender, boolean allowSelectors) throws CommandSyntaxException {
            if (this.parts.length == 0 || !allowSelectors) {
                return Component.literal(this.text);
            }
            MutableComponent result = Component.literal(this.text.substring(0, this.parts[0].start()));
            int readTo = this.parts[0].start();
            for (Part part : this.parts) {
                Component component = part.toComponent(sender);
                if (readTo < part.start()) {
                    result.append(this.text.substring(readTo, part.start()));
                }
                result.append(component);
                readTo = part.end();
            }
            if (readTo < this.text.length()) {
                result.append(this.text.substring(readTo));
            }
            return result;
        }

        public static Message parseText(StringReader reader, boolean allowSelectors) throws CommandSyntaxException {
            if (reader.getRemainingLength() > 256) {
                throw TOO_LONG.create((Object)reader.getRemainingLength(), (Object)256);
            }
            String text = reader.getRemaining();
            if (!allowSelectors) {
                reader.setCursor(reader.getTotalLength());
                return new Message(text, new Part[0]);
            }
            ArrayList result = Lists.newArrayList();
            int offset = reader.getCursor();
            while (reader.canRead()) {
                if (reader.peek() == '@') {
                    EntitySelector parse;
                    int start = reader.getCursor();
                    try {
                        EntitySelectorParser parser = new EntitySelectorParser(reader, true);
                        parse = parser.parse();
                    }
                    catch (CommandSyntaxException ex) {
                        if (ex.getType() == EntitySelectorParser.ERROR_MISSING_SELECTOR_TYPE || ex.getType() == EntitySelectorParser.ERROR_UNKNOWN_SELECTOR_TYPE) {
                            reader.setCursor(start + 1);
                            continue;
                        }
                        throw ex;
                    }
                    result.add(new Part(start - offset, reader.getCursor() - offset, parse));
                    continue;
                }
                reader.skip();
            }
            return new Message(text, result.toArray(new Part[0]));
        }
    }

    public record Part(int start, int end, EntitySelector selector) {
        public Component toComponent(CommandSourceStack sender) throws CommandSyntaxException {
            return EntitySelector.joinNames(this.selector.findEntities(sender));
        }
    }
}

