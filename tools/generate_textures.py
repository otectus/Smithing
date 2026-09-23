#!/usr/bin/env python3
"""Generates Ote's Smithing pixel-art textures (items, blocks, particles, GUI sheets).

Run from the project root:  python3 tools/generate_textures.py
Output goes to src/main/resources/assets/otes_smithing/textures/. The output is deterministic.
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "otes_smithing", "textures")

TIERS = {
    "stone": {"m": (138, 138, 138), "l": (178, 178, 178), "d": (92, 92, 92), "h": (107, 74, 43), "hl": (138, 98, 56)},
    "iron": {"m": (206, 206, 206), "l": (250, 250, 250), "d": (140, 140, 140), "h": (107, 74, 43), "hl": (138, 98, 56)},
    "diamond": {"m": (74, 222, 205), "l": (170, 251, 236), "d": (30, 150, 138), "h": (107, 74, 43), "hl": (138, 98, 56)},
    "netherite": {"m": (76, 66, 68), "l": (112, 100, 102), "d": (46, 39, 41), "h": (58, 40, 30), "hl": (82, 58, 42)},
}
OUTLINE = (30, 22, 20, 255)


def ensure(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)


def save(img, *parts):
    path = os.path.join(ROOT, *parts)
    ensure(path)
    img.save(path)


def line_pixels(x0, y0, x1, y1, width=1):
    """Pixels of a thick line from (x0, y0) to (x1, y1)."""
    pts = set()
    steps = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for i in range(steps + 1):
        t = i / steps
        x = x0 + (x1 - x0) * t
        y = y0 + (y1 - y0) * t
        r = (width - 1) / 2.0
        for dx in range(-int(math.ceil(r)), int(math.ceil(r)) + 1):
            for dy in range(-int(math.ceil(r)), int(math.ceil(r)) + 1):
                if dx * dx + dy * dy <= r * r + 0.5:
                    px, py = int(round(x + dx)), int(round(y + dy))
                    if 0 <= px < 16 and 0 <= py < 16:
                        pts.add((px, py))
    return pts


def draw_layers(layers, size=16):
    """layers: list of (pixels, colour_fn(x, y)). Later layers overwrite earlier ones. Adds a dark outline."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    filled = set()
    for pixels, colour in layers:
        for (x, y) in pixels:
            img.putpixel((x, y), colour(x, y))
            filled.add((x, y))
    outline = set()
    for (x, y) in filled:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < size and 0 <= ny < size and (nx, ny) not in filled:
                outline.add((nx, ny))
    for p in outline:
        img.putpixel(p, OUTLINE)
    return img


def shade(base, light, dark, axis_bias=1):
    """Colour function: lighter towards the top-left of a diagonal shape, darker bottom-right."""
    def fn(x, y):
        v = (x - y) * axis_bias
        if v <= -3:
            c = light
        elif v >= 3:
            c = dark
        else:
            c = base
        return c + (255,)
    return fn


def hammer(tier):
    t = TIERS[tier]
    handle = line_pixels(2, 14, 9, 7, 2)
    head = line_pixels(7, 3, 13, 9, 4)
    head_hi = line_pixels(7, 3, 12, 8, 1)
    return draw_layers([
        (handle, lambda x, y: (t["hl"] if (x + y) % 3 == 0 else t["h"]) + (255,)),
        (head, lambda x, y: (t["d"] if x + y >= 17 else t["m"]) + (255,)),
        (head_hi, lambda x, y: t["l"] + (255,)),
    ])


def tongs(tier):
    t = TIERS[tier]
    handle_a = line_pixels(1, 12, 8, 6, 1)
    handle_b = line_pixels(3, 15, 9, 8, 1)
    jaw_a = line_pixels(8, 6, 12, 2, 1) | {(13, 2), (13, 3)}
    jaw_b = line_pixels(9, 8, 13, 5, 1) | {(14, 5), (14, 4)}
    rivet = {(9, 7)}
    return draw_layers([
        (handle_a | handle_b, lambda x, y: (t["hl"] if (x + y) % 4 == 0 else t["h"]) + (255,)),
        (jaw_a | jaw_b, lambda x, y: (t["l"] if y <= 3 else t["m"]) + (255,)),
        (rivet, lambda x, y: t["d"] + (255,)),
    ])


def tongs_workpiece():
    bar = line_pixels(11, 6, 15, 2, 2)
    core = line_pixels(11, 5, 14, 2, 1)
    img = draw_layers([
        (bar, lambda x, y: (235, 110, 30, 255)),
        (core, lambda x, y: (255, 214, 120, 255)),
    ])
    # The overlay outline would cover the jaws; soften it to a hot red rim.
    for x in range(16):
        for y in range(16):
            if img.getpixel((x, y)) == OUTLINE:
                img.putpixel((x, y), (120, 30, 10, 200))
    return img


def hot_workpiece():
    bar = line_pixels(3, 12, 12, 3, 3)
    core = line_pixels(4, 10, 11, 3, 1)
    return draw_layers([
        (bar, lambda x, y: ((225, 95, 25) if x + y > 15 else (245, 140, 40)) + (255,)),
        (core, lambda x, y: (255, 225, 140, 255)),
    ])


def guide_book():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cover = (104, 62, 34)
    cover_hi = (132, 84, 46)
    spine = (70, 40, 22)
    pages = (236, 226, 196)
    gold = (224, 178, 68)
    for y in range(2, 15):
        for x in range(3, 14):
            c = cover
            if x == 3 or x == 4:
                c = spine
            elif x == 13:
                c = pages if 3 <= y <= 13 else cover
            elif y == 2 or y == 14:
                c = cover_hi if y == 2 else spine
            img.putpixel((x, y), c + (255,))
    # Gold anvil emblem.
    for (x, y) in [(7, 6), (8, 6), (9, 6), (10, 6), (11, 6), (8, 7), (9, 7), (10, 7), (9, 8), (8, 9), (9, 9), (10, 9)]:
        img.putpixel((x, y), gold + (255,))
    for (x, y) in [(6, 11), (7, 11), (8, 11), (9, 11), (10, 11), (11, 11), (12, 11)]:
        img.putpixel((x, y), (180, 140, 60, 255))
    for y in range(1, 16):
        for x in range(2, 15):
            if img.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and img.getpixel((nx, ny))[3] == 255 and img.getpixel((nx, ny))[:3] != OUTLINE[:3]:
                        img.putpixel((x, y), OUTLINE)
                        break
    return img


def noise_texture(seed, base, spread, size=16, speckle=None):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            n = rnd.randint(-spread, spread)
            c = tuple(max(0, min(255, v + n)) for v in base)
            if speckle and rnd.random() < speckle[0]:
                c = speckle[1]
            img.putpixel((x, y), c + (255,))
    return img


def anvil_body():
    img = noise_texture(11, (58, 61, 66), 7, speckle=(0.05, (84, 88, 94)))
    for x in range(16):
        img.putpixel((x, 0), (80, 84, 90, 255))
        img.putpixel((x, 15), (34, 36, 40, 255))
    return img


def anvil_top():
    img = noise_texture(12, (74, 78, 84), 6, speckle=(0.04, (104, 108, 114)))
    for y in range(16):
        for x in range(5, 11):
            r, g, b, a = img.getpixel((x, y))
            img.putpixel((x, y), (min(255, r + 18), min(255, g + 18), min(255, b + 20), 255))
    return img


def molten_metal():
    """Four animation frames of bright, near-white molten metal; the block entity tints it per family."""
    frames = 4
    img = Image.new("RGBA", (16, 16 * frames))
    for f in range(frames):
        phase = f / frames * math.tau
        for y in range(16):
            for x in range(16):
                v = (math.sin((x + y * 0.5) * 0.7 + phase) + math.sin((y - x * 0.4) * 0.9 - phase * 1.3)) * 0.5
                val = int(200 + v * 45)
                glow = int(230 + v * 25)
                img.putpixel((x, y + f * 16), (min(255, glow), min(255, val), min(255, int(val * 0.85)), 255))
    return img


def spark(frame):
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    radius = [2.2, 1.7, 1.2, 0.7][frame]
    for y in range(8):
        for x in range(8):
            d = math.hypot(x - 3.5, y - 3.5)
            if d <= radius:
                a = int(255 * min(1.0, (radius - d + 0.5)))
                img.putpixel((x, y), (255, 255, 255, max(0, min(255, a))))
    return img


def gui_sheet():
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    rnd = random.Random(7)
    # Panel (0,0,64,64), nine-slice 8.
    for y in range(64):
        for x in range(64):
            n = rnd.randint(-4, 4)
            c = (43 + n, 38 + n, 36 + n)
            edge = min(x, y, 63 - x, 63 - y)
            if edge == 0:
                c = (14, 12, 11)
            elif edge in (1, 2):
                c = (176, 138, 85) if (x < 32) == (y < 32) else (140, 106, 63)
            elif edge == 3:
                c = (92, 70, 44)
            elif edge == 4:
                c = (24, 21, 20)
            img.putpixel((x, y), c + (255,))
    for (cx, cy) in [(5, 5), (58, 5), (5, 58), (58, 58)]:
        img.putpixel((cx, cy), (214, 172, 110, 255))
    # Well (64,0,32,32), nine-slice 4.
    for y in range(32):
        for x in range(32):
            c = (21, 18, 17)
            if x == 0 or y == 0:
                c = (8, 7, 6)
            elif x == 31 or y == 31:
                c = (70, 58, 48)
            elif x == 1 or y == 1:
                c = (14, 12, 11)
            img.putpixel((64 + x, y), c + (255,))
    # Ring (96,0,64,64) and disc (160,0,64,64), anti-aliased, white.
    for y in range(64):
        for x in range(64):
            d = math.hypot(x - 31.5, y - 31.5)
            ring = max(0.0, 1.0 - abs(d - 28.0) / 3.0)
            if ring > 0:
                img.putpixel((96 + x, y), (255, 255, 255, int(255 * min(1.0, ring * 1.4))))
            disc = max(0.0, min(1.0, 30.5 - d))
            if disc > 0:
                img.putpixel((160 + x, y), (255, 255, 255, int(255 * disc)))
    return img


def guide_sheet():
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    rnd = random.Random(9)
    # Page with leather border (0,0,64,64), nine-slice 12.
    for y in range(64):
        for x in range(64):
            edge = min(x, y, 63 - x, 63 - y)
            n = rnd.randint(-5, 5)
            if edge == 0:
                c = (40, 24, 14)
            elif edge < 9:
                c = (92 + n, 58 + n, 32 + n)
            elif edge == 9:
                c = (60, 38, 22)
            elif edge < 12:
                c = (206 + n, 190 + n, 150 + n)
            else:
                c = (233 + n, 222 + n, 190 + n)
            img.putpixel((x, y), c + (255,))
    # Chapter list inset (64,0,32,32), nine-slice 6.
    for y in range(32):
        for x in range(32):
            edge = min(x, y, 31 - x, 31 - y)
            n = rnd.randint(-3, 3)
            c = (178, 155, 112) if edge == 0 else (220 + n, 205 + n, 168 + n)
            img.putpixel((64 + x, y), c + (255,))
    return img


def main():
    for tier in TIERS:
        save(hammer(tier), "item", f"{tier}_smithing_hammer.png")
        save(tongs(tier), "item", f"{tier}_smithing_tongs.png")
    save(tongs_workpiece(), "item", "tongs_workpiece.png")
    save(hot_workpiece(), "item", "hot_workpiece.png")
    save(guide_book(), "item", "smithing_guide.png")
    save(anvil_body(), "block", "smiths_anvil_body.png")
    save(anvil_top(), "block", "smiths_anvil_top.png")
    save(molten_metal(), "block", "molten_metal.png")
    for i in range(4):
        save(spark(i), "particle", f"spark_{i}.png")
    save(gui_sheet(), "gui", "smithing.png")
    save(guide_sheet(), "gui", "guide.png")
    mcmeta = os.path.join(ROOT, "block", "molten_metal.png.mcmeta")
    with open(mcmeta, "w") as f:
        f.write('{\n  "animation": {\n    "frametime": 5,\n    "interpolate": true\n  }\n}\n')
    print("textures written to", os.path.normpath(ROOT))


if __name__ == "__main__":
    main()
