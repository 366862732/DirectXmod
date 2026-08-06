/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class PlayerTabOverlay {
    private static final Identifier PING_UNKNOWN_SPRITE = Identifier.withDefaultNamespace("icon/ping_unknown");
    private static final Identifier PING_1_SPRITE = Identifier.withDefaultNamespace("icon/ping_1");
    private static final Identifier PING_2_SPRITE = Identifier.withDefaultNamespace("icon/ping_2");
    private static final Identifier PING_3_SPRITE = Identifier.withDefaultNamespace("icon/ping_3");
    private static final Identifier PING_4_SPRITE = Identifier.withDefaultNamespace("icon/ping_4");
    private static final Identifier PING_5_SPRITE = Identifier.withDefaultNamespace("icon/ping_5");
    private static final Identifier HEART_CONTAINER_BLINKING_SPRITE = Identifier.withDefaultNamespace("hud/heart/container_blinking");
    private static final Identifier HEART_CONTAINER_SPRITE = Identifier.withDefaultNamespace("hud/heart/container");
    private static final Identifier HEART_FULL_BLINKING_SPRITE = Identifier.withDefaultNamespace("hud/heart/full_blinking");
    private static final Identifier HEART_HALF_BLINKING_SPRITE = Identifier.withDefaultNamespace("hud/heart/half_blinking");
    private static final Identifier HEART_ABSORBING_FULL_BLINKING_SPRITE = Identifier.withDefaultNamespace("hud/heart/absorbing_full_blinking");
    private static final Identifier HEART_FULL_SPRITE = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier HEART_ABSORBING_HALF_BLINKING_SPRITE = Identifier.withDefaultNamespace("hud/heart/absorbing_half_blinking");
    private static final Identifier HEART_HALF_SPRITE = Identifier.withDefaultNamespace("hud/heart/half");
    private static final Comparator<PlayerInfo> PLAYER_COMPARATOR = Comparator.comparingInt(p -> -p.getTabListOrder()).thenComparingInt(p -> p.getGameMode() == GameType.SPECTATOR ? 1 : 0).thenComparing(p -> Optionull.mapOrDefault(p.getTeam(), PlayerTeam::getName, "")).thenComparing(p -> p.getProfile().name(), String::compareToIgnoreCase);
    public static final int MAX_ROWS_PER_COL = 20;
    private final Minecraft minecraft;
    private final Hud hud;
    private @Nullable Component footer;
    private @Nullable Component header;
    private boolean visible;
    private final Map<UUID, HealthState> healthStates = new Object2ObjectOpenHashMap();

    public PlayerTabOverlay(Minecraft minecraft, Hud hud) {
        this.minecraft = minecraft;
        this.hud = hud;
    }

    public Component getNameForDisplay(PlayerInfo info) {
        if (info.getTabListDisplayName() != null) {
            return this.decorateName(info, info.getTabListDisplayName().copy());
        }
        return this.decorateName(info, PlayerTeam.formatNameForTeam(info.getTeam(), Component.literal(info.getProfile().name())));
    }

    private Component decorateName(PlayerInfo info, MutableComponent name) {
        return info.getGameMode() == GameType.SPECTATOR ? name.withStyle(ChatFormatting.ITALIC) : name;
    }

    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.healthStates.clear();
            this.visible = visible;
            if (visible) {
                MutableComponent players = ComponentUtils.formatList(this.getPlayerInfos(), Component.literal(", "), this::getNameForDisplay);
                this.minecraft.getNarrator().saySystemNow(Component.translatable("multiplayer.player.list.narration", players));
            }
        }
    }

    private List<PlayerInfo> getPlayerInfos() {
        return this.minecraft.player.connection.getListedOnlinePlayers().stream().sorted(PLAYER_COMPARATOR).limit(80L).toList();
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int screenWidth, Scoreboard scoreboard, @Nullable Objective displayObjective) {
        int slots;
        List<PlayerInfo> playerInfos = this.getPlayerInfos();
        ArrayList<ScoreDisplayEntry> entriesToDisplay = new ArrayList<ScoreDisplayEntry>(playerInfos.size());
        int spacerWidth = this.minecraft.font.width(" ");
        int maxNameWidth = 0;
        int maxScoreWidth = 0;
        for (PlayerInfo info : playerInfos) {
            Component playerName = this.getNameForDisplay(info);
            maxNameWidth = Math.max(maxNameWidth, this.minecraft.font.width(playerName));
            int playerScore = 0;
            MutableComponent formattedPlayerScore = null;
            int playerScoreWidth = 0;
            if (displayObjective != null) {
                ScoreHolder scoreHolder = ScoreHolder.fromGameProfile(info.getProfile());
                ReadOnlyScoreInfo scoreInfo = scoreboard.getPlayerScoreInfo(scoreHolder, displayObjective);
                if (scoreInfo != null) {
                    playerScore = scoreInfo.value();
                }
                if (displayObjective.getRenderType() != ObjectiveCriteria.RenderType.HEARTS) {
                    if (scoreInfo != null) {
                        NumberFormat objectiveDefaultFormat = displayObjective.numberFormatOrDefault(StyledFormat.PLAYER_LIST_DEFAULT);
                        formattedPlayerScore = scoreInfo.formatValue(objectiveDefaultFormat);
                        playerScoreWidth = this.minecraft.font.width(formattedPlayerScore);
                    }
                    maxScoreWidth = Math.max(maxScoreWidth, playerScoreWidth > 0 ? spacerWidth + playerScoreWidth : 0);
                }
            }
            entriesToDisplay.add(new ScoreDisplayEntry(playerName, playerScore, formattedPlayerScore, playerScoreWidth));
        }
        if (!this.healthStates.isEmpty()) {
            Set playerIds = playerInfos.stream().map(player -> player.getProfile().id()).collect(Collectors.toSet());
            this.healthStates.keySet().removeIf(id -> !playerIds.contains(id));
        }
        int rows = slots = playerInfos.size();
        int cols = 1;
        while (rows > 20) {
            rows = (slots + ++cols - 1) / cols;
        }
        boolean showHead = this.minecraft.getConnection().onlineMode();
        int widthForScore = displayObjective != null ? (displayObjective.getRenderType() == ObjectiveCriteria.RenderType.HEARTS ? 90 : maxScoreWidth) : 0;
        int slotWidth = Math.min(cols * ((showHead ? 9 : 0) + maxNameWidth + widthForScore + 13), screenWidth - 50) / cols;
        int xxo = screenWidth / 2 - (slotWidth * cols + (cols - 1) * 5) / 2;
        int yyo = 10;
        int maxLineWidth = slotWidth * cols + (cols - 1) * 5;
        List<FormattedCharSequence> headerLines = null;
        if (this.header != null) {
            headerLines = this.minecraft.font.split(this.header, screenWidth - 50);
            for (FormattedCharSequence formattedCharSequence : headerLines) {
                maxLineWidth = Math.max(maxLineWidth, this.minecraft.font.width(formattedCharSequence));
            }
        }
        List<FormattedCharSequence> footerLines = null;
        if (this.footer != null) {
            footerLines = this.minecraft.font.split(this.footer, screenWidth - 50);
            for (FormattedCharSequence line : footerLines) {
                maxLineWidth = Math.max(maxLineWidth, this.minecraft.font.width(line));
            }
        }
        if (headerLines != null) {
            graphics.fill(screenWidth / 2 - maxLineWidth / 2 - 1, yyo - 1, screenWidth / 2 + maxLineWidth / 2 + 1, yyo + headerLines.size() * this.minecraft.font.lineHeight, Integer.MIN_VALUE);
            for (FormattedCharSequence line : headerLines) {
                int lineWidth = this.minecraft.font.width(line);
                graphics.text(this.minecraft.font, line, screenWidth / 2 - lineWidth / 2, yyo, -1);
                yyo += this.minecraft.font.lineHeight;
            }
            ++yyo;
        }
        graphics.fill(screenWidth / 2 - maxLineWidth / 2 - 1, yyo - 1, screenWidth / 2 + maxLineWidth / 2 + 1, yyo + rows * 9, Integer.MIN_VALUE);
        int n = this.minecraft.options.getBackgroundColor(0x20FFFFFF);
        for (int i = 0; i < slots; ++i) {
            int left;
            int right;
            int col = i / rows;
            int row = i % rows;
            int xo = xxo + col * slotWidth + col * 5;
            int yo = yyo + row * 9;
            graphics.fill(xo, yo, xo + slotWidth, yo + 8, n);
            if (i >= playerInfos.size()) continue;
            PlayerInfo info = playerInfos.get(i);
            ScoreDisplayEntry displayInfo = (ScoreDisplayEntry)entriesToDisplay.get(i);
            GameProfile profile = info.getProfile();
            if (showHead) {
                Player playerByUUID = this.minecraft.level.getPlayerByUUID(profile.id());
                boolean flip = playerByUUID != null && AvatarRenderer.isPlayerUpsideDown(playerByUUID);
                PlayerFaceExtractor.extractRenderState(graphics, info.getSkin().body().texturePath(), xo, yo, 8, info.showHat(), flip, -1);
                xo += 9;
            }
            graphics.text(this.minecraft.font, displayInfo.name, xo, yo, info.getGameMode() == GameType.SPECTATOR ? -1862270977 : -1);
            if (displayObjective != null && info.getGameMode() != GameType.SPECTATOR && (right = (left = xo + maxNameWidth + 1) + widthForScore) - left > 5) {
                this.extractTablistScore(displayObjective, yo, displayInfo, left, right, profile.id(), graphics);
            }
            this.extractPingIcon(graphics, slotWidth, xo - (showHead ? 9 : 0), yo, info);
        }
        if (footerLines != null) {
            graphics.fill(screenWidth / 2 - maxLineWidth / 2 - 1, (yyo += rows * 9 + 1) - 1, screenWidth / 2 + maxLineWidth / 2 + 1, yyo + footerLines.size() * this.minecraft.font.lineHeight, Integer.MIN_VALUE);
            for (FormattedCharSequence line : footerLines) {
                int lineWidth = this.minecraft.font.width(line);
                graphics.text(this.minecraft.font, line, screenWidth / 2 - lineWidth / 2, yyo, -1);
                yyo += this.minecraft.font.lineHeight;
            }
        }
    }

    protected void extractPingIcon(GuiGraphicsExtractor graphics, int slotWidth, int xo, int yo, PlayerInfo info) {
        Identifier sprite = info.getLatency() < 0 ? PING_UNKNOWN_SPRITE : (info.getLatency() < 150 ? PING_5_SPRITE : (info.getLatency() < 300 ? PING_4_SPRITE : (info.getLatency() < 600 ? PING_3_SPRITE : (info.getLatency() < 1000 ? PING_2_SPRITE : PING_1_SPRITE))));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, xo + slotWidth - 11, yo, 10, 8);
    }

    private void extractTablistScore(Objective displayObjective, int yo, ScoreDisplayEntry entry, int left, int right, UUID profileId, GuiGraphicsExtractor graphics) {
        if (displayObjective.getRenderType() == ObjectiveCriteria.RenderType.HEARTS) {
            this.extractTablistHearts(yo, left, right, profileId, graphics, entry.score);
        } else if (entry.formattedScore != null) {
            graphics.text(this.minecraft.font, entry.formattedScore, right - entry.scoreWidth, yo, -1);
        }
    }

    private void extractTablistHearts(int yo, int left, int right, UUID profileId, GuiGraphicsExtractor graphics, int score) {
        int heart;
        HealthState health = this.healthStates.computeIfAbsent(profileId, id -> new HealthState(score));
        health.update(score, this.hud.getGuiTicks());
        int fullHearts = Mth.positiveCeilDiv(Math.max(score, health.displayedValue()), 2);
        int heartsToRender = Math.max(score, Math.max(health.displayedValue(), 20)) / 2;
        boolean blink = health.isBlinking(this.hud.getGuiTicks());
        if (fullHearts <= 0) {
            return;
        }
        int widthPerHeart = Mth.floor(Math.min((float)(right - left - 4) / (float)heartsToRender, 9.0f));
        if (widthPerHeart <= 3) {
            float pct = Mth.clamp((float)score / 20.0f, 0.0f, 1.0f);
            int color = (int)((1.0f - pct) * 255.0f) << 16 | (int)(pct * 255.0f) << 8;
            float hearts = (float)score / 2.0f;
            MutableComponent hpText = Component.translatable("multiplayer.player.list.hp", Float.valueOf(hearts));
            MutableComponent text = right - this.minecraft.font.width(hpText) >= left ? hpText : Component.literal(Float.toString(hearts));
            graphics.text(this.minecraft.font, text, (right + left - this.minecraft.font.width(text)) / 2, yo, ARGB.opaque(color));
            return;
        }
        Identifier sprite = blink ? HEART_CONTAINER_BLINKING_SPRITE : HEART_CONTAINER_SPRITE;
        for (heart = fullHearts; heart < heartsToRender; ++heart) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, left + heart * widthPerHeart, yo, 9, 9);
        }
        for (heart = 0; heart < fullHearts; ++heart) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, left + heart * widthPerHeart, yo, 9, 9);
            if (blink) {
                if (heart * 2 + 1 < health.displayedValue()) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_FULL_BLINKING_SPRITE, left + heart * widthPerHeart, yo, 9, 9);
                }
                if (heart * 2 + 1 == health.displayedValue()) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_HALF_BLINKING_SPRITE, left + heart * widthPerHeart, yo, 9, 9);
                }
            }
            if (heart * 2 + 1 < score) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, heart >= 10 ? HEART_ABSORBING_FULL_BLINKING_SPRITE : HEART_FULL_SPRITE, left + heart * widthPerHeart, yo, 9, 9);
            }
            if (heart * 2 + 1 != score) continue;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, heart >= 10 ? HEART_ABSORBING_HALF_BLINKING_SPRITE : HEART_HALF_SPRITE, left + heart * widthPerHeart, yo, 9, 9);
        }
    }

    public void setFooter(@Nullable Component footer) {
        this.footer = footer;
    }

    public void setHeader(@Nullable Component header) {
        this.header = header;
    }

    public void reset() {
        this.header = null;
        this.footer = null;
    }

    @Environment(value=EnvType.CLIENT)
    private record ScoreDisplayEntry(Component name, int score, @Nullable Component formattedScore, int scoreWidth) {
    }

    @Environment(value=EnvType.CLIENT)
    private static class HealthState {
        private static final long DISPLAY_UPDATE_DELAY = 20L;
        private static final long DECREASE_BLINK_DURATION = 20L;
        private static final long INCREASE_BLINK_DURATION = 10L;
        private int lastValue;
        private int displayedValue;
        private long lastUpdateTick;
        private long blinkUntilTick;

        public HealthState(int value) {
            this.displayedValue = value;
            this.lastValue = value;
        }

        public void update(int value, long tick) {
            if (value != this.lastValue) {
                long blinkDuration = value < this.lastValue ? 20L : 10L;
                this.blinkUntilTick = tick + blinkDuration;
                this.lastValue = value;
                this.lastUpdateTick = tick;
            }
            if (tick - this.lastUpdateTick > 20L) {
                this.displayedValue = value;
            }
        }

        public int displayedValue() {
            return this.displayedValue;
        }

        public boolean isBlinking(long tick) {
            return this.blinkUntilTick > tick && (this.blinkUntilTick - tick) % 6L >= 3L;
        }
    }
}

