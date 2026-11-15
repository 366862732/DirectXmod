# DirectXMod D3D11 (Minecraft 1.21.11)

This module implements the additive Direct3D 11 backend for Minecraft 1.21.11
and the experimental performance layer used with `native-dx11`. It is not part
of the 26.2 / D3D12 backend.

## Scope

The layer adds:

- region-based terrain storage, coalesced region draws and a per-frame upload
  budget
- compact 24-byte terrain vertices when the active shader supports them
- translucent region sorting and optional fog/entity culling
- ImmediatelyFast-style immediate-mode batching, text lookup, GUI layer
  lookup, map atlas caching and sign-text buffering
- `/dx11perf` counters and a FPS/CPU/GPU debug overlay

The OpenGL renderer remains the fallback. The layer is only active after a
successful D3D11 probe.

## Build

Windows x64, JDK 21, Visual Studio 2022 with the C++ workload, and CMake.

```bat
cd native-dx11
build.bat

cd ..\fabric-1.21.11
gradlew.bat build
```

The native build writes `native-dx11\build\bin\Release\dx11_mc.dll`. The
Gradle build packages it when present.

## Running

The backend selects itself automatically on Windows after a successful native
probe. Useful development properties:

| Property | Purpose |
| --- | --- |
| `-Dgl4dx12.backend=gl` | Keep the vanilla OpenGL renderer |
| `-Dgl4dx12.backend=dx11` | Require D3D11 when the probe succeeds |
| `-Dgl4dx11.nativePath=C:\path\dx11_mc.dll` | Load a locally built DLL |
| `-Dgl4dx11.maxFeatureLevel=10_1` | Clamp the requested feature level |
| `-Dgl4dx11.debug=true` | Verbose per-frame D3D11 logging (very noisy) |
| `-Dgl4dx11.perf.enabled=true` | Force the performance layer on |

## Configuration

The layer is configured in `config/gl4dx11-perf.json`, created on first
launch. New or missing configs start with every performance feature enabled:

```json
{
  "enabled": true,
  "regionEnabled": true,
  "immediateEnabled": true,
  "regionDraws": true,
  "compactVertices": true,
  "translucentRegion": true,
  "fogOcclusion": true,
  "deferredOffscreenUpdates": true,
  "entityCulling": true,
  "signTextBuffering": true
}
```

Existing config files keep their stored values. Run
`/dx11perf performancemode on` once after upgrading an older config.

`/dx11perf performancemode` switches the whole group:

```text
/dx11perf performancemode on
/dx11perf performancemode off
/dx11perf performancemode toggle
```

`on` also sets `enabled=true`. `off` disables both umbrellas and every
individual optimization, but deliberately leaves the overlay and counters
live so vanilla-vs-performance comparisons remain possible. Region changes
trigger a full chunk rebuild.

The two umbrellas are independent:

- `immediateEnabled` covers batched immediate buffers, text lookup/sorting,
  font atlas resizing, GUI layer lookup, map atlas generation and sign-text
  buffering.
- `regionEnabled` covers region arenas/uploads, coalesced region draws, compact
  vertices, translucent region sorting, upload budget, fog occlusion, deferred
  offscreen uploads and entity culling.

## Commands

```text
/dx11perf stats | reset | leaks | reload | save
/dx11perf debugOverlay [on|off|toggle]
/dx11perf performancemode [on|off|toggle]
/dx11perf toggle
/dx11perf toggle <flag>
/dx11perf regionSize <REGION_4|REGION_8>
```

`REGION_8` is the only recommended region size. `REGION_4` is benchmark-only.

## Overlay

Enable it with:

```text
/dx11perf debugOverlay on
```

The panel is drawn at the top-left and hides with F1 or while F3 is open. It
shows FPS, current and worst recent frame time, whole-system CPU load,
render-thread CPU time, D3D11 timestamp GPU time, draws per frame, texture and
upload throughput, adapter/feature-level details and a CPU/GPU/VSync
bottleneck badge.

GPU timestamps start at `GameRenderer.render` and end at `flipFrame`, so they
measure GPU work rather than idle time. On WARP the CPU percentage includes
software rasterization. Use the render-thread and GPU timestamp shares for
frame comparisons.

## Validation

WARP validation covered mixin application, shader compilation, terrain and
translucent rendering, counters, fallback paths, resource lifetime and the
`performancemode` toggle. Hardware FL10.x frame times are still unverified.

WARP is a software rasterizer. Its frame times are not a performance claim.
