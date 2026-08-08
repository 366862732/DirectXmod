import struct, sys, os

def analyze(path):
    with open(path, 'rb') as f:
        data = f.read()
    if data[:2] != b'BM':
        print(f"{path}: not BMP"); return
    off = struct.unpack('<I', data[10:14])[0]
    w = struct.unpack('<i', data[18:22])[0]
    h = struct.unpack('<i', data[22:26])[0]
    bpp = struct.unpack('<H', data[28:30])[0]
    print(f"{os.path.basename(path)}: {w}x{h} bpp={bpp} data={len(data)}B")
    if bpp != 32 or w <= 0 or h <= 0:
        return
    row_bytes = w * 4
    def px(x, y):  # y in image coords (bottom-up); returns (b,g,r,a)
        row = (h - 1 - y)  # native dumps bottom-up; y=0 is top
        base = off + row * row_bytes + x * 4
        return (data[base], data[base+1], data[base+2], data[base+3])
    # 3x3 sample grid
    xs = [0, w//2, w-1]; ys = [0, h//2, h-1]
    print(" 3x3 grid (top-left, center, bottom-right):")
    for yi in ys:
        line = []
        for xi in xs:
            b,g,r,a = px(xi, yi)
            line.append(f"({r:3d},{g:3d},{b:3d},{a:3d})")
        print("   " + "  ".join(line))
    # stats over a coarse grid
    n = 0; total = [0,0,0]; nz = 0; colors = set()
    step = 8
    for y in range(0, h, step):
        for x in range(0, w, step):
            b,g,r,a = px(x, y)
            total[0]+=r; total[1]+=g; total[2]+=b
            if r or g or b: nz += 1
            colors.add((r//32, g//32, b//32))
            n += 1
    print(f" sampled={n} nonblack={nz} ({100.0*nz/n:.1f}%) unique_color_buckets={len(colors)}")
    print(f" avg(R,G,B)=({total[0]//n},{total[1]//n},{total[2]//n})")
    # ascii: 48x27 grid
    cw, ch = max(1, w//48), max(1, h//27)
    chars = " .:-=+*#%@"
    print(" ASCII thumbnail:")
    for y in range(h-1, -1, -ch):
        row = []
        for x in range(0, w, cw):
            b,g,r,a = px(x, y)
            lum = (r*299 + g*587 + b*114) // 1000
            row.append(chars[min(9, lum*10//256)])
        print("  " + "".join(row))

for p in sys.argv[1:]:
    analyze(p)
