package com.dx12;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 * Main entry point for GL4DX12. wgpu renders blue bg, Java uploads pixels via OpenGL.
 */
public class Dx12Mod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12");

    private static boolean textureCreated = false;
    private static int texId = 0;

    @Override
    public void onInitializeClient() {
        LOGGER.info("GL4DX12 Mod initializing...");

        D3D12Bridge.init();
        String response = D3D12Bridge.sayHello("Hello from Minecraft!");
        LOGGER.info("Rust responded: {}", response);

        String deviceInfo = D3D12Bridge.getDeviceInfo();
        LOGGER.info("Device info: {}", deviceInfo);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.getWindow() == null) return;

            int width = client.getWindow().getWidth();
            int height = client.getWindow().getHeight();
            
            // Debug: confirm every tick
            System.out.println("[WGPU DEBUG] Tick render called: " + width + "x" + height + " textureCreated=" + textureCreated);

            // One-time window setup for Rust renderer
            if (!textureCreated) {
                long hwnd = D3D12Bridge.getWindowHandle();
                System.out.println("[WGPU DEBUG] getWindowHandle returned: " + hwnd);
                if (hwnd != 0) {
                    D3D12Bridge.setWindow(hwnd);
                    LOGGER.info("WGPU window HWND set: 0x{:016x}", hwnd);
                } else {
                    System.out.println("[WGPU DEBUG] hwnd is 0, skipping setWindow");
                }

                // Create OpenGL texture
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
            }

            D3D12Bridge.syncWindowSize(width, height);

            System.out.println("[WGPU DEBUG] Calling renderFrame()...");
            ByteBuffer pixels = D3D12Bridge.renderFrame();
            if (pixels == null || !pixels.hasRemaining()) {
                System.out.println("[WGPU DEBUG] pixels is null or empty");
                return;
            }
            System.out.println("[WGPU DEBUG] Got " + pixels.remaining() + " pixels, uploading to OpenGL...");

            glBindTexture(GL_TEXTURE_2D, texId);
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            glBindTexture(GL_TEXTURE_2D, 0);

            // Draw fullscreen quad
            glPushMatrix();
            glLoadIdentity();
            glOrtho(0, width, 0, height, -1, 1);
            glDisable(GL_DEPTH_TEST);
            glEnable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glBindTexture(GL_TEXTURE_2D, texId);
            glColor4f(1, 1, 1, 1);

            glBegin(GL_QUADS);
            glTexCoord2f(0, 0); glVertex2f(0, 0);
            glTexCoord2f(1, 0); glVertex2f(width, 0);
            glTexCoord2f(1, 1); glVertex2f(width, height);
            glTexCoord2f(0, 1); glVertex2f(0, height);
            glEnd();

            glDisable(GL_TEXTURE_2D);
            glDisable(GL_BLEND);
            glEnable(GL_DEPTH_TEST);
            glPopMatrix();
        });

        LOGGER.info("GL4DX12 Mod initialized!");
    }
}
