package com.xgdt.dx12.d3d12;

import java.util.Objects;

/**
 * Immutable snapshot of a DXGI adapter description.
 *
 * Populated by {@link Dx12Device#enumerateAdapters()} so callers can inspect
 * what hardware is available without touching native handles.
 */
public record Dx12AdapterInfo(
    String name,
    long luid,
    int vendorId,
    int deviceId,
    long dedicatedVideoMemory,
    long dedicatedSystemMemory,
    long sharedSystemMemory,
    int maxTextureDimension
) {
    public static Dx12AdapterInfo of(String name, long luid, int vid, int did,
        long dvram, long dsmem, long sshmem, int maxTex) {
        return new Dx12AdapterInfo(
            Objects.requireNonNull(name, "name"),
            luid, vid, did, dvram, dsmem, sshmem, maxTex
        );
    }
}
