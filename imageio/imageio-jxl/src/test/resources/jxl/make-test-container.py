"""Wrap a JPEG XL codestream in a container with Exif and XMP metadata boxes (pure Python, no dependencies).

Usage: python3 make-test-container.py <codestream.jxl> <out.jxl>
The metadata values are mirrored in JXLImageReaderTest, keep them in sync.

Box layout:
  'JXL ' signature, 'ftyp',
  'Exif' (before the codestream),
  'brob' (Brotli compressed 'xml ' box, the content is just garbage, readers can't decompress it and must skip it),
  'jxlp' (index 0), 'jxlp' (index 1, last): the codestream split in two parts,
  'xml ' (XMP, after the codestream)
"""
import struct
import sys

SIGNATURE = b'\x00\x00\x00\x0cJXL \r\n\x87\n'

DESCRIPTION = 'JXL test image'
MAKE = 'TwelveMonkeys'
MODEL = 'ImageIO JXL'
SOFTWARE = 'make-test-container.py'
DATE_TIME = '2026:11:05 19:30:42'
COPYRIGHT = 'CC0'

XMP = ('<?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>'
       '<x:xmpmeta xmlns:x="adobe:ns:meta/"><rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">'
       '<rdf:Description rdf:about="" xmlns:dc="http://purl.org/dc/elements/1.1/">'
       '<dc:title><rdf:Alt><rdf:li xml:lang="x-default">JXL XMP test</rdf:li></rdf:Alt></dc:title>'
       '</rdf:Description></rdf:RDF></x:xmpmeta><?xpacket end="r"?>')


def box(tag, data):
    return struct.pack('>I', 8 + len(data)) + tag + data


def tiff(entries):
    """Big endian TIFF with a single IFD of ASCII entries [(tag, value)]."""
    entries = sorted(entries)
    ifd_size = 2 + len(entries) * 12 + 4
    data_offset = 8 + ifd_size
    ifd = struct.pack('>H', len(entries))
    data = b''
    for tag, value in entries:
        raw = value.encode('ascii') + b'\x00'
        if len(raw) <= 4:
            ifd += struct.pack('>HHI', tag, 2, len(raw)) + raw.ljust(4, b'\x00')
        else:
            ifd += struct.pack('>HHII', tag, 2, len(raw), data_offset + len(data))
            data += raw + (b'\x00' if len(raw) & 1 else b'')
    ifd += struct.pack('>I', 0)  # No next IFD
    return b'MM\x00\x2a' + struct.pack('>I', 8) + ifd + data


with open(sys.argv[1], 'rb') as f:
    codestream = f.read()
assert codestream[:2] == b'\xff\x0a', 'Not a codestream'

exif = tiff([(270, DESCRIPTION), (271, MAKE), (272, MODEL), (305, SOFTWARE), (306, DATE_TIME), (33432, COPYRIGHT)])
half = len(codestream) // 2

container = SIGNATURE
container += box(b'ftyp', b'jxl ' + struct.pack('>I', 0) + b'jxl ')
container += box(b'Exif', struct.pack('>I', 0) + exif)  # 4 byte offset to the TIFF header
container += box(b'brob', b'xml ' + bytes(range(32)))
container += box(b'jxlp', struct.pack('>I', 0) + codestream[:half])
container += box(b'jxlp', struct.pack('>I', 0x80000001) + codestream[half:])
container += box(b'xml ', XMP.encode('utf-8'))

with open(sys.argv[2], 'wb') as f:
    f.write(container)
