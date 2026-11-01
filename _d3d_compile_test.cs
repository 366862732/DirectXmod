using System;
using System.IO;
using System.Runtime.InteropServices;

class D3DCompileTest
{
    [DllImport("d3dcompiler_47.dll", CallingConvention = CallingConvention.StdCall)]
    static extern int D3DCompile(byte[] source, int sourceSize, string pEntryName,
        byte[] pArg, IntPtr pInclude, string pMainEntryPoint, string pProfile,
        int flags, ref byte[] ppCode, ref byte[] ppErrorMsgs);

    static void Main()
    {
        string file = @"d:\dx12-lib-template-26.1.2\_test_sky_full.hlsl";
        byte[] source = File.ReadAllBytes(file);
        byte[] code = new byte[0];
        byte[] errors = new byte[0];
        int hr = D3DCompile(source, source.Length, null, null, IntPtr.Zero,
            "main", "ps_5_1", 0, ref code, ref errors);
        if (hr != 0)
        {
            int len = 0;
            for (int i = 0; i < errors.Length; i++)
                if (errors[i] == 0) { len = i; break; }
            string errMsg = System.Text.Encoding.UTF8.GetString(errors, 0, len > 0 ? len : errors.Length);
            Console.WriteLine("D3DCompile FAILED hr=0x" + hr.ToString("X8"));
            Console.WriteLine(errMsg);
        }
        else
        {
            Console.WriteLine("D3DCompile OK, " + code.Length + " bytes");
        }
    }
}
