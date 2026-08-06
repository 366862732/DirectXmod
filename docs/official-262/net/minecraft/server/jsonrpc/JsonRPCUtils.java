/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.jsonrpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;

public class JsonRPCUtils {
    public static final String JSON_RPC_VERSION = "2.0";
    public static final String OPEN_RPC_VERSION = "1.3.2";

    public static JsonObject createSuccessResult(JsonElement id, JsonElement result) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", JSON_RPC_VERSION);
        response.add("id", id);
        response.add("result", result);
        return response;
    }

    public static JsonObject createRequest(@Nullable Integer id, Identifier method, List<JsonElement> params) {
        return JsonRPCUtils.createRequest(id, method.toString(), params);
    }

    public static JsonObject createRequest(@Nullable Integer id, String method, List<JsonElement> params) {
        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", JSON_RPC_VERSION);
        if (id != null) {
            request.addProperty("id", (Number)id);
        }
        request.addProperty("method", method);
        if (!params.isEmpty()) {
            JsonArray jsonArray = new JsonArray(params.size());
            for (JsonElement param : params) {
                jsonArray.add(param);
            }
            request.add("params", (JsonElement)jsonArray);
        }
        return request;
    }

    public static JsonObject createError(JsonElement id, String message, int errorCode, @Nullable String data) {
        JsonObject errorResponse = new JsonObject();
        errorResponse.addProperty("jsonrpc", JSON_RPC_VERSION);
        errorResponse.add("id", id);
        JsonObject error = new JsonObject();
        error.addProperty("code", (Number)errorCode);
        error.addProperty("message", message);
        if (data != null && !data.isBlank()) {
            error.addProperty("data", data);
        }
        errorResponse.add("error", (JsonElement)error);
        return errorResponse;
    }

    public static @Nullable JsonElement getRequestId(JsonObject jsonObject) {
        return jsonObject.get("id");
    }

    public static @Nullable String getMethodName(JsonObject jsonObject) {
        return GsonHelper.getAsString(jsonObject, "method", null);
    }

    public static @Nullable JsonElement getParams(JsonObject jsonObject) {
        return jsonObject.get("params");
    }

    public static @Nullable JsonElement getResult(JsonObject jsonObject) {
        return jsonObject.get("result");
    }

    public static @Nullable JsonObject getError(JsonObject jsonObject) {
        return GsonHelper.getAsJsonObject(jsonObject, "error", null);
    }
}

