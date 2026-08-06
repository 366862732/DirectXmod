/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  com.google.common.collect.LinkedHashMultiset
 *  com.google.common.collect.Multiset
 *  com.google.common.collect.Multisets
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

public class MapItem
extends Item {
    public static final int IMAGE_WIDTH = 128;
    public static final int IMAGE_HEIGHT = 128;

    public MapItem(Item.Properties properties) {
        super(properties);
    }

    public static ItemStack create(ServerLevel level, int originX, int originZ, byte scale, boolean trackPosition, boolean unlimitedTracking) {
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        MapId newId = MapItem.createNewSavedData(level, originX, originZ, scale, trackPosition, unlimitedTracking, level.dimension());
        map.set(DataComponents.MAP_ID, newId);
        return map;
    }

    public static @Nullable MapItemSavedData getSavedData(@Nullable MapId id, Level level) {
        return id == null ? null : level.getMapData(id);
    }

    public static @Nullable MapItemSavedData getSavedData(ItemStack itemStack, Level level) {
        MapId id = itemStack.get(DataComponents.MAP_ID);
        return MapItem.getSavedData(id, level);
    }

    private static MapId createNewSavedData(ServerLevel level, int xSpawn, int zSpawn, int scale, boolean trackingPosition, boolean unlimitedTracking, ResourceKey<Level> dimension) {
        MapItemSavedData newData = MapItemSavedData.createFresh(xSpawn, zSpawn, (byte)scale, trackingPosition, unlimitedTracking, dimension);
        MapId id = level.getFreeMapId();
        level.setMapData(id, newData);
        return id;
    }

    public void update(Level level, Entity player, MapItemSavedData data) {
        if (level.dimension() != data.dimension || !(player instanceof Player)) {
            return;
        }
        Player player2 = (Player)player;
        int scale = 1 << data.scale;
        int centerX = data.centerX;
        int centerZ = data.centerZ;
        int playerImgX = Mth.floor(player.getX() - (double)centerX) / scale + 64;
        int playerImgY = Mth.floor(player.getZ() - (double)centerZ) / scale + 64;
        int radius = 128 / scale;
        if (level.dimensionType().hasCeiling()) {
            radius /= 2;
        }
        MapItemSavedData.HoldingPlayer holdingPlayer = data.getHoldingPlayer(player2);
        ++holdingPlayer.step;
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();
        boolean foundConsecutiveChanges = false;
        for (int imgX = playerImgX - radius + 1; imgX < playerImgX + radius; ++imgX) {
            if ((imgX & 0xF) != (holdingPlayer.step & 0xF) && !foundConsecutiveChanges) continue;
            foundConsecutiveChanges = false;
            double previousAverageAreaHeight = 0.0;
            for (int imgY = playerImgY - radius - 1; imgY < playerImgY + radius; ++imgY) {
                double diff;
                if (imgX < 0 || imgY < -1 || imgX >= 128 || imgY >= 128) continue;
                int distanceToPlayerSqr = Mth.square(imgX - playerImgX) + Mth.square(imgY - playerImgY);
                boolean ditherBlack = distanceToPlayerSqr > (radius - 2) * (radius - 2);
                int averagingAreaMinX = (centerX / scale + imgX - 64) * scale;
                int averagingAreaMinZ = (centerZ / scale + imgY - 64) * scale;
                LinkedHashMultiset colorCount = LinkedHashMultiset.create();
                LevelChunk chunk = level.getChunk(SectionPos.blockToSectionCoord(averagingAreaMinX), SectionPos.blockToSectionCoord(averagingAreaMinZ));
                if (chunk.isEmpty()) continue;
                int waterDepth = 0;
                double averageAreaHeight = 0.0;
                if (level.dimensionType().hasCeiling()) {
                    int ceilingNoise = averagingAreaMinX + averagingAreaMinZ * 231871;
                    if (((ceilingNoise = ceilingNoise * ceilingNoise * 31287121 + ceilingNoise * 11) >> 20 & 1) == 0) {
                        colorCount.add((Object)Blocks.DIRT.defaultBlockState().getMapColor(level, BlockPos.ZERO), 10);
                    } else {
                        colorCount.add((Object)Blocks.STONE.defaultBlockState().getMapColor(level, BlockPos.ZERO), 100);
                    }
                    averageAreaHeight = 100.0;
                } else {
                    for (int averagingAreaDeltaX = 0; averagingAreaDeltaX < scale; ++averagingAreaDeltaX) {
                        for (int averagingAreaDeltaZ = 0; averagingAreaDeltaZ < scale; ++averagingAreaDeltaZ) {
                            BlockState state;
                            blockPos.set(averagingAreaMinX + averagingAreaDeltaX, 0, averagingAreaMinZ + averagingAreaDeltaZ);
                            int columnY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, blockPos.getX(), blockPos.getZ()) + 1;
                            if (columnY > level.getMinY()) {
                                do {
                                    blockPos.setY(--columnY);
                                } while ((state = chunk.getBlockState(blockPos)).getMapColor(level, blockPos) == MapColor.NONE && columnY > level.getMinY());
                                if (columnY > level.getMinY() && !state.getFluidState().isEmpty()) {
                                    BlockState belowBlock;
                                    int solidY = columnY - 1;
                                    belowPos.set(blockPos);
                                    do {
                                        belowPos.setY(solidY--);
                                        belowBlock = chunk.getBlockState(belowPos);
                                        ++waterDepth;
                                    } while (solidY > level.getMinY() && !belowBlock.getFluidState().isEmpty());
                                    state = this.getCorrectStateForFluidBlock(level, state, blockPos);
                                }
                            } else {
                                state = Blocks.BEDROCK.defaultBlockState();
                            }
                            data.checkBanners(level, blockPos.getX(), blockPos.getZ());
                            averageAreaHeight += (double)columnY / (double)(scale * scale);
                            colorCount.add((Object)state.getMapColor(level, blockPos));
                        }
                    }
                }
                MapColor color = (MapColor)Iterables.getFirst((Iterable)Multisets.copyHighestCountFirst((Multiset)colorCount), (Object)MapColor.NONE);
                MapColor.Brightness brightness = color == MapColor.WATER ? ((diff = (double)(waterDepth /= scale * scale) * 0.1 + (double)(imgX + imgY & 1) * 0.2) < 0.5 ? MapColor.Brightness.HIGH : (diff > 0.9 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL)) : ((diff = (averageAreaHeight - previousAverageAreaHeight) * 4.0 / (double)(scale + 4) + ((double)(imgX + imgY & 1) - 0.5) * 0.4) > 0.6 ? MapColor.Brightness.HIGH : (diff < -0.6 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL));
                previousAverageAreaHeight = averageAreaHeight;
                if (imgY < 0 || distanceToPlayerSqr >= radius * radius || ditherBlack && (imgX + imgY & 1) == 0) continue;
                foundConsecutiveChanges |= data.updateColor(imgX, imgY, color.getPackedId(brightness));
            }
        }
    }

    private BlockState getCorrectStateForFluidBlock(Level level, BlockState state, BlockPos pos) {
        FluidState fluidState = state.getFluidState();
        if (!fluidState.isEmpty() && !state.isFaceSturdy(level, pos, Direction.UP)) {
            return fluidState.createLegacyBlock();
        }
        return state;
    }

    private static boolean isBiomeWatery(boolean[] isBiomeWatery, int x, int z) {
        return isBiomeWatery[z * 128 + x];
    }

    public static void renderBiomePreviewMap(ServerLevel level, ItemStack mapItemStack) {
        MapItemSavedData data = MapItem.getSavedData(mapItemStack, (Level)level);
        if (data == null) {
            return;
        }
        if (level.dimension() != data.dimension) {
            return;
        }
        int scale = 1 << data.scale;
        int centerX = data.centerX;
        int centerZ = data.centerZ;
        boolean[] isBiomeWatery = new boolean[16384];
        int unscaledStartX = centerX / scale - 64;
        int unscaledStartZ = centerZ / scale - 64;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int row = 0; row < 128; ++row) {
            for (int column = 0; column < 128; ++column) {
                Holder<Biome> biome = level.getBiome(pos.set((unscaledStartX + column) * scale, 0, (unscaledStartZ + row) * scale));
                isBiomeWatery[row * 128 + column] = biome.is(BiomeTags.WATER_ON_MAP_OUTLINES);
            }
        }
        for (int mx = 1; mx < 127; ++mx) {
            for (int mz = 1; mz < 127; ++mz) {
                int waterCount = 0;
                for (int dx = -1; dx < 2; ++dx) {
                    for (int dz = -1; dz < 2; ++dz) {
                        if (dx == 0 && dz == 0 || !MapItem.isBiomeWatery(isBiomeWatery, mx + dx, mz + dz)) continue;
                        ++waterCount;
                    }
                }
                MapColor.Brightness brightness = MapColor.Brightness.LOWEST;
                MapColor newColor = MapColor.NONE;
                if (MapItem.isBiomeWatery(isBiomeWatery, mx, mz)) {
                    newColor = MapColor.COLOR_ORANGE;
                    if (waterCount > 7 && mz % 2 == 0) {
                        switch ((mx + (int)(Mth.sin((float)mz + 0.0f) * 7.0f)) / 8 % 5) {
                            case 0: 
                            case 4: {
                                brightness = MapColor.Brightness.LOW;
                                break;
                            }
                            case 1: 
                            case 3: {
                                brightness = MapColor.Brightness.NORMAL;
                                break;
                            }
                            case 2: {
                                brightness = MapColor.Brightness.HIGH;
                            }
                        }
                    } else if (waterCount > 7) {
                        newColor = MapColor.NONE;
                    } else if (waterCount > 5) {
                        brightness = MapColor.Brightness.NORMAL;
                    } else if (waterCount > 3) {
                        brightness = MapColor.Brightness.LOW;
                    } else if (waterCount > 1) {
                        brightness = MapColor.Brightness.LOW;
                    }
                } else if (waterCount > 0) {
                    newColor = MapColor.COLOR_BROWN;
                    brightness = waterCount > 3 ? MapColor.Brightness.NORMAL : MapColor.Brightness.LOWEST;
                }
                if (newColor == MapColor.NONE) continue;
                data.setColor(mx, mz, newColor.getPackedId(brightness));
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        MapItemSavedData data = MapItem.getSavedData(itemStack, (Level)level);
        if (data == null) {
            return;
        }
        if (owner instanceof Player) {
            Player player = (Player)owner;
            data.tickCarriedBy(player, itemStack, null);
        }
        if (!data.locked && slot != null && slot.getType() == EquipmentSlot.Type.HAND) {
            this.update(level, owner, data);
        }
    }

    @Override
    public void onCraftedPostProcess(ItemStack itemStack, Level level) {
        MapPostProcessing postProcessing = itemStack.remove(DataComponents.MAP_POST_PROCESSING);
        if (postProcessing == null) {
            return;
        }
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            switch (postProcessing) {
                case LOCK: {
                    MapItem.lockMap(itemStack, serverLevel);
                    break;
                }
                case SCALE: {
                    MapItem.scaleMap(itemStack, serverLevel);
                }
            }
        }
    }

    private static void scaleMap(ItemStack itemStack, ServerLevel level) {
        MapItemSavedData original = MapItem.getSavedData(itemStack, (Level)level);
        if (original != null) {
            MapId id = level.getFreeMapId();
            level.setMapData(id, original.scaled());
            itemStack.set(DataComponents.MAP_ID, id);
        }
    }

    private static void lockMap(ItemStack map, ServerLevel level) {
        MapItemSavedData mapData = MapItem.getSavedData(map, (Level)level);
        if (mapData != null) {
            MapId id = level.getFreeMapId();
            MapItemSavedData newData = mapData.locked();
            level.setMapData(id, newData);
            map.set(DataComponents.MAP_ID, id);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockState clicked = context.getLevel().getBlockState(context.getClickedPos());
        if (clicked.is(BlockTags.BANNERS)) {
            MapItemSavedData data;
            if (!context.getLevel().isClientSide() && (data = MapItem.getSavedData(context.getItemInHand(), context.getLevel())) != null && !data.toggleBanner(context.getLevel(), context.getClickedPos())) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }
}

