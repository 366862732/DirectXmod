/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.RealmsText;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class RealmsNotification {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NOTIFICATION_UUID = "notificationUuid";
    private static final String DISMISSABLE = "dismissable";
    private static final String SEEN = "seen";
    private static final String TYPE = "type";
    private static final String VISIT_URL = "visitUrl";
    private static final String INFO_POPUP = "infoPopup";
    private static final Component BUTTON_TEXT_FALLBACK = Component.translatable("mco.notification.visitUrl.buttonText.default");
    private final UUID uuid;
    private final boolean dismissable;
    private final boolean seen;
    private final String type;

    private RealmsNotification(UUID uuid, boolean dismissable, boolean seen, String type) {
        this.uuid = uuid;
        this.dismissable = dismissable;
        this.seen = seen;
        this.type = type;
    }

    public boolean seen() {
        return this.seen;
    }

    public boolean dismissable() {
        return this.dismissable;
    }

    public UUID uuid() {
        return this.uuid;
    }

    public static List<RealmsNotification> parseList(String json) {
        ArrayList<RealmsNotification> result = new ArrayList<RealmsNotification>();
        try {
            JsonArray array = LenientJsonParser.parse(json).getAsJsonObject().get("notifications").getAsJsonArray();
            for (JsonElement element : array) {
                result.add(RealmsNotification.parse(element.getAsJsonObject()));
            }
        }
        catch (Exception e) {
            LOGGER.error("Could not parse list of RealmsNotifications", (Throwable)e);
        }
        return result;
    }

    private static RealmsNotification parse(JsonObject jsonObject) {
        UUID uuid = JsonUtils.getUuidOr(NOTIFICATION_UUID, jsonObject, null);
        if (uuid == null) {
            throw new IllegalStateException("Missing required property notificationUuid");
        }
        boolean dismissable = JsonUtils.getBooleanOr(DISMISSABLE, jsonObject, true);
        boolean seen = JsonUtils.getBooleanOr(SEEN, jsonObject, false);
        String type = JsonUtils.getRequiredString(TYPE, jsonObject);
        RealmsNotification base = new RealmsNotification(uuid, dismissable, seen, type);
        return switch (type) {
            case VISIT_URL -> VisitUrl.parse(base, jsonObject);
            case INFO_POPUP -> InfoPopup.parse(base, jsonObject);
            default -> base;
        };
    }

    @Environment(value=EnvType.CLIENT)
    public static class VisitUrl
    extends RealmsNotification {
        private static final String URL = "url";
        private static final String BUTTON_TEXT = "buttonText";
        private static final String MESSAGE = "message";
        private final String url;
        private final RealmsText buttonText;
        private final RealmsText message;

        private VisitUrl(RealmsNotification base, String url, RealmsText buttonText, RealmsText message) {
            super(base.uuid, base.dismissable, base.seen, base.type);
            this.url = url;
            this.buttonText = buttonText;
            this.message = message;
        }

        public static VisitUrl parse(RealmsNotification base, JsonObject jsonObject) {
            String url = JsonUtils.getRequiredString(URL, jsonObject);
            RealmsText buttonText = JsonUtils.getRequired(BUTTON_TEXT, jsonObject, RealmsText::parse);
            RealmsText message = JsonUtils.getRequired(MESSAGE, jsonObject, RealmsText::parse);
            return new VisitUrl(base, url, buttonText, message);
        }

        public Component getMessage() {
            return this.message.createComponent(Component.translatable("mco.notification.visitUrl.message.default"));
        }

        public Button buildOpenLinkButton(Screen parentScreen) {
            Component buttonLabel = this.buttonText.createComponent(BUTTON_TEXT_FALLBACK);
            return Button.builder(buttonLabel, ConfirmLinkScreen.confirmLink(parentScreen, this.url)).build();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class InfoPopup
    extends RealmsNotification {
        private static final String TITLE = "title";
        private static final String MESSAGE = "message";
        private static final String IMAGE = "image";
        private static final String URL_BUTTON = "urlButton";
        private final RealmsText title;
        private final RealmsText message;
        private final Identifier image;
        private final @Nullable UrlButton urlButton;

        private InfoPopup(RealmsNotification base, RealmsText title, RealmsText message, Identifier image, @Nullable UrlButton urlButton) {
            super(base.uuid, base.dismissable, base.seen, base.type);
            this.title = title;
            this.message = message;
            this.image = image;
            this.urlButton = urlButton;
        }

        public static InfoPopup parse(RealmsNotification base, JsonObject object) {
            RealmsText title = JsonUtils.getRequired(TITLE, object, RealmsText::parse);
            RealmsText message = JsonUtils.getRequired(MESSAGE, object, RealmsText::parse);
            Identifier image = Identifier.parse(JsonUtils.getRequiredString(IMAGE, object));
            UrlButton urlButton = JsonUtils.getOptional(URL_BUTTON, object, UrlButton::parse);
            return new InfoPopup(base, title, message, image, urlButton);
        }

        public @Nullable PopupScreen buildScreen(Screen parentScreen, Consumer<UUID> dismiss) {
            Component title = this.title.createComponent();
            if (title == null) {
                LOGGER.warn("Realms info popup had title with no available translation: {}", (Object)this.title);
                return null;
            }
            PopupScreen.Builder builder = new PopupScreen.Builder(parentScreen, title).setImage(this.image).addMessage(this.message.createComponent(CommonComponents.EMPTY));
            if (this.urlButton != null) {
                builder.addButton(this.urlButton.urlText.createComponent(BUTTON_TEXT_FALLBACK), popup -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.gui.setScreen(new ConfirmLinkScreen(result -> {
                        if (result) {
                            Util.getPlatform().openUri(this.urlButton.url);
                            minecraft.gui.setScreen(parentScreen);
                        } else {
                            minecraft.gui.setScreen((Screen)popup);
                        }
                    }, this.urlButton.url, true));
                    dismiss.accept(this.uuid());
                });
            }
            builder.addButton(CommonComponents.GUI_OK, popup -> {
                popup.onClose();
                dismiss.accept(this.uuid());
            });
            builder.onClose(() -> dismiss.accept(this.uuid()));
            return builder.build();
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record UrlButton(String url, RealmsText urlText) {
        private static final String URL = "url";
        private static final String URL_TEXT = "urlText";

        public static UrlButton parse(JsonObject jsonObject) {
            String url = JsonUtils.getRequiredString(URL, jsonObject);
            RealmsText urlText = JsonUtils.getRequired(URL_TEXT, jsonObject, RealmsText::parse);
            return new UrlButton(url, urlText);
        }
    }
}

