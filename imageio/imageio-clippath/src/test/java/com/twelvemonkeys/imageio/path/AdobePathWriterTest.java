/*
 * Copyright (c) 2020 Harald Kuhr
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

package com.twelvemonkeys.imageio.path;

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static com.twelvemonkeys.imageio.path.AdobePathSegment.*;
import static com.twelvemonkeys.imageio.path.PathsTest.assertPathEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * AdobePathWriterTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by haraldk: harald.kuhr$
 * @version : AdobePathWriterTest.java,v 1.0 2020-01-02 harald.kuhr Exp$
 */
public class AdobePathWriterTest {

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWriterNull() {
        new AdobePathWriter(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWriterInvalid() {
        new AdobePathWriter(new Path2D.Double(Path2D.WIND_NON_ZERO));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWriterOutOfBounds() {
        Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
        path.append(new Ellipse2D.Double(.5, 0.5, 2, 2), false);

        new AdobePathWriter(path);
    }

    @Test
    public void testCreateWriterValid() {
        Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
        path.append(new Ellipse2D.Double(.25, .25, .5, .5), false);

        new AdobePathWriter(path);
    }

    @Test
    public void testCreateWriterMulti() {
        Path2D path = new GeneralPath(Path2D.WIND_EVEN_ODD);
        path.append(new Ellipse2D.Float(.25f, .25f, .5f, .5f), false);
        path.append(new Rectangle2D.Double(0, 0, 1, .5), false);
        path.append(new Polygon(new int[] {1, 2, 0, 1}, new int[] {0, 2, 2, 0}, 4)
                .getPathIterator(AffineTransform.getScaleInstance(1 / 2.0, 1 / 2.0)), false);

        new AdobePathWriter(path);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNotClosed() {
        GeneralPath path = new GeneralPath(Path2D.WIND_EVEN_ODD);
        path.moveTo(.5, .5);
        path.lineTo(1, .5);
        path.curveTo(1, 1, 1, 1, .5, 1);

        new AdobePathWriter(path).writePath();
    }

    @Test
    public void testCreateClosed() {
        GeneralPath path = new GeneralPath(Path2D.WIND_EVEN_ODD);
        path.moveTo(.5, .5);
        path.lineTo(1, .5);
        path.curveTo(1, 1, 1, 1, .5, 1);
        path.closePath();

        byte[] bytes = new AdobePathWriter(path).writePath();

        assertEquals(6 * 26, bytes.length);

        int off = 0;

        // Path/initial fill rule: Even-Odd (0)
        assertArrayEquals(new byte[] {0, PATH_FILL_RULE_RECORD, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, INITIAL_FILL_RULE_RECORD, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));

        // Rectangle 1: 0, 0, 1, .5
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_LENGTH_RECORD, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 0, -128, 0, 0, 0, -128, 0, 0, 0, -128, 0, 0, 0, -128, 0, 0, 0, -128, 0, 0, 1, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 0, -128, 0, 0, 1, 0, 0, 0, 0, -128, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, -128, 0, 0, 0, -128, 0, 0, 0, -128, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));

        // Sanity
        assertEquals(bytes.length, off);
    }

    @Test
    public void testCreateImplicitClosed() {
        GeneralPath path = new GeneralPath(Path2D.WIND_EVEN_ODD);
        path.moveTo(.5, .5);
        path.lineTo(1, .5);
        path.curveTo(1, 1, 1, 1, .5, 1);
        path.lineTo(.5, .5);

        byte[] bytes = new AdobePathWriter(path).writePath();

        assertEquals(6 * 26, bytes.length);

        int off = 0;

        // Path/initial fill rule: Even-Odd (0)
        assertArrayEquals(new byte[] {0, PATH_FILL_RULE_RECORD, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, INITIAL_FILL_RULE_RECORD, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));

        // Rectangle 1: 0, 0, 1, .5
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_LENGTH_RECORD, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 0, -128, 0, 0, 0, -128, 0, 0, 0, -128, 0, 0, 0, -128, 0, 0, 0, -128, 0, 0, 1, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 0, -128, 0, 0, 1, 0, 0, 0, 0, -128, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, -128, 0, 0, 0, -128, 0, 0, 0, -128, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));

        // Sanity
        assertEquals(bytes.length, off);

    }

    @Test
    public void testCreateDoubleClosed() {
        GeneralPath path = new GeneralPath(Path2D.WIND_EVEN_ODD);
        path.moveTo(.5, .5);
        path.lineTo(1, .5);
        path.curveTo(1, 1, 1, 1, .5, 1);
        path.lineTo(.5, .5);
        path.closePath();

        byte[] bytes = new AdobePathWriter(path).writePath();

        assertEquals(6 * 26, bytes.length);

        int off = 0;

        // Path/initial fill rule: Even-Odd (0)
        assertArrayEquals(new byte[] {0, PATH_FILL_RULE_RECORD, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, INITIAL_FILL_RULE_RECORD, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));

        // Rectangle 1: 0, 0, 1, .5
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_LENGTH_RECORD, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 0, -128, 0, 0, 0, -128, 0, 0, 0, -128, 0, 0, 0, -128, 0, 0, 0, -128, 0, 0, 1, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 0, -128, 0, 0, 1, 0, 0, 0, 0, -128, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, -128, 0, 0, 0, -128, 0, 0, 0, -128, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));

        // Sanity
        assertEquals(bytes.length, off);
    }

    @Test
    public void testWriteToStream() throws IOException {
        Path2D path = new GeneralPath(Path2D.WIND_EVEN_ODD);
        path.append(new Ellipse2D.Double(0, 0, 1, 1), false);
        path.append(new Ellipse2D.Double(.5, .5, .5, .5), false);
        path.append(new Ellipse2D.Double(.25, .25, .5, .5), false);

        AdobePathWriter pathCreator = new AdobePathWriter(path);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(byteStream)) {
            pathCreator.writePath(output);
        }

        assertEquals(17 * 26, byteStream.size());

        byte[] bytes = byteStream.toByteArray();

        int off = 0;

        // Path/initial fill rule: Even-Odd (0)
        assertArrayEquals(new byte[] {0, PATH_FILL_RULE_RECORD, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, INITIAL_FILL_RULE_RECORD, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));

        // Elipse 1: 0, 0, 1, 1
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_LENGTH_RECORD, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_LINKED, 0, 57, 78, -68, 1, 0, 0, 0, 0, -128, 0, 0, 1, 0, 0, 0, 0, -58, -79, 68, 1, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_LINKED, 1, 0, 0, 0, 0, -58, -79, 68, 1, 0, 0, 0, 0, -128, 0, 0, 1, 0, 0, 0, 0, 57, 78, -68},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_LINKED, 0, -58, -79, 68, 0, 0, 0, 0, 0, -128, 0, 0, 0, 0, 0, 0, 0, 57, 78, -68, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_LINKED, 0, 0, 0, 0, 0, 57, 78, -68, 0, 0, 0, 0, 0, -128, 0, 0, 0, 0, 0, 0, 0, -58, -79, 68},
                Arrays.copyOfRange(bytes, off, off += 26));

        // Elipse 2: .5, .5, .5, .5
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_LENGTH_RECORD, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_LINKED, 0, -100, -89, 94, 1, 0, 0, 0, 0, -64, 0, 0, 1, 0, 0, 0, 0, -29, 88, -94, 1, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_LINKED, 1, 0, 0, 0, 0, -29, 88, -94, 1, 0, 0, 0, 0, -64, 0, 0, 1, 0, 0, 0, 0, -100, -89, 94},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_LINKED, 0, -29, 88, -94, 0, -128, 0, 0, 0, -64, 0, 0, 0, -128, 0, 0, 0, -100, -89, 94, 0, -128, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_LINKED, 0, -128, 0, 0, 0, -100, -89, 94, 0, -128, 0, 0, 0, -64, 0, 0, 0, -128, 0, 0, 0, -29, 88, -94},
                Arrays.copyOfRange(bytes, off, off += 26));

        // Elipse32: .25, .25, .5, .5
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_LENGTH_RECORD, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_LINKED, 0, 92, -89, 94, 0, -64, 0, 0, 0, -128, 0, 0, 0, -64, 0, 0, 0, -93, 88, -94, 0, -64, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_LINKED, 0, -64, 0, 0, 0, -93, 88, -94, 0, -64, 0, 0, 0, -128, 0, 0, 0, -64, 0, 0, 0, 92, -89, 94},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_LINKED, 0, -93, 88, -94, 0, 64, 0, 0, 0, -128, 0, 0, 0, 64, 0, 0, 0, 92, -89, 94, 0, 64, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_LINKED, 0, 64, 0, 0, 0, 92, -89, 94, 0, 64, 0, 0, 0, -128, 0, 0, 0, 64, 0, 0, 0, -93, 88, -94},
                Arrays.copyOfRange(bytes, off, off += 26));

        // Sanity
        assertEquals(bytes.length, off);
    }

    @Test
    public void testCreateArray() {
        Path2D path = new GeneralPath(Path2D.WIND_EVEN_ODD);
        path.append(new Rectangle2D.Double(0, 0, 1, .5), false);
        path.append(new Rectangle2D.Double(.25, .25, .5, .5), false);

        AdobePathWriter pathCreator = new AdobePathWriter(path);

        byte[] bytes = pathCreator.writePath();

        assertEquals(12 * 26, bytes.length);

        int off = 0;

        // Path/initial fill rule: Even-Odd (0)
        assertArrayEquals(new byte[] {0, PATH_FILL_RULE_RECORD, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, INITIAL_FILL_RULE_RECORD, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));

        // Rectangle 1: 0, 0, 1, .5
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_LENGTH_RECORD, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, -128, 0, 0, 1, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 0, -128, 0, 0, 1, 0, 0, 0, 0, -128, 0, 0, 1, 0, 0, 0, 0, -128, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 0, -128, 0, 0, 0, 0, 0, 0, 0, -128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));

        // Rectangle 2: .25, .25, .5, .5
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_LENGTH_RECORD, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 0, 64, 0, 0, 0, 64, 0, 0, 0, 64, 0, 0, 0, 64, 0, 0, 0, 64, 0, 0, 0, -64, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 0, 64, 0, 0, 0, -64, 0, 0, 0, 64, 0, 0, 0, -64, 0, 0, 0, -64, 0, 0, 0, -64, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 0, -64, 0, 0, 0, -64, 0, 0, 0, -64, 0, 0, 0, -64, 0, 0, 0, -64, 0, 0, 0, 64, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));
        assertArrayEquals(new byte[] {0, CLOSED_SUBPATH_BEZIER_UNLINKED, 0, -64, 0, 0, 0, 64, 0, 0, 0, -64, 0, 0, 0, 64, 0, 0, 0, 64, 0, 0, 0, 64, 0, 0},
                Arrays.copyOfRange(bytes, off, off += 26));

        // Sanity
        assertEquals(bytes.length, off);
    }

    @Test
    public void testRoundtrip0() throws IOException {
        Path2D path = new GeneralPath(Path2D.WIND_EVEN_ODD);
        path.append(new Rectangle2D.Double(0, 0, 1, .5), false);
        path.append(new Rectangle2D.Double(.25, .25, .5, .5), false);

        byte[] bytes = new AdobePathWriter(path).writePath();
        Path2D readPath = new AdobePathReader(new ByteArrayImageInputStream(bytes)).readPath();

        assertEquals(path.getWindingRule(), readPath.getWindingRule());
        assertEquals(path.getBounds2D(), readPath.getBounds2D());

        // TODO: Would be nice, but hard to do, as we convert all points to cubic...
//        assertPathEquals(path, readPath);
    }

    @Test
    public void testRoundtrip1() throws IOException {
        // We'll read this from a real file, with hardcoded offsets for simplicity
        // PSD IRB: offset: 34, length: 32598
        // Clipping path: offset: 31146, length: 1248
        ImageInputStream stream = PathsTest.resourceAsIIOStream("/psd/grape_with_path.psd");
        stream.seek(34 + 31146);
        byte[] data = new byte[1248];
        stream.readFully(data);

        Path2D path = new AdobePathReader(data).readPath();
        byte[] bytes = new AdobePathWriter(path).writePath();

        Path2D readPath = new AdobePathReader(new ByteArrayImageInputStream(bytes)).readPath();
        assertEquals(path.getWindingRule(), readPath.getWindingRule());
        assertEquals(path.getBounds2D(), readPath.getBounds2D());

        assertPathEquals(path, readPath);

        assertEquals(data.length, bytes.length);

        // Path segment 3 contains some unknown bits in the filler bytes, we'll ignore those...
        cleanLengthRecords(data);

        assertEquals(formatSegments(data), formatSegments(bytes));
        assertArrayEquals(data, bytes);
    }

    private static void cleanLengthRecords(byte[] data) {
        for (int i = 0; i < data.length; i += 26) {
            if (data[i + 1] == CLOSED_SUBPATH_LENGTH_RECORD) {
                // Clean everything after record type and length field
                for (int j = 4; j < 26; j++) {
                    data[i + j] = 0;
                }
            }
        }
    }

    private static String formatSegments(byte[] data) {
        StringBuilder builder = new StringBuilder(data.length * 5);

        for (int i = 0; i < data.length; i += 26) {
            builder.append(Arrays.toString(Arrays.copyOfRange(data, i, i + 26))).append('\n');
        }

        return builder.toString();
    }

    @Test
    public void testRoundtrip2() throws IOException {
        // We'll read this from a real file, with hardcoded offsets for simplicity
        // PSD IRB: offset: 16970, length: 11152
        // Clipping path: offset: 9250, length: 1534
        ImageInputStream stream = PathsTest.resourceAsIIOStream("/tiff/big-endian-multiple-clips.tif");
        stream.seek(16970 + 9250);
        byte[] data = new byte[1534];
        stream.readFully(data);

        Path2D path = new AdobePathReader(data).readPath();
        byte[] bytes = new AdobePathWriter(path).writePath();

        Path2D readPath = new AdobePathReader(new ByteArrayImageInputStream(bytes)).readPath();
        assertEquals(path.getWindingRule(), readPath.getWindingRule());
        assertEquals(path.getBounds2D(), readPath.getBounds2D());

        assertPathEquals(path, readPath);

        assertEquals(data.length, bytes.length);

        // Path segment 3 and 48 contains some unknown bits in the filler bytes, we'll ignore that:
        cleanLengthRecords(data);

        assertEquals(formatSegments(data), formatSegments(bytes));
        assertArrayEquals(data, bytes);
    }
}
