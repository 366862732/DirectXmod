// Find actual DXGI enum values on this machine
#include <stdio.h>
#include <dxgi.h>
int main() {
    printf("=== DXGI Format Enum Values ===\n");
    printf("R8G8B8A8_SNORM       = %d\n", DXGI_FORMAT_R8G8B8A8_SNORM);
    printf("R8G8B8A8_UNORM       = %d\n", DXGI_FORMAT_R8G8B8A8_UNORM);
    printf("R16_UNORM            = %d\n", DXGI_FORMAT_R16_UNORM);
    printf("R16G16_UNORM         = %d\n", DXGI_FORMAT_R16G16_UNORM);
    printf("R16G16B16A16_UNORM   = %d\n", DXGI_FORMAT_R16G16B16A16_UNORM);
    printf("R32_UINT             = %d\n", DXGI_FORMAT_R32_UINT);
    printf("R32G32_UINT          = %d\n", DXGI_FORMAT_R32G32_UINT);
    printf("R32G32B32_UINT       = %d\n", DXGI_FORMAT_R32G32B32_UINT);
    printf("R32_FLOAT            = %d\n", DXGI_FORMAT_R32_FLOAT);
    printf("R32G32_FLOAT         = %d\n", DXGI_FORMAT_R32G32_FLOAT);
    printf("R32G32B32_FLOAT      = %d\n", DXGI_FORMAT_R32G32B32_FLOAT);
    printf("R32G32B32A32_FLOAT   = %d\n", DXGI_FORMAT_R32G32B32A32_FLOAT);
    printf("R16_FLOAT            = %d\n", DXGI_FORMAT_R16_FLOAT);
    printf("R16G16_FLOAT         = %d\n", DXGI_FORMAT_R16G16_FLOAT);
    printf("R16G16B16_FLOAT      = %d\n", DXGI_FORMAT_R16G16B16_FLOAT);
    printf("R16G16B16A16_FLOAT   = %d\n", DXGI_FORMAT_R16G16B16A16_FLOAT);
    printf("R10G10B10A2_UNORM    = %d\n", DXGI_FORMAT_R10G10B10A2_UNORM);
    printf("R11G11B10_FLOAT      = %d\n", DXGI_FORMAT_R11G11B10_FLOAT);
    printf("D32_FLOAT            = %d\n", DXGI_FORMAT_D32_FLOAT);
    printf("D24_UNORM_S8_UINT    = %d\n", DXGI_FORMAT_D24_UNORM_S8_UINT);
    printf("D16_UNORM            = %d\n", DXGI_FORMAT_D16_UNORM);
    return 0;
}
