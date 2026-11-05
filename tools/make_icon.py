# -*- coding: utf-8 -*-
"""v3 图标：用 4444-Regular（fa1zau4）字体里的 fa1 字形做图标。
   同时生成传统 PNG 图标 + Android 8+ 的自适应图标前景。"""
import pathlib
from PIL import Image, ImageDraw, ImageFont

ROOT = pathlib.Path(__file__).resolve().parent.parent
FONT = ROOT / "assets" / "hzfont.ttf"
RES = ROOT / "res"

syl = {}
for line in (ROOT / "assets" / "dict.tsv").read_text(encoding="utf-8").splitlines():
    s, g = line.split("\t")
    syl[s] = g

GLYPH_KEY = "fa1"
glyph = syl[GLYPH_KEY][0]
print("icon glyph for", GLYPH_KEY, "->", hex(ord(glyph)))

BLUE = (46, 109, 180, 255)
WHITE = (255, 255, 255, 255)
LEGACY = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
ADAPTIVE = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
SS = 4  # 超采样倍数


def draw_glyph(canvas_px, ratio, bg=None, radius_ratio=0.22):
    S = canvas_px * SS
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if bg is not None:
        d.rounded_rectangle([0, 0, S - 1, S - 1], radius=int(S * radius_ratio), fill=bg)
    f = ImageFont.truetype(str(FONT), int(S * ratio))
    bb = d.textbbox((0, 0), glyph, font=f)
    w, h = bb[2] - bb[0], bb[3] - bb[1]
    d.text(((S - w) / 2 - bb[0], (S - h) / 2 - bb[1]), glyph, font=f, fill=WHITE)
    return img.resize((canvas_px, canvas_px), Image.LANCZOS)


for bucket, size in LEGACY.items():
    out = draw_glyph(size, 0.72, bg=BLUE)
    dest = RES / f"mipmap-{bucket}" / "ic_launcher.png"
    dest.parent.mkdir(parents=True, exist_ok=True)
    out.save(dest)
    print("legacy", dest.name, bucket, out.size)

for bucket, size in ADAPTIVE.items():
    out = draw_glyph(size, 0.46)  # 前景只占中间安全区
    dest = RES / f"mipmap-{bucket}" / "ic_launcher_foreground.png"
    dest.parent.mkdir(parents=True, exist_ok=True)
    out.save(dest)
    print("foreground", dest.name, bucket, out.size)

# 预览：大图 + 圆形遮罩 + 48px 小图（浅色底）
bg = Image.new("RGBA", (384, 384), (255, 255, 255, 255))
bg.alpha_composite(draw_glyph(384, 0.72, bg=BLUE))
legacy = bg.convert("RGB")

mask = Image.new("L", (384, 384), 0)
ImageDraw.Draw(mask).ellipse([0, 0, 383, 383], fill=255)
circle = Image.new("RGBA", (384, 384), (255, 255, 255, 255))
circle.alpha_composite(draw_glyph(384, 0.72, bg=BLUE).crop((0, 0, 384, 384)))
circle.putalpha(mask)

preview = Image.new("RGB", (384 * 2 + 220, 460), (246, 247, 249))
preview.paste(legacy, (20, 20))
preview.paste(circle.convert("RGB"), (384 + 40, 20), circle)
small = legacy.resize((48, 48), Image.LANCZOS)
preview.paste(small, (384 * 2 + 80, 210))
small2 = legacy.resize((96, 96), Image.LANCZOS)
preview.paste(small2, (384 * 2 + 80, 50))
d = ImageDraw.Draw(preview)
f = ImageFont.truetype(r"C:\Windows\Fonts\msyh.ttc", 20)
f2 = ImageFont.truetype(r"C:\Windows\Fonts\msyh.ttc", 14)
d.text((20, 420), "传统图标（方形圆角）", font=f, fill=(90, 98, 108))
d.text((384 + 40, 420), "自适应图标（圆形遮罩）", font=f, fill=(90, 98, 108))
d.text((384 * 2 + 80, 270), "48px", font=f2, fill=(120, 128, 138))
d.text((384 * 2 + 80, 158), "96px", font=f2, fill=(120, 128, 138))
preview.save(ROOT / "preview-icon.png")
print("saved preview-icon.png")
