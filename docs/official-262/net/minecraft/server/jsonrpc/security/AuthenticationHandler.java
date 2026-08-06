/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  com.mojang.logging.LogUtils
 *  io.netty.buffer.Unpooled
 *  io.netty.channel.ChannelDuplexHandler
 *  io.netty.channel.ChannelHandler$Sharable
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.channel.ChannelPromise
 *  io.netty.handler.codec.http.DefaultFullHttpResponse
 *  io.netty.handler.codec.http.HttpHeaderNames
 *  io.netty.handler.codec.http.HttpRequest
 *  io.netty.handler.codec.http.HttpResponse
 *  io.netty.handler.codec.http.HttpResponseStatus
 *  io.netty.handler.codec.http.HttpVersion
 *  io.netty.util.AttributeKey
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.jsonrpc.security;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import net.minecraft.server.jsonrpc.security.SecurityConfig;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@ChannelHandler.Sharable
public class AuthenticationHandler
extends ChannelDuplexHandler {
    private final Logger LOGGER = LogUtils.getLogger();
    private static final AttributeKey<Boolean> AUTHENTICATED_KEY = AttributeKey.valueOf((String)"authenticated");
    private static final AttributeKey<Boolean> ATTR_WEBSOCKET_ALLOWED = AttributeKey.valueOf((String)"websocket_auth_allowed");
    private static final String SUBPROTOCOL_VALUE = "minecraft-v1";
    private static final String SUBPROTOCOL_HEADER_PREFIX = "minecraft-v1,";
    public static final String BEARER_PREFIX = "Bearer ";
    private final SecurityConfig securityConfig;
    private final Set<String> allowedOrigins;

    public AuthenticationHandler(SecurityConfig securityConfig, String allowedOrigins) {
        this.securityConfig = securityConfig;
        this.allowedOrigins = Sets.newHashSet((Object[])allowedOrigins.split(","));
    }

    public void channelRead(ChannelHandlerContext context, Object msg) throws Exception {
        Boolean isAuthenticated;
        String clientIp = this.getClientIp(context);
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest)msg;
            SecurityCheckResult result = this.performSecurityChecks(request);
            if (result.isAllowed()) {
                context.channel().attr(AUTHENTICATED_KEY).set((Object)true);
                if (result.isTokenSentInSecWebsocketProtocol()) {
                    context.channel().attr(ATTR_WEBSOCKET_ALLOWED).set((Object)Boolean.TRUE);
                }
            } else {
                this.LOGGER.debug("Authentication rejected for connection with ip {}: {}", (Object)clientIp, (Object)result.getReason());
                context.channel().attr(AUTHENTICATED_KEY).set((Object)false);
                this.sendUnauthorizedResponse(context, result.getReason());
                return;
            }
        }
        if (Boolean.TRUE.equals(isAuthenticated = (Boolean)context.channel().attr(AUTHENTICATED_KEY).get())) {
            super.channelRead(context, msg);
        } else {
            this.LOGGER.debug("Dropping unauthenticated connection with ip {}", (Object)clientIp);
            context.close();
        }
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        HttpResponse response;
        if (msg instanceof HttpResponse && (response = (HttpResponse)msg).status().code() == HttpResponseStatus.SWITCHING_PROTOCOLS.code() && ctx.channel().attr(ATTR_WEBSOCKET_ALLOWED).get() != null && ((Boolean)ctx.channel().attr(ATTR_WEBSOCKET_ALLOWED).get()).equals(Boolean.TRUE)) {
            response.headers().set((CharSequence)HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL, (Object)SUBPROTOCOL_VALUE);
        }
        super.write(ctx, msg, promise);
    }

    private SecurityCheckResult performSecurityChecks(HttpRequest request) {
        String tokenInAuthorizationHeader = this.parseTokenInAuthorizationHeader(request);
        if (tokenInAuthorizationHeader != null) {
            if (this.isValidApiKey(tokenInAuthorizationHeader)) {
                return SecurityCheckResult.allowed();
            }
            return SecurityCheckResult.denied("Invalid API key");
        }
        String tokenInSecWebsocketProtocolHeader = this.parseTokenInSecWebsocketProtocolHeader(request);
        if (tokenInSecWebsocketProtocolHeader != null) {
            if (!this.isAllowedOriginHeader(request)) {
                return SecurityCheckResult.denied("Origin Not Allowed");
            }
            if (this.isValidApiKey(tokenInSecWebsocketProtocolHeader)) {
                return SecurityCheckResult.allowed(true);
            }
            return SecurityCheckResult.denied("Invalid API key");
        }
        return SecurityCheckResult.denied("Missing API key");
    }

    private boolean isAllowedOriginHeader(HttpRequest request) {
        String originHeader = request.headers().get((CharSequence)HttpHeaderNames.ORIGIN);
        if (originHeader == null || originHeader.isEmpty()) {
            return false;
        }
        return this.allowedOrigins.contains(originHeader);
    }

    private @Nullable String parseTokenInAuthorizationHeader(HttpRequest request) {
        String authHeader = request.headers().get((CharSequence)HttpHeaderNames.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    private @Nullable String parseTokenInSecWebsocketProtocolHeader(HttpRequest request) {
        String authHeader = request.headers().get((CharSequence)HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);
        if (authHeader != null && authHeader.startsWith(SUBPROTOCOL_HEADER_PREFIX)) {
            return authHeader.substring(SUBPROTOCOL_HEADER_PREFIX.length()).trim();
        }
        return null;
    }

    public boolean isValidApiKey(String suppliedKey) {
        if (suppliedKey.isEmpty()) {
            return false;
        }
        byte[] suppliedKeyBytes = suppliedKey.getBytes(StandardCharsets.UTF_8);
        byte[] configuredKeyBytes = this.securityConfig.secretKey().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(suppliedKeyBytes, configuredKeyBytes);
    }

    private String getClientIp(ChannelHandlerContext context) {
        InetSocketAddress remoteAddress = (InetSocketAddress)context.channel().remoteAddress();
        return remoteAddress.getAddress().getHostAddress();
    }

    private void sendUnauthorizedResponse(ChannelHandlerContext context, String reason) {
        String responseBody = "{\"error\":\"Unauthorized\",\"message\":\"" + reason + "\"}";
        byte[] content = responseBody.getBytes(StandardCharsets.UTF_8);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED, Unpooled.wrappedBuffer((byte[])content));
        response.headers().set((CharSequence)HttpHeaderNames.CONTENT_TYPE, (Object)"application/json");
        response.headers().set((CharSequence)HttpHeaderNames.CONTENT_LENGTH, (Object)content.length);
        response.headers().set((CharSequence)HttpHeaderNames.CONNECTION, (Object)"close");
        context.writeAndFlush((Object)response).addListener(future -> context.close());
    }

    private static class SecurityCheckResult {
        private final boolean allowed;
        private final String reason;
        private final boolean tokenSentInSecWebsocketProtocol;

        private SecurityCheckResult(boolean allowed, String reason, boolean tokenSentInSecWebsocketProtocol) {
            this.allowed = allowed;
            this.reason = reason;
            this.tokenSentInSecWebsocketProtocol = tokenSentInSecWebsocketProtocol;
        }

        public static SecurityCheckResult allowed() {
            return new SecurityCheckResult(true, null, false);
        }

        public static SecurityCheckResult allowed(boolean tokenSentInSecWebsocketProtocol) {
            return new SecurityCheckResult(true, null, tokenSentInSecWebsocketProtocol);
        }

        public static SecurityCheckResult denied(String reason) {
            return new SecurityCheckResult(false, reason, false);
        }

        public boolean isAllowed() {
            return this.allowed;
        }

        public String getReason() {
            return this.reason;
        }

        public boolean isTokenSentInSecWebsocketProtocol() {
            return this.tokenSentInSecWebsocketProtocol;
        }
    }
}

