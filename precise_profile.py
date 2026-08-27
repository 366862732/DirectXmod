# P28: 精确行密度分析 —— 每个 atlas 按行统计内容像素, 输出 y-密度剖面,
# 判定内容分布: 均匀 / 贴底 / 贴顶 / 翻转压缩. 也可按矩形区域放大查看.
# 用法: python precise_profile.py [filter词...]  (如 python precise_profile.py 2048x2048 512x512)
import os, struct, sys

folder = r"d:\dx12-lib-template-26.1.2\图片"
filters = sys.argv[1:] or ["2048x2048", "1024x1024", "512x512", "256x128", "128x128", "128x64", "1024x512", "512x256", "2048x1024"]

def load_bmp(path):
    with open(path, "rb") as f:
        data = f.read()
    assert data[:2] == b"BM", "not BMP"
    pix_off = struct.unpack_from("<I", data, 10)[0]
    w = struct.unpack_from("<i", data, 18)[0]
    h_raw = struct.unpack_from("<i", data, 22)[0]
    bpp = struct.unpack_from("<H", data, 28)[0]
    assert bpp == 32, f"bpp={bpp}"
    bottom_up = h_raw > 0
    h = abs(h_raw)
    px = [None] * (w * h)
    row_bytes = w * 4
    for r in range(h):
        src_row = (h - 1 - r) if bottom_up else r
        off = pix_off + src_row * row_bytes
        for x in range(w):
            b = data[off + x*4 + 0]
            g = data[off + x*4 + 1]
            r2 = data[off + x*4 + 2]
            a = data[off + x*4 + 3]
            px[r*w + x] = (r2, g, b, a)
    return w, h, px

def analyze(path):
    name = os.path.basename(path)
    try:
        w, h, px = load_bmp(path)
    except Exception as e:
        print(f"{name}: ERROR {e}")
        return
    # 行密度: 每行非黑像素数 (RGB>8 且 A>8)
    rows = []
    for y in range(h):
        base = y*w
        cnt = 0
        for x in range(w):
            r, g, b, a = px[base+x]
            if (r > 8 or g > 8 or b > 8) and a > 8:
                cnt += 1
        rows.append(cnt)
    total = sum(rows)
    print(f"=== {name}  {w}x{h}  content_rows_total={total}")
    if total == 0:
        print("  (全黑)")
        return
    # bbox
    ys = [y for y, c in enumerate(rows) if c]
    print(f"  bbox y[{min(ys)}..{max(ys)}]  贴底={max(ys)>=h-4}  贴顶={min(ys)<=4}")
    # 32 段密度条
    bands = 40
    bw = max(1, h // bands)
    print(f"  y-density ({bands} bands, each ~{bw}px, char=row/band content%):")
    maxrow = max(rows)
    for bi in range(bands):
        y0 = bi*bw; y1 = min(h, y0+bw)
        seg = sum(rows[y0:y1])
        frac = seg / (max(1, y1-y0)) / max(1, w) * 100.0
        bar = "#" * max(1, int(frac / 5)) if frac > 0 else " "
        print(f"    y{bi*bw:>5}-{y1-1:>5} | {frac:5.1f}% {bar}")
    # 顶部/底部 100 行对比
    def band_stats(y0, y1):
        seg = sum(rows[y0:y1])
        return seg
    top = band_stats(0, min(h, 100))
    bot = band_stats(max(0, h-100), h)
    print(f"  top100px={top}  bot100px={bot}  (总内容={total})")
    # 底部 1/8 区域 vs 其余区域
    q = h // 8
    bottom8 = band_stats(h-q, h)
    print(f"  底部1/8={bottom8} ({100.0*bottom8/max(1,total):.1f}%)  上方7/8={total-bottom8} ({100.0*(total-bottom8)/max(1,total):.1f}%)")
    print()

files = sorted(os.path.join(folder, f) for f in os.listdir(folder)
               if f.startswith("dx12_dump_atlas") and f.endswith(".bmp"))
for f in files:
    if filters and not any(t in os.path.basename(f) for t in filters):
        continue
    analyze(f)
