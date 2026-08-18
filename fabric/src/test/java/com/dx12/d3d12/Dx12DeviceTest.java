package com.dx12.d3d12;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class Dx12DeviceTest {

    @Test
    void testEnumerateAdapters() throws Exception {
        System.out.println("[1] Enumerating DXGI adapters...");
        List<Dx12AdapterInfo> adapters = Dx12Device.enumerateAdapters();
        assertFalse(adapters.isEmpty(), "Should find at least one D3D12 adapter");
        var info = adapters.getFirst();
        assertNotNull(info.name(), "Adapter name should not be empty");
        System.out.printf("  Found: %s | LUID=0x%s | Vid=0x%s Did=0x%s VRAM=%d GiB%n",
            info.name(),
            Long.toHexString(info.luid()),
            Integer.toHexString(info.vendorId()),
            Integer.toHexString(info.deviceId()),
            info.dedicatedVideoMemory() / (1024L * 1024 * 1024));
    }

    @Test
    void testCreateDevice() throws Exception {
        System.out.println("[2] Creating D3D12 device...");
        var adapters = Dx12Device.enumerateAdapters();
        if (adapters.isEmpty()) {
            System.out.println("  No adapters found, skipping device creation");
            return;
        }
        try (var device = Dx12Device.create()) {
            assertNotNull(device, "Device should not be null");
            System.out.printf("  Device created successfully (ptr=%s)%n", device.getDevicePointer());
        }
    }
}
