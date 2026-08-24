import datetime, os, zipfile

# Check dx12mod DLL timestamp
dll_path = r'D:\.minecraft\versions\26.2-Fabric_0.19.3\dx12mod\dx12_mc.dll'
if os.path.exists(dll_path):
    mtime = os.path.getmtime(dll_path)
    print(f'dx12mod DLL: {os.path.getsize(dll_path)} bytes, mod={datetime.datetime.fromtimestamp(mtime)}')

# Check JAR entry timestamp
jar_path = r'D:\.minecraft\versions\26.2-Fabric_0.19.3\mods\gl4dx12-0.1.0.jar'
with zipfile.ZipFile(jar_path, 'r') as z:
    for e in z.infolist():
        if 'dx12_mc' in e.filename:
            print(f'JAR entry: {e.file_size} bytes, mod={e.date_time}')

# Check new DLL
new_dll = r'D:\dx12-lib-template-26.1.2\native\build\bin\Release\dx12_mc.dll'
if os.path.exists(new_dll):
    mtime = os.path.getmtime(new_dll)
    print(f'New DLL: {os.path.getsize(new_dll)} bytes, mod={datetime.datetime.fromtimestamp(mtime)}')
