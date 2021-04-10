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

import static com.twelvemonkeys.imageio.plugins.webp.vp8.Globals.clamp;

final class SegmentQuant {
    private int filterStrength;
    private int Qindex;
    private int uvac;
    private int uvdc;
    private int y1ac;
    private int y1dc;
    private int y2ac;
    private int y2dc;

    public int getQindex() {
        return Qindex;
    }

    public int getUvac_delta_q() {
        return uvac;
    }

    public int getUvdc_delta_q() {
        return uvdc;
    }

    public int getY1ac() {
        return y1ac;
    }

    public int getY1dc() {
        return y1dc;
    }

    public int getY2ac_delta_q() {
        return y2ac;
    }

    public int getY2dc() {
        return y2dc;
    }

    public void setFilterStrength(int value) {
        this.filterStrength = value;
    }

    public void setQindex(int qindex) {
        Qindex = qindex;
    }

    public void setUvac_delta_q(int uvac_delta_q) {
        this.uvac = Globals.vp8AcQLookup[clamp(Qindex + uvac_delta_q, 127)];
    }

    public void setUvdc_delta_q(int uvdc_delta_q) {
        this.uvdc = Globals.vp8DcQLookup[clamp(Qindex + uvdc_delta_q, 127)];
    }

    public void setY1ac() {
        this.y1ac = Globals.vp8AcQLookup[clamp(Qindex, 127)];
    }

    public void setY1dc(int y1dc) {
        this.y1dc = Globals.vp8DcQLookup[clamp(Qindex + y1dc, 127)];
        this.setY1ac();
    }

    public void setY2ac_delta_q(int y2ac_delta_q) {
        this.y2ac = Globals.vp8AcQLookup[clamp(Qindex + y2ac_delta_q, 127)] * 155 / 100;
        if (this.y2ac < 8) {
            this.y2ac = 8;
        }
    }

    public void setY2dc(int y2dc_delta_q) {
        this.y2dc = Globals.vp8DcQLookup[clamp(Qindex + y2dc_delta_q, 127)] * 2;
    }

    public int getFilterStrength() {
        return filterStrength;
    }
}
