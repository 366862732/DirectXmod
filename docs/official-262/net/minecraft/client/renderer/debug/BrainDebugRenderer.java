/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.debug;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugBrainDump;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class BrainDebugRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private static final boolean SHOW_NAME_FOR_ALL = true;
    private static final boolean SHOW_PROFESSION_FOR_ALL = false;
    private static final boolean SHOW_BEHAVIORS_FOR_ALL = false;
    private static final boolean SHOW_ACTIVITIES_FOR_ALL = false;
    private static final boolean SHOW_INVENTORY_FOR_ALL = false;
    private static final boolean SHOW_GOSSIPS_FOR_ALL = false;
    private static final boolean SHOW_HEALTH_FOR_ALL = false;
    private static final boolean SHOW_WANTS_GOLEM_FOR_ALL = true;
    private static final boolean SHOW_ANGER_LEVEL_FOR_ALL = false;
    private static final boolean SHOW_NAME_FOR_SELECTED = true;
    private static final boolean SHOW_PROFESSION_FOR_SELECTED = true;
    private static final boolean SHOW_BEHAVIORS_FOR_SELECTED = true;
    private static final boolean SHOW_ACTIVITIES_FOR_SELECTED = true;
    private static final boolean SHOW_MEMORIES_FOR_SELECTED = true;
    private static final boolean SHOW_INVENTORY_FOR_SELECTED = true;
    private static final boolean SHOW_GOSSIPS_FOR_SELECTED = true;
    private static final boolean SHOW_HEALTH_FOR_SELECTED = true;
    private static final boolean SHOW_WANTS_GOLEM_FOR_SELECTED = true;
    private static final boolean SHOW_ANGER_LEVEL_FOR_SELECTED = true;
    private static final int MAX_RENDER_DIST_FOR_BRAIN_INFO = 30;
    private static final int MAX_TARGETING_DIST = 8;
    private static final float TEXT_SCALE = 0.32f;
    private static final int CYAN = -16711681;
    private static final int GRAY = -3355444;
    private static final int PINK = -98404;
    private static final int ORANGE = -23296;
    private final Minecraft minecraft;
    private @Nullable UUID lastLookedAtUuid;

    public BrainDebugRenderer(Minecraft minecraft) {
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
        debugValues.forEachEntity(DebugSubscriptions.BRAINS, (entity, brainDump) -> {
            if (this.minecraft.player.closerThan((Entity)entity, 30.0)) {
                this.renderBrainInfo((Entity)entity, (DebugBrainDump)brainDump);
            }
        });
    }

    private void renderBrainInfo(Entity entity, DebugBrainDump brainDump) {
        boolean selected = this.isMobSelected(entity);
        int row = 0;
        Gizmos.billboardTextOverMob(entity, row, brainDump.name(), -1, 0.48f);
        ++row;
        if (selected) {
            Gizmos.billboardTextOverMob(entity, row, brainDump.profession() + " " + brainDump.xp() + " xp", -1, 0.32f);
            ++row;
        }
        if (selected) {
            int color = brainDump.health() < brainDump.maxHealth() ? -23296 : -1;
            Gizmos.billboardTextOverMob(entity, row, "health: " + String.format(Locale.ROOT, "%.1f", Float.valueOf(brainDump.health())) + " / " + String.format(Locale.ROOT, "%.1f", Float.valueOf(brainDump.maxHealth())), color, 0.32f);
            ++row;
        }
        if (selected && !brainDump.inventory().equals("")) {
            Gizmos.billboardTextOverMob(entity, row, brainDump.inventory(), -98404, 0.32f);
            ++row;
        }
        if (selected) {
            for (String goal : brainDump.behaviors()) {
                Gizmos.billboardTextOverMob(entity, row, goal, -16711681, 0.32f);
                ++row;
            }
        }
        if (selected) {
            for (String activity : brainDump.activities()) {
                Gizmos.billboardTextOverMob(entity, row, activity, -16711936, 0.32f);
                ++row;
            }
        }
        if (brainDump.wantsGolem()) {
            Gizmos.billboardTextOverMob(entity, row, "Wants Golem", -23296, 0.32f);
            ++row;
        }
        if (selected && brainDump.angerLevel() != -1) {
            Gizmos.billboardTextOverMob(entity, row, "Anger Level: " + brainDump.angerLevel(), -98404, 0.32f);
            ++row;
        }
        if (selected) {
            for (String gossip : brainDump.gossips()) {
                if (gossip.startsWith(brainDump.name())) {
                    Gizmos.billboardTextOverMob(entity, row, gossip, -1, 0.32f);
                } else {
                    Gizmos.billboardTextOverMob(entity, row, gossip, -23296, 0.32f);
                }
                ++row;
            }
        }
        if (selected) {
            for (String memory : Lists.reverse(brainDump.memories())) {
                Gizmos.billboardTextOverMob(entity, row, memory, -3355444, 0.32f);
                ++row;
            }
        }
    }

    private boolean isMobSelected(Entity entity) {
        return Objects.equals(this.lastLookedAtUuid, entity.getUUID());
    }

    public Map<BlockPos, List<String>> getGhostPois(DebugValueAccess debugValues) {
        HashMap ghostPois = Maps.newHashMap();
        debugValues.forEachEntity(DebugSubscriptions.BRAINS, (entity, brainDump) -> {
            for (BlockPos poiPos : Iterables.concat(brainDump.pois(), brainDump.potentialPois())) {
                ghostPois.computeIfAbsent(poiPos, k -> Lists.newArrayList()).add(brainDump.name());
            }
        });
        return ghostPois;
    }

    private void updateLastLookedAtUuid() {
        DebugRenderer.getTargetedEntity(this.minecraft.getCameraEntity(), 8).ifPresent(entity -> {
            this.lastLookedAtUuid = entity.getUUID();
        });
    }
}

