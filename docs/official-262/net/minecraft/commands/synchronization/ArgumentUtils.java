/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.tree.ArgumentCommandNode
 *  com.mojang.brigadier.tree.CommandNode
 *  com.mojang.brigadier.tree.LiteralCommandNode
 *  com.mojang.brigadier.tree.RootCommandNode
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
 *  org.slf4j.Logger
 */
package net.minecraft.commands.synchronization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.lang.runtime.SwitchBootstraps;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionProviderCheck;
import org.slf4j.Logger;

public class ArgumentUtils {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final byte NUMBER_FLAG_MIN = 1;
    private static final byte NUMBER_FLAG_MAX = 2;

    public static int createNumberFlags(boolean hasMin, boolean hasMax) {
        int result = 0;
        if (hasMin) {
            result |= 1;
        }
        if (hasMax) {
            result |= 2;
        }
        return result;
    }

    public static boolean numberHasMin(byte flags) {
        return (flags & 1) != 0;
    }

    public static boolean numberHasMax(byte flags) {
        return (flags & 2) != 0;
    }

    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeArgumentCap(JsonObject result, ArgumentTypeInfo<A, T> info, ArgumentTypeInfo.Template<A> argumentType) {
        info.serializeToJson(argumentType, result);
    }

    private static <T extends ArgumentType<?>> void serializeArgumentToJson(JsonObject result, T argument) {
        ArgumentTypeInfo.Template<T> template = ArgumentTypeInfos.unpack(argument);
        result.addProperty("type", "argument");
        result.addProperty("parser", String.valueOf(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(template.type())));
        JsonObject type = new JsonObject();
        ArgumentUtils.serializeArgumentCap(type, template.type(), template);
        if (!type.isEmpty()) {
            result.add("properties", (JsonElement)type);
        }
    }

    public static <S> JsonObject serializeNodeToJson(CommandDispatcher<S> dispatcher, CommandNode<S> node) {
        Collection path;
        Object rootNode;
        JsonObject result = new JsonObject();
        CommandNode<S> commandNode = node;
        Objects.requireNonNull(commandNode);
        CommandNode<S> commandNode2 = commandNode;
        int n = 0;
        switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{RootCommandNode.class, LiteralCommandNode.class, ArgumentCommandNode.class}, commandNode2, n)) {
            case 0: {
                rootNode = (RootCommandNode)commandNode2;
                result.addProperty("type", "root");
                break;
            }
            case 1: {
                LiteralCommandNode literalNode = (LiteralCommandNode)commandNode2;
                result.addProperty("type", "literal");
                break;
            }
            case 2: {
                ArgumentCommandNode argumentNode = (ArgumentCommandNode)commandNode2;
                ArgumentUtils.serializeArgumentToJson(result, argumentNode.getType());
                break;
            }
            default: {
                LOGGER.error("Could not serialize node {} ({})!", node, node.getClass());
                result.addProperty("type", "unknown");
            }
        }
        Collection children = node.getChildren();
        if (!children.isEmpty()) {
            JsonObject childrenObject = new JsonObject();
            rootNode = children.iterator();
            while (rootNode.hasNext()) {
                CommandNode child = (CommandNode)rootNode.next();
                childrenObject.add(child.getName(), (JsonElement)ArgumentUtils.serializeNodeToJson(dispatcher, child));
            }
            result.add("children", (JsonElement)childrenObject);
        }
        if (node.getCommand() != null) {
            result.addProperty("executable", Boolean.valueOf(true));
        }
        if ((rootNode = node.getRequirement()) instanceof PermissionProviderCheck) {
            PermissionProviderCheck permissionCheck = (PermissionProviderCheck)rootNode;
            JsonElement permissions = (JsonElement)PermissionCheck.CODEC.encodeStart((DynamicOps)JsonOps.INSTANCE, (Object)permissionCheck.test()).getOrThrow(error -> new IllegalStateException("Failed to serialize requirement: " + error));
            result.add("permissions", permissions);
        }
        if (node.getRedirect() != null && !(path = dispatcher.getPath(node.getRedirect())).isEmpty()) {
            JsonArray target = new JsonArray();
            for (String piece : path) {
                target.add(piece);
            }
            result.add("redirect", (JsonElement)target);
        }
        return result;
    }

    public static <T> Set<ArgumentType<?>> findUsedArgumentTypes(CommandNode<T> node) {
        ReferenceOpenHashSet visitedNodes = new ReferenceOpenHashSet();
        HashSet result = new HashSet();
        ArgumentUtils.findUsedArgumentTypes(node, result, visitedNodes);
        return result;
    }

    private static <T> void findUsedArgumentTypes(CommandNode<T> node, Set<ArgumentType<?>> output, Set<CommandNode<T>> visitedNodes) {
        if (!visitedNodes.add(node)) {
            return;
        }
        if (node instanceof ArgumentCommandNode) {
            ArgumentCommandNode arg = (ArgumentCommandNode)node;
            output.add(arg.getType());
        }
        node.getChildren().forEach(child -> ArgumentUtils.findUsedArgumentTypes(child, output, visitedNodes));
        CommandNode redirect = node.getRedirect();
        if (redirect != null) {
            ArgumentUtils.findUsedArgumentTypes(redirect, output, visitedNodes);
        }
    }
}

