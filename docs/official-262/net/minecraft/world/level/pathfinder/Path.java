/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.pathfinder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Target;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class Path {
    public static final StreamCodec<FriendlyByteBuf, Path> STREAM_CODEC = StreamCodec.of((output, value) -> value.writeToStream((FriendlyByteBuf)((Object)output)), Path::createFromStream);
    private final List<Node> nodes;
    private @Nullable DebugData debugData;
    private int nextNodeIndex;
    private final BlockPos target;
    private final float distToTarget;
    private final boolean reached;

    public Path(List<Node> nodes, BlockPos target, boolean reached) {
        this.nodes = nodes;
        this.target = target;
        this.distToTarget = nodes.isEmpty() ? Float.MAX_VALUE : this.nodes.get(this.nodes.size() - 1).distanceManhattan(this.target);
        this.reached = reached;
    }

    public void advance() {
        ++this.nextNodeIndex;
    }

    public boolean notStarted() {
        return this.nextNodeIndex <= 0;
    }

    public boolean isDone() {
        return this.nextNodeIndex >= this.nodes.size();
    }

    public @Nullable Node getEndNode() {
        if (!this.nodes.isEmpty()) {
            return this.nodes.get(this.nodes.size() - 1);
        }
        return null;
    }

    public Node getNode(int i) {
        return this.nodes.get(i);
    }

    public void truncateNodes(int index) {
        if (this.nodes.size() > index) {
            this.nodes.subList(index, this.nodes.size()).clear();
        }
    }

    public void replaceNode(int index, Node replaceWith) {
        this.nodes.set(index, replaceWith);
    }

    public int getNodeCount() {
        return this.nodes.size();
    }

    public int getNextNodeIndex() {
        return this.nextNodeIndex;
    }

    public void setNextNodeIndex(int nextNodeIndex) {
        this.nextNodeIndex = nextNodeIndex;
    }

    public Vec3 getEntityPosAtNode(Entity entity, int index) {
        Node node = this.nodes.get(index);
        double x = (double)node.x + (double)((int)(entity.getBbWidth() + 1.0f)) * 0.5;
        double y = node.y;
        double z = (double)node.z + (double)((int)(entity.getBbWidth() + 1.0f)) * 0.5;
        return new Vec3(x, y, z);
    }

    public BlockPos getNodePos(int index) {
        return this.nodes.get(index).asBlockPos();
    }

    public Vec3 getNextEntityPos(Entity entity) {
        return this.getEntityPosAtNode(entity, this.nextNodeIndex);
    }

    public BlockPos getNextNodePos() {
        return this.nodes.get(this.nextNodeIndex).asBlockPos();
    }

    public Node getNextNode() {
        return this.nodes.get(this.nextNodeIndex);
    }

    public @Nullable Node getPreviousNode() {
        return this.nextNodeIndex > 0 ? this.nodes.get(this.nextNodeIndex - 1) : null;
    }

    public boolean sameAs(@Nullable Path path) {
        return path != null && this.nodes.equals(path.nodes);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Path)) {
            return false;
        }
        Path path = (Path)obj;
        return this.nextNodeIndex == path.nextNodeIndex && this.debugData == path.debugData && this.reached == path.reached && this.target.equals(path.target) && this.nodes.equals(path.nodes);
    }

    public int hashCode() {
        return this.nextNodeIndex + this.nodes.hashCode() * 31;
    }

    public boolean canReach() {
        return this.reached;
    }

    @VisibleForDebug
    void setDebug(Node[] openSet, Node[] closedSet, Set<Target> targets) {
        this.debugData = new DebugData(openSet, closedSet, targets);
    }

    public @Nullable DebugData debugData() {
        return this.debugData;
    }

    public void writeToStream(FriendlyByteBuf buffer) {
        if (this.debugData == null || this.debugData.targetNodes.isEmpty()) {
            throw new IllegalStateException("Missing debug data");
        }
        buffer.writeBoolean(this.reached);
        buffer.writeInt(this.nextNodeIndex);
        buffer.writeBlockPos(this.target);
        buffer.writeCollection(this.nodes, (out, node) -> node.writeToStream((FriendlyByteBuf)((Object)out)));
        this.debugData.write(buffer);
    }

    public static Path createFromStream(FriendlyByteBuf buffer) {
        boolean reached = buffer.readBoolean();
        int indexStream = buffer.readInt();
        BlockPos target = buffer.readBlockPos();
        List<Node> nodes = buffer.readList(Node::createFromStream);
        DebugData debugData = DebugData.read(buffer);
        Path path = new Path(nodes, target, reached);
        path.debugData = debugData;
        path.nextNodeIndex = indexStream;
        return path;
    }

    public String toString() {
        return "Path(length=" + this.nodes.size() + ")";
    }

    public BlockPos getTarget() {
        return this.target;
    }

    public float getDistToTarget() {
        return this.distToTarget;
    }

    private static Node[] readNodeArray(FriendlyByteBuf input) {
        Node[] nodes = new Node[input.readVarInt()];
        for (int i = 0; i < nodes.length; ++i) {
            nodes[i] = Node.createFromStream(input);
        }
        return nodes;
    }

    private static void writeNodeArray(FriendlyByteBuf output, Node[] nodes) {
        output.writeVarInt(nodes.length);
        for (Node node : nodes) {
            node.writeToStream(output);
        }
    }

    public Path copy() {
        Path result = new Path(this.nodes, this.target, this.reached);
        result.debugData = this.debugData;
        result.nextNodeIndex = this.nextNodeIndex;
        return result;
    }

    public record DebugData(Node[] openSet, Node[] closedSet, Set<Target> targetNodes) {
        public void write(FriendlyByteBuf output) {
            output.writeCollection(this.targetNodes, (out, target) -> target.writeToStream((FriendlyByteBuf)((Object)out)));
            Path.writeNodeArray(output, this.openSet);
            Path.writeNodeArray(output, this.closedSet);
        }

        public static DebugData read(FriendlyByteBuf input) {
            HashSet targets = input.readCollection(HashSet::new, Target::createFromStream);
            Node[] openSet = Path.readNodeArray(input);
            Node[] closedSet = Path.readNodeArray(input);
            return new DebugData(openSet, closedSet, targets);
        }
    }
}

