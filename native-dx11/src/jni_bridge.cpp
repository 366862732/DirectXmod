// JNI bridge for the additive D3D11 backend.
//
// Symbol names must match com.xgdt.dx11.dx11.Dx11Native exactly.

#include <jni.h>

#include "dx11_probe.h"

#include <string>

extern "C" {

JNIEXPORT jstring JNICALL Java_com_xgdt_dx11_dx11_Dx11Native_dx11Probe(
    JNIEnv* env, jclass) {
    const std::string result = dx11mc::runProbe();
    return env->NewStringUTF(result.c_str());
}

}  // extern "C"
