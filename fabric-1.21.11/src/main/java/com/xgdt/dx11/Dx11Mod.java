package com.xgdt.dx11;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.xgdt.dx11.config.Dx11Config;
import com.xgdt.dx11.perf.PerfState;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the additive Minecraft 1.21.11 / D3D11 backend.
 *
 * <p>This module is deliberately independent from the 26.2 / D3D12 module. It
 * activates automatically on Windows when the D3D11 probe succeeds; otherwise
 * the game uses the vanilla OpenGL path.</p>
 */
public final class Dx11Mod implements ClientModInitializer {
    public static final String MOD_ID = "gl4dx11";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        // Load the experimental perf config even when the D3D11 backend is off:
        // /dx11perf must be able to explain why the perf layer is inactive.
        PerfState.init();

        if (!Dx11Config.isWindows()) {
            LOGGER.info("[dx11] Not running on Windows; D3D11 backend disabled.");
            return;
        }
        if (!Dx11Config.isDx11Requested()) {
            LOGGER.info("[dx11] Backend '{}' does not select D3D11.", Dx11Config.backend());
            return;
        }

        Dx11Config.applyFeatureLevelCap();
        logVertexFormat("BLOCK", DefaultVertexFormat.BLOCK);
        logVertexFormat(
            "POSITION_COLOR_NORMAL_LINE_WIDTH",
            DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH);

        Dx11Config.ensureProbe();
        String result = Dx11Config.probeResult();
        PerfState.refresh();
        if (result.startsWith("OK")) {
            LOGGER.info("[dx11] Probe: {}", result);
            LOGGER.info("[dx11] D3D11 backend selected. perf active={}", PerfState.active());
        } else {
            LOGGER.warn("[dx11] Probe: {}", result);
            LOGGER.warn("[dx11] D3D11 probe failed; using the vanilla OpenGL path.");
        }
    }

    /**
     * Phase 0 verification: log the exact runtime vertex layout used by the
     * terrain path.  The region renderer's rebasing and compact-format work
     * depend on this, so it is recorded in the baseline document.
     */
    private static void logVertexFormat(String label, VertexFormat format) {
        LOGGER.info("[dx11] {} stride={} elements={}", label, format.getVertexSize(), format.getElements().size());
        for (VertexFormatElement element : format.getElements()) {
            LOGGER.info(
                "[dx11]   {}[{}] offset={} type={} count={} usage={}",
                label,
                element.index(),
                format.getOffset(element),
                element.type(),
                element.count(),
                element.usage());
        }
    }
}
