/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.saveddata.maps;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapFrame;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jspecify.annotations.Nullable;

public class MapItemSavedData
extends SavedData {
    private static final int MAP_SIZE = 128;
    private static final int HALF_MAP_SIZE = 64;
    public static final int MAX_SCALE = 4;
    public static final int TRACKED_DECORATION_LIMIT = 256;
    private static final String FRAME_PREFIX = "frame-";
    public static final Codec<MapItemSavedData> CODEC = RecordCodecBuilder.create(i -> i.group((App)Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(m -> m.dimension), (App)Codec.INT.fieldOf("xCenter").forGetter(m -> m.centerX), (App)Codec.INT.fieldOf("zCenter").forGetter(m -> m.centerZ), (App)Codec.BYTE.optionalFieldOf("scale", (Object)0).forGetter(m -> m.scale), (App)Codec.BYTE_BUFFER.fieldOf("colors").forGetter(m -> ByteBuffer.wrap(m.colors)), (App)Codec.BOOL.optionalFieldOf("trackingPosition", (Object)true).forGetter(m -> m.trackingPosition), (App)Codec.BOOL.optionalFieldOf("unlimitedTracking", (Object)false).forGetter(m -> m.unlimitedTracking), (App)Codec.BOOL.optionalFieldOf("locked", (Object)false).forGetter(m -> m.locked), (App)MapBanner.CODEC.listOf().optionalFieldOf("banners", List.of()).forGetter(m -> List.copyOf(m.bannerMarkers.values())), (App)MapFrame.CODEC.listOf().optionalFieldOf("frames", List.of()).forGetter(m -> List.copyOf(m.frameMarkers.values()))).apply((Applicative)i, MapItemSavedData::new));
    public final int centerX;
    public final int centerZ;
    public final ResourceKey<Level> dimension;
    private final boolean trackingPosition;
    private final boolean unlimitedTracking;
    public final byte scale;
    public byte[] colors = new byte[16384];
    public final boolean locked;
    private final List<HoldingPlayer> carriedBy = Lists.newArrayList();
    private final Map<Player, HoldingPlayer> carriedByPlayers = Maps.newHashMap();
    private final Map<String, MapBanner> bannerMarkers = Maps.newHashMap();
    private final Map<String, MapDecoration> decorations = Maps.newLinkedHashMap();
    private final Map<String, MapFrame> frameMarkers = Maps.newHashMap();
    private int trackedDecorationCount;

    public static SavedDataType<MapItemSavedData> type(MapId id) {
        return new SavedDataType<MapItemSavedData>(Identifier.withDefaultNamespace(id.key()), () -> {
            throw new IllegalStateException("Should never create an empty map saved data");
        }, CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);
    }

    private MapItemSavedData(int centerX, int centerZ, byte scale, boolean trackingPosition, boolean unlimitedTracking, boolean locked, ResourceKey<Level> dimension) {
        this.scale = scale;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.dimension = dimension;
        this.trackingPosition = trackingPosition;
        this.unlimitedTracking = unlimitedTracking;
        this.locked = locked;
    }

    private MapItemSavedData(ResourceKey<Level> dimension, int centerX, int centerZ, byte scale, ByteBuffer colors, boolean trackingPosition, boolean unlimitedTracking, boolean locked, List<MapBanner> banners, List<MapFrame> frames) {
        this(centerX, centerZ, (byte)Mth.clamp(scale, 0, 4), trackingPosition, unlimitedTracking, locked, dimension);
        if (colors.array().length == 16384) {
            this.colors = colors.array();
        }
        for (MapBanner banner : banners) {
            this.bannerMarkers.put(banner.getId(), banner);
            this.addDecoration(banner.getDecoration(), null, banner.getId(), banner.pos().getX(), banner.pos().getZ(), 180.0, banner.name().orElse(null));
        }
        for (MapFrame frame : frames) {
            this.frameMarkers.put(frame.getId(), frame);
            this.addDecoration(MapDecorationTypes.FRAME, null, MapItemSavedData.getFrameKey(frame.entityId()), frame.pos().getX(), frame.pos().getZ(), frame.rotation(), null);
        }
    }

    public static MapItemSavedData createFresh(double originX, double originY, byte scale, boolean trackingPosition, boolean unlimitedTracking, ResourceKey<Level> dimension) {
        int size = 128 * (1 << scale);
        int areaX = Mth.floor((originX + 64.0) / (double)size);
        int areaZ = Mth.floor((originY + 64.0) / (double)size);
        int x = areaX * size + size / 2 - 64;
        int z = areaZ * size + size / 2 - 64;
        return new MapItemSavedData(x, z, scale, trackingPosition, unlimitedTracking, false, dimension);
    }

    public static MapItemSavedData createForClient(byte scale, boolean isLocked, ResourceKey<Level> dimension) {
        return new MapItemSavedData(0, 0, scale, false, false, isLocked, dimension);
    }

    public MapItemSavedData locked() {
        MapItemSavedData result = new MapItemSavedData(this.centerX, this.centerZ, this.scale, this.trackingPosition, this.unlimitedTracking, true, this.dimension);
        result.bannerMarkers.putAll(this.bannerMarkers);
        result.decorations.putAll(this.decorations);
        result.trackedDecorationCount = this.trackedDecorationCount;
        System.arraycopy(this.colors, 0, result.colors, 0, this.colors.length);
        return result;
    }

    public MapItemSavedData scaled() {
        return MapItemSavedData.createFresh(this.centerX, this.centerZ, (byte)Mth.clamp(this.scale + 1, 0, 4), this.trackingPosition, this.unlimitedTracking, this.dimension);
    }

    private static Predicate<ItemStack> mapMatcher(ItemStack mapStack) {
        MapId mapId = mapStack.get(DataComponents.MAP_ID);
        return stack -> {
            if (stack == mapStack) {
                return true;
            }
            return stack.is(mapStack.getItem()) && Objects.equals(mapId, stack.get(DataComponents.MAP_ID));
        };
    }

    public void tickCarriedBy(Player tickingPlayer, ItemStack itemStack, @Nullable ItemFrame placedInFrame) {
        if (!this.carriedByPlayers.containsKey(tickingPlayer)) {
            HoldingPlayer holdingPlayer = new HoldingPlayer(this, tickingPlayer);
            this.carriedByPlayers.put(tickingPlayer, holdingPlayer);
            this.carriedBy.add(holdingPlayer);
        }
        Predicate<ItemStack> mapMatcher = MapItemSavedData.mapMatcher(itemStack);
        if (!tickingPlayer.getInventory().contains(mapMatcher)) {
            this.removeDecoration(tickingPlayer.getPlainTextName());
        }
        for (int i = 0; i < this.carriedBy.size(); ++i) {
            HoldingPlayer otherHoldingPlayer = this.carriedBy.get(i);
            Player otherPlayer = otherHoldingPlayer.player;
            String otherPlayerName = otherPlayer.getPlainTextName();
            if (otherPlayer.isRemoved() || placedInFrame == null && !otherPlayer.getInventory().contains(mapMatcher)) {
                this.carriedByPlayers.remove(otherPlayer);
                this.carriedBy.remove(otherHoldingPlayer);
                this.removeDecoration(otherPlayerName);
            } else if (placedInFrame == null && otherPlayer.level().dimension() == this.dimension && this.trackingPosition) {
                this.addDecoration(MapDecorationTypes.PLAYER, otherPlayer.level(), otherPlayerName, otherPlayer.getX(), otherPlayer.getZ(), otherPlayer.getYRot(), null);
            }
            if (otherPlayer.equals(tickingPlayer) || !MapItemSavedData.hasMapInvisibilityItemEquipped(otherPlayer)) continue;
            this.removeDecoration(otherPlayerName);
        }
        if (placedInFrame != null && this.trackingPosition) {
            BlockPos pos = placedInFrame.getPos();
            MapFrame existingFrame = this.frameMarkers.get(MapFrame.frameId(pos));
            if (existingFrame != null && placedInFrame.getId() != existingFrame.entityId() && this.frameMarkers.containsKey(existingFrame.getId())) {
                this.removeDecoration(MapItemSavedData.getFrameKey(existingFrame.entityId()));
            }
            MapFrame mapFrame = new MapFrame(pos, placedInFrame.getDirection().get2DDataValue() * 90, placedInFrame.getId());
            this.addDecoration(MapDecorationTypes.FRAME, tickingPlayer.level(), MapItemSavedData.getFrameKey(placedInFrame.getId()), pos.getX(), pos.getZ(), placedInFrame.getDirection().get2DDataValue() * 90, null);
            MapFrame oldFrame = this.frameMarkers.put(mapFrame.getId(), mapFrame);
            if (!mapFrame.equals(oldFrame)) {
                this.setDirty();
            }
        }
        MapDecorations staticDecorations = itemStack.getOrDefault(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY);
        if (!this.decorations.keySet().containsAll(staticDecorations.decorations().keySet())) {
            staticDecorations.decorations().forEach((id, entry) -> {
                if (!this.decorations.containsKey(id)) {
                    this.addDecoration(entry.type(), tickingPlayer.level(), (String)id, entry.x(), entry.z(), entry.rotation(), null);
                }
            });
        }
    }

    private static boolean hasMapInvisibilityItemEquipped(Player player) {
        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            if (equipmentSlot == EquipmentSlot.MAINHAND || equipmentSlot == EquipmentSlot.OFFHAND || !player.getItemBySlot(equipmentSlot).is(ItemTags.MAP_INVISIBILITY_EQUIPMENT)) continue;
            return true;
        }
        return false;
    }

    private void removeDecoration(String string) {
        MapDecoration decoration = this.decorations.remove(string);
        if (decoration != null && decoration.type().value().trackCount()) {
            --this.trackedDecorationCount;
        }
        this.setDecorationsDirty();
    }

    public static void addTargetDecoration(ItemStack itemStack, BlockPos position, String key, Holder<MapDecorationType> decorationType) {
        MapDecorations.Entry newDecoration = new MapDecorations.Entry(decorationType, position.getX(), position.getZ(), 180.0f);
        itemStack.update(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY, decorations -> decorations.withDecoration(key, newDecoration));
        if (decorationType.value().hasMapColor()) {
            itemStack.set(DataComponents.MAP_COLOR, new MapItemColor(decorationType.value().mapColor()));
        }
    }

    private void addDecoration(Holder<MapDecorationType> type, @Nullable LevelAccessor level, String key, double xPos, double zPos, double yRot, @Nullable Component name) {
        MapDecoration previousDecoration;
        int scaling = 1 << this.scale;
        float xDeltaFromCenter = (float)(xPos - (double)this.centerX) / (float)scaling;
        float yDeltaFromCenter = (float)(zPos - (double)this.centerZ) / (float)scaling;
        MapDecorationLocation locationAndType = this.calculateDecorationLocationAndType(type, level, yRot, xDeltaFromCenter, yDeltaFromCenter);
        if (locationAndType == null) {
            this.removeDecoration(key);
            return;
        }
        MapDecoration newDecoration = new MapDecoration(locationAndType.type(), locationAndType.x(), locationAndType.y(), locationAndType.rot(), Optional.ofNullable(name));
        if (!newDecoration.equals(previousDecoration = this.decorations.put(key, newDecoration))) {
            if (previousDecoration != null && previousDecoration.type().value().trackCount()) {
                --this.trackedDecorationCount;
            }
            if (locationAndType.type().value().trackCount()) {
                ++this.trackedDecorationCount;
            }
            this.setDecorationsDirty();
        }
    }

    private @Nullable MapDecorationLocation calculateDecorationLocationAndType(Holder<MapDecorationType> type, @Nullable LevelAccessor level, double yRot, float xDeltaFromCenter, float yDeltaFromCenter) {
        byte clampedXDeltaFromCenter = MapItemSavedData.clampMapCoordinate(xDeltaFromCenter);
        byte clampedYDeltaFromCenter = MapItemSavedData.clampMapCoordinate(yDeltaFromCenter);
        if (type.is(MapDecorationTypes.PLAYER)) {
            Pair<Holder<MapDecorationType>, Byte> typeAndRotation = this.playerDecorationTypeAndRotation(type, level, yRot, xDeltaFromCenter, yDeltaFromCenter);
            return typeAndRotation == null ? null : new MapDecorationLocation((Holder)typeAndRotation.getFirst(), clampedXDeltaFromCenter, clampedYDeltaFromCenter, (Byte)typeAndRotation.getSecond());
        }
        if (MapItemSavedData.isInsideMap(xDeltaFromCenter, yDeltaFromCenter) || this.unlimitedTracking) {
            return new MapDecorationLocation(type, clampedXDeltaFromCenter, clampedYDeltaFromCenter, this.calculateRotation(level, yRot));
        }
        return null;
    }

    private @Nullable Pair<Holder<MapDecorationType>, Byte> playerDecorationTypeAndRotation(Holder<MapDecorationType> type, @Nullable LevelAccessor level, double yRot, float xDeltaFromCenter, float yDeltaFromCenter) {
        if (MapItemSavedData.isInsideMap(xDeltaFromCenter, yDeltaFromCenter)) {
            return Pair.of(type, (Object)this.calculateRotation(level, yRot));
        }
        Holder<MapDecorationType> outsideMapDecorationType = this.decorationTypeForPlayerOutsideMap(xDeltaFromCenter, yDeltaFromCenter);
        if (outsideMapDecorationType == null) {
            return null;
        }
        return Pair.of(outsideMapDecorationType, (Object)0);
    }

    private byte calculateRotation(@Nullable LevelAccessor level, double yRot) {
        if (this.dimension == Level.NETHER && level != null) {
            int s = (int)(level.getGameTime() / 10L);
            return (byte)(s * s * 34187121 + s * 121 >> 15 & 0xF);
        }
        double adjustedYRot = yRot < 0.0 ? yRot - 8.0 : yRot + 8.0;
        return (byte)(adjustedYRot * 16.0 / 360.0);
    }

    private static boolean isInsideMap(float xd, float yd) {
        int halfSize = 63;
        return xd >= -63.0f && yd >= -63.0f && xd <= 63.0f && yd <= 63.0f;
    }

    private @Nullable Holder<MapDecorationType> decorationTypeForPlayerOutsideMap(float xDeltaFromCenter, float yDeltaFromCenter) {
        boolean isWithinLimits;
        int rangeLimit = 320;
        boolean bl = isWithinLimits = Math.abs(xDeltaFromCenter) < 320.0f && Math.abs(yDeltaFromCenter) < 320.0f;
        if (isWithinLimits) {
            return MapDecorationTypes.PLAYER_OFF_MAP;
        }
        return this.unlimitedTracking ? MapDecorationTypes.PLAYER_OFF_LIMITS : null;
    }

    private static byte clampMapCoordinate(float deltaFromCenter) {
        int halfSize = 63;
        if (deltaFromCenter <= -63.0f) {
            return -128;
        }
        if (deltaFromCenter >= 63.0f) {
            return 127;
        }
        return (byte)((double)(deltaFromCenter * 2.0f) + 0.5);
    }

    public @Nullable Packet<?> getUpdatePacket(MapId id, Player player) {
        HoldingPlayer holdingPlayer = this.carriedByPlayers.get(player);
        if (holdingPlayer == null) {
            return null;
        }
        return holdingPlayer.nextUpdatePacket(id);
    }

    private void setColorsDirty(int x, int y) {
        this.setDirty();
        for (HoldingPlayer holdingPlayer : this.carriedBy) {
            holdingPlayer.markColorsDirty(x, y);
        }
    }

    private void setDecorationsDirty() {
        this.carriedBy.forEach(HoldingPlayer::markDecorationsDirty);
    }

    public HoldingPlayer getHoldingPlayer(Player player) {
        HoldingPlayer holdingPlayer = this.carriedByPlayers.get(player);
        if (holdingPlayer == null) {
            holdingPlayer = new HoldingPlayer(this, player);
            this.carriedByPlayers.put(player, holdingPlayer);
            this.carriedBy.add(holdingPlayer);
        }
        return holdingPlayer;
    }

    public boolean toggleBanner(LevelAccessor level, BlockPos pos) {
        double xPos = (double)pos.getX() + 0.5;
        double zPos = (double)pos.getZ() + 0.5;
        int scale = 1 << this.scale;
        double xd = (xPos - (double)this.centerX) / (double)scale;
        double yd = (zPos - (double)this.centerZ) / (double)scale;
        int halfSize = 63;
        if (xd >= -63.0 && yd >= -63.0 && xd <= 63.0 && yd <= 63.0) {
            MapBanner banner = MapBanner.fromWorld(level, pos);
            if (banner == null) {
                return false;
            }
            if (this.bannerMarkers.remove(banner.getId(), banner)) {
                this.removeDecoration(banner.getId());
                this.setDirty();
                return true;
            }
            if (!this.isTrackedCountOverLimit(256)) {
                this.bannerMarkers.put(banner.getId(), banner);
                this.addDecoration(banner.getDecoration(), level, banner.getId(), xPos, zPos, 180.0, banner.name().orElse(null));
                this.setDirty();
                return true;
            }
        }
        return false;
    }

    public void checkBanners(BlockGetter level, int x, int z) {
        Iterator<MapBanner> iterator = this.bannerMarkers.values().iterator();
        while (iterator.hasNext()) {
            MapBanner current;
            MapBanner expected = iterator.next();
            if (expected.pos().getX() != x || expected.pos().getZ() != z || expected.equals(current = MapBanner.fromWorld(level, expected.pos()))) continue;
            iterator.remove();
            this.removeDecoration(expected.getId());
            this.setDirty();
        }
    }

    public Collection<MapBanner> getBanners() {
        return this.bannerMarkers.values();
    }

    public void removedFromFrame(BlockPos pos, int entityID) {
        this.removeDecoration(MapItemSavedData.getFrameKey(entityID));
        this.frameMarkers.remove(MapFrame.frameId(pos));
        this.setDirty();
    }

    public boolean updateColor(int x, int y, byte newColor) {
        byte oldColor = this.colors[x + y * 128];
        if (oldColor != newColor) {
            this.setColor(x, y, newColor);
            return true;
        }
        return false;
    }

    public void setColor(int x, int y, byte newColor) {
        this.colors[x + y * 128] = newColor;
        this.setColorsDirty(x, y);
    }

    public boolean isExplorationMap() {
        for (MapDecoration decoration : this.decorations.values()) {
            if (!decoration.type().value().explorationMapElement()) continue;
            return true;
        }
        return false;
    }

    public void addClientSideDecorations(List<MapDecoration> decorations) {
        this.decorations.clear();
        this.trackedDecorationCount = 0;
        for (int i = 0; i < decorations.size(); ++i) {
            MapDecoration decoration = decorations.get(i);
            this.decorations.put("icon-" + i, decoration);
            if (!decoration.type().value().trackCount()) continue;
            ++this.trackedDecorationCount;
        }
    }

    public Iterable<MapDecoration> getDecorations() {
        return this.decorations.values();
    }

    public boolean isTrackedCountOverLimit(int limit) {
        return this.trackedDecorationCount > limit;
    }

    private static String getFrameKey(int id) {
        return FRAME_PREFIX + id;
    }

    public class HoldingPlayer {
        public final Player player;
        private boolean dirtyData;
        private int minDirtyX;
        private int minDirtyY;
        private int maxDirtyX;
        private int maxDirtyY;
        private boolean dirtyDecorations;
        private int tick;
        public int step;
        final /* synthetic */ MapItemSavedData this$0;

        private HoldingPlayer(MapItemSavedData this$0, Player player) {
            MapItemSavedData mapItemSavedData = this$0;
            Objects.requireNonNull(mapItemSavedData);
            this.this$0 = mapItemSavedData;
            this.dirtyData = true;
            this.maxDirtyX = 127;
            this.maxDirtyY = 127;
            this.dirtyDecorations = true;
            this.player = player;
        }

        private MapPatch createPatch() {
            int startX = this.minDirtyX;
            int startY = this.minDirtyY;
            int width = this.maxDirtyX + 1 - this.minDirtyX;
            int height = this.maxDirtyY + 1 - this.minDirtyY;
            byte[] patch = new byte[width * height];
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    patch[x + y * width] = this.this$0.colors[startX + x + (startY + y) * 128];
                }
            }
            return new MapPatch(startX, startY, width, height, patch);
        }

        private @Nullable Packet<?> nextUpdatePacket(MapId id) {
            Collection<MapDecoration> decorations;
            MapPatch patch;
            if (this.dirtyData) {
                this.dirtyData = false;
                patch = this.createPatch();
            } else {
                patch = null;
            }
            if (this.dirtyDecorations && this.tick++ % 5 == 0) {
                this.dirtyDecorations = false;
                decorations = this.this$0.decorations.values();
            } else {
                decorations = null;
            }
            if (decorations != null || patch != null) {
                return new ClientboundMapItemDataPacket(id, this.this$0.scale, this.this$0.locked, decorations, patch);
            }
            return null;
        }

        private void markColorsDirty(int x, int y) {
            if (this.dirtyData) {
                this.minDirtyX = Math.min(this.minDirtyX, x);
                this.minDirtyY = Math.min(this.minDirtyY, y);
                this.maxDirtyX = Math.max(this.maxDirtyX, x);
                this.maxDirtyY = Math.max(this.maxDirtyY, y);
            } else {
                this.dirtyData = true;
                this.minDirtyX = x;
                this.minDirtyY = y;
                this.maxDirtyX = x;
                this.maxDirtyY = y;
            }
        }

        private void markDecorationsDirty() {
            this.dirtyDecorations = true;
        }
    }

    private record MapDecorationLocation(Holder<MapDecorationType> type, byte x, byte y, byte rot) {
    }

    public record MapPatch(int startX, int startY, int width, int height, byte[] mapColors) {
        public static final StreamCodec<ByteBuf, Optional<MapPatch>> STREAM_CODEC = StreamCodec.of(MapPatch::write, MapPatch::read);

        private static void write(ByteBuf output, Optional<MapPatch> optional) {
            if (optional.isPresent()) {
                MapPatch patch = optional.get();
                output.writeByte(patch.width);
                output.writeByte(patch.height);
                output.writeByte(patch.startX);
                output.writeByte(patch.startY);
                FriendlyByteBuf.writeByteArray(output, patch.mapColors);
            } else {
                output.writeByte(0);
            }
        }

        private static Optional<MapPatch> read(ByteBuf input) {
            short width = input.readUnsignedByte();
            if (width > 0) {
                short height = input.readUnsignedByte();
                short startX = input.readUnsignedByte();
                short startY = input.readUnsignedByte();
                byte[] mapColors = FriendlyByteBuf.readByteArray(input);
                return Optional.of(new MapPatch(startX, startY, width, height, mapColors));
            }
            return Optional.empty();
        }

        public void applyToMap(MapItemSavedData map) {
            for (int x = 0; x < this.width; ++x) {
                for (int y = 0; y < this.height; ++y) {
                    map.setColor(this.startX + x, this.startY + y, this.mapColors[x + y * this.width]);
                }
            }
        }
    }
}

