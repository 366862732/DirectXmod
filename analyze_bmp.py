# P27: 直接读取 dump BMP 原始字节 (BGRA bottom-up 32bpp), 分析图集真实内容
# 用法: python analyze_bmp.py
import os, struct, sys

folder = r"d:\dx12-lib-template-26.1.2\图片"
targets = sys.argv[1:] if len(sys.argv) > 1 else None

def load_bmp(path):
    with open(path, "rb") as f:
        data = f.read()
    assert data[:2] == b"BM", "not BMP"
    pix_off = struct.unpack_from("<I", data, 10)[0]
    w = struct.unpack_from("<i", data, 18)[0]
    h_raw = struct.unpack_from("<i", data, 22)[0]
    bpp = struct.unpack_from("<H", data, 28)[0]
    bottom_up = h_raw > 0
    h = abs(h_raw)
    assert bpp == 32, f"bpp={bpp}"
    # bottom-up: BMP row 0 (file) = image last row
    px = []
    row_bytes = w * 4
    for r in range(h):
        src_row = (h - 1 - r) if bottom_up else r
        off = pix_off + src_row * row_bytes
        for x in range(w):
            b = data[off + x*4 + 0]
            g = data[off + x*4 + 1]
            r2 = data[off + x*4 + 2]
            a = data[off + x*4 + 3]
            px.append((r2, g, b, a))
    return w, h, px

def analyze(path):
    name = os.path.basename(path)
    try:
        w, h, px = load_bmp(path)
    except Exception as e:
        print(f"{name}: ERROR {e}")
        return
    n = len(px)
    n_rgb = sum(1 for (r,g,b,a) in px if r > 8 or g > 8 or b > 8)
    n_alpha = sum(1 for (r,g,b,a) in px if a > 8)
    n_both = sum(1 for (r,g,b,a) in px if (r>8 or g>8 or b>8) and a>8)
    avg_r = sum(p[0] for p in px)//n; avg_g = sum(p[1] for p in px)//n
    avg_b = sum(p[2] for p in px)//n; avg_a = sum(p[3] for p in px)//n
    print(f"=== {name}  {w}x{h} ===")
    print(f"  pixels={n}  RGB>8: {n_rgb} ({100.0*n_rgb/n:.1f}%)  A>8: {n_alpha} ({100.0*n_alpha/n:.1f}%)  RGB&alpha: {n_both} ({100.0*n_both/n:.1f}%)")
    print(f"  avgRGB=({avg_r},{avg_g},{avg_b})  avgA={avg_a}")
    # 亮像素边界框 (RGB>8)
    xs = [x for x in range(w) for i,(r,g,b,a) in enumerate(px[i*w+x] for i in range(0))]  # placeholder
    # find bounding box of non-black pixels
    minx, maxx, miny, maxy = w, -1, h, -1
    for y in range(h):
        for x in range(w):
            r,g,b,a = px[y*w+x]
            if r > 8 or g > 8 or b > 8:
                if x < minx: minx = x
                if x > maxx: maxx = x
                if y < miny: miny = y
                if y > maxy: maxy = y
    if maxx >= 0:
        print(f"  content bbox: x[{minx}..{maxx}] y[{miny}..{maxy}] (w={maxx-minx+1} h={maxy-miny+1})")
    else:
        print(f"  content bbox: NONE (all black)")
    # ASCII thumbnail (alpha 忽略, RGB 亮度)
    cols, rows = 64, max(8, 64 * h // w // 1)
    rows = min(rows, 48)
    chars = " .:-=+*#%@"
    print(f"  ASCII (RGB luma, {cols}x{rows}):")
    for ry in range(rows):
        y0 = ry*h//rows; y1 = max(y0+1, (ry+1)*h//rows)
        line = ""
        for rx in range(cols):
            x0 = rx*w//cols; x1 = max(x0+1, (rx+1)*w//cols)
            r=g=b=cnt=0
            for y in range(y0, y1):
                for x in range(x0, x1):
                    pr,pg,pb,pa = px[y*w+x]
                    r+=pr; g+=pg; b+=pb; cnt+=1
            if cnt==0: line+=' '; continue
            r//=cnt; g//=cnt; b//=cnt
            luma = (r*299+g*587+b*114)//1000
            mx=max(r,g,b); mn=min(r,g,b)
            sat = 0 if mx==0 else (mx-mn)*255//mx
            if sat > 90 and luma > 40:
                hue='?'
                if r>g and r>b: hue='R'
                elif g>r and g>b: hue='G'
                elif b>r and b>g: hue='B'
                elif r>120 and g>120 and b<90: hue='Y'
                elif r>120 and b>120 and g<90: hue='M'
                elif g>120 and b>120 and r<90: hue='C'
                elif r>180 and g>180 and b>180: hue='W'
                line += hue
            else:
                line += chars[luma*9//255]
        print("    " + line.rstrip())

files = [os.path.join(folder, f) for f in os.listdir(folder) if f.startswith("dx12_dump") and f.endswith(".bmp")]
files.sort()
for f in files:
    if targets:
        base = os.path.basename(f)
        if not any(t in base for t in targets):
            continue
    analyze(f)
