# dx11_mc — additive Direct3D 11 backend for Minecraft 1.21.11.
#
# This library is intentionally separate from ../native (dx12_mc.dll):
# dx12_mc.dll imports d3d12.dll, which does not exist on DirectX 10.1-only
# machines. dx11_mc.dll imports only d3d11.dll and dxgi.dll.

cmake_minimum_required(VERSION 3.20)
project(dx11_mc LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ---- Output ----
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)

# ---- JNI headers (JDK) ----
if(NOT DEFINED JAVA_HOME)
    set(JAVA_HOME "C:/Program Files/Java/jdk-21")
endif()
include_directories("${JAVA_HOME}/include" "${JAVA_HOME}/include/win32")

add_library(dx11_mc SHARED
    src/dx11_common.cpp
    src/dx11_device.cpp
    src/dx11_probe.cpp
    src/jni_bridge.cpp
    src/jni_bridge_device.cpp
)

# d3d11 + dxgi only. D3DCompiler is loaded dynamically at runtime
# (d3dcompiler_47/46/43.dll) so that dx11_mc.dll still loads on systems
# without the compiler DLL and the probe can report a clean error instead
# of making the whole JVM fail to resolve the native library.
target_link_libraries(dx11_mc PRIVATE d3d11 dxgi)

if(MSVC)
    target_compile_options(dx11_mc PRIVATE /W3 /utf-8)
    # Link the MSVC runtime statically (/MT).  The target machine is a Windows
    # install that may not have the Visual C++ redistributable, and dx11_mc.dll
    # is the only native component of this backend.  CMP0091 is NEW because of
    # cmake_minimum_required(3.20), so the property is honored.
    set_property(TARGET dx11_mc PROPERTY MSVC_RUNTIME_LIBRARY
        "MultiThreaded$<$<CONFIG:Debug>:Debug>")
endif()
