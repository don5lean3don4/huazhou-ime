# -*- coding: utf-8 -*-
"""v4 预览图：声调 1-6 在键盘顶部，第四行是 符号/英文/数字/字母 入口。"""
import pathlib
from PIL import Image, ImageDraw, ImageFont

ROOT = pathlib.Path(__file__).resolve().parent.parent
FONT_HZ = str(ROOT / "assets" / "hzfont.ttf")
FONT_UI = r"C:\Windows\Fonts\msyh.ttc"

S = 3
W = 360 * S
BAR_H = 58 * S
TONE_H = 38 * S
KEY_H = 50 * S
PAD = 3 * S
TAB_H = 36 * S
SCROLL_H = 164 * S

BG = (233, 235, 239)
WHITE = (255, 255, 255)
KEY_ALT = (221, 227, 236)
KEY_TONE = (201, 221, 245)
KEY_FUNC = (185, 195, 207)
KEY_ON = (157, 195, 236)
KEY_TAB = (200, 212, 224)
TEXT = (27, 31, 36)
MUTED = (107, 116, 128)

single = {}
for line in (ROOT / "assets" / "letter_index.tsv").read_text(encoding="utf-8").splitlines():
    p = line.split("\t")
    if len(p) == 3 and len(p[1]) == 1:
        single[p[1]] = p[2]

syl = {}
for line in (ROOT / "assets" / "dict.tsv").read_text(encoding="utf-8").splitlines():
    s, g = line.split("\t")
    syl[s] = list(g)

f_latin = ImageFont.truetype(FONT_UI, 20 * S)
f_latin_s = ImageFont.truetype(FONT_UI, 8 * S)
f_glyph = ImageFont.truetype(FONT_HZ, 21 * S)
f_glyph_big = ImageFont.truetype(FONT_HZ, 24 * S)
f_tone = ImageFont.truetype(FONT_UI, 17 * S)
f_fn = ImageFont.truetype(FONT_UI, 13 * S)
f_chip_lbl = ImageFont.truetype(FONT_UI, 8 * S)
f_buf = ImageFont.truetype(FONT_UI, 15 * S)
f_tab = ImageFont.truetype(FONT_UI, 13 * S)
f_punct = ImageFont.truetype(FONT_UI, 19 * S)
f_cap = ImageFont.truetype(FONT_UI, 12 * S)

PANEL1 = BAR_H + TONE_H + 4 * (KEY_H + 4 * S) + 8 * S
PANEL2 = TAB_H + 4 * S + SCROLL_H + KEY_H + 4 * S + 4 * S
H = 40 * S + PANEL1 + 56 * S + PANEL2 + 20 * S

img = Image.new("RGB", (W, H), WHITE)
d = ImageDraw.Draw(img)


def center(dr, box, s, font, fill):
    x0, y0, x1, y1 = box
    bb = dr.textbbox((0, 0), s, font=font)
    w, h = bb[2] - bb[0], bb[3] - bb[1]
    dr.text((x0 + (x1 - x0 - w) / 2 - bb[0], y0 + (y1 - y0 - h) / 2 - bb[1]), s, font=font, fill=fill)


def keys_panel(top):
    d.rounded_rectangle([0, top, W, top + PANEL1], radius=16 * S, fill=BG)
    y = top + 12 * S

    # 候选栏
    x = PAD + 4 * S
    d.rounded_rectangle([x, y, x + 56 * S, y + 44 * S], radius=10 * S, fill=WHITE)
    center(d, (x, y, x + 56 * S, y + 44 * S), "ba4", f_buf, TEXT)
    x += 62 * S
    for i, g in enumerate(syl["ba4"]):
        label = "ba4" if i == 0 else "ba4\u00b72"
        cw = 44 * S
        d.rounded_rectangle([x, y + 1 * S, x + cw, y + 53 * S], radius=10 * S, fill=WHITE)
        center(d, (x, y + 6 * S, x + cw, y + 38 * S), g, f_glyph_big, TEXT)
        center(d, (x, y + 38 * S, x + cw, y + 52 * S), label, f_chip_lbl, MUTED)
        x += cw + 4 * S
    d.text((W - 96 * S, y + 16 * S), "拼音 + 声调", font=f_cap, fill=MUTED)
    y += BAR_H

    # 声调行（1-6 在最上面）
    tw = (W - 2 * PAD) / 6
    for i in range(6):
        box = (PAD + i * tw + 2 * S, y + 2 * S, PAD + (i + 1) * tw - 2 * S, y + 2 * S + TONE_H - 4 * S)
        d.rounded_rectangle(box, radius=9 * S, fill=KEY_TONE)
        center(d, box, str(i + 1), f_tone, TEXT)
    y += TONE_H + 4 * S

    rows = [
        ("q w e r t y u i o p".split(), [1] * 10),
        ("a s d f g h j k l".split(), [1] * 9),
        ("z x c v b n m".split() + ["\u5220"], [1] * 7 + [1.4]),
        (["\u7b26\u53f7", "\u82f1\u6587", "\u6570\u5b57", "\u5b57\u6bcd", "\u7a7a\u683c", "\u56de\u8f66"],
         [1.2, 1.2, 1.2, 1.2, 2.4, 1.4]),
    ]
    usable = W - 2 * PAD
    for labels, weights in rows:
        total = sum(weights)
        kx = PAD
        for lbl, w in zip(labels, weights):
            kw = usable * w / total
            fill = WHITE
            if lbl in ("\u5220", "\u56de\u8f66"):
                fill = KEY_FUNC
            elif lbl in ("\u7b26\u53f7", "\u82f1\u6587", "\u6570\u5b57", "\u5b57\u6bcd"):
                fill = KEY_TAB
            box = (kx + 2 * S, y + 2 * S, kx + kw - 2 * S, y + KEY_H - 2 * S)
            d.rounded_rectangle(box, radius=10 * S, fill=fill)
            if lbl == "\u5220":
                center(d, box, lbl, f_tone, TEXT)
            elif lbl in ("\u7a7a\u683c", "\u56de\u8f66", "\u7b26\u53f7", "\u82f1\u6587", "\u6570\u5b57", "\u5b57\u6bcd"):
                center(d, box, lbl, f_fn, TEXT)
            elif lbl in single:
                center(d, (box[0], box[1] + 1 * S, box[2], box[3] - 10 * S), single[lbl], f_glyph, TEXT)
                center(d, (box[0], box[3] - 11 * S, box[2], box[3]), lbl, f_latin_s, MUTED)
            else:
                center(d, box, lbl, f_latin, TEXT)
            kx += kw
        y += KEY_H + 4 * S


def punct_panel(top):
    d.rounded_rectangle([0, top, W, top + PANEL2], radius=16 * S, fill=BG)
    y = top + 8 * S
    tabs = ["\u6807\u70b9", "\u82f1\u6587", "\u6570\u5b57", "\u5b57\u6bcd"]
    tw = (W - 2 * PAD) / 4
    for i, t in enumerate(tabs):
        box = (PAD + i * tw + 2 * S, y + 2 * S, PAD + (i + 1) * tw - 2 * S, y + 2 * S + TAB_H - 4 * S)
        d.rounded_rectangle(box, radius=10 * S, fill=KEY_ON if i == 1 else KEY_ALT)
        center(d, box, t, f_tab, TEXT)
    y += TAB_H + 4 * S

    area = Image.new("RGB", (W - 2 * PAD, SCROLL_H), BG)
    da = ImageDraw.Draw(area)
    items = list("abcdefghijklmnopqrstuvwxyz") + list("ABCDEFGHIJKLMNOPQRSTUVWXYZ") + [
        ",", ".", ";", ":", "?", "!", '"', "'", "(", ")",
        "[", "]", "{", "}", "<", ">", "/", "\\", "|", "@"]
    cw = (W - 2 * PAD) / 5
    for i, ch in enumerate(items):
        r, c = divmod(i, 5)
        box = (c * cw + 2 * S, r * 32 * S + 2 * S, (c + 1) * cw - 2 * S, (r + 1) * 32 * S - 2 * S)
        da.rounded_rectangle(box, radius=8 * S, fill=WHITE)
        center(da, box, ch, f_punct, TEXT)
    img.paste(area, (PAD, y))

    by = top + PANEL2 - KEY_H - 4 * S
    labels = ["\u8fd4\u56de", "\u7a7a\u683c", "\u56de\u8f66", "\u5220"]
    weights = [1.4, 2.4, 1.3, 1.3]
    total = sum(weights)
    kx = PAD
    for lbl, w in zip(labels, weights):
        kw = (W - 2 * PAD) * w / total
        box = (kx + 2 * S, by + 2 * S, kx + kw - 2 * S, by + KEY_H - 2 * S)
        fill = WHITE if lbl == "\u7a7a\u683c" else (KEY_ALT if lbl == "\u8fd4\u56de" else KEY_FUNC)
        d.rounded_rectangle(box, radius=10 * S, fill=fill)
        center(d, box, lbl, f_fn, TEXT)
        kx += kw


d.text((14 * S, 12 * S), "1-6 声调移到最上面；第四行改成 符号 / 英文 / 数字 / 字母 入口",
       font=f_cap, fill=(90, 98, 108))
keys_panel(36 * S)
y2 = 36 * S + PANEL1 + 34 * S
d.text((14 * S, y2), "“英文”标签：26 小写 + 26 大写 + ASCII 标点（可滚动）", font=f_cap, fill=(90, 98, 108))
punct_panel(y2 + 22 * S)

out = ROOT / "preview-v5.png"
img.save(out)
print("saved", out, img.size)
