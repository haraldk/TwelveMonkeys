/*
 * Copyright (c) 2017, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.webp.lossless.transform;

/**
 * PredictorMode.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 */
public interface PredictorMode {
    // Special rules:
    // Top-left pixel of image is predicted BLACK
    // Rest of top pixels is predicted L
    // Rest of leftmost pixels are predicted T
    // Rightmost pixels using TR, uses LEFTMOST pixel on SAME ROW (same distance as TR in memory!)

    int BLACK = 0; // 0xff000000 (represents solid black color in ARGB)
    int L = 1; // L
    int T = 2; // T
    int TR = 3; // TR
    int TL = 4; // TL
    int AVG_L_TR_T = 5; // Average2(Average2(L, TR), T)
    int AVG_L_TL = 6; // Average2(L, TL)
    int AVG_L_T = 7; // Average2(L, T)
    int AVG_TL_T = 8; // Average2(TL, T)
    int AVG_T_TR = 9; // Average2(T, TR)
    int AVG_L_TL_T_TR = 10; // Average2(Average2(L, TL), Average2(T, TR))
    int SELECT = 11; // Select(L, T, TL)
    int CLAMP_ADD_SUB_FULL = 12; // ClampAddSubtractFull(L, T, TL)
    int CLAMP_ADD_SUB_HALF = 13; //	ClampAddSubtractHalf(Average2(L, T), TL)
}
