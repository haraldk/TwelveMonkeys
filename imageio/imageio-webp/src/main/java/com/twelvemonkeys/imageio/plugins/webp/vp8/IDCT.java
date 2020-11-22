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

final class IDCT {
	/* IDCT implementation */
	private static final int cospi8sqrt2minus1 = 20091;

	private static final int sinpi8sqrt2 = 35468;

	public static int[][] idct4x4llm(int input[]) {
		int i;
		int a1, b1, c1, d1;
		int offset = 0;

		int[] output = new int[16];
		int temp1, temp2;

		for (i = 0; i < 4; i++) {
			a1 = input[offset + 0] + input[offset + 8];
			b1 = input[offset + 0] - input[offset + 8];

			temp1 = (input[offset + 4] * sinpi8sqrt2) >> 16;
			temp2 = input[offset + 12]
					+ ((input[offset + 12] * cospi8sqrt2minus1) >> 16);

			c1 = temp1 - temp2;

			temp1 = input[offset + 4]
					+ ((input[offset + 4] * cospi8sqrt2minus1) >> 16);
			temp2 = (input[offset + 12] * sinpi8sqrt2) >> 16;
			d1 = temp1 + temp2;

			output[offset + (0 * 4)] = a1 + d1;
			output[offset + (3 * 4)] = a1 - d1;
			output[offset + (1 * 4)] = b1 + c1;
			output[offset + (2 * 4)] = b1 - c1;

			offset++;
		}

		int diffo = 0;
		int[][] diff = new int[4][4];
		offset = 0;
		for (i = 0; i < 4; i++) {
			a1 = output[(offset * 4) + 0] + output[(offset * 4) + 2];
			b1 = output[(offset * 4) + 0] - output[(offset * 4) + 2];

			temp1 = (output[(offset * 4) + 1] * sinpi8sqrt2) >> 16;
			temp2 = output[(offset * 4) + 3]
					+ ((output[(offset * 4) + 3] * cospi8sqrt2minus1) >> 16);
			c1 = temp1 - temp2;

			temp1 = output[(offset * 4) + 1]
					+ ((output[(offset * 4) + 1] * cospi8sqrt2minus1) >> 16);
			temp2 = (output[(offset * 4) + 3] * sinpi8sqrt2) >> 16;
			d1 = temp1 + temp2;

			output[(offset * 4) + 0] = (a1 + d1 + 4) >> 3;
			output[(offset * 4) + 3] = (a1 - d1 + 4) >> 3;
			output[(offset * 4) + 1] = (b1 + c1 + 4) >> 3;
			output[(offset * 4) + 2] = (b1 - c1 + 4) >> 3;

			diff[0][diffo] = (a1 + d1 + 4) >> 3;
			diff[3][diffo] = (a1 - d1 + 4) >> 3;
			diff[1][diffo] = (b1 + c1 + 4) >> 3;
			diff[2][diffo] = (b1 - c1 + 4) >> 3;

			offset++;
			diffo++;
		}

		return diff;

	}

	public static int[][] iwalsh4x4(int[] input) {
		int i;
		int a1, b1, c1, d1;
		int a2, b2, c2, d2;

		int[] output = new int[16];
		int[][] diff = new int[4][4];
		int offset = 0;
		for (i = 0; i < 4; i++) {
			a1 = input[offset + 0] + input[offset + 12];
			b1 = input[offset + 4] + input[offset + 8];
			c1 = input[offset + 4] - input[offset + 8];
			d1 = input[offset + 0] - input[offset + 12];

			output[offset + 0] = a1 + b1;
			output[offset + 4] = c1 + d1;
			output[offset + 8] = a1 - b1;
			output[offset + 12] = d1 - c1;
			offset++;
		}

		offset = 0;

		for (i = 0; i < 4; i++) {
			a1 = output[offset + 0] + output[offset + 3];
			b1 = output[offset + 1] + output[offset + 2];
			c1 = output[offset + 1] - output[offset + 2];
			d1 = output[offset + 0] - output[offset + 3];

			a2 = a1 + b1;
			b2 = c1 + d1;
			c2 = a1 - b1;
			d2 = d1 - c1;
			output[offset + 0] = (a2 + 3) >> 3;
			output[offset + 1] = (b2 + 3) >> 3;
			output[offset + 2] = (c2 + 3) >> 3;
			output[offset + 3] = (d2 + 3) >> 3;
			diff[0][i] = (a2 + 3) >> 3;
			diff[1][i] = (b2 + 3) >> 3;
			diff[2][i] = (c2 + 3) >> 3;
			diff[3][i] = (d2 + 3) >> 3;
			offset += 4;
		}

		return diff;

	}
}
