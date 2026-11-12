// Phase 0 probe for the additive D3D11 backend.
//
// Verifies, without a Minecraft window, that:
//   * a hardware D3D11 device can be created (feature level 10_1 on HD 3000),
//   * the adapter is reported correctly,
//   * SM4.0 and SM4.1 shaders compile through D3DCompile,
//   * a legacy (DISCARD) swapchain can be created, cleared and presented.
//
// The result is a single-line string beginning with "OK" or "ERROR".
#pragma once

#include <string>

namespace dx11mc {

std::string runProbe();

}  // namespace dx11mc
