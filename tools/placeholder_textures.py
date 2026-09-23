#!/usr/bin/env python3
"""Generate 16x16 placeholder block textures for Easy Storage.

Pure standard library (no Pillow). Output is deterministic, so re-running it
produces identical files. Final art will replace these.

Usage: python3 tools/placeholder_textures.py
"""
import struct
import zlib
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/easystorage/textures/block"

# Blue-tinted iron, matching the filter-slot tint used in the screen (0x3A6EA5).
BASE = (0x3A, 0x6E, 0xA5)
LIGHT = (0x6F, 0x9C, 0xCC)
DARK = (0x22, 0x3F, 0x60)


def png(path, pixels):
    raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in pixels)

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    data = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )
    path.write_bytes(data)


def shade(rgb, amount):
    return tuple(max(0, min(255, c + amount)) for c in rgb)


def noise(x, y, seed):
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return (n & 0xF) - 8  # -8..7


def texture(seed, fill, edge_top, edge_bottom):
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            if y == 0 or x == 0:
                rgb = edge_top
            elif y == 15 or x == 15:
                rgb = edge_bottom
            else:
                rgb = shade(fill, noise(x, y, seed))
            row.append(rgb + (255,))
        rows.append(row)
    return rows


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    png(OUT / "sorter_hopper_outside.png", texture(1, BASE, LIGHT, DARK))
    png(OUT / "sorter_hopper_top.png", texture(2, LIGHT, shade(LIGHT, 30), BASE))
    png(OUT / "sorter_hopper_inside.png", texture(3, DARK, BASE, shade(DARK, -20)))
    print(f"wrote placeholders to {OUT}")


if __name__ == "__main__":
    main()
