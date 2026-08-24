import zipfile, os, shutil, time

classes_root = r"d:\dx12-lib-template-26.1.2\fabric\build\classes\java\main"
resources_root = r"d:\dx12-lib-template-26.1.2\fabric\src\main\resources"
out_jar = r"d:\dx12-lib-template-26.1.2\fabric\build\libs-new\gl4dx12-0.1.0.jar"
os.makedirs(os.path.dirname(out_jar), exist_ok=True)
dst_game = r"D:\.minecraft\versions\26.2-Fabric_0.19.3\mods\gl4dx12-0.1.0.jar"
dst_dll = r"D:\.minecraft\versions\26.2-Fabric_0.19.3\dx12mod\dx12_mc.dll"
src_dll = r"d:\dx12-lib-template-26.1.2\native\build\bin\Release\dx12_mc.dll"

# Remove old JAR
if os.path.exists(out_jar):
    os.remove(out_jar)

# Create JAR from classes + resources
with zipfile.ZipFile(out_jar, 'w', zipfile.ZIP_DEFLATED) as zf:
    for root, dirs, files in os.walk(classes_root):
        for f in files:
            fp = os.path.join(root, f)
            arcname = fp.replace(classes_root + os.sep, '')
            zf.write(fp, arcname)
    for root, dirs, files in os.walk(resources_root):
        for f in files:
            fp = os.path.join(root, f)
            arcname = fp.replace(resources_root + os.sep, '')
            zf.write(fp, arcname)

print(f"JAR created: {os.path.getsize(out_jar)} bytes at {time.strftime('%H:%M:%S')}")

# Verify
with zipfile.ZipFile(out_jar, 'r') as zf:
    names = [n for n in zf.namelist() if 'BufferBuilderMixin' in n]
    print(f"Mixin entries: {names}")
    data = zf.read('com/dx12/mixin/BufferBuilderMixin.class')
    text = data.decode('utf-8', errors='replace')
    for tok in text.split('\x00'):
        if 'putVec3f' in tok:
            print(f"Descriptor: {tok[:80]}")

# Deploy to game
shutil.copy2(out_jar, dst_game)
print(f"Game JAR deployed: {os.path.getsize(dst_game)} bytes at {time.strftime('%H:%M:%S')}")

shutil.copy2(src_dll, dst_dll)
print(f"DLL deployed: {os.path.getsize(dst_dll)} bytes at {time.strftime('%H:%M:%S')}")
