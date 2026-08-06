/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.ImmutableSet$Builder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.multiplayer.chat;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.chat.ChatRestriction;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;

@Environment(value=EnvType.CLIENT)
public class ChatAbilities {
    public static final ChatAbilities NO_RESTRICTIONS = new ChatAbilities(Set.of());
    private final Set<ChatRestriction> restrictionReasons;
    private final PermissionSet permissions;
    private final Predicate<GuiMessage> visibleMessagesFilter;

    private ChatAbilities(Set<ChatRestriction> restrictionReasons) {
        this.restrictionReasons = restrictionReasons;
        HashSet<Permission> permissionSet = new HashSet<Permission>(Permissions.CHAT_PERMISSIONS);
        for (ChatRestriction restrictionReason : restrictionReasons) {
            restrictionReason.modifyPermissions(permissionSet);
        }
        this.permissions = Set.copyOf(permissionSet)::contains;
        this.visibleMessagesFilter = ChatAbilities.selectVisibleMessages(this);
    }

    private static Predicate<GuiMessage> selectVisibleMessages(ChatAbilities chatAbilities) {
        ImmutableSet.Builder visibleSourcesBuilder = ImmutableSet.builder();
        visibleSourcesBuilder.add((Object)GuiMessageSource.SYSTEM_CLIENT);
        if (chatAbilities.canReceivePlayerMessages()) {
            visibleSourcesBuilder.add((Object)GuiMessageSource.PLAYER);
        }
        if (chatAbilities.canReceiveSystemMessages()) {
            visibleSourcesBuilder.add((Object)GuiMessageSource.SYSTEM_SERVER);
        }
        ImmutableSet visibleSources = visibleSourcesBuilder.build();
        return guiMessage -> visibleSources.contains((Object)guiMessage.source());
    }

    public boolean hasAnyRestrictions() {
        return !this.restrictionReasons.isEmpty();
    }

    public Stream<ChatRestriction> restrictions() {
        return this.restrictionReasons.stream();
    }

    public PermissionSet permissions() {
        return this.permissions;
    }

    public boolean canSendMessages() {
        return this.permissions.hasPermission(Permissions.CHAT_SEND_MESSAGES);
    }

    public boolean canSendCommands() {
        return this.permissions.hasPermission(Permissions.CHAT_SEND_COMMANDS);
    }

    public boolean canReceivePlayerMessages() {
        return this.permissions.hasPermission(Permissions.CHAT_RECEIVE_PLAYER_MESSAGES);
    }

    public boolean canReceiveSystemMessages() {
        return this.permissions.hasPermission(Permissions.CHAT_RECEIVE_SYSTEM_MESSAGES);
    }

    public Predicate<GuiMessage> visibleMessagesFilter() {
        return this.visibleMessagesFilter;
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder {
        private final Set<ChatRestriction> restrictions = new HashSet<ChatRestriction>();

        public Builder addRestriction(ChatRestriction restriction) {
            this.restrictions.add(restriction);
            return this;
        }

        public ChatAbilities build() {
            if (this.restrictions.isEmpty()) {
                return NO_RESTRICTIONS;
            }
            return new ChatAbilities(Set.copyOf(this.restrictions));
        }
    }
}

