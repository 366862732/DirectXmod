# 放大 854x480 屏幕顶部区域, 判断 logo/文字朝向
import struct

path = r"d:\dx12-lib-template-26.1.2\图片\dx12_dump_colorTex-before-copy.bmp"
with open(path, "rb") as f:
    data = f.read()
pix_off = struct.unpack_from("<I", data, 10)[0]
w = struct.unpack_from("<i", data, 18)[0]
h_raw = struct.unpack_from("<i", data, 22)[0]
h = abs(h_raw)
bottom_up = h_raw > 0
px = []
row_bytes = w * 4
for r in range(h):
    src = (h - 1 - r) if bottom_up else r
    off = pix_off + src * row_bytes
    for x in range(w):
        b = data[off + x*4]
        g = data[off + x*4 + 1]
        r2 = data[off + x*4 + 2]
        px.append((r2, g, b))

def view(x0, y0, x1, y1, cols=120):
    rows = max(8, cols * (y1-y0) // max(1, x1-x0) // 2)
    chars = " .:-=+*#%@"
    print(f"--- region x[{x0}..{x1}] y[{y0}..{y1}] ---")
    for ry in range(rows):
        yy0 = y0 + (y1-y0)*ry//rows
        yy1 = max(yy0+1, y0 + (y1-y0)*(ry+1)//rows)
        line = ""
        for rx in range(cols):
            xx0 = x0 + (x1-x0)*rx//cols
            xx1 = max(xx0+1, x0 + (x1-x0)*(rx+1)//cols)
            r=g=b=cnt=0
            for y in range(yy0, yy1):
                base = y*w
                for x in range(xx0, xx1):
                    pr,pg,pb = px[base+x]
                    r+=pr; g+=pg; b+=pb; cnt+=1
            r//=cnt; g//=cnt; b//=cnt
            luma = (r*299+g*587+b*114)//1000
            line += chars[luma*9//255]
        print(line.rstrip())

# 顶部中间 (可能是 Minecraft logo)
view(200, 0, 650, 110)
# 顶部左侧 (可能是版本文字)
view(10, 0, 300, 80)
# 底部中间 (可能是按钮/版权)
view(250, 380, 600, 479)
