// check_dxgi.cpp - 打印 DXGI_FORMAT 枚举值
#include <stdio.h>
int main() {
    // 使用 DXGI 头文件的枚举值
    // 直接硬编码来避免头文件问题
    printf("Checking DXGI format values:\n");

    // 如果这些能编译，说明编译器知道正确的值
    int R8_UNORM            = 55;
    int R8G8_UNORM          = 56;
    int R8G8B8A8_UNORM      = 28;  // 已知
    int R8G8B8A8_SNORM      = 29;  // 已知
    int R16_UNORM           = 33;
    int R16G16_UNORM        = 35;
    int R16G16B16A16_UNORM  = 37;
    int R16G16B16A16_UINT   = 41;
    int R32_UINT            = 43;
    int R32G32_UINT         = 44;
    int R32_FLOAT           = 46;
    int R32G32_FLOAT        = 47;
    int R32G32B32_FLOAT     = 51;
    int R32G32B32A32_FLOAT  = 52;

    printf("If DXGI_FORMAT_R8G8B8A8_UNORM == %d, then the enum is correct.\n", R8G8B8A8_UNORM);
    printf("If DXGI_FORMAT_R32G32B32_FLOAT == %d, then the enum is correct.\n", R32G32B32_FLOAT);

    // But we need the REAL values from the header. Let's try to include it.
    return 0;
}
