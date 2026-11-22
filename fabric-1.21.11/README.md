# DirectXMod for Minecraft 1.21.11

This module implements the additive Direct3D backend for Minecraft 1.21.11. It ships as two jars:

| Jar | Contents | Required |
| --- | --- | --- |
| `build/libs/gl4dx11-0.1.0.jar` | the D3D rendering engine | yes |
| `perf/build/libs/gl4dx11-perf-0.1.0.jar` | the optional performance layer | no |

The core jar works on its own: install it and Minecraft renders through D3D with vanilla optimization behaviour. Add the performance jar alongside it for the region renderer, immediate-mode batching, culling, upload scheduling and the `/dx11perf` overlay. Delete the performance jar to return to plain D3D.

The performance jar depends on the core jar; the core never references the performance jar.

## Requirements

- Windows x64 (Windows 10/11, or Windows 7 SP1 with the Platform Update)
- JDK 21 (64-bit, for `jni.h` / `jni_md.h`)
- Visual Studio 2022 Build Tools with **Desktop development with C++**
- CMake 3.20 or newer

## Install

Put both jars in the Fabric `mods` folder, or only the core jar for plain D3D11 rendering. The performance jar declares a Fabric dependency on `gl4dx11`, so a version mismatch fails at startup instead of at render time.

## Native DLL

The native build produces `dx11_mc.dll`, an additive Direct3D 11 backend DLL for Minecraft 1.21.11.

## Build

Open an **x64 Native Tools Command Prompt for VS 2022** and run:

```bat
cd native-dx11
build.bat

cd ..\fabric-1.21.11
gradlew.bat build
```

The native build writes `native-dx11\build\bin\Release\dx11_mc.dll`. The Gradle build packages it into the core jar when present, so there is no copy step and no tracked DLL in the source resources. `gradlew build` builds both jars; `gradlew :build` builds only the core jar and `gradlew :perf:build` only the performance jar.

## Phase 0 probe

The probe creates a temporary D3D11 device, reports the adapter and feature level, compiles SM4.0 and SM4.1 shaders, then creates/clears/presents a 64x64 legacy swapchain.

Run it without Minecraft:

```bat
cd fabric-1.21.11
gradlew runDx11Probe -Dgl4dx11.nativePath=..\native-dx11\build\bin\Release\dx11_mc.dll
```

Expected on Intel HD 3000 (driver permitting):

```
OK adapter="Intel(R) HD Graphics 3000" vendor=0x8086 device=0x0126 vram=...MB
   featureLevel=D3D_FEATURE_LEVEL_10_1 sm4_0=yes sm4_1=yes
   d3dcompiler=d3dcompiler_47.dll swapchain=64x64 present=yes
```

If the probe reports `ERROR`, the game keeps using vanilla OpenGL. When the probe succeeds, the backend is installed automatically; no extra JVM flag is required.

## Running

The backend selects itself automatically on Windows after a successful native probe. Useful development properties:

| Property | Purpose |
| --- | --- |
| `-Dgl4dx.backend=gl` | Keep the vanilla OpenGL renderer |
| `-Dgl4dx.backend=dx11` | Require D3D11 when the probe succeeds |
| `-Dgl4dx11.nativePath=C:\path\dx11_mc.dll` | Load a locally built DLL |
| `-Dgl4dx11.maxFeatureLevel=10_1` | Clamp the requested feature level |
| `-Dgl4dx11.debug=true` | Verbose per-frame D3D11 logging (very noisy) |
| `-Dgl4dx11.perf.enabled=true` | Force the performance layer on (performance jar only) |

## Scope

The performance jar adds:

- region-based terrain storage, coalesced region draws and a per-frame upload budget
- compact 24-byte terrain vertices when the active shader supports them
- translucent region sorting and optional fog/entity culling
- ImmediatelyFast-style immediate-mode batching, text lookup, GUI layer lookup, map atlas caching and sign-text buffering
- Lithium-style client tick micro-optimizations: a resolved entity-type lookup cache, a client-side ambient sound short-circuit, a biome-particle lookup skip, and bounded entity-section queries for small boxes
- stb_rect_pack sprite-atlas stitching for large atlases, which shortens startup and resource reloads
- hidden-face culling for leaves, which drops the canopy faces that vanilla's shape test cannot reject
- `/dx11perf` counters and a FPS/CPU/GPU debug overlay

The OpenGL renderer remains the fallback. The layer is only active after a successful D3D11 probe.

## Configuration

The performance jar is configured in `config/gl4dx11-perf.json`, created on first launch. New or missing configs start with every performance feature enabled:

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
  "fastEntityLookup": true,
  "skipClientAmbientSounds": true,
  "fastAmbientParticles": true,
  "fastEntityQueries": true,
  "cullLeafFaces": true,
  "leafCullDepth": 2,
  "fastStitcher": true,
  "signTextBuffering": true
}
```

Existing config files keep their stored values. Run `/dx11perf performancemode on` once after upgrading an older config.

`/dx11perf performancemode` switches the whole group:

```text
/dx11perf performancemode on
/dx11perf performancemode off
/dx11perf performancemode toggle
```

`on` also sets `enabled=true`. `off` disables both umbrellas and every individual optimization, but deliberately leaves the overlay and counters live so vanilla-vs-performance comparisons remain possible. Region changes trigger a full chunk rebuild.

The two umbrellas are independent:

- `immediateEnabled` covers batched immediate buffers, text lookup/sorting, font atlas resizing, GUI layer lookup, map atlas generation and sign-text buffering.
- `regionEnabled` covers region arenas/uploads, coalesced region draws, compact vertices, translucent region sorting, upload budget, fog occlusion, deferred offscreen uploads and entity culling.

`fastEntityLookup`, `skipClientAmbientSounds`, `fastAmbientParticles`, `fastEntityQueries` and `fastStitcher` are independent optimizations. `performancemode` switches them with the two umbrellas, but they can also be toggled on their own and never trigger a chunk rebuild.

`cullLeafFaces` is also independent, but it removes geometry, so toggling it rebuilds the loaded chunks. Vanilla culls a block face when the neighbour's face occlusion shape covers it; leaves have an empty occlusion shape, so their interior faces reach the mesh anyway. With the flag on, a leaf face is dropped when the block it faces and the next `leafCullDepth - 1` blocks behind it are all in `#minecraft:leaves`. The default depth of two only removes faces that have a second leaf layer in front of them, which is indistinguishable through a cutout texture; a one-layer canopy keeps its outer surface, and a face whose probe would leave the loaded render region is always kept.

`fastStitcher` is the only one of them that loads resources instead of ticking: atlases with fewer than 100 sprites keep vanilla's exact layout, larger ones are packed with `stb_rect_pack`. Sprite positions stay aligned to `1 << mipLevel`, so mip filtering cannot bleed neighbouring sprites. The flag applies at the next resource reload, so `/dx11perf toggle fastStitcher` takes effect after F3+T.

## Commands

These commands exist only when the performance jar is installed.

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

The panel is drawn at the top-left and hides with F1 or while F3 is open. It shows FPS, current and worst recent frame time, whole-system CPU load, render-thread CPU time, D3D11 timestamp GPU time, draws per frame, texture and upload throughput, adapter/feature-level details and a CPU/GPU/VSync bottleneck badge.

GPU timestamps start at `GameRenderer.render` and end at `flipFrame`, so they measure GPU work rather than idle time. On WARP the CPU percentage includes software rasterization. Use the render-thread and GPU timestamp shares for frame comparisons.

## Validation

WARP validation covered mixin application, shader compilation, terrain and translucent rendering, counters, fallback paths, resource lifetime and the `performancemode` toggle. Hardware FL10.x frame times are still unverified.

WARP is a software rasterizer. Its frame times are not a performance claim.
