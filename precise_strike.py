# P27 精确打击: 解析新日志 P27 pending area= 行 + dump BMP,
# 逐 sprite 对比"期望区域" vs "dump 实际内容位置", 判定 Y 翻转 or 偏移.
# 用法: python precise_strike.py [log路径] [bmp过滤词]
import os, re, struct, sys

log_path = (sys.argv[1] if len(sys.argv) > 1 else
            r"d:\dx12-lib-template-26.1.2\游戏日志 - 26.2-Fabric_0.19.3.log")
bmp_folder = r"d:\dx12-lib-template-26.1.2\图片"
filter_ = sys.argv[2] if len(sys.argv) > 2 else "atlas_1024x1024"

def load_bmp(path):
    """读 32bpp BMP -> (w,h,px) px 为 (r,g,b,a) 按 top-down 行序."""
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

def region_content(px, w, h, x0, y0, ww, hh):
    """统计区域 [x0,y0,ww,hh] 内非黑像素比例 (RGB>8 且 A>8)."""
    x0 = max(0, x0); y0 = max(0, y0)
    x1 = min(w, x0+ww); y1 = min(h, y0+hh)
    cnt = tot = 0
    for y in range(y0, y1):
        base = y*w
        for x in range(x0, x1):
            r, g, b, a = px[base+x]
            tot += 1
            if (r > 8 or g > 8 or b > 8) and a > 8:
                cnt += 1
    return (cnt/tot) if tot else 0.0

# ---- 1. 解析日志 pending 行 ----
pat = re.compile(
    r"P27 pending atlas dump ctx=0x([0-9a-f]+) pass=(\S+) colorTex=0x([0-9a-f]+) area=(\d+),(\d+) (\d+)x(\d+)")
pending = []  # (colorTex_lo16, loc, x, y, w, h)
with open(log_path, encoding="utf-8", errors="replace") as f:
    for line in f:
        m = pat.search(line)
        if m:
            _, loc, ct, x, y, w, h = m.groups()
            pending.append((int(ct, 16) & 0xFFFF, loc, int(x), int(y), int(w), int(h)))
print(f"parsed {len(pending)} P27 pending area= lines")

if not pending:
    print("!! 日志中没有 area= 行 —— 用户还没用新构建跑过. 先跑新构建再分析.")
    sys.exit(0)

# ---- 2. 按 dump 文件分组分析 ----
atlas_passes = {}
for lo16, loc, x, y, w, h in pending:
    atlas_passes.setdefault(lo16, []).append((loc, x, y, w, h))

for f in sorted(os.listdir(bmp_folder)):
    if not (f.startswith("dx12_dump_atlas") and f.endswith(".bmp")):
        continue
    if filter_ not in f:
        continue
    m = re.search(r"_([0-9a-f]{4})\.bmp$", f)
    lo16 = int(m.group(1), 16) if m else -1
    try:
        w, h, px = load_bmp(os.path.join(bmp_folder, f))
    except Exception as e:
        print(f"== {f}: load error {e}")
        continue
    passes = atlas_passes.get(lo16, [])
    print(f"\n===== {f}  {w}x{h}  (lo16=0x{lo16:04x}, {len(passes)} blit passes) =====")
    if not passes:
        print("  (无对应 pending 记录)")
        continue
    # 全图实际内容 bbox
    minx, maxx, miny, maxy = w, -1, h, -1
    for y in range(h):
        base = y*w
        for x in range(w):
            r, g, b, a = px[base+x]
            if (r > 8 or g > 8 or b > 8) and a > 8:
                if x < minx: minx = x
                if x > maxx: maxx = x
                if y < miny: miny = y
                if y > maxy: maxy = y
    print(f"  dump 实际内容 bbox: x[{minx}..{maxx}] y[{miny}..{maxy}]" if maxx >= 0 else "  dump 全黑")

    # 汇总所有期望区域的覆盖 union (clip 到图集内)
    exp_minx, exp_maxx, exp_miny, exp_maxy = w, -1, h, -1
    print(f"  {'期望区域(x,y,w,h)':>24}  区域内实占%  翻转后区域(y->H-y-h)  翻转区域实占%")
    flip_ok = shift_const = None
    for loc, x, y, ew, eh in passes:
        # clip 期望区域
        ex1, ey1 = min(w, x+ew), min(h, y+eh)
        if ex1 > x and ey1 > y:
            exp_minx = min(exp_minx, x); exp_maxx = max(exp_maxx, ex1-1)
            exp_miny = min(exp_miny, y); exp_maxy = max(exp_maxy, ey1-1)
        c_exp = region_content(px, w, h, x, y, ew, eh)
        # Y 翻转后的期望区域
        fy = h - y - eh
        c_flip = region_content(px, w, h, x, fy, ew, eh)
        mark = ""
        if c_exp > 0.02 and c_flip > 0.02:
            mark = "  <-- 两处都有内容?!"
        print(f"  {loc} ({x},{y},{ew},{eh})   {c_exp*100:6.1f}%      ({x},{fy},{ew},{eh})    {c_flip*100:6.1f}%{mark}")
    if exp_maxx >= 0:
        print(f"  期望区域 union bbox: x[{exp_minx}..{exp_maxx}] y[{exp_miny}..{exp_maxy}]")
    # 判定
    if maxx >= 0 and exp_maxx >= 0:
        # 实际 bbox 是否与期望 union 重叠
        def overlap(a0, a1, b0, b1):
            return max(0, min(a1, b1) - max(a0, b0))
        ov_x = overlap(minx, maxx, exp_minx, exp_maxx)
        ov_y = overlap(miny, maxy, exp_miny, exp_maxy)
        print(f"  判定: 期望union vs 实际内容  X重叠={ov_x}px Y重叠={ov_y}px")
        if ov_y < 8:
            print(f"    >> Y 方向内容完全不落在期望区域 -> 强烈提示 Y 翻转/偏移")
        else:
            print(f"    >> Y 方向有重叠 -> 可能是局部问题")
