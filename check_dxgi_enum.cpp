#include <stdio.h>
#include <dxgi.h>
int main() {
    printf("DXGI enum check:\n");
    printf("  R8G8B8A8_UNORM   = %d\n", DXGI_FORMAT_R8G8B8A8_UNORM);
    printf("  R8G8B8A8_SNORM   = %d\n", DXGI_FORMAT_R8G8B8A8_SNORM);
    printf("  R16_UNORM        = %d\n", DXGI_FORMAT_R16_UNORM);
    printf("  R16G16_UNORM     = %d\n", DXGI_FORMAT_R16G16_UNORM);
    printf("  R16G16B16A16_UNORM = %d\n", DXGI_FORMAT_R16G16B16A16_UNORM);
    printf("  R32_UINT         = %d\n", DXGI_FORMAT_R32_UINT);
    printf("  R32G32_UINT      = %d\n", DXGI_FORMAT_R32G32_UINT);
    printf("  R32_FLOAT        = %d\n", DXGI_FORMAT_R32_FLOAT);
    printf("  R32G32_FLOAT     = %d\n", DXGI_FORMAT_R32G32_FLOAT);
    printf("  R32G32B32_FLOAT  = %d\n", DXGI_FORMAT_R32G32B32_FLOAT);
    printf("  R32G32B32A32_FLOAT = %d\n", DXGI_FORMAT_R32G32B32A32_FLOAT);
    return 0;
}
