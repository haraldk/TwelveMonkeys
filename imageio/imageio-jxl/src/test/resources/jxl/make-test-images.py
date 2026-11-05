"""Write small synthetic PNG sources for JXL test images (pure Python, no dependencies).

Usage: python3 -I make_pngs.py <out_dir> <icc_profile>
The pixel formulas are mirrored in JXLImageReaderTest, keep them in sync.
"""
import struct
import sys
import zlib

W, H = 64, 48


def chunk(tag, data):
    c = tag + data
    return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xFFFFFFFF)


def write_png(path, color_type, depth, pixel, icc=None):
    """pixel(x, y) returns a tuple of sample values."""
    raw = bytearray()
    for y in range(H):
        raw.append(0)  # No filter
        for x in range(W):
            for v in pixel(x, y):
                raw += struct.pack('>H', v) if depth == 16 else struct.pack('B', v)
    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', W, H, depth, color_type, 0, 0, 0))
    if icc is not None:
        png += chunk(b'iCCP', b'icc\x00\x00' + zlib.compress(icc))
    png += chunk(b'IDAT', zlib.compress(bytes(raw), 9))
    png += chunk(b'IEND', b'')
    with open(path, 'wb') as f:
        f.write(png)


def gray8(x, y):
    return (x * 4 + y) & 0xFF


def gray16(x, y):
    return (x * 1024 + y * 21 + 7) & 0xFFFF


def alpha8(y):
    return (y * 5) & 0xFF


def rgb16(x, y):
    return (x * 1024 + y) & 0xFFFF, (y * 1365 ^ x) & 0xFFFF, 65535 - x * 1000


def alpha16(x, y):
    return ((x + y) * 700) & 0xFFFF


def pq16(x, y):
    # PQ code values, neutral gray (R = G = B)
    return x * 1000 + y * 20


def rgb8_primaries(x, y):
    # Left third red, middle third green, right third blue
    return [(255, 0, 0), (0, 255, 0), (0, 0, 255)][min(x * 3 // W, 2)]


out = sys.argv[1]
with open(sys.argv[2], 'rb') as f:
    icc = f.read()

write_png(out + '/gray8.png', 0, 8, lambda x, y: (gray8(x, y),))
write_png(out + '/gray16.png', 0, 16, lambda x, y: (gray16(x, y),))
write_png(out + '/graya8.png', 4, 8, lambda x, y: (gray8(x, y), alpha8(y)))
write_png(out + '/rgb16.png', 2, 16, rgb16)
write_png(out + '/rgba16.png', 6, 16, lambda x, y: rgb16(x, y) + (alpha16(x, y),))
write_png(out + '/rgb8-icc.png', 2, 8, rgb8_primaries, icc)
write_png(out + '/rgb16-pq.png', 2, 16, lambda x, y: (pq16(x, y),) * 3)
