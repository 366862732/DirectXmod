/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.builder.ArgumentBuilder
 *  com.mojang.brigadier.tree.ArgumentCommandNode
 *  com.mojang.brigadier.tree.CommandNode
 *  com.mojang.brigadier.tree.LiteralCommandNode
 *  com.mojang.brigadier.tree.RootCommandNode
 *  it.unimi.dsi.fastutil.ints.IntCollection
 *  it.unimi.dsi.fastutil.ints.IntOpenHashSet
 *  it.unimi.dsi.fastutil.ints.IntSet
 *  it.unimi.dsi.fastutil.ints.IntSets
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMap$Entry
 *  it.unimi.dsi.fastutil.objects.Object2IntMaps
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.protocol.game;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class ClientboundCommandsPacket
implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundCommandsPacket> STREAM_CODEC = Packet.codec(ClientboundCommandsPacket::write, ClientboundCommandsPacket::new);
    private static final byte MASK_TYPE = 3;
    private static final byte FLAG_EXECUTABLE = 4;
    private static final byte FLAG_REDIRECT = 8;
    private static final byte FLAG_CUSTOM_SUGGESTIONS = 16;
    private static final byte FLAG_RESTRICTED = 32;
    private static final byte TYPE_ROOT = 0;
    private static final byte TYPE_LITERAL = 1;
    private static final byte TYPE_ARGUMENT = 2;
    private final int rootIndex;
    private final List<Entry> entries;

    public <S> ClientboundCommandsPacket(RootCommandNode<S> root, NodeInspector<S> inspector) {
        Object2IntMap<CommandNode<S>> nodeToId = ClientboundCommandsPacket.enumerateNodes(root);
        this.entries = ClientboundCommandsPacket.createEntries(nodeToId, inspector);
        this.rootIndex = nodeToId.getInt(root);
    }

    private ClientboundCommandsPacket(FriendlyByteBuf input) {
        this.entries = input.readList(ClientboundCommandsPacket::readNode);
        this.rootIndex = input.readVarInt();
        ClientboundCommandsPacket.validateEntries(this.entries);
    }

    private void write(FriendlyByteBuf output) {
        output.writeCollection(this.entries, (buffer, entry) -> entry.write((FriendlyByteBuf)((Object)buffer)));
        output.writeVarInt(this.rootIndex);
    }

    private static void validateEntries(List<Entry> entries, BiPredicate<Entry, IntSet> validator) {
        IntOpenHashSet elementsToCheck = new IntOpenHashSet((IntCollection)IntSets.fromTo((int)0, (int)entries.size()));
        while (!elementsToCheck.isEmpty()) {
            boolean worked = elementsToCheck.removeIf(arg_0 -> ClientboundCommandsPacket.lambda$validateEntries$0(validator, entries, (IntSet)elementsToCheck, arg_0));
            if (worked) continue;
            throw new IllegalStateException("Server sent an impossible command tree");
        }
    }

    private static void validateEntries(List<Entry> entries) {
        ClientboundCommandsPacket.validateEntries(entries, Entry::canBuild);
        ClientboundCommandsPacket.validateEntries(entries, Entry::canResolve);
    }

    private static <S> Object2IntMap<CommandNode<S>> enumerateNodes(RootCommandNode<S> root) {
        CommandNode node;
        Object2IntOpenHashMap nodeToId = new Object2IntOpenHashMap();
        ArrayDeque<Object> queue = new ArrayDeque<Object>();
        queue.add(root);
        while ((node = (CommandNode)queue.poll()) != null) {
            if (nodeToId.containsKey((Object)node)) continue;
            int id = nodeToId.size();
            nodeToId.put((Object)node, id);
            queue.addAll(node.getChildren());
            if (node.getRedirect() == null) continue;
            queue.add(node.getRedirect());
        }
        return nodeToId;
    }

    private static <S> List<Entry> createEntries(Object2IntMap<CommandNode<S>> nodeToId, NodeInspector<S> inspector) {
        ObjectArrayList result = new ObjectArrayList(nodeToId.size());
        result.size(nodeToId.size());
        for (Object2IntMap.Entry entry : Object2IntMaps.fastIterable(nodeToId)) {
            result.set(entry.getIntValue(), (Object)ClientboundCommandsPacket.createEntry((CommandNode)entry.getKey(), inspector, nodeToId));
        }
        return result;
    }

    private static Entry readNode(FriendlyByteBuf input) {
        byte flags = input.readByte();
        int[] children = input.readVarIntArray();
        int redirect = (flags & 8) != 0 ? input.readVarInt() : 0;
        NodeStub stub = ClientboundCommandsPacket.read(input, flags);
        return new Entry(stub, flags, redirect, children);
    }

    private static @Nullable NodeStub read(FriendlyByteBuf input, byte flags) {
        int type = flags & 3;
        if (type == 2) {
            String name = input.readUtf();
            int id = input.readVarInt();
            ArgumentTypeInfo argumentType = (ArgumentTypeInfo)BuiltInRegistries.COMMAND_ARGUMENT_TYPE.byId(id);
            if (argumentType == null) {
                return null;
            }
            Object argument = argumentType.deserializeFromNetwork(input);
            Identifier suggestionId = (flags & 0x10) != 0 ? input.readIdentifier() : null;
            return new ArgumentNodeStub(name, (ArgumentTypeInfo.Template<?>)argument, suggestionId);
        }
        if (type == 1) {
            String id = input.readUtf();
            return new LiteralNodeStub(id);
        }
        return null;
    }

    private static <S> Entry createEntry(CommandNode<S> node, NodeInspector<S> inspector, Object2IntMap<CommandNode<S>> ids) {
        Record nodeStub;
        int redirect;
        int flags = 0;
        if (node.getRedirect() != null) {
            flags |= 8;
            redirect = ids.getInt((Object)node.getRedirect());
        } else {
            redirect = 0;
        }
        if (inspector.isExecutable(node)) {
            flags |= 4;
        }
        if (inspector.isRestricted(node)) {
            flags |= 0x20;
        }
        CommandNode<S> commandNode = node;
        Objects.requireNonNull(commandNode);
        CommandNode<S> commandNode2 = commandNode;
        int n = 0;
        switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{RootCommandNode.class, ArgumentCommandNode.class, LiteralCommandNode.class}, commandNode2, n)) {
            case 0: {
                RootCommandNode ignored = (RootCommandNode)commandNode2;
                flags |= 0;
                nodeStub = null;
                break;
            }
            case 1: {
                ArgumentCommandNode arg = (ArgumentCommandNode)commandNode2;
                Identifier suggestionId = inspector.suggestionId(arg);
                nodeStub = new ArgumentNodeStub(arg.getName(), ArgumentTypeInfos.unpack(arg.getType()), suggestionId);
                flags |= 2;
                if (suggestionId != null) {
                    flags |= 0x10;
                }
                break;
            }
            case 2: {
                LiteralCommandNode literal = (LiteralCommandNode)commandNode2;
                nodeStub = new LiteralNodeStub(literal.getLiteral());
                flags |= 1;
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown node type " + String.valueOf(node));
            }
        }
        int[] childrenIds = node.getChildren().stream().mapToInt(arg_0 -> ids.getInt(arg_0)).toArray();
        return new Entry((NodeStub)((Object)nodeStub), flags, redirect, childrenIds);
    }

    @Override
    public PacketType<ClientboundCommandsPacket> type() {
        return GamePacketTypes.CLIENTBOUND_COMMANDS;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleCommands(this);
    }

    public <S> RootCommandNode<S> getRoot(CommandBuildContext context, NodeBuilder<S> builder) {
        return (RootCommandNode)new NodeResolver<S>(context, builder, this.entries).resolve(this.rootIndex);
    }

    private static /* synthetic */ boolean lambda$validateEntries$0(BiPredicate validator, List entries, IntSet elementsToCheck, int index) {
        return validator.test((Entry)entries.get(index), elementsToCheck);
    }

    public static interface NodeInspector<S> {
        public @Nullable Identifier suggestionId(ArgumentCommandNode<S, ?> var1);

        public boolean isExecutable(CommandNode<S> var1);

        public boolean isRestricted(CommandNode<S> var1);
    }

    private record Entry(@Nullable NodeStub stub, int flags, int redirect, int[] children) {
        public void write(FriendlyByteBuf output) {
            output.writeByte(this.flags);
            output.writeVarIntArray(this.children);
            if ((this.flags & 8) != 0) {
                output.writeVarInt(this.redirect);
            }
            if (this.stub != null) {
                this.stub.write(output);
            }
        }

        public boolean canBuild(IntSet unbuiltNodes) {
            if ((this.flags & 8) != 0) {
                return !unbuiltNodes.contains(this.redirect);
            }
            return true;
        }

        public boolean canResolve(IntSet unresolvedNodes) {
            for (int child : this.children) {
                if (!unresolvedNodes.contains(child)) continue;
                return false;
            }
            return true;
        }
    }

    private static interface NodeStub {
        public <S> ArgumentBuilder<S, ?> build(CommandBuildContext var1, NodeBuilder<S> var2);

        public void write(FriendlyByteBuf var1);
    }

    private record ArgumentNodeStub(String id, ArgumentTypeInfo.Template<?> argumentType, @Nullable Identifier suggestionId) implements NodeStub
    {
        @Override
        public <S> ArgumentBuilder<S, ?> build(CommandBuildContext context, NodeBuilder<S> builder) {
            Object type = this.argumentType.instantiate(context);
            return builder.createArgument(this.id, (ArgumentType<?>)type, this.suggestionId);
        }

        @Override
        public void write(FriendlyByteBuf output) {
            output.writeUtf(this.id);
            ArgumentNodeStub.serializeCap(output, this.argumentType);
            if (this.suggestionId != null) {
                output.writeIdentifier(this.suggestionId);
            }
        }

        private static <A extends ArgumentType<?>> void serializeCap(FriendlyByteBuf output, ArgumentTypeInfo.Template<A> argumentType) {
            ArgumentNodeStub.serializeCap(output, argumentType.type(), argumentType);
        }

        private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeCap(FriendlyByteBuf output, ArgumentTypeInfo<A, T> info, ArgumentTypeInfo.Template<A> argumentType) {
            output.writeVarInt(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId(info));
            info.serializeToNetwork(argumentType, output);
        }
    }

    private record LiteralNodeStub(String id) implements NodeStub
    {
        @Override
        public <S> ArgumentBuilder<S, ?> build(CommandBuildContext context, NodeBuilder<S> builder) {
            return builder.createLiteral(this.id);
        }

        @Override
        public void write(FriendlyByteBuf output) {
            output.writeUtf(this.id);
        }
    }

    private static class NodeResolver<S> {
        private final CommandBuildContext context;
        private final NodeBuilder<S> builder;
        private final List<Entry> entries;
        private final List<CommandNode<S>> nodes;

        private NodeResolver(CommandBuildContext context, NodeBuilder<S> builder, List<Entry> entries) {
            this.context = context;
            this.builder = builder;
            this.entries = entries;
            ObjectArrayList nodes = new ObjectArrayList();
            nodes.size(entries.size());
            this.nodes = nodes;
        }

        public CommandNode<S> resolve(int index) {
            RootCommandNode result;
            CommandNode<S> currentNode = this.nodes.get(index);
            if (currentNode != null) {
                return currentNode;
            }
            Entry entry = this.entries.get(index);
            if (entry.stub == null) {
                result = new RootCommandNode();
            } else {
                ArgumentBuilder<S, ?> resultBuilder = entry.stub.build(this.context, this.builder);
                if ((entry.flags & 8) != 0) {
                    resultBuilder.redirect(this.resolve(entry.redirect));
                }
                boolean isExecutable = (entry.flags & 4) != 0;
                boolean isRestricted = (entry.flags & 0x20) != 0;
                result = this.builder.configure(resultBuilder, isExecutable, isRestricted).build();
            }
            this.nodes.set(index, (CommandNode<S>)result);
            for (int childId : entry.children) {
                CommandNode<S> child = this.resolve(childId);
                if (child instanceof RootCommandNode) continue;
                result.addChild(child);
            }
            return result;
        }
    }

    public static interface NodeBuilder<S> {
        public ArgumentBuilder<S, ?> createLiteral(String var1);

        public ArgumentBuilder<S, ?> createArgument(String var1, ArgumentType<?> var2, @Nullable Identifier var3);

        public ArgumentBuilder<S, ?> configure(ArgumentBuilder<S, ?> var1, boolean var2, boolean var3);
    }
}

