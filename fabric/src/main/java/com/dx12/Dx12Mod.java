package com.dx12;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL12;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

public class Dx12Mod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12");

    private static boolean textureCreated = false;
    private static int texId = 0;
    private static ByteBuffer lastPixels = null;
    private static int lastWidth = 0;
    private static int lastHeight = 0;
    private static boolean hudRegistered = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("GL4DX12 Mod initializing...");

        D3D12Bridge.init();
        String response = D3D12Bridge.sayHello("Hello from Minecraft!");
        LOGGER.info("Rust responded: {}", response);

        String deviceInfo = D3D12Bridge.getDeviceInfo();
        LOGGER.info("Device info: {}", deviceInfo);

        // Tick: call Rust render, store pixels
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.getWindow() == null) return;

            int width = client.getWindow().getWidth();
            int height = client.getWindow().getHeight();

            if (!textureCreated) {
                long hwnd = D3D12Bridge.getWindowHandle();
                if (hwnd != 0) {
                    D3D12Bridge.setWindow(hwnd);
                    LOGGER.info("WGPU window HWND set: 0x%016x", hwnd);
                }

                texId = glGenTextures();
                glBindTexture(GL_TEXTURE_2D, texId);
                glTexImage2D(GL_TEXTURE_2D, 0, GL12.GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, 10497);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, 10497);
                glBindTexture(GL_TEXTURE_2D, 0);
                textureCreated = true;
                LOGGER.info("OpenGL texture created: {}", texId);

                // Register HUD callback AFTER Minecraft is fully initialized
                 if (!hudRegistered) {
                     try {
                         java.lang.reflect.Method registerMethod = HudRenderCallback.class.getMethod("register", java.util.function.BiConsumer.class);
                         registerMethod.invoke(null, (java.util.function.BiConsumer<net.minecraft.client.gui.DrawContext, Float>) (dc, tickDelta) -> {
                             LOGGER.info("HudRenderCallback invoked! pixels={}, width={}, height={}",
                                 lastPixels != null ? lastPixels.remaining() : 0, lastWidth, lastHeight);
                             drawOverlay(dc, tickDelta);
                         });
                         hudRegistered = true;
                         LOGGER.info("HudRenderCallback registered");
                     } catch (Exception e) {
                         LOGGER.error("Failed to register HUD callback: {}", e.getMessage(), e);
                     }
                 }
            }

            D3D12Bridge.syncWindowSize(width, height);

            ByteBuffer pixels = D3D12Bridge.renderFrame();
            if (pixels != null && pixels.hasRemaining()) {
                lastPixels = pixels;
                lastWidth = width;
                lastHeight = height;
            }
        });

        LOGGER.info("GL4DX12 Mod initialized!");
    }

    private static void drawOverlay(net.minecraft.client.gui.DrawContext dc, float tickDelta) {
        if (lastPixels == null || !lastPixels.hasRemaining()) {
            LOGGER.debug("No pixels yet");
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;

        int width = client.getWindow().getWidth();
        int height = client.getWindow().getHeight();

        LOGGER.info("drawOverlay: uploading {} bytes to texture", lastPixels.remaining());

        glBindTexture(GL_TEXTURE_2D, texId);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, lastPixels);
        glBindTexture(GL_TEXTURE_2D, 0);

        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glBindTexture(GL_TEXTURE_2D, texId);
        glColor4f(1, 1, 1, 1);

        glBegin(GL_QUADS);
        glTexCoord2f(0, 0); glVertex2i(0, 0);
        glTexCoord2f(1, 0); glVertex2i(width, 0);
        glTexCoord2f(1, 1); glVertex2i(width, height);
        glTexCoord2f(0, 1); glVertex2i(0, height);
        glEnd();

        glBindTexture(GL_TEXTURE_2D, 0);

        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopAttrib();
    }
}
