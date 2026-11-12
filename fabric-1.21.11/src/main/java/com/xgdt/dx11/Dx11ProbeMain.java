package com.xgdt.dx11;

import com.xgdt.dx11.dx11.Dx11Native;

/**
 * Standalone Phase 0 probe entry point.
 *
 * <p>Run on Windows with:</p>
 * <pre>
 * gradlew runDx11Probe -Dgl4dx11.nativePath=C:\path\to\dx11_mc.dll
 * </pre>
 *
 * <p>Exit codes: 0 = OK, 1 = probe reported an error, 2 = DLL not loaded,
 * 3 = unexpected exception.</p>
 */
public final class Dx11ProbeMain {
    private Dx11ProbeMain() {
    }

    public static void main(String[] args) {
        if (!Dx11Native.isLoaded()) {
            System.err.println("[dx11] Native library is not loaded.");
            System.exit(2);
        }

        final String result;
        try {
            result = Dx11Native.dx11Probe();
        } catch (Throwable t) {
            System.err.println("[dx11] Probe threw an exception.");
            t.printStackTrace();
            System.exit(3);
            return;
        }

        System.out.println("[dx11] Probe result: " + result);
        System.exit(result != null && result.startsWith("OK") ? 0 : 1);
    }
}
