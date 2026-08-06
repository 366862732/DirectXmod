/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.inventory.tooltip;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

@Environment(value=EnvType.CLIENT)
public class ClientActivePlayersTooltip
implements ClientTooltipComponent {
    private static final int SKIN_SIZE = 10;
    private static final int PADDING = 2;
    private final List<PlayerSkinRenderCache.RenderInfo> activePlayers;

    public ClientActivePlayersTooltip(ActivePlayersTooltip activePlayersTooltip) {
        this.activePlayers = activePlayersTooltip.profiles();
    }

    @Override
    public int getHeight(Font font) {
        return this.activePlayers.size() * 12 + 2;
    }

    private static String getName(PlayerSkinRenderCache.RenderInfo activePlayer) {
        return activePlayer.gameProfile().name();
    }

    @Override
    public int getWidth(Font font) {
        int widest = 0;
        for (PlayerSkinRenderCache.RenderInfo activePlayer : this.activePlayers) {
            int width = font.width(ClientActivePlayersTooltip.getName(activePlayer));
            if (width <= widest) continue;
            widest = width;
        }
        return widest + 10 + 6;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        for (int i = 0; i < this.activePlayers.size(); ++i) {
            PlayerSkinRenderCache.RenderInfo activePlayer = this.activePlayers.get(i);
            int y1 = y + 2 + i * 12;
            PlayerFaceExtractor.extractRenderState(graphics, activePlayer.playerSkin(), x + 2, y1, 10);
            graphics.text(font, ClientActivePlayersTooltip.getName(activePlayer), x + 10 + 4, y1 + 2, -1);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record ActivePlayersTooltip(List<PlayerSkinRenderCache.RenderInfo> profiles) implements TooltipComponent
    {
    }
}

