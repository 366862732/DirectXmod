# 查看 854x480 主渲染目标的高分辨率结构, 判断 GUI 是否翻转
import os, struct

path = r"d:\dx12-lib-template-26.1.2\图片\dx12_dump_colorTex-before-copy.bmp"
with open(path, "rb") as f:
    data = f.read()
pix_off = struct.unpack_from("<I", data, 10)[0]
w = struct.unpack_from("<i", data, 18)[0]
h_raw = struct.unpack_from("<i", data, 22)[0]
h = abs(h_raw)
bottom_up = h_raw > 0
print(f"{w}x{h} bottom_up={bottom_up}")
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

cols, rows = 96, 54
chars = " .:-=+*#%@"
for ry in range(rows):
    y0 = ry*h//rows; y1 = max(y0+1, (ry+1)*h//rows)
    line = ""
    for rx in range(cols):
        x0 = rx*w//cols; x1 = max(x0+1, (rx+1)*w//cols)
        r=g=b=cnt=0
        for y in range(y0, y1):
            for x in range(x0, x1):
                pr,pg,pb = px[y*w+x]
                r+=pr; g+=pg; b+=pb; cnt+=1
        r//=cnt; g//=cnt; b//=cnt
        luma = (r*299+g*587+b*114)//1000
        mx=max(r,g,b); mn=min(r,g,b)
        sat = 0 if mx==0 else (mx-mn)*255//mx
        if sat > 60 and luma > 30:
            if r>g and r>b: c='R'
            elif g>r and g>b: c='G'
            elif b>r and b>g: c='B'
            elif r>150 and g>150 and b<110: c='Y'
            else: c='?'
            line += c
        else:
            line += chars[luma*9//255]
    print(line.rstrip())
