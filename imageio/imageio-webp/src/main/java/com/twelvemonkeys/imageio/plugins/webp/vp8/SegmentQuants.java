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

import java.io.IOException;

final class SegmentQuants {

    private static DeltaQ get_delta_q(BoolDecoder bc, int prev)
            throws IOException {
        DeltaQ ret = new DeltaQ();
        ret.v = 0;
        ret.update = false;

        if (bc.readBit() > 0) {
            ret.v = bc.readLiteral(4);

            if (bc.readBit() > 0) {
                ret.v = -ret.v;
            }
        }

		/* Trigger a quantizer update if the delta-q value has changed */
        if (ret.v != prev) {
            ret.update = true;
        }

        return ret;
    }

    private int qIndex;

    private SegmentQuant[] segQuants = new SegmentQuant[Globals.MAX_MB_SEGMENTS];

    public SegmentQuants() {
        for (int x = 0; x < Globals.MAX_MB_SEGMENTS; x++) {
            segQuants[x] = new SegmentQuant();
        }
    }

    public int getqIndex() {
        return qIndex;
    }

    public SegmentQuant[] getSegQuants() {
        return segQuants;
    }

    public void parse(BoolDecoder bc, boolean segmentation_enabled,
            boolean mb_segement_abs_delta) throws IOException {
        qIndex = bc.readLiteral(7);
        boolean q_update = false;
        DeltaQ v = get_delta_q(bc, 0);
        int y1dc_delta_q = v.v;
        q_update = q_update || v.update;
        v = get_delta_q(bc, 0);
        int y2dc_delta_q = v.v;
        q_update = q_update || v.update;
        v = get_delta_q(bc, 0);
        int y2ac_delta_q = v.v;
        q_update = q_update || v.update;
        v = get_delta_q(bc, 0);
        int uvdc_delta_q = v.v;
        q_update = q_update || v.update;
        v = get_delta_q(bc, 0);
        int uvac_delta_q = v.v;
        q_update = q_update || v.update;

        for (SegmentQuant s : segQuants) {
            if (!segmentation_enabled) {
                s.setQindex(qIndex);
            } else if (!mb_segement_abs_delta) {
                s.setQindex(s.getQindex() + qIndex);
            }

            s.setY1dc(y1dc_delta_q);
            s.setY2dc(y2dc_delta_q);
            s.setY2ac_delta_q(y2ac_delta_q);
            s.setUvdc_delta_q(uvdc_delta_q);
            s.setUvac_delta_q(uvac_delta_q);

        }
    }

    public void setSegQuants(SegmentQuant[] segQuants) {
        this.segQuants = segQuants;
    }
}
