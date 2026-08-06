/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Either
 *  com.mojang.logging.LogUtils
 *  io.netty.buffer.ByteBuf
 *  org.apache.commons.lang3.function.TriFunction
 *  org.slf4j.Logger
 */
package net.minecraft.world.waypoints;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.PartialTickSupplier;
import net.minecraft.world.waypoints.Waypoint;
import org.apache.commons.lang3.function.TriFunction;
import org.slf4j.Logger;

public abstract class TrackedWaypoint
implements Waypoint {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final StreamCodec<ByteBuf, TrackedWaypoint> STREAM_CODEC = StreamCodec.ofMember(TrackedWaypoint::write, TrackedWaypoint::read);
    protected final Either<UUID, String> identifier;
    private final Waypoint.Icon icon;
    private final Type type;

    private TrackedWaypoint(Either<UUID, String> identifier, Waypoint.Icon icon, Type type) {
        this.identifier = identifier;
        this.icon = icon;
        this.type = type;
    }

    public Either<UUID, String> id() {
        return this.identifier;
    }

    public abstract void update(TrackedWaypoint var1);

    public void write(ByteBuf buf) {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(buf);
        byteBuf.writeEither(this.identifier, UUIDUtil.STREAM_CODEC, FriendlyByteBuf::writeUtf);
        Waypoint.Icon.STREAM_CODEC.encode(byteBuf, this.icon);
        byteBuf.writeEnum(this.type);
        this.writeContents(buf);
    }

    public abstract void writeContents(ByteBuf var1);

    private static TrackedWaypoint read(ByteBuf buf) {
        FriendlyByteBuf byteBuf = new FriendlyByteBuf(buf);
        Either<UUID, String> identifier = byteBuf.readEither(UUIDUtil.STREAM_CODEC, FriendlyByteBuf::readUtf);
        Waypoint.Icon icon = (Waypoint.Icon)Waypoint.Icon.STREAM_CODEC.decode(byteBuf);
        Type type = byteBuf.readEnum(Type.class);
        return (TrackedWaypoint)type.constructor.apply(identifier, (Object)icon, (Object)byteBuf);
    }

    public static TrackedWaypoint setPosition(UUID identifier, Waypoint.Icon icon, Vec3i position) {
        return new Vec3iWaypoint(identifier, icon, position);
    }

    public static TrackedWaypoint setChunk(UUID identifier, Waypoint.Icon icon, ChunkPos chunk) {
        return new ChunkWaypoint(identifier, icon, chunk);
    }

    public static TrackedWaypoint setAzimuth(UUID identifier, Waypoint.Icon icon, float angle) {
        return new AzimuthWaypoint(identifier, icon, angle);
    }

    public static TrackedWaypoint empty(UUID identifier) {
        return new EmptyWaypoint(identifier);
    }

    public abstract double yawAngleToCamera(Level var1, Camera var2, PartialTickSupplier var3);

    public abstract PitchDirection pitchDirectionToCamera(Level var1, Projector var2, PartialTickSupplier var3);

    public abstract double distanceSquared(Entity var1);

    public Waypoint.Icon icon() {
        return this.icon;
    }

    private static enum Type {
        EMPTY((TriFunction<Either<UUID, String>, Waypoint.Icon, FriendlyByteBuf, TrackedWaypoint>)((TriFunction)EmptyWaypoint::new)),
        VEC3I((TriFunction<Either<UUID, String>, Waypoint.Icon, FriendlyByteBuf, TrackedWaypoint>)((TriFunction)Vec3iWaypoint::new)),
        CHUNK((TriFunction<Either<UUID, String>, Waypoint.Icon, FriendlyByteBuf, TrackedWaypoint>)((TriFunction)ChunkWaypoint::new)),
        AZIMUTH((TriFunction<Either<UUID, String>, Waypoint.Icon, FriendlyByteBuf, TrackedWaypoint>)((TriFunction)AzimuthWaypoint::new));

        private final TriFunction<Either<UUID, String>, Waypoint.Icon, FriendlyByteBuf, TrackedWaypoint> constructor;

        private Type(TriFunction<Either<UUID, String>, Waypoint.Icon, FriendlyByteBuf, TrackedWaypoint> constructor) {
            this.constructor = constructor;
        }
    }

    private static class Vec3iWaypoint
    extends TrackedWaypoint {
        private Vec3i vector;

        public Vec3iWaypoint(UUID identifier, Waypoint.Icon icon, Vec3i vector) {
            super((Either<UUID, String>)Either.left((Object)identifier), icon, Type.VEC3I);
            this.vector = vector;
        }

        public Vec3iWaypoint(Either<UUID, String> identifier, Waypoint.Icon icon, FriendlyByteBuf byteBuf) {
            super(identifier, icon, Type.VEC3I);
            this.vector = new Vec3i(byteBuf.readVarInt(), byteBuf.readVarInt(), byteBuf.readVarInt());
        }

        @Override
        public void update(TrackedWaypoint other) {
            if (other instanceof Vec3iWaypoint) {
                Vec3iWaypoint vec3iWaypoint = (Vec3iWaypoint)other;
                this.vector = vec3iWaypoint.vector;
            } else {
                LOGGER.warn("Unsupported Waypoint update operation: {}", other.getClass());
            }
        }

        @Override
        public void writeContents(ByteBuf buf) {
            VarInt.write(buf, this.vector.getX());
            VarInt.write(buf, this.vector.getY());
            VarInt.write(buf, this.vector.getZ());
        }

        private Vec3 position(Level level, PartialTickSupplier partialTick) {
            return this.identifier.left().map(level::getEntity).map(e -> {
                if (e.blockPosition().distManhattan(this.vector) > 3) {
                    return null;
                }
                return e.getEyePosition(partialTick.apply((Entity)e));
            }).orElseGet(() -> Vec3.atCenterOf(this.vector));
        }

        @Override
        public double yawAngleToCamera(Level level, Camera camera, PartialTickSupplier partialTickSupplier) {
            Vec3 direction = camera.position().subtract(this.position(level, partialTickSupplier)).rotateClockwise90();
            float waypointAngle = (float)Mth.atan2(direction.z(), direction.x()) * 57.295776f;
            return Mth.degreesDifference(camera.yaw(), waypointAngle);
        }

        @Override
        public PitchDirection pitchDirectionToCamera(Level level, Projector projector, PartialTickSupplier partialTickSupplier) {
            double yInFrontOfCamera;
            Vec3 pointOnScreen = projector.projectPointToScreen(this.position(level, partialTickSupplier));
            boolean isBehindCamera = pointOnScreen.z > 1.0;
            double d = yInFrontOfCamera = isBehindCamera ? -pointOnScreen.y : pointOnScreen.y;
            if (yInFrontOfCamera < -1.0) {
                return PitchDirection.DOWN;
            }
            if (yInFrontOfCamera > 1.0) {
                return PitchDirection.UP;
            }
            if (isBehindCamera) {
                if (pointOnScreen.y > 0.0) {
                    return PitchDirection.UP;
                }
                if (pointOnScreen.y < 0.0) {
                    return PitchDirection.DOWN;
                }
            }
            return PitchDirection.NONE;
        }

        @Override
        public double distanceSquared(Entity fromEntity) {
            return fromEntity.distanceToSqr(Vec3.atCenterOf(this.vector));
        }
    }

    private static class ChunkWaypoint
    extends TrackedWaypoint {
        private ChunkPos chunkPos;

        public ChunkWaypoint(UUID identifier, Waypoint.Icon icon, ChunkPos chunkPos) {
            super((Either<UUID, String>)Either.left((Object)identifier), icon, Type.CHUNK);
            this.chunkPos = chunkPos;
        }

        public ChunkWaypoint(Either<UUID, String> identifier, Waypoint.Icon icon, FriendlyByteBuf byteBuf) {
            super(identifier, icon, Type.CHUNK);
            this.chunkPos = new ChunkPos(byteBuf.readVarInt(), byteBuf.readVarInt());
        }

        @Override
        public void update(TrackedWaypoint other) {
            if (other instanceof ChunkWaypoint) {
                ChunkWaypoint chunkWaypoint = (ChunkWaypoint)other;
                this.chunkPos = chunkWaypoint.chunkPos;
            } else {
                LOGGER.warn("Unsupported Waypoint update operation: {}", other.getClass());
            }
        }

        @Override
        public void writeContents(ByteBuf buf) {
            VarInt.write(buf, this.chunkPos.x());
            VarInt.write(buf, this.chunkPos.z());
        }

        private Vec3 position(double positionY) {
            return Vec3.atCenterOf(this.chunkPos.getMiddleBlockPosition((int)positionY));
        }

        @Override
        public double yawAngleToCamera(Level level, Camera camera, PartialTickSupplier partialTickSupplier) {
            Vec3 cameraPosition = camera.position();
            Vec3 direction = cameraPosition.subtract(this.position(cameraPosition.y())).rotateClockwise90();
            float waypointAngle = (float)Mth.atan2(direction.z(), direction.x()) * 57.295776f;
            return Mth.degreesDifference(camera.yaw(), waypointAngle);
        }

        @Override
        public PitchDirection pitchDirectionToCamera(Level level, Projector projector, PartialTickSupplier partialTickSupplier) {
            double onScreenHorizon = projector.projectHorizonToScreen();
            if (onScreenHorizon < -1.0) {
                return PitchDirection.DOWN;
            }
            if (onScreenHorizon > 1.0) {
                return PitchDirection.UP;
            }
            return PitchDirection.NONE;
        }

        @Override
        public double distanceSquared(Entity fromEntity) {
            return fromEntity.distanceToSqr(Vec3.atCenterOf(this.chunkPos.getMiddleBlockPosition(fromEntity.getBlockY())));
        }
    }

    private static class AzimuthWaypoint
    extends TrackedWaypoint {
        private float angle;

        public AzimuthWaypoint(UUID identifier, Waypoint.Icon icon, float angle) {
            super((Either<UUID, String>)Either.left((Object)identifier), icon, Type.AZIMUTH);
            this.angle = angle;
        }

        public AzimuthWaypoint(Either<UUID, String> identifier, Waypoint.Icon icon, FriendlyByteBuf byteBuf) {
            super(identifier, icon, Type.AZIMUTH);
            this.angle = byteBuf.readFloat();
        }

        @Override
        public void update(TrackedWaypoint other) {
            if (other instanceof AzimuthWaypoint) {
                AzimuthWaypoint azimuthWaypoint = (AzimuthWaypoint)other;
                this.angle = azimuthWaypoint.angle;
            } else {
                LOGGER.warn("Unsupported Waypoint update operation: {}", other.getClass());
            }
        }

        @Override
        public void writeContents(ByteBuf buf) {
            buf.writeFloat(this.angle);
        }

        @Override
        public double yawAngleToCamera(Level level, Camera camera, PartialTickSupplier partialTickSupplier) {
            return Mth.degreesDifference(camera.yaw(), this.angle * 57.295776f);
        }

        @Override
        public PitchDirection pitchDirectionToCamera(Level level, Projector projector, PartialTickSupplier partialTickSupplier) {
            double horizon = projector.projectHorizonToScreen();
            if (horizon < -1.0) {
                return PitchDirection.DOWN;
            }
            if (horizon > 1.0) {
                return PitchDirection.UP;
            }
            return PitchDirection.NONE;
        }

        @Override
        public double distanceSquared(Entity fromEntity) {
            return Double.POSITIVE_INFINITY;
        }
    }

    private static class EmptyWaypoint
    extends TrackedWaypoint {
        private EmptyWaypoint(Either<UUID, String> identifier, Waypoint.Icon icon, FriendlyByteBuf byteBuf) {
            super(identifier, icon, Type.EMPTY);
        }

        private EmptyWaypoint(UUID identifier) {
            super((Either<UUID, String>)Either.left((Object)identifier), Waypoint.Icon.NULL, Type.EMPTY);
        }

        @Override
        public void update(TrackedWaypoint other) {
        }

        @Override
        public void writeContents(ByteBuf buf) {
        }

        @Override
        public double yawAngleToCamera(Level level, Camera camera, PartialTickSupplier partialTickSupplier) {
            return Double.NaN;
        }

        @Override
        public PitchDirection pitchDirectionToCamera(Level level, Projector projector, PartialTickSupplier partialTickSupplier) {
            return PitchDirection.NONE;
        }

        @Override
        public double distanceSquared(Entity fromEntity) {
            return Double.POSITIVE_INFINITY;
        }
    }

    public static interface Camera {
        public float yaw();

        public Vec3 position();
    }

    public static interface Projector {
        public Vec3 projectPointToScreen(Vec3 var1);

        public double projectHorizonToScreen();
    }

    public static enum PitchDirection {
        NONE,
        UP,
        DOWN;

    }
}

