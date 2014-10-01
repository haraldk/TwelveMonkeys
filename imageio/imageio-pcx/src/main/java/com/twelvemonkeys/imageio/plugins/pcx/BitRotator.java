package com.twelvemonkeys.imageio.plugins.pcx;

/**
 * IFFUtil
 * <p/>
 * Bit rotate methods based on Sue-Ken Yap, "A Fast 90-Degree Bitmap Rotator,"
 * in GRAPHICS GEMS II, James Arvo ed., Academic Press, 1991, ISBN 0-12-064480-0.
 *
 * @author Unascribed (C version)
 * @author Harald Kuhr (Java port)
 * @version $Id: IFFUtil.java,v 1.0 06.mar.2006 13:31:35 haku Exp$
 */
final class BitRotator {
    // TODO: Extract and merge with IFFUtil

    /**
     * Creates a rotation table
     * @param n number of bits -1
     *
     * @return the rotation table
     */
    private static long[] rtable(int n) {
        return new long[]{
                0x00000000l << n, 0x00000001l << n, 0x00000100l << n, 0x00000101l << n,
                0x00010000l << n, 0x00010001l << n, 0x00010100l << n, 0x00010101l << n,
                0x01000000l << n, 0x01000001l << n, 0x01000100l << n, 0x01000101l << n,
                0x01010000l << n, 0x01010001l << n, 0x01010100l << n, 0x01010101l << n
        };
    }

    private static final long[][] RTABLE = {
            rtable(0), rtable(1), rtable(2), rtable(3),
            rtable(4), rtable(5), rtable(6), rtable(7)
    };

    /**
     * Rotate bits clockwise.
     * The IFFImageReader uses this to convert pixel bits from planar to chunky.
     * Bits from the source are rotated 90 degrees clockwise written to the
     * destination.
     *
     * @param pSrc     source pixel data
     * @param pSrcPos  starting index of 8 x 8 bit source tile
     * @param pSrcStep byte offset between adjacent rows in source
     * @param pDst     destination pixel data
     * @param pDstPos  starting index of 8 x 8 bit destination tile
     * @param pDstStep byte offset between adjacent rows in destination
     */
    static void bitRotateCW(final byte[] pSrc, int pSrcPos, int pSrcStep,
                            final byte[] pDst, int pDstPos, int pDstStep) {
        int idx = pSrcPos;

        int lonyb;
        int hinyb;
        long lo = 0;
        long hi = 0;

        for (int i = 0; i < 8; i++) {
            lonyb = pSrc[idx] & 0xF;
            hinyb = (pSrc[idx] >> 4) & 0xF;
            lo |= RTABLE[i][lonyb];
            hi |= RTABLE[i][hinyb];
            idx += pSrcStep;
        }

        idx = pDstPos;

        pDst[idx] = (byte)((hi >> 24) & 0xFF);
        idx += pDstStep;
        if (idx < pDst.length) {
            pDst[idx] = (byte)((hi >> 16) & 0xFF);
            idx += pDstStep;
            if (idx < pDst.length) {
                pDst[idx] = (byte)((hi >> 8) & 0xFF);
                idx += pDstStep;
                if (idx < pDst.length) {
                    pDst[idx] = (byte)(hi & 0xFF);
                    idx += pDstStep;
                }
            }
        }

        if (idx < pDst.length) {
            pDst[idx] = (byte)((lo >> 24) & 0xFF);
            idx += pDstStep;
            if (idx < pDst.length) {
                pDst[idx] = (byte)((lo >> 16) & 0xFF);
                idx += pDstStep;
                if (idx < pDst.length) {
                    pDst[idx] = (byte)((lo >> 8) & 0xFF);
                    idx += pDstStep;
                    if (idx < pDst.length) {
                        pDst[idx] = (byte)(lo & 0xFF);
                    }
                }
            }
        }
    }
}
