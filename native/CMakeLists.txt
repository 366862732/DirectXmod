cmake_minimum_required(VERSION 3.20)
project(dx12_mc LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ---- 输出 ----
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)

# ---- JNI 头文件（JDK）----
if(NOT DEFINED JAVA_HOME)
    set(JAVA_HOME "C:/Program Files/Java/jdk-26-openjdk/jdk-26")
endif()
include_directories("${JAVA_HOME}/include" "${JAVA_HOME}/include/win32")

# ---- DirectX / Windows SDK ----
# d3d12.lib、dxgi.lib 由 MSVC 的 Windows SDK 环境自动提供。

# ---- NVIDIA NGX（DLSS）SDK ----
# 头文件 + 动态 CRT 版加载器库（nvsdk_ngx_d.lib）：本工程用 /MD，静态 CRT 版
# （nvsdk_ngx_s.lib）会触发 LNK2038 运行库不匹配。加载器把 NGX 链进 dx12_mc.dll，
# 未配置 App ID 时不会产生额外 DLL 依赖，只在显式初始化时返回失败。
set(NGX_ROOT "${CMAKE_SOURCE_DIR}/../DLSS")
include_directories("${NGX_ROOT}/include")

add_library(dx12_mc SHARED
    src/dx12_descriptor_allocator.cpp
    src/dx12_device.cpp
    src/ngx_dlss.cpp
    src/dx12_raytracing.cpp
    src/dx12_rt_scene.cpp
    src/dx12_async_executor.cpp
    src/dx12_async_bundle.cpp
    src/dx12_async_descriptor.cpp
    src/dx12_async_fence.cpp
    src/jni_bridge.cpp
    src/jni_bridge_p3.cpp
    src/jni_bridge_p4.cpp
    src/dx12_surface.cpp
    src/jni_bridge_p5.cpp
    src/jni_bridge_p6.cpp
    src/jni_bridge_p33.cpp
    src/jni_bridge_p68.cpp
)

# d3dcompiler：D3DCompile 把 HLSL 编译为 DXBC（vs_5_1/ps_5_1）
target_link_libraries(dx12_mc PRIVATE d3d12 dxgi d3dcompiler
    "${NGX_ROOT}/lib/Windows_x86_64/x64/nvsdk_ngx_d.lib")

# 警告级别 + 强制源文件按 UTF-8 解析（源码含中文注释，避免 GBK 代码页误读）
if(MSVC)
    target_compile_options(dx12_mc PRIVATE /W3 /utf-8)
endif()
