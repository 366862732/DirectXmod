/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.game;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundChangeGameModePacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket;
import net.minecraft.network.protocol.game.ServerboundDebugSubscriptionRequestPacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetGameRulePacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetTestBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSpectatorActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.network.protocol.game.ServerboundTestInstanceBlockActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.ping.ServerPingPacketListener;

public interface ServerGamePacketListener
extends ServerCommonPacketListener,
ServerPingPacketListener {
    @Override
    default public ConnectionProtocol protocol() {
        return ConnectionProtocol.PLAY;
    }

    public void handleAnimate(ServerboundSwingPacket var1);

    public void handleChat(ServerboundChatPacket var1);

    public void handleChatCommand(ServerboundChatCommandPacket var1);

    public void handleSignedChatCommand(ServerboundChatCommandSignedPacket var1);

    public void handleChatAck(ServerboundChatAckPacket var1);

    public void handleClientCommand(ServerboundClientCommandPacket var1);

    public void handleContainerButtonClick(ServerboundContainerButtonClickPacket var1);

    public void handleContainerClick(ServerboundContainerClickPacket var1);

    public void handlePlaceRecipe(ServerboundPlaceRecipePacket var1);

    public void handleContainerClose(ServerboundContainerClosePacket var1);

    public void handleAttack(ServerboundAttackPacket var1);

    public void handleInteract(ServerboundInteractPacket var1);

    public void handleSpectatorAction(ServerboundSpectatorActionPacket var1);

    public void handleMovePlayer(ServerboundMovePlayerPacket var1);

    public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket var1);

    public void handlePlayerAction(ServerboundPlayerActionPacket var1);

    public void handlePlayerCommand(ServerboundPlayerCommandPacket var1);

    public void handlePlayerInput(ServerboundPlayerInputPacket var1);

    public void handleSetCarriedItem(ServerboundSetCarriedItemPacket var1);

    public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket var1);

    public void handleSignUpdate(ServerboundSignUpdatePacket var1);

    public void handleUseItemOn(ServerboundUseItemOnPacket var1);

    public void handleUseItem(ServerboundUseItemPacket var1);

    public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket var1);

    public void handlePaddleBoat(ServerboundPaddleBoatPacket var1);

    public void handleMoveVehicle(ServerboundMoveVehiclePacket var1);

    public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket var1);

    public void handleAcceptPlayerLoad(ServerboundPlayerLoadedPacket var1);

    public void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket var1);

    public void handleBundleItemSelectedPacket(ServerboundSelectBundleItemPacket var1);

    public void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket var1);

    public void handleSeenAdvancements(ServerboundSeenAdvancementsPacket var1);

    public void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket var1);

    public void handleSetCommandBlock(ServerboundSetCommandBlockPacket var1);

    public void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket var1);

    public void handlePickItemFromBlock(ServerboundPickItemFromBlockPacket var1);

    public void handlePickItemFromEntity(ServerboundPickItemFromEntityPacket var1);

    public void handleRenameItem(ServerboundRenameItemPacket var1);

    public void handleSetBeaconPacket(ServerboundSetBeaconPacket var1);

    public void handleSetGameRule(ServerboundSetGameRulePacket var1);

    public void handleSetStructureBlock(ServerboundSetStructureBlockPacket var1);

    public void handleSetTestBlock(ServerboundSetTestBlockPacket var1);

    public void handleTestInstanceBlockAction(ServerboundTestInstanceBlockActionPacket var1);

    public void handleSelectTrade(ServerboundSelectTradePacket var1);

    public void handleEditBook(ServerboundEditBookPacket var1);

    public void handleEntityTagQuery(ServerboundEntityTagQueryPacket var1);

    public void handleContainerSlotStateChanged(ServerboundContainerSlotStateChangedPacket var1);

    public void handleBlockEntityTagQuery(ServerboundBlockEntityTagQueryPacket var1);

    public void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket var1);

    public void handleJigsawGenerate(ServerboundJigsawGeneratePacket var1);

    public void handleChangeDifficulty(ServerboundChangeDifficultyPacket var1);

    public void handleChangeGameMode(ServerboundChangeGameModePacket var1);

    public void handleLockDifficulty(ServerboundLockDifficultyPacket var1);

    public void handleChatSessionUpdate(ServerboundChatSessionUpdatePacket var1);

    public void handleConfigurationAcknowledged(ServerboundConfigurationAcknowledgedPacket var1);

    public void handleChunkBatchReceived(ServerboundChunkBatchReceivedPacket var1);

    public void handleDebugSubscriptionRequest(ServerboundDebugSubscriptionRequestPacket var1);

    public void handleClientTickEnd(ServerboundClientTickEndPacket var1);
}

