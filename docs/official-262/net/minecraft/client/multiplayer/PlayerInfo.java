/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.SignedMessageValidator;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class PlayerInfo {
    private final GameProfile profile;
    private @Nullable Supplier<PlayerSkin> skinLookup;
    private GameType gameMode = GameType.DEFAULT_MODE;
    private int latency;
    private @Nullable Component tabListDisplayName;
    private boolean showHat = true;
    private @Nullable RemoteChatSession chatSession;
    private SignedMessageValidator messageValidator;
    private int tabListOrder;

    public PlayerInfo(GameProfile profile, boolean enforcesSecureChat) {
        this.profile = profile;
        this.messageValidator = PlayerInfo.fallbackMessageValidator(enforcesSecureChat);
    }

    private static Supplier<PlayerSkin> createSkinLookup(GameProfile profile) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean requireSecure = !minecraft.isLocalPlayer(profile.id());
        return minecraft.getSkinManager().createLookup(profile, requireSecure);
    }

    public GameProfile getProfile() {
        return this.profile;
    }

    public @Nullable RemoteChatSession getChatSession() {
        return this.chatSession;
    }

    public SignedMessageValidator getMessageValidator() {
        return this.messageValidator;
    }

    public boolean hasVerifiableChat() {
        return this.chatSession != null;
    }

    protected void setChatSession(RemoteChatSession chatSession) {
        this.chatSession = chatSession;
        this.messageValidator = chatSession.createMessageValidator(ProfilePublicKey.EXPIRY_GRACE_PERIOD);
    }

    protected void clearChatSession(boolean enforcesSecureChat) {
        this.chatSession = null;
        this.messageValidator = PlayerInfo.fallbackMessageValidator(enforcesSecureChat);
    }

    private static SignedMessageValidator fallbackMessageValidator(boolean enforcesSecureChat) {
        return enforcesSecureChat ? SignedMessageValidator.REJECT_ALL : SignedMessageValidator.ACCEPT_UNSIGNED;
    }

    public GameType getGameMode() {
        return this.gameMode;
    }

    protected void setGameMode(GameType gameMode) {
        this.gameMode = gameMode;
    }

    public int getLatency() {
        return this.latency;
    }

    protected void setLatency(int latency) {
        this.latency = latency;
    }

    public PlayerSkin getSkin() {
        if (this.skinLookup == null) {
            this.skinLookup = PlayerInfo.createSkinLookup(this.profile);
        }
        return this.skinLookup.get();
    }

    public @Nullable PlayerTeam getTeam() {
        return Minecraft.getInstance().level.getScoreboard().getPlayersTeam(this.getProfile().name());
    }

    public void setTabListDisplayName(@Nullable Component tabListDisplayName) {
        this.tabListDisplayName = tabListDisplayName;
    }

    public @Nullable Component getTabListDisplayName() {
        return this.tabListDisplayName;
    }

    public void setShowHat(boolean showHat) {
        this.showHat = showHat;
    }

    public boolean showHat() {
        return this.showHat;
    }

    public void setTabListOrder(int tabListOrder) {
        this.tabListOrder = tabListOrder;
    }

    public int getTabListOrder() {
        return this.tabListOrder;
    }
}

