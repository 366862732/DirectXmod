/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Strings
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.exception.RealmsHttpException;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LenientJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public interface RealmsError {
    public static final Component NO_MESSAGE = Component.translatable("mco.errorMessage.noDetails");
    public static final Logger LOGGER = LogUtils.getLogger();

    public int errorCode();

    public Component errorMessage();

    public String logMessage();

    public static RealmsError parse(int httpCode, String payload) {
        if (httpCode == 429) {
            return CustomError.SERVICE_BUSY;
        }
        if (Strings.isNullOrEmpty((String)payload)) {
            return CustomError.noPayload(httpCode);
        }
        try {
            JsonObject object = LenientJsonParser.parse(payload).getAsJsonObject();
            String errorReason = GsonHelper.getAsString(object, "reason", null);
            String errorMessage = GsonHelper.getAsString(object, "errorMsg", null);
            int errorCode = GsonHelper.getAsInt(object, "errorCode", -1);
            if (errorMessage != null || errorReason != null || errorCode != -1) {
                return new ErrorWithJsonPayload(httpCode, errorCode != -1 ? errorCode : httpCode, errorReason, errorMessage);
            }
        }
        catch (Exception e) {
            LOGGER.error("Could not parse RealmsError", (Throwable)e);
        }
        return new ErrorWithRawPayload(httpCode, payload);
    }

    @Environment(value=EnvType.CLIENT)
    public record CustomError(int httpCode, @Nullable Component payload) implements RealmsError
    {
        public static final CustomError SERVICE_BUSY = new CustomError(429, Component.translatable("mco.errorMessage.serviceBusy"));
        public static final Component RETRY_MESSAGE = Component.translatable("mco.errorMessage.retry");
        public static final String BODY_TAG = "<body>";
        public static final String CLOSING_BODY_TAG = "</body>";

        public static CustomError unknownCompatibilityResponse(String response) {
            return new CustomError(500, Component.translatable("mco.errorMessage.realmsService.unknownCompatibility", response));
        }

        public static CustomError configurationError() {
            return new CustomError(500, Component.translatable("mco.errorMessage.realmsService.configurationError"));
        }

        public static CustomError connectivityError(RealmsHttpException exception) {
            return new CustomError(500, Component.translatable("mco.errorMessage.realmsService.connectivity", exception.getMessage()));
        }

        public static CustomError retry(int statusCode) {
            return new CustomError(statusCode, RETRY_MESSAGE);
        }

        public static CustomError noPayload(int statusCode) {
            return new CustomError(statusCode, null);
        }

        public static CustomError htmlPayload(int statusCode, String payload) {
            int bodyStart = payload.indexOf(BODY_TAG);
            int bodyEnd = payload.indexOf(CLOSING_BODY_TAG);
            if (bodyStart >= 0 && bodyEnd > bodyStart) {
                return new CustomError(statusCode, Component.literal(payload.substring(bodyStart + BODY_TAG.length(), bodyEnd).trim()));
            }
            LOGGER.error("Got an error with an unreadable html body {}", (Object)payload);
            return new CustomError(statusCode, null);
        }

        @Override
        public int errorCode() {
            return this.httpCode;
        }

        @Override
        public Component errorMessage() {
            return this.payload != null ? this.payload : NO_MESSAGE;
        }

        @Override
        public String logMessage() {
            if (this.payload != null) {
                return String.format(Locale.ROOT, "Realms service error (%d) with message '%s'", this.httpCode, this.payload.getString());
            }
            return String.format(Locale.ROOT, "Realms service error (%d) with no payload", this.httpCode);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record ErrorWithJsonPayload(int httpCode, int code, @Nullable String reason, @Nullable String message) implements RealmsError
    {
        @Override
        public int errorCode() {
            return this.code;
        }

        @Override
        public Component errorMessage() {
            String codeTranslationKey = "mco.errorMessage." + this.code;
            if (Language.getInstance().has(codeTranslationKey)) {
                return Component.translatable(codeTranslationKey);
            }
            if (this.reason != null) {
                String reasonTranslationKey = "mco.errorReason." + this.reason;
                if (Language.getInstance().has(reasonTranslationKey)) {
                    return Component.translatable(reasonTranslationKey);
                }
            }
            return this.message != null ? Component.literal(this.message) : NO_MESSAGE;
        }

        @Override
        public String logMessage() {
            return String.format(Locale.ROOT, "Realms service error (%d/%d/%s) with message '%s'", this.httpCode, this.code, this.reason, this.message);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record ErrorWithRawPayload(int httpCode, String payload) implements RealmsError
    {
        @Override
        public int errorCode() {
            return this.httpCode;
        }

        @Override
        public Component errorMessage() {
            return Component.literal(this.payload);
        }

        @Override
        public String logMessage() {
            return String.format(Locale.ROOT, "Realms service error (%d) with raw payload '%s'", this.httpCode, this.payload);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record AuthenticationError(String message) implements RealmsError
    {
        public static final int ERROR_CODE = 401;

        @Override
        public int errorCode() {
            return 401;
        }

        @Override
        public Component errorMessage() {
            return Component.literal(this.message);
        }

        @Override
        public String logMessage() {
            return String.format(Locale.ROOT, "Realms authentication error with message '%s'", this.message);
        }
    }
}

