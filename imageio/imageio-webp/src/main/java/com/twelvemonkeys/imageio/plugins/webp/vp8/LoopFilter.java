/*
 * Copyright (c) 2017, Brooss, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.webp.vp8;

import static java.lang.Math.*;

final class LoopFilter {
    private static int clamp(int value) {
        return max(min(value, 127), -128);
    }

    private static int common_adjust(boolean use_outer_taps, /* filter is 2 or 4 taps wide */ Segment seg) {
        int p1 = u2s(seg.P1); /* retrieve and convert all 4 pixels */
        int p0 = u2s(seg.P0);
        int q0 = u2s(seg.Q0);
        int q1 = u2s(seg.Q1);

        /*
         * Disregarding clamping, when "use_outer_taps" is false, "a" is
         * 3*(q0-p0). Since we are about to divide "a" by 8, in this case we end
         * up multiplying the edge difference by 5/8. When "use_outer_taps" is
         * true (as for the simple filter), "a" is p1 - 3*p0 + 3*q0 - q1, which
         * can be thought of as a refinement of 2*(q0 - p0) and the adjustment
         * is something like (q0 - p0)/4.
         */
        int a = clamp((use_outer_taps ? clamp(p1 - q1) : 0) + 3 * (q0 - p0));
        /*
         * b is used to balance the rounding of a/8 in the case where the
         * "fractional" part "f" of a/8 is exactly 1/2.
         */
        int b = (clamp(a + 3)) >> 3;
        /*
         * Divide a by 8, rounding up when f >= 1/2. Although not strictly part
         * of the "C" language, the right-shift is assumed to propagate the sign
         * bit.
         */
        a = clamp(a + 4) >> 3;
        /* Subtract "a" from q0, "bringing it closer" to p0. */
        seg.Q0 = s2u(q0 - a);
        /*
         * Add "a" (with adjustment "b") to p0, "bringing it closer" to q0. The
         * clamp of "a+b", while present in the reference decoder, is
         * superfluous; we have -16 <= a <= 15 at this point.
         */
        seg.P0 = s2u(p0 + b);

        return a;
    }

    /*
     * All functions take (among other things) a segment (of length at most 4 +
     * 4 = 8) symmetrically straddling an edge. The pixel values (or pointers)
     * are always given in order, from the "beforemost" to the "aftermost". So,
     * for a horizontal edge (written "|"), an 8-pixel segment would be ordered
     * p3 p2 p1 p0 | q0 q1 q2 q3.
     */
    /*
     * Filtering is disabled if the difference between any two adjacent
     * "interior" pixels in the 8-pixel segment exceeds the relevant threshold
     * (I). A more complex thresholding calculation is done for the group of
     * four pixels that straddle the edge, in line with the calculation in
     * simple_segment() above.
     */
    private static boolean filter_yes(int I, // limit on interior differences
                                      int E, // limit at the edge
                                      int p3, int p2, int p1, int p0, // pixels before edge
                                      int q0, int q1, int q2, int q3 // pixels after edge
    ) {
        return (abs(p0 - q0) * 2 + abs(p1 - q1) / 2) <= E && abs(p3 - p2) <= I
                && abs(p2 - p1) <= I && abs(p1 - p0) <= I && abs(q3 - q2) <= I
                && abs(q2 - q1) <= I && abs(q1 - q0) <= I;
    }

    private static Segment getSegH(SubBlock rsb, SubBlock lsb, int a) {
        Segment seg = new Segment();
        int[][] rdest = rsb.getDest();
        int[][] ldest = lsb.getDest();

        seg.P0 = ldest[3][a];
        seg.P1 = ldest[2][a];
        seg.P2 = ldest[1][a];
        seg.P3 = ldest[0][a];
        seg.Q0 = rdest[0][a];
        seg.Q1 = rdest[1][a];
        seg.Q2 = rdest[2][a];
        seg.Q3 = rdest[3][a];

        return seg;
    }

    private static Segment getSegV(SubBlock bsb, SubBlock tsb, int a) {
        Segment seg = new Segment();
        int[][] bdest = bsb.getDest();
        int[][] tdest = tsb.getDest();

        seg.P0 = tdest[a][3];
        seg.P1 = tdest[a][2];
        seg.P2 = tdest[a][1];
        seg.P3 = tdest[a][0];
        seg.Q0 = bdest[a][0];
        seg.Q1 = bdest[a][1];
        seg.Q2 = bdest[a][2];
        seg.Q3 = bdest[a][3];

        return seg;
    }

    /*
     * Filtering is altered if (at least) one of the differences on either side
     * of the edge exceeds a threshold (we have "high edge variance").
     */
    private static boolean hev(int threshold,
                               int p1, int p0, // pixels before edge
                               int q0, int q1 // pixels after edge
    ) {
        return abs(p1 - p0) > threshold || abs(q1 - q0) > threshold;
    }

    static void loopFilterBlock(final MacroBlock cmb, final MacroBlock lmb, final MacroBlock tmb, int frameType, boolean simpleFilter, int sharpness) {
        if (simpleFilter) {
            loopFilterSimpleBlock(cmb, lmb, tmb, sharpness);
        }
        else {
            loopFilterUVBlock(cmb, lmb, tmb, sharpness, frameType);
            loopFilterYBlock(cmb, lmb, tmb, sharpness, frameType);
        }
    }

    static void loopFilterSimpleBlock(final MacroBlock cmb, final MacroBlock lmb, final MacroBlock tmb, final int sharpnessLevel) {
        int loop_filter_level = cmb.getFilterLevel();
        if (loop_filter_level != 0) {
            int interior_limit = cmb.getFilterLevel();

            if (sharpnessLevel > 0) {
                interior_limit >>= sharpnessLevel > 4 ? 2 : 1;
                if (interior_limit > 9 - sharpnessLevel) {
                    interior_limit = 9 - sharpnessLevel;
                }
            }
            if (interior_limit == 0) {
                interior_limit = 1;
            }

            // Luma and Chroma use the same inter-subblock edge limit
            int sub_bedge_limit = (loop_filter_level * 2) + interior_limit;
            if (sub_bedge_limit < 1) {
                sub_bedge_limit = 1;
            }

            // Luma and Chroma use the same inter-macroblock edge limit
            int mbedge_limit = sub_bedge_limit + 4;

            // left
            if (lmb != null) {
                for (int b = 0; b < 4; b++) {
                    SubBlock rsb = cmb.getSubBlock(SubBlock.Plane.Y1, 0, b);
                    SubBlock lsb = lmb.getSubBlock(SubBlock.Plane.Y1, 3, b);
                    for (int a = 0; a < 4; a++) {
                        Segment seg = getSegH(rsb, lsb, a);
                        // MBfilter(hev_threshold, interior_limit,
                        // mbedge_limit, seg);
                        // System.out.println(mbedge_limit);
                        simple_segment(mbedge_limit, seg);
                        setSegH(rsb, lsb, seg, a);
                    }
                }
            }

            // sb left
            if (!cmb.isSkip_inner_lf()) {
                for (int a = 1; a < 4; a++) {
                    for (int b = 0; b < 4; b++) {
                        SubBlock lsb = cmb.getSubBlock(SubBlock.Plane.Y1, a - 1, b);
                        SubBlock rsb = cmb.getSubBlock(SubBlock.Plane.Y1, a, b);
                        for (int c = 0; c < 4; c++) {
                            // System.out.println("sbleft a:"+a+" b:"+b+" c:"+c);
                            Segment seg = getSegH(rsb, lsb, c);
                            simple_segment(sub_bedge_limit, seg);
                            // System.out.println(sub_bedge_limit);
                            // subblock_filter(hev_threshold,interior_limit,sub_bedge_limit,
                            // seg);
                            setSegH(rsb, lsb, seg, c);
                        }
                    }
                }
            }

            // top
            if (tmb != null) {
                for (int b = 0; b < 4; b++) {
                    SubBlock tsb = tmb.getSubBlock(SubBlock.Plane.Y1, b, 3);
                    SubBlock bsb = cmb.getSubBlock(SubBlock.Plane.Y1, b, 0);
                    for (int a = 0; a < 4; a++) {
                        Segment seg = getSegV(bsb, tsb, a);
                        simple_segment(mbedge_limit, seg);
                        // System.out.println(mbedge_limit);
                        // MBfilter(hev_threshold, interior_limit,
                        // mbedge_limit, seg);
                        setSegV(bsb, tsb, seg, a);
                    }
                }
            }

            // sb top
            if (!cmb.isSkip_inner_lf()) {
                for (int a = 1; a < 4; a++) {
                    for (int b = 0; b < 4; b++) {
                        SubBlock tsb = cmb.getSubBlock(SubBlock.Plane.Y1, b, a - 1);
                        SubBlock bsb = cmb.getSubBlock(SubBlock.Plane.Y1, b, a);
                        for (int c = 0; c < 4; c++) {
                            // System.out.println("sbtop");
                            Segment seg = getSegV(bsb, tsb, c);
                            simple_segment(sub_bedge_limit, seg);
                            // System.out.println(sub_bedge_limit);
                            // subblock_filter(hev_threshold,interior_limit,sub_bedge_limit,
                            // seg);
                            setSegV(bsb, tsb, seg, c);
                        }
                    }
                }
            }
        }
    }

    static void loopFilterUVBlock(final MacroBlock cmb, final MacroBlock lmb, final MacroBlock tmb, final int sharpnessLevel, final int frameType) {
        int loop_filter_level = cmb.getFilterLevel();
        if (loop_filter_level != 0) {
            int interior_limit = cmb.getFilterLevel();
            if (sharpnessLevel > 0) {
                interior_limit >>= sharpnessLevel > 4 ? 2 : 1;
                if (interior_limit > 9 - sharpnessLevel) {
                    interior_limit = 9 - sharpnessLevel;
                }
            }
            if (interior_limit == 0) {
                interior_limit = 1;
            }

            int hev_threshold = 0;
            if (frameType == 0) { // current frame is a key frame
                if (loop_filter_level >= 40) {
                    hev_threshold = 2;
                }
                else if (loop_filter_level >= 15) {
                    hev_threshold = 1;
                }
            }
            else  { // current frame is an interframe
                if (loop_filter_level >= 40) {
                    hev_threshold = 3;
                }
                else if (loop_filter_level >= 20) {
                    hev_threshold = 2;
                }
                else if (loop_filter_level >= 15) {
                    hev_threshold = 1;
                }
            }

            // Luma and Chroma use the same inter-macroblock edge limit
            int mbedge_limit = ((loop_filter_level + 2) * 2) + interior_limit;
            // Luma and Chroma use the same inter-subblock edge limit
            int sub_bedge_limit = (loop_filter_level * 2) + interior_limit;

            if (lmb != null) {
                for (int b = 0; b < 2; b++) {
                    SubBlock rsbU = cmb.getSubBlock(SubBlock.Plane.U, 0, b);
                    SubBlock lsbU = lmb.getSubBlock(SubBlock.Plane.U, 1, b);
                    SubBlock rsbV = cmb.getSubBlock(SubBlock.Plane.V, 0, b);
                    SubBlock lsbV = lmb.getSubBlock(SubBlock.Plane.V, 1, b);
                    for (int a = 0; a < 4; a++) {
                        Segment seg = getSegH(rsbU, lsbU, a);
                        MBfilter(hev_threshold, interior_limit, mbedge_limit, seg);
                        setSegH(rsbU, lsbU, seg, a);
                        seg = getSegH(rsbV, lsbV, a);
                        MBfilter(hev_threshold, interior_limit, mbedge_limit, seg);
                        setSegH(rsbV, lsbV, seg, a);
                    }
                }
            }
            // sb left

            if (!cmb.isSkip_inner_lf()) {
                for (int a = 1; a < 2; a++) { // TODO: This does not loop?
                    for (int b = 0; b < 2; b++) {
                        SubBlock lsbU = cmb.getSubBlock(SubBlock.Plane.U, a - 1, b);
                        SubBlock rsbU = cmb.getSubBlock(SubBlock.Plane.U, a, b);
                        SubBlock lsbV = cmb.getSubBlock(SubBlock.Plane.V, a - 1, b);
                        SubBlock rsbV = cmb.getSubBlock(SubBlock.Plane.V, a, b);
                        for (int c = 0; c < 4; c++) {
                            Segment seg = getSegH(rsbU, lsbU, c);
                            subblock_filter(hev_threshold, interior_limit, sub_bedge_limit, seg);
                            setSegH(rsbU, lsbU, seg, c);
                            seg = getSegH(rsbV, lsbV, c);
                            subblock_filter(hev_threshold, interior_limit, sub_bedge_limit, seg);
                            setSegH(rsbV, lsbV, seg, c);
                        }
                    }
                }
            }

            // top
            if (tmb != null) {
                for (int b = 0; b < 2; b++) {
                    SubBlock tsbU = tmb.getSubBlock(SubBlock.Plane.U, b, 1);
                    SubBlock bsbU = cmb.getSubBlock(SubBlock.Plane.U, b, 0);
                    SubBlock tsbV = tmb.getSubBlock(SubBlock.Plane.V, b, 1);
                    SubBlock bsbV = cmb.getSubBlock(SubBlock.Plane.V, b, 0);
                    for (int a = 0; a < 4; a++) {
                        // System.out.println("l");
                        Segment seg = getSegV(bsbU, tsbU, a);
                        MBfilter(hev_threshold, interior_limit, mbedge_limit, seg);
                        setSegV(bsbU, tsbU, seg, a);
                        seg = getSegV(bsbV, tsbV, a);
                        MBfilter(hev_threshold, interior_limit, mbedge_limit, seg);
                        setSegV(bsbV, tsbV, seg, a);
                    }
                }
            }

            // sb top
            if (!cmb.isSkip_inner_lf()) {
                for (int a = 1; a < 2; a++) { // TODO: This does not loop...
                    for (int b = 0; b < 2; b++) {
                        SubBlock tsbU = cmb.getSubBlock(SubBlock.Plane.U, b, a - 1);
                        SubBlock bsbU = cmb.getSubBlock(SubBlock.Plane.U, b, a);
                        SubBlock tsbV = cmb.getSubBlock(SubBlock.Plane.V, b, a - 1);
                        SubBlock bsbV = cmb.getSubBlock(SubBlock.Plane.V, b, a);
                        for (int c = 0; c < 4; c++) {
                            Segment seg = getSegV(bsbU, tsbU, c);
                            subblock_filter(hev_threshold, interior_limit, sub_bedge_limit, seg);
                            setSegV(bsbU, tsbU, seg, c);
                            seg = getSegV(bsbV, tsbV, c);
                            subblock_filter(hev_threshold, interior_limit, sub_bedge_limit, seg);
                            setSegV(bsbV, tsbV, seg, c);
                        }
                    }
                }
            }
        }
    }

    static void loopFilterYBlock(final MacroBlock cmb, final MacroBlock lmb, final MacroBlock tmb, final int sharpnessLevel, final int frameType) {
        int loop_filter_level = cmb.getFilterLevel();

        if (loop_filter_level != 0) {
            int interior_limit = cmb.getFilterLevel();

            if (sharpnessLevel > 0) {
                interior_limit >>= sharpnessLevel > 4 ? 2 : 1;
                if (interior_limit > 9 - sharpnessLevel) {
                    interior_limit = 9 - sharpnessLevel;
                }
            }
            if (interior_limit == 0) {
                interior_limit = 1;
            }

            int hev_threshold = 0;
            if (frameType == 0) { // current frame is a key frame
                if (loop_filter_level >= 40) {
                    hev_threshold = 2;
                }
                else if (loop_filter_level >= 15) {
                    hev_threshold = 1;
                }
            }
            else { // current frame is an interframe
                if (loop_filter_level >= 40) {
                    hev_threshold = 3;
                }
                else if (loop_filter_level >= 20) {
                    hev_threshold = 2;
                }
                else if (loop_filter_level >= 15) {
                    hev_threshold = 1;
                }
            }

            /* Luma and Chroma use the same inter-macroblock edge limit */
            int mbedge_limit = ((loop_filter_level + 2) * 2) + interior_limit;
            /* Luma and Chroma use the same inter-subblock edge limit */
            int sub_bedge_limit = (loop_filter_level * 2) + interior_limit;

            // left
            if (lmb != null) {
                for (int b = 0; b < 4; b++) {
                    SubBlock rsb = cmb.getSubBlock(SubBlock.Plane.Y1, 0, b);
                    SubBlock lsb = lmb.getSubBlock(SubBlock.Plane.Y1, 3, b);
                    for (int a = 0; a < 4; a++) {
                        Segment seg = getSegH(rsb, lsb, a);
                        MBfilter(hev_threshold, interior_limit, mbedge_limit, seg);
                        setSegH(rsb, lsb, seg, a);
                    }
                }
            }
            // sb left
            if (!cmb.isSkip_inner_lf()) {
                for (int a = 1; a < 4; a++) {
                    for (int b = 0; b < 4; b++) {
                        SubBlock lsb = cmb.getSubBlock(SubBlock.Plane.Y1, a - 1, b);
                        SubBlock rsb = cmb.getSubBlock(SubBlock.Plane.Y1, a, b);
                        for (int c = 0; c < 4; c++) {
                            // System.out.println("sbleft a:"+a+" b:"+b+" c:"+c);
                            Segment seg = getSegH(rsb, lsb, c);
                            subblock_filter(hev_threshold, interior_limit, sub_bedge_limit, seg);
                            setSegH(rsb, lsb, seg, c);
                        }
                    }
                }
            }
            // top
            if (tmb != null) {
                for (int b = 0; b < 4; b++) {
                    SubBlock tsb = tmb.getSubBlock(SubBlock.Plane.Y1, b, 3);
                    SubBlock bsb = cmb.getSubBlock(SubBlock.Plane.Y1, b, 0);
                    for (int a = 0; a < 4; a++) {
                        Segment seg = getSegV(bsb, tsb, a);
                        MBfilter(hev_threshold, interior_limit, mbedge_limit, seg);
                        setSegV(bsb, tsb, seg, a);
                    }
                }
            }
            // sb top
            if (!cmb.isSkip_inner_lf()) {
                for (int a = 1; a < 4; a++) {
                    for (int b = 0; b < 4; b++) {
                        SubBlock tsb = cmb.getSubBlock(SubBlock.Plane.Y1, b, a - 1);
                        SubBlock bsb = cmb.getSubBlock(SubBlock.Plane.Y1, b, a);
                        for (int c = 0; c < 4; c++) {
                            Segment seg = getSegV(bsb, tsb, c);
                            subblock_filter(hev_threshold, interior_limit, sub_bedge_limit, seg);
                            setSegV(bsb, tsb, seg, c);
                        }
                    }
                }
            }
        }
    }

    private static void MBfilter(int hev_threshold, /* detect high edge variance */
                                 int interior_limit, /* possibly disable filter */
                                 int edge_limit, Segment seg) {
        int p3 = u2s(seg.P3), p2 = u2s(seg.P2), p1 = u2s(seg.P1), p0 = u2s(seg.P0);
        int q0 = u2s(seg.Q0), q1 = u2s(seg.Q1), q2 = u2s(seg.Q2), q3 = u2s(seg.Q3);
        if (filter_yes(interior_limit, edge_limit, q3, q2, q1, q0, p0, p1, p2, p3)) {
            if (!hev(hev_threshold, p1, p0, q0, q1)) {
                // Same as the initial calculation in "common_adjust",
                // w is something like twice the edge difference
                int w = clamp(clamp(p1 - q1) + 3 * (q0 - p0));

                // 9/64 is approximately 9/63 = 1/7 and 1<<7 = 128 = 2*64.
                // So this a, used to adjust the pixels adjacent to the edge,
                // is something like 3/7 the edge difference.
                int a = (27 * w + 63) >> 7;

                seg.Q0 = s2u(q0 - a);
                seg.P0 = s2u(p0 + a);
                // Next two are adjusted by 2/7 the edge difference
                a = (18 * w + 63) >> 7;
                // System.out.println("a: "+a);
                seg.Q1 = s2u(q1 - a);
                seg.P1 = s2u(p1 + a);
                // Last two are adjusted by 1/7 the edge difference
                a = (9 * w + 63) >> 7;
                seg.Q2 = s2u(q2 - a);
                seg.P2 = s2u(p2 + a);
            }
            else {
                // if hev, do simple filter
                common_adjust(true, seg); // using outer taps
            }
        }
    }

    /* Clamp, then convert signed number back to pixel value. */
    private static int s2u(int v) {
        return clamp(v) + 128;
    }

    private static void setSegH(SubBlock rsb, SubBlock lsb, Segment seg, int a) {
        int[][] rdest = rsb.getDest();
        int[][] ldest = lsb.getDest();
        ldest[3][a] = seg.P0;
        ldest[2][a] = seg.P1;
        ldest[1][a] = seg.P2;
        ldest[0][a] = seg.P3;
        rdest[0][a] = seg.Q0;
        rdest[1][a] = seg.Q1;
        rdest[2][a] = seg.Q2;
        rdest[3][a] = seg.Q3;
    }

    private static void setSegV(SubBlock bsb, SubBlock tsb, Segment seg, int a) {
        int[][] bdest = bsb.getDest();
        int[][] tdest = tsb.getDest();
        tdest[a][3] = seg.P0;
        tdest[a][2] = seg.P1;
        tdest[a][1] = seg.P2;
        tdest[a][0] = seg.P3;
        bdest[a][0] = seg.Q0;
        bdest[a][1] = seg.Q1;
        bdest[a][2] = seg.Q2;
        bdest[a][3] = seg.Q3;
    }

    private static void simple_segment(int edge_limit, // do nothing if edge difference exceeds limit
                                       Segment seg) {
        if ((abs(seg.P0 - seg.Q0) * 2 + abs(seg.P1 - seg.Q1) / 2) <= edge_limit) {
            common_adjust(true, seg); // use outer taps
        }
    }

    private static void subblock_filter(int hev_threshold, // detect high edge variance
                                        int interior_limit, // possibly disable filter
                                        int edge_limit, Segment seg) {
        int p3 = u2s(seg.P3), p2 = u2s(seg.P2), p1 = u2s(seg.P1), p0 = u2s(seg.P0);
        int q0 = u2s(seg.Q0), q1 = u2s(seg.Q1), q2 = u2s(seg.Q2), q3 = u2s(seg.Q3);
        if (filter_yes(interior_limit, edge_limit, q3, q2, q1, q0, p0, p1, p2, p3)) {
            boolean hv = hev(hev_threshold, p1, p0, q0, q1);
            int a = (common_adjust(hv, seg) + 1) >> 1;
            if (!hv) {
                seg.Q1 = s2u(q1 - a);
                seg.P1 = s2u(p1 + a);
            }
        }
    }

    /* Convert pixel value (0 <= v <= 255) to an 8-bit signed number. */
    private static int u2s(int v) {
        return v - 128;
    }
}
