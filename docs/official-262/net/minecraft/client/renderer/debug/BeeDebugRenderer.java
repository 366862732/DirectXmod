/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugBeeInfo;
import net.minecraft.util.debug.DebugGoalInfo;
import net.minecraft.util.debug.DebugHiveInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class BeeDebugRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private static final boolean SHOW_GOAL_FOR_ALL_BEES = true;
    private static final boolean SHOW_NAME_FOR_ALL_BEES = true;
    private static final boolean SHOW_HIVE_FOR_ALL_BEES = true;
    private static final boolean SHOW_FLOWER_POS_FOR_ALL_BEES = true;
    private static final boolean SHOW_TRAVEL_TICKS_FOR_ALL_BEES = true;
    private static final boolean SHOW_GOAL_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_NAME_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_HIVE_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_FLOWER_POS_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_TRAVEL_TICKS_FOR_SELECTED_BEE = true;
    private static final boolean SHOW_HIVE_MEMBERS = true;
    private static final boolean SHOW_BLACKLISTS = true;
    private static final int MAX_RENDER_DIST_FOR_HIVE_OVERLAY = 30;
    private static final int MAX_RENDER_DIST_FOR_BEE_OVERLAY = 30;
    private static final int MAX_TARGETING_DIST = 8;
    private static final float TEXT_SCALE = 0.32f;
    private static final int ORANGE = -23296;
    private static final int GRAY = -3355444;
    private static final int PINK = -98404;
    private final Minecraft minecraft;
    private @Nullable UUID lastLookedAtUuid;

    public BeeDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        this.doRender(debugValues);
        if (!this.minecraft.player.isSpectator()) {
            this.updateLastLookedAtUuid();
        }
    }

    private void doRender(DebugValueAccess debugValues) {
        BlockPos playerPos = this.getCamera().blockPosition();
        debugValues.forEachEntity(DebugSubscriptions.BEES, (entity, beeInfo) -> {
            if (this.minecraft.player.closerThan((Entity)entity, 30.0)) {
                DebugGoalInfo goalInfo = debugValues.getEntityValue(DebugSubscriptions.GOAL_SELECTORS, (Entity)entity);
                this.renderBeeInfo((Entity)entity, (DebugBeeInfo)beeInfo, goalInfo);
            }
        });
        this.renderFlowerInfos(debugValues);
        Map<BlockPos, Set<UUID>> hiveBlacklistMap = this.createHiveBlacklistMap(debugValues);
        debugValues.forEachBlock(DebugSubscriptions.BEE_HIVES, (pos, hive) -> {
            if (playerPos.closerThan((Vec3i)pos, 30.0)) {
                BeeDebugRenderer.highlightHive(pos);
                Set<UUID> beesWhoBlacklistThisHive = hiveBlacklistMap.getOrDefault(pos, Set.of());
                this.renderHiveInfo((BlockPos)pos, (DebugHiveInfo)hive, (Collection<UUID>)beesWhoBlacklistThisHive, debugValues);
            }
        });
        this.getGhostHives(debugValues).forEach((ghostHivePos, value) -> {
            if (playerPos.closerThan((Vec3i)ghostHivePos, 30.0)) {
                this.renderGhostHive((BlockPos)ghostHivePos, (List<String>)value);
            }
        });
    }

    private Map<BlockPos, Set<UUID>> createHiveBlacklistMap(DebugValueAccess debugValues) {
        HashMap<BlockPos, Set<UUID>> hiveBlacklistMap = new HashMap<BlockPos, Set<UUID>>();
        debugValues.forEachEntity(DebugSubscriptions.BEES, (entity, bee) -> {
            for (BlockPos blacklistedFlowerPos : bee.blacklistedHives()) {
                hiveBlacklistMap.computeIfAbsent(blacklistedFlowerPos, k -> new HashSet()).add(entity.getUUID());
            }
        });
        return hiveBlacklistMap;
    }

    private void renderFlowerInfos(DebugValueAccess debugValues) {
        HashMap<BlockPos, Set> beesPerFlower = new HashMap<BlockPos, Set>();
        debugValues.forEachEntity(DebugSubscriptions.BEES, (entity, bee) -> {
            if (bee.flowerPos().isPresent()) {
                beesPerFlower.computeIfAbsent(bee.flowerPos().get(), k -> new HashSet()).add(entity.getUUID());
            }
        });
        beesPerFlower.forEach((flowerPos, beesWithThisFlower) -> {
            Set beeNames = beesWithThisFlower.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
            int row = 1;
            Gizmos.billboardTextOverBlock(beeNames.toString(), flowerPos, row++, -256, 0.32f);
            Gizmos.billboardTextOverBlock("Flower", flowerPos, row++, -1, 0.32f);
            Gizmos.cuboid(flowerPos, 0.05f, GizmoStyle.fill(ARGB.colorFromFloat(0.3f, 0.8f, 0.8f, 0.0f)));
        });
    }

    private static String getBeeUuidsAsString(Collection<UUID> uuids) {
        if (uuids.isEmpty()) {
            return "-";
        }
        if (uuids.size() > 3) {
            return uuids.size() + " bees";
        }
        return uuids.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet()).toString();
    }

    private static void highlightHive(BlockPos hivePos) {
        float padding = 0.05f;
        Gizmos.cuboid(hivePos, 0.05f, GizmoStyle.fill(ARGB.colorFromFloat(0.3f, 0.2f, 0.2f, 1.0f)));
    }

    private void renderGhostHive(BlockPos ghostHivePos, List<String> hiveMemberNames) {
        float padding = 0.05f;
        Gizmos.cuboid(ghostHivePos, 0.05f, GizmoStyle.fill(ARGB.colorFromFloat(0.3f, 0.2f, 0.2f, 1.0f)));
        Gizmos.billboardTextOverBlock(hiveMemberNames.toString(), ghostHivePos, 0, -256, 0.32f);
        Gizmos.billboardTextOverBlock("Ghost Hive", ghostHivePos, 1, -65536, 0.32f);
    }

    private void renderHiveInfo(BlockPos hivePos, DebugHiveInfo hive, Collection<UUID> beesWhoBlacklistThisHive, DebugValueAccess debugValues) {
        int row = 0;
        if (!beesWhoBlacklistThisHive.isEmpty()) {
            BeeDebugRenderer.renderTextOverHive("Blacklisted by " + BeeDebugRenderer.getBeeUuidsAsString(beesWhoBlacklistThisHive), hivePos, row++, -65536);
        }
        BeeDebugRenderer.renderTextOverHive("Out: " + BeeDebugRenderer.getBeeUuidsAsString(this.getHiveMembers(hivePos, debugValues)), hivePos, row++, -3355444);
        if (hive.occupantCount() == 0) {
            BeeDebugRenderer.renderTextOverHive("In: -", hivePos, row++, -256);
        } else if (hive.occupantCount() == 1) {
            BeeDebugRenderer.renderTextOverHive("In: 1 bee", hivePos, row++, -256);
        } else {
            BeeDebugRenderer.renderTextOverHive("In: " + hive.occupantCount() + " bees", hivePos, row++, -256);
        }
        BeeDebugRenderer.renderTextOverHive("Honey: " + hive.honeyLevel(), hivePos, row++, -23296);
        BeeDebugRenderer.renderTextOverHive(hive.type().getName().getString() + (hive.sedated() ? " (sedated)" : ""), hivePos, row++, -1);
    }

    private void renderBeeInfo(Entity entity, DebugBeeInfo beeInfo, @Nullable DebugGoalInfo goalInfo) {
        boolean selected = this.isBeeSelected(entity);
        int row = 0;
        Gizmos.billboardTextOverMob(entity, row++, beeInfo.toString(), -1, 0.48f);
        if (beeInfo.hivePos().isEmpty()) {
            Gizmos.billboardTextOverMob(entity, row++, "No hive", -98404, 0.32f);
        } else {
            Gizmos.billboardTextOverMob(entity, row++, "Hive: " + this.getPosDescription(entity, beeInfo.hivePos().get()), -256, 0.32f);
        }
        if (beeInfo.flowerPos().isEmpty()) {
            Gizmos.billboardTextOverMob(entity, row++, "No flower", -98404, 0.32f);
        } else {
            Gizmos.billboardTextOverMob(entity, row++, "Flower: " + this.getPosDescription(entity, beeInfo.flowerPos().get()), -256, 0.32f);
        }
        if (goalInfo != null) {
            for (DebugGoalInfo.DebugGoal goal : goalInfo.goals()) {
                if (!goal.isRunning()) continue;
                Gizmos.billboardTextOverMob(entity, row++, goal.name(), -16711936, 0.32f);
            }
        }
        if (beeInfo.travelTicks() > 0) {
            int color = beeInfo.travelTicks() < 2400 ? -3355444 : -23296;
            Gizmos.billboardTextOverMob(entity, row++, "Travelling: " + beeInfo.travelTicks() + " ticks", color, 0.32f);
        }
    }

    private static void renderTextOverHive(String text, BlockPos hivePos, int row, int color) {
        Gizmos.billboardTextOverBlock(text, hivePos, row, color, 0.32f);
    }

    private Camera getCamera() {
        return this.minecraft.gameRenderer.mainCamera();
    }

    private String getPosDescription(Entity entity, BlockPos pos) {
        double dist = pos.distToCenterSqr(entity.position());
        double distRounded = (double)Math.round(dist * 10.0) / 10.0;
        return pos.toShortString() + " (dist " + distRounded + ")";
    }

    private boolean isBeeSelected(Entity entity) {
        return Objects.equals(this.lastLookedAtUuid, entity.getUUID());
    }

    private Collection<UUID> getHiveMembers(BlockPos hivePos, DebugValueAccess debugValues) {
        HashSet<UUID> hiveMembers = new HashSet<UUID>();
        debugValues.forEachEntity(DebugSubscriptions.BEES, (entity, beeInfo) -> {
            if (beeInfo.hasHive(hivePos)) {
                hiveMembers.add(entity.getUUID());
            }
        });
        return hiveMembers;
    }

    private Map<BlockPos, List<String>> getGhostHives(DebugValueAccess debugValues) {
        HashMap<BlockPos, List<String>> ghostHives = new HashMap<BlockPos, List<String>>();
        debugValues.forEachEntity(DebugSubscriptions.BEES, (entity, beeInfo) -> {
            if (beeInfo.hivePos().isPresent() && debugValues.getBlockValue(DebugSubscriptions.BEE_HIVES, beeInfo.hivePos().get()) == null) {
                ghostHives.computeIfAbsent(beeInfo.hivePos().get(), k -> Lists.newArrayList()).add(DebugEntityNameGenerator.getEntityName(entity));
            }
        });
        return ghostHives;
    }

    private void updateLastLookedAtUuid() {
        DebugRenderer.getTargetedEntity(this.minecraft.getCameraEntity(), 8).ifPresent(entity -> {
            this.lastLookedAtUuid = entity.getUUID();
        });
    }
}

