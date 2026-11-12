package com.xgdt.dx11.dx11;

/** Thrown when GLSL -> SPIR-V -> HLSL translation fails. */
public final class Dx11ShaderCompileException extends Exception {
    public Dx11ShaderCompileException(String message) {
        super(message);
    }

    public Dx11ShaderCompileException(String message, Throwable cause) {
        super(message, cause);
    }
}
