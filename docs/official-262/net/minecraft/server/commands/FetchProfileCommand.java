/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.DynamicCommandExceptionType
 *  com.mojang.serialization.DynamicOps
 */
package net.minecraft.server.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.ResolvableProfile;

public class FetchProfileCommand {
    private static final DynamicCommandExceptionType NO_PROFILE = new DynamicCommandExceptionType(id -> Component.translatableEscape("commands.fetchprofile.no_profile", id));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("fetchprofile").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))).then(Commands.literal("name").then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> FetchProfileCommand.resolveName((CommandSourceStack)c.getSource(), StringArgumentType.getString((CommandContext)c, (String)"name")))))).then(Commands.literal("id").then(Commands.argument("id", UuidArgument.uuid()).executes(c -> FetchProfileCommand.resolveId((CommandSourceStack)c.getSource(), UuidArgument.getUuid((CommandContext<CommandSourceStack>)c, "id")))))).then(Commands.literal("entity").then(Commands.argument("entity", EntityArgument.entity()).executes(c -> FetchProfileCommand.printForEntity((CommandSourceStack)c.getSource(), EntityArgument.getEntity((CommandContext<CommandSourceStack>)c, "entity"))))));
    }

    private static void reportResolvedProfile(CommandSourceStack sender, GameProfile gameProfile, String messageId, Component argument) {
        FetchProfileCommand.reportResolvedProfile(sender, messageId, argument, ResolvableProfile.createResolved(gameProfile));
    }

    private static void reportResolvedProfile(CommandSourceStack sender, String messageId, Component argument, ResolvableProfile profile) {
        ResolvableProfile.CODEC.encodeStart((DynamicOps)NbtOps.INSTANCE, (Object)profile).ifSuccess(encodedProfile -> {
            String encodedProfileAsString = encodedProfile.toString();
            MutableComponent headComponent = Component.object(new PlayerSprite(profile, true));
            ComponentSerialization.CODEC.encodeStart((DynamicOps)NbtOps.INSTANCE, (Object)headComponent).ifSuccess(encodedComponent -> {
                String encodedComponentAsString = encodedComponent.toString();
                sender.sendSuccess(() -> {
                    MutableComponent clickable = ComponentUtils.formatList(List.of(Component.translatable("commands.fetchprofile.copy_component").withStyle(s -> s.withClickEvent(new ClickEvent.CopyToClipboard(encodedProfileAsString))), Component.translatable("commands.fetchprofile.give_item").withStyle(s -> s.withClickEvent(new ClickEvent.RunCommand("give @s minecraft:player_head[profile=" + encodedProfileAsString + "]"))), Component.translatable("commands.fetchprofile.summon_mannequin").withStyle(s -> s.withClickEvent(new ClickEvent.RunCommand("summon minecraft:mannequin ~ ~ ~ {profile:" + encodedProfileAsString + "}"))), Component.translatable("commands.fetchprofile.copy_text", headComponent.withStyle(ChatFormatting.WHITE)).withStyle(s -> s.withClickEvent(new ClickEvent.CopyToClipboard(encodedComponentAsString)))), CommonComponents.SPACE, c -> ComponentUtils.wrapInSquareBrackets(c.withStyle(ChatFormatting.GREEN)));
                    return Component.translatable(messageId, argument, clickable);
                }, false);
            }).ifError(componentEncodingError -> sender.sendFailure(Component.translatable("commands.fetchprofile.failed_to_serialize", componentEncodingError.message())));
        }).ifError(error -> sender.sendFailure(Component.translatable("commands.fetchprofile.failed_to_serialize", error.message())));
    }

    private static int resolveName(CommandSourceStack source, String name) {
        MinecraftServer server = source.getServer();
        ProfileResolver resolver = server.services().profileResolver();
        Util.nonCriticalIoPool().execute(() -> {
            MutableComponent nameComponent = Component.literal(name);
            Optional<GameProfile> result = resolver.fetchByName(name);
            server.execute(() -> result.ifPresentOrElse(profile -> FetchProfileCommand.reportResolvedProfile(source, profile, "commands.fetchprofile.name.success", nameComponent), () -> source.sendFailure(Component.translatable("commands.fetchprofile.name.failure", nameComponent))));
        });
        return 1;
    }

    private static int resolveId(CommandSourceStack source, UUID id) {
        MinecraftServer server = source.getServer();
        ProfileResolver resolver = server.services().profileResolver();
        Util.nonCriticalIoPool().execute(() -> {
            Component idComponent = Component.translationArg(id);
            Optional<GameProfile> result = resolver.fetchById(id);
            server.execute(() -> result.ifPresentOrElse(profile -> FetchProfileCommand.reportResolvedProfile(source, profile, "commands.fetchprofile.id.success", idComponent), () -> source.sendFailure(Component.translatable("commands.fetchprofile.id.failure", idComponent))));
        });
        return 1;
    }

    private static int printForEntity(CommandSourceStack source, Entity entity) throws CommandSyntaxException {
        if (!(entity instanceof Avatar)) {
            throw NO_PROFILE.create((Object)entity.getDisplayName());
        }
        Avatar avatar = (Avatar)entity;
        FetchProfileCommand.printForAvatar(source, avatar);
        return 1;
    }

    public static void printForAvatar(CommandSourceStack source, Avatar avatar) {
        FetchProfileCommand.reportResolvedProfile(source, "commands.fetchprofile.entity.success", avatar.getDisplayName(), avatar.getProfile());
    }
}

