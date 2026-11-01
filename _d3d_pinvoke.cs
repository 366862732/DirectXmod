using System;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;

class Program
{
    // HRESULT D3DCompile(LPCVOID, SIZE_T, LPCSTR, D3D_SHADER_MACRO*, ID3DInclude*,
    //                    LPCSTR entry, LPCSTR target, UINT flags1, UINT flags2,
    //                    ID3DBlob** code, ID3DBlob** errors)
    [DllImport("d3dcompiler_47.dll", CallingConvention = CallingConvention.StdCall)]
    static extern int D3DCompile(
        IntPtr pSrcData, IntPtr srcDataSize,
        IntPtr pSourceName,
        IntPtr pDefines, IntPtr pInclude,
        [MarshalAs(UnmanagedType.LPStr)] string pEntrypoint,
        [MarshalAs(UnmanagedType.LPStr)] string pTarget,
        uint flags1, uint flags2,
        out IntPtr ppCode, out IntPtr ppErrorMsgs);

    [DllImport("d3dcompiler_47.dll", CallingConvention = CallingConvention.StdCall)]
    static extern IntPtr D3DGetBlobPart(IntPtr blob, int part, ref Guid riid);

    const string HEADER = @"cbuffer ShaderPackUniforms : register(b0)
{
    float4 Viewport;
    float4 Time;
    float4 CameraPosition;
    float4 SunDirection;
    float4 PassParams;
    float4 Sky;
    float4 SkyColor;
    float4 SunsetColor;
    float4 Misc;
    float4x4 InvViewProj;
    float4x4 ViewRotation;
    float4 Options[6];
};
";

    static int Main()
    {
        string sky = @"d:\dx12-lib-template-26.1.2\shaderpacks\StormBreak\shaders\composite\sky.hlsl";
        string fin = @"d:\dx12-lib-template-26.1.2\shaderpacks\StormBreak\shaders\final\final.hlsl";
        bool ok = true;
        ok &= Compile("sky", sky);
        ok &= Compile("final", fin);
        return ok ? 0 : 1;
    }

    static bool Compile(string name, string path)
    {
        string src = HEADER + File.ReadAllText(path);
        byte[] bytes = Encoding.UTF8.GetBytes(src);
        IntPtr buf = Marshal.AllocHGlobal(bytes.Length);
        try
        {
            Marshal.Copy(bytes, 0, buf, bytes.Length);
            IntPtr code, errors;
            int hr = D3DCompile(buf, new IntPtr(bytes.Length), IntPtr.Zero, IntPtr.Zero,
                IntPtr.Zero, "main", "ps_5_1", 0, 0, out code, out errors);
            if (hr != 0)
            {
                Console.WriteLine("=== " + name + " FAILED hr=0x" + hr.ToString("X8") + " ===");
                if (errors != IntPtr.Zero)
                {
                    Console.WriteLine(ReadBlob(errors));
                    Marshal.Release(errors);
                }
                return false;
            }
            Console.WriteLine("=== " + name + " COMPILE OK (" + bytes.Length + " bytes) ===");
            if (code != IntPtr.Zero) Marshal.Release(code);
            if (errors != IntPtr.Zero) Marshal.Release(errors);
            return true;
        }
        finally
        {
            Marshal.FreeHGlobal(buf);
        }
    }

    // ID3DBlob: vtable[0]=QueryInterface [1]=AddRef [2]=Release [3]=GetBufferPointer [4]=GetBufferSize
    static string ReadBlob(IntPtr blob)
    {
        IntPtr vtbl = Marshal.ReadIntPtr(blob);
        IntPtr getPtr = Marshal.ReadIntPtr(vtbl, 3 * IntPtr.Size);
        IntPtr getSize = Marshal.ReadIntPtr(vtbl, 4 * IntPtr.Size);
        var fnPtr = (GetBufferPointerDelegate)Marshal.GetDelegateForFunctionPointer(getPtr, typeof(GetBufferPointerDelegate));
        var fnSize = (GetBufferSizeDelegate)Marshal.GetDelegateForFunctionPointer(getSize, typeof(GetBufferSizeDelegate));
        IntPtr p = fnPtr(blob);
        UIntPtr n = fnSize(blob);
        int len = (int)n.ToUInt64();
        byte[] data = new byte[len];
        Marshal.Copy(p, data, 0, len);
        return Encoding.UTF8.GetString(data);
    }

    [UnmanagedFunctionPointer(CallingConvention.StdCall)]
    delegate IntPtr GetBufferPointerDelegate(IntPtr self);
    [UnmanagedFunctionPointer(CallingConvention.StdCall)]
    delegate UIntPtr GetBufferSizeDelegate(IntPtr self);
}
