/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.pathfinder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Node {
    public final int x;
    public final int y;
    public final int z;
    private final int hash;
    public int heapIdx = -1;
    public float g;
    public float h;
    public float f;
    public @Nullable Node cameFrom;
    public boolean closed;
    public float walkedDistance;
    public float costMalus;
    public PathType type = PathType.BLOCKED;

    public Node(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.hash = Node.createHash(x, y, z);
    }

    public Node cloneAndMove(int x, int y, int z) {
        Node node = new Node(x, y, z);
        node.heapIdx = this.heapIdx;
        node.g = this.g;
        node.h = this.h;
        node.f = this.f;
        node.cameFrom = this.cameFrom;
        node.closed = this.closed;
        node.walkedDistance = this.walkedDistance;
        node.costMalus = this.costMalus;
        node.type = this.type;
        return node;
    }

    public static int createHash(int x, int y, int z) {
        return y & 0xFF | (x & Short.MAX_VALUE) << 8 | (z & Short.MAX_VALUE) << 24 | (x < 0 ? Integer.MIN_VALUE : 0) | (z < 0 ? 32768 : 0);
    }

    public float distanceTo(Node to) {
        float xd = to.x - this.x;
        float yd = to.y - this.y;
        float zd = to.z - this.z;
        return Mth.sqrt(xd * xd + yd * yd + zd * zd);
    }

    public float distanceToXZ(Node to) {
        float xd = to.x - this.x;
        float zd = to.z - this.z;
        return Mth.sqrt(xd * xd + zd * zd);
    }

    public float distanceTo(BlockPos pos) {
        float xd = pos.getX() - this.x;
        float yd = pos.getY() - this.y;
        float zd = pos.getZ() - this.z;
        return Mth.sqrt(xd * xd + yd * yd + zd * zd);
    }

    public float distanceToSqr(Node to) {
        float xd = to.x - this.x;
        float yd = to.y - this.y;
        float zd = to.z - this.z;
        return xd * xd + yd * yd + zd * zd;
    }

    public float distanceToSqr(BlockPos pos) {
        float xd = pos.getX() - this.x;
        float yd = pos.getY() - this.y;
        float zd = pos.getZ() - this.z;
        return xd * xd + yd * yd + zd * zd;
    }

    public float distanceManhattan(Node to) {
        float xd = Math.abs(to.x - this.x);
        float yd = Math.abs(to.y - this.y);
        float zd = Math.abs(to.z - this.z);
        return xd + yd + zd;
    }

    public float distanceManhattan(BlockPos pos) {
        float xd = Math.abs(pos.getX() - this.x);
        float yd = Math.abs(pos.getY() - this.y);
        float zd = Math.abs(pos.getZ() - this.z);
        return xd + yd + zd;
    }

    public BlockPos asBlockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }

    public Vec3 asVec3() {
        return new Vec3(this.x, this.y, this.z);
    }

    public boolean equals(Object o) {
        if (o instanceof Node) {
            Node no = (Node)o;
            return this.hash == no.hash && this.x == no.x && this.y == no.y && this.z == no.z;
        }
        return false;
    }

    public int hashCode() {
        return this.hash;
    }

    public boolean inOpenSet() {
        return this.heapIdx >= 0;
    }

    public String toString() {
        return "Node{x=" + this.x + ", y=" + this.y + ", z=" + this.z + "}";
    }

    public void writeToStream(FriendlyByteBuf buffer) {
        buffer.writeInt(this.x);
        buffer.writeInt(this.y);
        buffer.writeInt(this.z);
        buffer.writeFloat(this.walkedDistance);
        buffer.writeFloat(this.costMalus);
        buffer.writeBoolean(this.closed);
        buffer.writeEnum(this.type);
        buffer.writeFloat(this.f);
    }

    public static Node createFromStream(FriendlyByteBuf buffer) {
        Node node = new Node(buffer.readInt(), buffer.readInt(), buffer.readInt());
        Node.readContents(buffer, node);
        return node;
    }

    protected static void readContents(FriendlyByteBuf buffer, Node node) {
        node.walkedDistance = buffer.readFloat();
        node.costMalus = buffer.readFloat();
        node.closed = buffer.readBoolean();
        node.type = buffer.readEnum(PathType.class);
        node.f = buffer.readFloat();
    }
}

