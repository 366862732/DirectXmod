package com.xgdt.dx11.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Dx11ConfigTest {
    private String originalOsName;
    private String originalBackend;
    private String originalEnabled;

    @BeforeEach
    void saveProperties() {
        originalOsName = System.getProperty("os.name");
        originalBackend = System.getProperty(Dx11Config.BACKEND_PROPERTY);
        originalEnabled = System.getProperty(Dx11Config.ENABLE_PROPERTY);
        Dx11Config.resetProbeState();
    }

    @AfterEach
    void restoreProperties() {
        restore("os.name", originalOsName);
        restore(Dx11Config.BACKEND_PROPERTY, originalBackend);
        restore(Dx11Config.ENABLE_PROPERTY, originalEnabled);
        Dx11Config.resetProbeState();
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @Test
    void backendDefaultsToAuto() {
        System.clearProperty(Dx11Config.BACKEND_PROPERTY);
        assertEquals("auto", Dx11Config.backend());
    }

    @Test
    void backendIsNormalizedToLowerCase() {
        System.setProperty(Dx11Config.BACKEND_PROPERTY, "D3D11");
        assertEquals("d3d11", Dx11Config.backend());
    }

    @Test
    void dx11IsRequestedOnWindows() {
        System.setProperty("os.name", "Windows 10");
        System.setProperty(Dx11Config.BACKEND_PROPERTY, "dx11");
        assertTrue(Dx11Config.isDx11Requested());
    }

    @Test
    void dx11IsNotRequestedForTheGlBackend() {
        System.setProperty("os.name", "Windows 10");
        System.setProperty(Dx11Config.BACKEND_PROPERTY, "gl");
        assertFalse(Dx11Config.isDx11Requested());
    }

    @Test
    void dx11IsNotRequestedOnNonWindows() {
        System.setProperty("os.name", "Linux");
        System.setProperty(Dx11Config.BACKEND_PROPERTY, "dx11");
        assertFalse(Dx11Config.isDx11Requested());
    }

    @Test
    void nonWindowsNeverUsesDx11() {
        System.setProperty("os.name", "Linux");
        System.setProperty(Dx11Config.BACKEND_PROPERTY, "dx11");
        System.setProperty(Dx11Config.ENABLE_PROPERTY, "true");
        assertFalse(Dx11Config.shouldUseDx11());
    }

    @Test
    void probeIsNotRunWhenTheBackendIsNotEnabled() {
        System.setProperty("os.name", "Windows 10");
        System.setProperty(Dx11Config.BACKEND_PROPERTY, "dx11");
        System.clearProperty(Dx11Config.ENABLE_PROPERTY);

        assertFalse(Dx11Config.shouldUseDx11());
        assertEquals("not run", Dx11Config.probeResult());
    }

    @Test
    void probeResultTracksTheSuccessPrefix() {
        Dx11Config.setProbeResult("OK adapter=test");
        assertTrue(Dx11Config.probeOk());

        Dx11Config.setProbeResult("ERROR: no adapter");
        assertFalse(Dx11Config.probeOk());
    }
}
