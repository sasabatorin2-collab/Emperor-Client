"""Generates src/main/resources/assets/emperor/icon.png (128x128 crown on a dark rounded tile)."""
import math
import struct
import zlib

S = 128
GOLD = (255, 211, 110)
PURPLE = (180, 92, 255)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def inside_round_rect(x, y, r=26):
    cx = min(max(x, r), S - 1 - r)
    cy = min(max(y, r), S - 1 - r)
    return (x - cx) ** 2 + (y - cy) ** 2 <= r * r


def in_crown(x, y):
    # base band
    if 82 <= y <= 94 and 30 <= x <= 98:
        return True
    # zigzag body: three spikes
    if 44 <= y < 82 and 30 <= x <= 98:
        peaks = [(34, 44), (64, 36), (94, 44)]
        valleys = [(49, 62), (79, 62)]
        pts = [(30, 82), peaks[0], valleys[0], peaks[1], valleys[1], peaks[2], (98, 82)]
        for i in range(len(pts) - 1):
            (x1, y1), (x2, y2) = pts[i], pts[i + 1]
            if x1 <= x <= x2:
                top = y1 + (y2 - y1) * (x - x1) / max(1, x2 - x1)
                return y >= top
    return False


def jewel(x, y):
    for cx, cy, r in [(34, 38, 6), (64, 30, 7), (94, 38, 6)]:
        if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
            return True
    return False


rows = []
for y in range(S):
    row = bytearray([0])
    for x in range(S):
        if not inside_round_rect(x, y):
            row += bytes((0, 0, 0, 0))
            continue
        bg = lerp((20, 16, 30), (44, 22, 70), y / S)
        glow = max(0.0, 1 - math.hypot(x - 64, y - 66) / 70)
        c = lerp(bg, PURPLE, glow * 0.35)
        if in_crown(x, y) or jewel(x, y):
            c = lerp(GOLD, PURPLE, (x + y) / (2 * S) * 0.9)
            if 86 <= y <= 90 and 34 <= x <= 94:
                c = lerp(c, (255, 255, 255), 0.35)
        row += bytes((*c, 255))
    rows.append(bytes(row))


def chunk(tag, data):
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", S, S, 8, 6, 0, 0, 0)) \
    + chunk(b"IDAT", zlib.compress(b"".join(rows), 9)) + chunk(b"IEND", b"")
open("src/main/resources/assets/emperor/icon.png", "wb").write(png)
print("icon written", len(png), "bytes")
