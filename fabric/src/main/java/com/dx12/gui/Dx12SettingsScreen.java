package com.dx12.gui;

import com.dx12.config.Dx12Config;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * GL4DX12 Mod Settings Screen.
 *
 * The anti-aliasing preference is persisted to config only. It will be wired
 * to the native DX12 backend once the backend implements AA.
 */
public class Dx12SettingsScreen extends Screen {
    private static final Component TITLE = Component.literal("D3D12 Mod Settings");
    
    private static final Component AA_NONE = Component.literal("None");
    private static final Component AA_FXAA = Component.literal("FXAA");
    private static final Component AA_SMAA = Component.literal("SMAA (placeholder)");
    private static final Component AA_TAA = Component.literal("TAA (placeholder)");
    
    private static final Component[] AA_VALUES = {AA_NONE, AA_FXAA, AA_SMAA, AA_TAA};
    
    private final Screen parent;

    public Dx12SettingsScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int y = this.height / 2 - 40;
        int btnWidth = 240;

        // ── Anti-aliasing mode (manual cycling button) ────
        int currentAa = Dx12Config.getInstance().getAaMode();
        var aaButton = Button.builder(
            Component.literal("Anti-Aliasing: ").append(AA_VALUES[currentAa]),
            (btn) -> {
                int next = (Dx12Config.getInstance().getAaMode() + 1) % 4;
                Dx12Config.getInstance().setAaMode(next);
                btn.setMessage(Component.literal("Anti-Aliasing: ").append(AA_VALUES[next]));
            }
        ).bounds(centerX - btnWidth / 2, y, btnWidth, 20).build();
        this.addRenderableWidget(aaButton);
        y += 40;

        // ── Back button ─────────────────────────────────
        this.addRenderableWidget(Button.builder(
            Component.literal("Done"),
            (btn) -> this.onClose()
        ).bounds(centerX - 50, this.height - 40, 100, 20).build());
    }

    @Override
    public void onClose() {
        // MC 26.2 renamed Minecraft.setScreen → setScreenAndShow
        this.minecraft.setScreenAndShow(parent);
    }
}
