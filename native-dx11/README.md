# native-dx11

Additive Direct3D 11 backend DLL for Minecraft 1.21.11.

This project is separate from `../native` on purpose:

- `dx12_mc.dll` imports `d3d12.dll`, which does not exist on DirectX 10.1-only
  machines (Intel HD 3000).
- `dx11_mc.dll` imports only `d3d11.dll` and `dxgi.dll`.
- `d3dcompiler_47.dll` is loaded dynamically at runtime, so a missing compiler
  DLL is reported as a probe error instead of preventing the DLL from loading.

## Requirements (Windows)

- Visual Studio 2022 Build Tools with **Desktop development with C++**
- CMake 3.20 or newer
- 64-bit JDK 21 (for `jni.h` / `jni_md.h`)
- Windows 10/11, or Windows 7 SP1 with the Platform Update

## Build

Open an **x64 Native Tools Command Prompt for VS 2022** and run:

```bat
cd native-dx11
build.bat
```

The script builds `dx11_mc.dll` and copies it to:

```
fabric-1.21.11\src\main\resources\dx11_mc.dll
```

## Phase 0 probe

The probe creates a temporary D3D11 device, reports the adapter and feature
level, compiles SM4.0 and SM4.1 shaders, then creates/clears/presents a
64x64 legacy swapchain.

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

If the probe reports `ERROR`, the game keeps using vanilla OpenGL; nothing is
installed into the renderer until the backend is explicitly enabled.
