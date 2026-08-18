package com.dx12.d3d12;

import java.util.List;

/**
 * Standalone test for pure-Java D3D12 device creation via JNA.
 */
public class Dx12DeviceTest {

    public static void main(String[] args) {
        System.out.println("=== DirectX12 Pure-Java Device Test ===");
        System.out.println();

        // Step 1: Enumerate adapters
        System.out.println("[1] Enumerating DXGI adapters...");
        List<Dx12AdapterInfo> adapters = Dx12Device.enumerateAdapters();
        System.out.println("    Found " + adapters.size() + " adapter(s)");
        for (int i = 0; i < adapters.size(); i++) {
            Dx12AdapterInfo info = adapters.get(i);
            long vramGib = info.dedicatedVideoMemory() / (1024L * 1024 * 1024);
            System.out.println("    [" + i + "] " + info.name()
                + " (VRAM: " + vramGib + " GiB)");
        }
        if (adapters.isEmpty()) {
            System.err.println("    ERROR: No D3D12 adapters found!");
            System.exit(1);
        }

        // Step 2: Create D3D12 device
        System.out.println();
        System.out.println("[2] Creating D3D12 device...");
        try (Dx12Device device = Dx12Device.create()) {
            System.out.println("    Device created successfully (ptr=" + device.getDevicePointer() + ")");
            System.out.println("    Device created successfully");
        }

        // Step 3: Re-enumerate after device creation
        System.out.println();
        System.out.println("[3] Verification — re-enumerating adapters...");
        List<Dx12AdapterInfo> afterCreate = Dx12Device.enumerateAdapters();
        System.out.println("    Adapters after device create: " + afterCreate.size());

        System.out.println();
        System.out.println("=== Test PASSED ===");
    }
}
