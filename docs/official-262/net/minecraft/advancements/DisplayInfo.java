/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.advancements;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;

public class DisplayInfo {
    public static final Codec<DisplayInfo> CODEC = RecordCodecBuilder.create(i -> i.group((App)ItemStackTemplate.CODEC.fieldOf("icon").forGetter(DisplayInfo::getIcon), (App)ComponentSerialization.CODEC.fieldOf("title").forGetter(DisplayInfo::getTitle), (App)ComponentSerialization.CODEC.fieldOf("description").forGetter(DisplayInfo::getDescription), (App)ClientAsset.ResourceTexture.CODEC.optionalFieldOf("background").forGetter(DisplayInfo::getBackground), (App)AdvancementType.CODEC.optionalFieldOf("frame", (Object)AdvancementType.TASK).forGetter(DisplayInfo::getType), (App)Codec.BOOL.optionalFieldOf("show_toast", (Object)true).forGetter(DisplayInfo::shouldShowToast), (App)Codec.BOOL.optionalFieldOf("announce_to_chat", (Object)true).forGetter(DisplayInfo::shouldAnnounceChat), (App)Codec.BOOL.optionalFieldOf("hidden", (Object)false).forGetter(DisplayInfo::isHidden)).apply((Applicative)i, DisplayInfo::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, DisplayInfo> STREAM_CODEC = StreamCodec.ofMember(DisplayInfo::serializeToNetwork, DisplayInfo::fromNetwork);
    private final Component title;
    private final Component description;
    private final ItemStackTemplate icon;
    private final Optional<ClientAsset.ResourceTexture> background;
    private final AdvancementType type;
    private final boolean showToast;
    private final boolean announceChat;
    private final boolean hidden;
    private float x;
    private float y;

    public DisplayInfo(ItemStackTemplate icon, Component title, Component description, Optional<ClientAsset.ResourceTexture> background, AdvancementType type, boolean showToast, boolean announceChat, boolean hidden) {
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.background = background;
        this.type = type;
        this.showToast = showToast;
        this.announceChat = announceChat;
        this.hidden = hidden;
    }

    public void setLocation(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Component getTitle() {
        return this.title;
    }

    public Component getDescription() {
        return this.description;
    }

    public ItemStackTemplate getIcon() {
        return this.icon;
    }

    public Optional<ClientAsset.ResourceTexture> getBackground() {
        return this.background;
    }

    public AdvancementType getType() {
        return this.type;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public boolean shouldShowToast() {
        return this.showToast;
    }

    public boolean shouldAnnounceChat() {
        return this.announceChat;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    private void serializeToNetwork(RegistryFriendlyByteBuf output) {
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(output, this.title);
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(output, this.description);
        ItemStackTemplate.STREAM_CODEC.encode(output, this.icon);
        output.writeEnum(this.type);
        int flags = 0;
        if (this.background.isPresent()) {
            flags |= 1;
        }
        if (this.showToast) {
            flags |= 2;
        }
        if (this.hidden) {
            flags |= 4;
        }
        output.writeInt(flags);
        this.background.map(ClientAsset::id).ifPresent(output::writeIdentifier);
        output.writeFloat(this.x);
        output.writeFloat(this.y);
    }

    private static DisplayInfo fromNetwork(RegistryFriendlyByteBuf input) {
        Component title = (Component)ComponentSerialization.TRUSTED_STREAM_CODEC.decode(input);
        Component description = (Component)ComponentSerialization.TRUSTED_STREAM_CODEC.decode(input);
        ItemStackTemplate icon = (ItemStackTemplate)ItemStackTemplate.STREAM_CODEC.decode(input);
        AdvancementType frame = input.readEnum(AdvancementType.class);
        int flags = input.readInt();
        Optional<ClientAsset.ResourceTexture> background = (flags & 1) != 0 ? Optional.of(new ClientAsset.ResourceTexture(input.readIdentifier())) : Optional.empty();
        boolean showToast = (flags & 2) != 0;
        boolean hidden = (flags & 4) != 0;
        DisplayInfo info = new DisplayInfo(icon, title, description, background, frame, showToast, false, hidden);
        info.setLocation(input.readFloat(), input.readFloat());
        return info;
    }
}

