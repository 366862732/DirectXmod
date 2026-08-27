#include <windows.h>
#include <dxgi1_6.h>
#include <stdio.h>
int main() {
    printf("DXGI_FORMAT values:\n");
    printf("  R8G8B8A8_UNORM = %d\n", DXGI_FORMAT_R8G8B8A8_UNORM);
    printf("  R8G8B8A8_SNORM = %d\n", DXGI_FORMAT_R8G8B8A8_SNORM);
    printf("  R16_UNORM      = %d\n", DXGI_FORMAT_R16_UNORM);
    printf("  R16G16_UNORM   = %d\n", DXGI_FORMAT_R16G16_UNORM);
    printf("  R16G16B16A16_UNORM = %d\n", DXGI_FORMAT_R16G16B16A16_UNORM);
    printf("  R16G16B16A16_UINT = %d\n", DXGI_FORMAT_R16G16B16A16_UINT);
    printf("  R32_UINT       = %d\n", DXGI_FORMAT_R32_UINT);
    printf("  R32G32_UINT    = %d\n", DXGI_FORMAT_R32G32_UINT);
    printf("  R32G32B32A32_UINT = %d\n", DXGI_FORMAT_R32G32B32A32_UINT);
    printf("  R32_FLOAT      = %d\n", DXGI_FORMAT_R32_FLOAT);
    printf("  R32G32_FLOAT   = %d\n", DXGI_FORMAT_R32G32_FLOAT);
    printf("  R32G32B32_FLOAT= %d\n", DXGI_FORMAT_R32G32B32_FLOAT);
    printf("  R32G32B32A32_FLOAT = %d\n", DXGI_FORMAT_R32G32B32A32_FLOAT);
    printf("  R8G8B8A8_UINT  = %d\n", DXGI_FORMAT_R8G8B8A8_UINT);
    printf("  R8G8B8A8_SINT  = %d\n", DXGI_FORMAT_R8G8B8A8_SINT);
    // Print fmt=6 and fmt=28
    DXGI_FORMAT f6 = (DXGI_FORMAT)6;
    DXGI_FORMAT f28 = (DXGI_FORMAT)28;
    printf("\nfmt=6  -> DXGI_FORMAT %d\n", f6);
    printf("fmt=28 -> DXGI_FORMAT %d\n", f28);
    return 0;
}
