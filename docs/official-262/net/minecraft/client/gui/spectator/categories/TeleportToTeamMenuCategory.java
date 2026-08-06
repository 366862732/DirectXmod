/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.spectator.categories;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.gui.spectator.SpectatorMenuCategory;
import net.minecraft.client.gui.spectator.SpectatorMenuItem;
import net.minecraft.client.gui.spectator.categories.TeleportToPlayerMenuCategory;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

@Environment(value=EnvType.CLIENT)
public class TeleportToTeamMenuCategory
implements SpectatorMenuCategory,
SpectatorMenuItem {
    private static final Identifier TELEPORT_TO_TEAM_SPRITE = Identifier.withDefaultNamespace("spectator/teleport_to_team");
    private static final Component TELEPORT_TEXT = Component.translatable("spectatorMenu.team_teleport");
    private static final Component TELEPORT_PROMPT = Component.translatable("spectatorMenu.team_teleport.prompt");
    private final List<SpectatorMenuItem> items;

    public TeleportToTeamMenuCategory() {
        Minecraft minecraft = Minecraft.getInstance();
        this.items = TeleportToTeamMenuCategory.createTeamEntries(minecraft, minecraft.level.getScoreboard());
    }

    private static List<SpectatorMenuItem> createTeamEntries(Minecraft minecraft, Scoreboard scoreboard) {
        return scoreboard.getPlayerTeams().stream().flatMap(team -> TeamSelectionItem.create(minecraft, team).stream()).toList();
    }

    @Override
    public List<SpectatorMenuItem> getItems() {
        return this.items;
    }

    @Override
    public Component getPrompt() {
        return TELEPORT_PROMPT;
    }

    @Override
    public void selectItem(SpectatorMenu menu) {
        menu.selectCategory(this);
    }

    @Override
    public Component getName() {
        return TELEPORT_TEXT;
    }

    @Override
    public void extractIcon(GuiGraphicsExtractor graphics, float brightness, float alpha) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TELEPORT_TO_TEAM_SPRITE, 0, 0, 16, 16, ARGB.colorFromFloat(alpha, brightness, brightness, brightness));
    }

    @Override
    public boolean isEnabled() {
        return !this.items.isEmpty();
    }

    @Environment(value=EnvType.CLIENT)
    private static class TeamSelectionItem
    implements SpectatorMenuItem {
        private final PlayerTeam team;
        private final Supplier<PlayerSkin> iconSkin;
        private final List<PlayerInfo> players;

        private TeamSelectionItem(PlayerTeam team, List<PlayerInfo> players, Supplier<PlayerSkin> iconSkin) {
            this.team = team;
            this.players = players;
            this.iconSkin = iconSkin;
        }

        public static Optional<SpectatorMenuItem> create(Minecraft minecraft, PlayerTeam team) {
            ArrayList<PlayerInfo> players = new ArrayList<PlayerInfo>();
            for (String name : team.getPlayers()) {
                PlayerInfo info = minecraft.getConnection().getPlayerInfo(name);
                if (info == null || info.getGameMode() == GameType.SPECTATOR) continue;
                players.add(info);
            }
            if (players.isEmpty()) {
                return Optional.empty();
            }
            PlayerInfo playerInfo = (PlayerInfo)players.get(RandomSource.createThreadLocalInstance().nextInt(players.size()));
            return Optional.of(new TeamSelectionItem(team, players, playerInfo::getSkin));
        }

        @Override
        public void selectItem(SpectatorMenu menu) {
            menu.selectCategory(new TeleportToPlayerMenuCategory(this.players));
        }

        @Override
        public Component getName() {
            return this.team.getDisplayName();
        }

        @Override
        public void extractIcon(GuiGraphicsExtractor graphics, float brightness, float alpha) {
            Optional<TeamColor> teamColor = this.team.getColor();
            if (teamColor.isPresent()) {
                graphics.fill(1, 1, 15, 15, ARGB.scaleRGB(teamColor.get().rgb(), brightness));
            }
            PlayerFaceExtractor.extractRenderState(graphics, this.iconSkin.get(), 2, 2, 12, ARGB.colorFromFloat(alpha, brightness, brightness, brightness));
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }
}

