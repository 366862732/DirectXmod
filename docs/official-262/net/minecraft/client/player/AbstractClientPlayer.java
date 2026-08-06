/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.player;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractClientPlayer
extends Player
implements ClientAvatarEntity {
    private @Nullable PlayerInfo playerInfo;
    private final boolean showExtraEars;
    private final ClientAvatarState clientAvatarState = new ClientAvatarState();

    public AbstractClientPlayer(ClientLevel level, GameProfile gameProfile) {
        super(level, gameProfile);
        this.showExtraEars = "deadmau5".equals(this.getGameProfile().name());
    }

    @Override
    public @Nullable GameType gameMode() {
        PlayerInfo info = this.getPlayerInfo();
        return info != null ? info.getGameMode() : null;
    }

    protected @Nullable PlayerInfo getPlayerInfo() {
        if (this.playerInfo == null) {
            this.playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(this.getUUID());
        }
        return this.playerInfo;
    }

    @Override
    public void tick() {
        this.clientAvatarState.tick(this.position(), this.getDeltaMovement());
        super.tick();
    }

    protected void addWalkedDistance(float distance) {
        this.clientAvatarState.addWalkDistance(distance);
    }

    @Override
    public ClientAvatarState avatarState() {
        return this.clientAvatarState;
    }

    @Override
    public PlayerSkin getSkin() {
        PlayerInfo info = this.getPlayerInfo();
        return info == null ? DefaultPlayerSkin.get(this.getUUID()) : info.getSkin();
    }

    @Override
    public  @Nullable Parrot.Variant getParrotVariantOnShoulder(boolean left) {
        return (left ? this.getShoulderParrotLeft() : this.getShoulderParrotRight()).orElse(null);
    }

    @Override
    public void rideTick() {
        super.rideTick();
        this.avatarState().resetBob();
    }

    @Override
    public void aiStep() {
        this.updateBob();
        super.aiStep();
    }

    protected void updateBob() {
        float tBob = !this.onGround() || this.isDeadOrDying() || this.isSwimming() ? 0.0f : Math.min(0.1f, (float)this.getDeltaMovement().horizontalDistance());
        this.avatarState().updateBob(tBob);
    }

    public float getFieldOfViewModifier(boolean firstPerson, float effectScale) {
        float walkingSpeed;
        float modifier = 1.0f;
        if (this.getAbilities().flying) {
            modifier *= 1.1f;
        }
        if ((walkingSpeed = this.getAbilities().getWalkingSpeed()) != 0.0f) {
            float speedFactor = (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) / walkingSpeed;
            modifier *= (speedFactor + 1.0f) / 2.0f;
        }
        if (this.isUsingItem()) {
            if (this.getUseItem().is(Items.BOW)) {
                float scale = Math.min((float)this.getTicksUsingItem() / 20.0f, 1.0f);
                modifier *= 1.0f - Mth.square(scale) * 0.15f;
            } else if (firstPerson && this.isScoping()) {
                return 0.1f;
            }
        }
        return Mth.lerp(effectScale, 1.0f, modifier);
    }

    @Override
    public boolean showExtraEars() {
        return this.showExtraEars;
    }
}

