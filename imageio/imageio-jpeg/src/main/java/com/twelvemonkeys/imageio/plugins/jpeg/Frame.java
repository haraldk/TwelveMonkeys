/*
 * Copyright (c) 2013, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.stream.SubImageInputStream;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Frame
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: SOFSegment.java,v 1.0 22.04.13 16:40 haraldk Exp$
 */
final class Frame extends Segment {
    final int samplePrecision; // Sample precision
    final int lines;          // Height
    final int samplesPerLine; // Width

    final Component[] components; // Components specifications

    private Frame(final int marker, final int samplePrecision, final int lines, final int samplesPerLine, final Component[] components) {
        super(marker);

        this.samplePrecision = samplePrecision;
        this.lines = lines;
        this.samplesPerLine = samplesPerLine;
        this.components = components;
    }

    int process() {
        return marker & 0xff - 0xc0;
    }

    int componentsInFrame() {
        return components.length;
    }

    Component getComponent(final int id) {
        for (Component component : components) {
            if (component.id == id) {
                return component;
            }
        }

        throw new IllegalArgumentException(String.format("No such component id: %d", id));
    }

    @Override
    public String toString() {
        return String.format(
                "SOF%d[%04x, precision: %d, lines: %d, samples/line: %d, components: %s]",
                process(), marker, samplePrecision, lines, samplesPerLine, Arrays.toString(components)
        );
    }

    static Frame read(final int marker, final DataInput data, final int length) throws IOException {
        int samplePrecision = data.readUnsignedByte();
        int lines = data.readUnsignedShort();
        int samplesPerLine = data.readUnsignedShort();
        int componentsInFrame = data.readUnsignedByte();

        int expected = 8 + componentsInFrame * 3;
        if (length != expected) {
            throw new IIOException(String.format("Unexpected SOF length: %d != %d", length, expected));
        }

        Component[] components = new Component[componentsInFrame];

        for (int i = 0; i < componentsInFrame; i++) {
            int id = data.readUnsignedByte();
            int sub = data.readUnsignedByte();
            int qtSel = data.readUnsignedByte();

            components[i] = new Component(id, ((sub & 0xF0) >> 4), (sub & 0xF), qtSel);
        }

        return new Frame(marker, samplePrecision, lines, samplesPerLine, components);
    }

    static Frame read(final int marker, final ImageInputStream data) throws IOException {
        int length = data.readUnsignedShort();

        return read(marker, new SubImageInputStream(data, length), length);
    }

    public static final class Component {
        final int id;
        final int hSub; // Horizontal sampling factor
        final int vSub; // Vertical sampling factor
        final int qtSel; // Quantization table destination selector

        Component(int id, int hSub, int vSub, int qtSel) {
            this.id = id;
            this.hSub = hSub;
            this.vSub = vSub;
            this.qtSel = qtSel;
        }

        @Override
        public String toString() {
            // Use id either as component number or component name, based on value
            Serializable idStr = (id >= 'a' && id <= 'z' || id >= 'A' && id <= 'Z') ? "'" + (char) id + "'" : id;
            return String.format("id: %s, sub: %d/%d, sel: %d", idStr, hSub, vSub, qtSel);
        }
    }
}
