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

add_library(dx12_mc SHARED
    src/dx12_device.cpp
    src/jni_bridge.cpp
    src/jni_bridge_p3.cpp
)

target_link_libraries(dx12_mc PRIVATE d3d12 dxgi)

# 警告级别 + 强制源文件按 UTF-8 解析（源码含中文注释，避免 GBK 代码页误读）
if(MSVC)
    target_compile_options(dx12_mc PRIVATE /W3 /utf-8)
endif()
