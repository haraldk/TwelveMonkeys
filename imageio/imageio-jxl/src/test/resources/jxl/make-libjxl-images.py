#!/usr/bin/env python3
# Writes lossless JPEG XL test images that cjxl and ffmpeg can't create, by calling the libjxl encoder API
# (libjxl.so.0.11) with ctypes: float samples in other formats than 32-bit (f16 etc.), and PQ with an intensity target
# other than 10000 nits. The pixel formulas are mirrored in JXLImageReaderTest.
#
# Usage: python3 make-libjxl-images.py <out dir>

import ctypes
import struct
import sys

W, H = 64, 48

jxl = ctypes.CDLL('libjxl.so.0.11')
jxl.JxlEncoderCreate.restype = ctypes.c_void_p
jxl.JxlEncoderDestroy.argtypes = [ctypes.c_void_p]
jxl.JxlEncoderFrameSettingsCreate.restype = ctypes.c_void_p
jxl.JxlEncoderFrameSettingsCreate.argtypes = [ctypes.c_void_p, ctypes.c_void_p]
for name in ('JxlEncoderSetBasicInfo', 'JxlEncoderSetColorEncoding', 'JxlEncoderSetFrameLossless',
             'JxlEncoderAddImageFrame', 'JxlEncoderProcessOutput', 'JxlEncoderFrameSettingsSetOption'):
    getattr(jxl, name).restype = ctypes.c_int

JXL_ENC_SUCCESS = 0
JXL_ENC_NEED_MORE_OUTPUT = 2
JXL_TYPE_FLOAT = 0
JXL_ENC_FRAME_SETTING_EFFORT = 0

# JxlBasicInfo, as uint32 fields (layout of libjxl 0.11, checked against the defaults of JxlEncoderInitBasicInfo)
XSIZE, YSIZE, BITS, EXP_BITS, INTENSITY_TARGET, USES_ORIGINAL_PROFILE, ORIENTATION, NUM_COLOR, NUM_EXTRA, ALPHA_BITS, \
    ALPHA_EXP_BITS = 1, 2, 3, 4, 5, 9, 12, 13, 14, 15, 16

# JxlColorEncoding, as int32 fields (layout of libjxl 0.11, checked against the values of JxlColorEncodingSetToLinearSRGB)
CE_WHITE_POINT, CE_PRIMARIES, CE_TRANSFER_FUNCTION = 1, 6, 20
JXL_WHITE_POINT_D65, JXL_PRIMARIES_SRGB, JXL_PRIMARIES_2100 = 1, 1, 9
JXL_TRANSFER_FUNCTION_LINEAR, JXL_TRANSFER_FUNCTION_PQ = 8, 16


class PixelFormat(ctypes.Structure):
    _fields_ = [('num_channels', ctypes.c_uint32), ('data_type', ctypes.c_int), ('endianness', ctypes.c_int),
                ('align', ctypes.c_size_t)]


def check(result, what):
    if result != JXL_ENC_SUCCESS:
        raise RuntimeError('%s failed: %d' % (what, result))


def encode(path, gray, alpha, bits, exp_bits, pixel, pq_intensity_target=None):
    # Samples are passed as floats (0-1 for integer formats), color is linear sRGB, or BT.2100 PQ
    info = (ctypes.c_uint32 * 64)()  # JxlBasicInfo is 204 bytes in 0.11, with padding
    jxl.JxlEncoderInitBasicInfo(info)
    assert info[BITS] == 8 and info[ORIENTATION] == 1 and info[NUM_COLOR] == 3, 'unexpected JxlBasicInfo layout'
    info[XSIZE], info[YSIZE] = W, H
    info[BITS], info[EXP_BITS] = bits, exp_bits
    info[USES_ORIGINAL_PROFILE] = 1  # required for lossless
    info[NUM_COLOR] = 1 if gray else 3
    if alpha:
        info[NUM_EXTRA], info[ALPHA_BITS], info[ALPHA_EXP_BITS] = 1, bits, exp_bits
    if pq_intensity_target:
        info[INTENSITY_TARGET] = struct.unpack('<I', struct.pack('<f', pq_intensity_target))[0]

    encoder = jxl.JxlEncoderCreate(None)
    try:
        check(jxl.JxlEncoderSetBasicInfo(ctypes.c_void_p(encoder), info), 'JxlEncoderSetBasicInfo')
        color = (ctypes.c_int32 * 64)()  # JxlColorEncoding is 104 bytes in 0.11
        jxl.JxlColorEncodingSetToLinearSRGB(color, 1 if gray else 0)
        assert (color[CE_WHITE_POINT], color[CE_PRIMARIES], color[CE_TRANSFER_FUNCTION]) == \
               (JXL_WHITE_POINT_D65, 0 if gray else JXL_PRIMARIES_SRGB, JXL_TRANSFER_FUNCTION_LINEAR), \
            'unexpected JxlColorEncoding layout'
        if pq_intensity_target:
            color[CE_PRIMARIES], color[CE_TRANSFER_FUNCTION] = JXL_PRIMARIES_2100, JXL_TRANSFER_FUNCTION_PQ
        check(jxl.JxlEncoderSetColorEncoding(ctypes.c_void_p(encoder), color), 'JxlEncoderSetColorEncoding')

        settings = jxl.JxlEncoderFrameSettingsCreate(encoder, None)
        check(jxl.JxlEncoderSetFrameLossless(ctypes.c_void_p(settings), 1), 'JxlEncoderSetFrameLossless')
        check(jxl.JxlEncoderFrameSettingsSetOption(ctypes.c_void_p(settings), JXL_ENC_FRAME_SETTING_EFFORT,
                                                   ctypes.c_int64(7)), 'effort')

        channels = (1 if gray else 3) + (1 if alpha else 0)
        samples = [s for y in range(H) for x in range(W) for s in pixel(x, y)]
        assert len(samples) == W * H * channels
        data = struct.pack('<%df' % len(samples), *samples)
        pixel_format = PixelFormat(channels, JXL_TYPE_FLOAT, 0, 0)
        check(jxl.JxlEncoderAddImageFrame(ctypes.c_void_p(settings), ctypes.byref(pixel_format), data,
                                          ctypes.c_size_t(len(data))), 'JxlEncoderAddImageFrame')
        jxl.JxlEncoderCloseInput(ctypes.c_void_p(encoder))

        out = b''
        chunk = ctypes.create_string_buffer(65536)
        while True:
            next_out = ctypes.c_void_p(ctypes.addressof(chunk))
            avail = ctypes.c_size_t(len(chunk))
            result = jxl.JxlEncoderProcessOutput(ctypes.c_void_p(encoder), ctypes.byref(next_out), ctypes.byref(avail))
            out += chunk.raw[:len(chunk) - avail.value]
            if result != JXL_ENC_NEED_MORE_OUTPUT:
                check(result, 'JxlEncoderProcessOutput')
                break
    finally:
        jxl.JxlEncoderDestroy(ctypes.c_void_p(encoder))

    with open(path, 'wb') as f:
        f.write(out)


def float16(x, y):
    # Values exactly representable as f16 (10 bit mantissa), first row f16 subnormals (multiples of 2^-24).
    # NOTE: Subnormals stay below 2^-15, as the libjxl 0.11 encoder stores f16 values in [2^-15, 2^-14) wrong
    # (as 2 * (v - 2^-15), djxl decodes them like that), see https://github.com/libjxl/libjxl/issues/3881
    # (fixed in libjxl 0.12.0)
    return x * 8 * 2.0 ** -24 if y == 0 else (x * 16 + y % 16) / 1024.0


def alpha16(x, y):
    return (y * 16 + x % 16) / 1024.0


if __name__ == '__main__':
    out = sys.argv[1]
    encode(out + '/grayf16-lossless.jxl', True, False, 16, 5, lambda x, y: (float16(x, y),))
    encode(out + '/rgbaf16-lossless.jxl', False, True, 16, 5,
           lambda x, y: (float16(x, y), float16(W - 1 - x, y), (y * 21) / 1024.0, alpha16(x, y)))
    # Unusual format: 24 bits with 8 exponent bits (15 bit mantissa), not a plain 32-bit float
    encode(out + '/grayf24-lossless.jxl', True, False, 24, 8, lambda x, y: (float16(x, y),))
    # Same neutral gray PQ code values as rgb16-pq.jxl (pq16 in make-test-images.py), but intensity target 1000 nits
    encode(out + '/rgb16-pq1000.jxl', False, False, 16, 0,
           lambda x, y: ((x * 1000 + y * 20) / 65535.0,) * 3, pq_intensity_target=1000.0)
