/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.contextualbar;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.PartialTickSupplier;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint;

@Environment(value=EnvType.CLIENT)
public class LocatorBar
implements ContextualBar {
    private static final Identifier LOCATOR_BAR_BACKGROUND = Identifier.withDefaultNamespace("hud/locator_bar_background");
    private static final Identifier LOCATOR_BAR_ARROW_UP = Identifier.withDefaultNamespace("hud/locator_bar_arrow_up");
    private static final Identifier LOCATOR_BAR_ARROW_DOWN = Identifier.withDefaultNamespace("hud/locator_bar_arrow_down");
    private static final int DOT_SIZE = 9;
    private static final int VISIBLE_DEGREE_RANGE = 60;
    private static final int ARROW_WIDTH = 7;
    private static final int ARROW_HEIGHT = 5;
    private static final int ARROW_LEFT = 1;
    private static final int ARROW_PADDING = 1;
    private final Minecraft minecraft;

    public LocatorBar(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LOCATOR_BAR_BACKGROUND, this.left(this.minecraft.getWindow()), this.top(this.minecraft.getWindow()), 182, 5);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        int top = this.top(this.minecraft.getWindow());
        Entity cameraEntity = this.minecraft.getCameraEntity();
        if (cameraEntity == null) {
            return;
        }
        Level level = cameraEntity.level();
        TickRateManager tickRateManager = level.tickRateManager();
        PartialTickSupplier partialTickSupplier = entity -> deltaTracker.getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(entity));
        this.minecraft.player.connection.getWaypointManager().forEachWaypoint(cameraEntity, waypoint -> {
            if (waypoint.id().left().map(uuid -> uuid.equals(cameraEntity.getUUID())).orElse(false).booleanValue()) {
                return;
            }
            double angle = waypoint.yawAngleToCamera(level, this.minecraft.gameRenderer.mainCamera(), partialTickSupplier);
            if (angle <= -60.0 || angle > 60.0) {
                return;
            }
            int screenMiddle = Mth.ceil((float)(graphics.guiWidth() - 9) / 2.0f);
            Waypoint.Icon icon = waypoint.icon();
            WaypointStyle style = this.minecraft.gui.hud.getWaypointStyles().get(icon.style);
            float distance = Mth.sqrt((float)waypoint.distanceSquared(cameraEntity));
            Identifier sprite = style.sprite(distance);
            int color = icon.color.orElseGet(() -> (Integer)waypoint.id().map(uuid -> ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9f), name -> ARGB.setBrightness(ARGB.color(255, name.hashCode()), 0.9f)));
            int dotPosition = Mth.floor(angle * 173.0 / 2.0 / 60.0);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, screenMiddle + dotPosition, top - 2, 9, 9, color);
            TrackedWaypoint.PitchDirection pitchDirection = waypoint.pitchDirectionToCamera(level, this.minecraft.gameRenderer, partialTickSupplier);
            if (pitchDirection != TrackedWaypoint.PitchDirection.NONE) {
                Identifier arrowSprite;
                int arrowTop;
                if (pitchDirection == TrackedWaypoint.PitchDirection.DOWN) {
                    arrowTop = 6;
                    arrowSprite = LOCATOR_BAR_ARROW_DOWN;
                } else {
                    arrowTop = -6;
                    arrowSprite = LOCATOR_BAR_ARROW_UP;
                }
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, arrowSprite, screenMiddle + dotPosition + 1, top + arrowTop, 7, 5);
            }
        });
    }
}

