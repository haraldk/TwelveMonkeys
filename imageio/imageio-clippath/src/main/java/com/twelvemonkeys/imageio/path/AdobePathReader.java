/*
 * Copyright (c) 2014, Harald Kuhr
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

import javax.imageio.IIOException;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.twelvemonkeys.lang.Validate.isTrue;
import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * Creates a {@code Shape} object from an Adobe Photoshop Path resource.
 *
 * @see <a href="http://www.adobe.com/devnet-apps/photoshop/fileformatashtml/#50577409_17587">Adobe Photoshop Path resource format</a>
 * @author <a href="mailto:jpalmer@itemmaster.com">Jason Palmer, itemMaster LLC</a>
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 */
public final class AdobePathReader {
    static final boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.path.debug"));

    private final DataInput data;

    /**
     * Creates a path reader that will read its data from a {@code DataInput},
     * such as an {@code ImageInputStream}.
     * The data length is assumed to be a multiple of 26.
     *
     * @param data the input to read data from.
     * @throws java.lang.IllegalArgumentException if {@code data} is {@code null}
     */
    public AdobePathReader(final DataInput data) {
        notNull(data, "data");
        this.data = data;
    }

    /**
     * Creates a path reader that will read its data from a {@code byte} array.
     * The array length must be a multiple of 26, and greater than 0.
     *
     * @param data the array to read data from.
     * @throws java.lang.IllegalArgumentException if {@code data} is {@code null}, or not a multiple of 26.
     */
    public AdobePathReader(final byte[] data) {
        this(new ByteArrayImageInputStream(
                notNull(data, "data"), 0,
                isTrue(data.length > 0 && data.length % 26 == 0, data.length, "data.length must be a multiple of 26: %d")
        ));
    }

    /**
     * Builds the path by reading from the supplied input.
     *
     * @return the path
     * @throws javax.imageio.IIOException if the input contains a bad path data.
     * @throws IOException if a general I/O exception occurs during reading.
     */
    public Path2D path() throws IOException {
        List<List<AdobePathSegment>> subPaths = new ArrayList<>();
        List<AdobePathSegment> currentPath = null;
        int currentPathLength = 0;

        AdobePathSegment segment;
        while ((segment = nextSegment()) != null) {

            if (DEBUG) {
                System.out.println(segment);
            }

            if (segment.selector == AdobePathSegment.OPEN_SUBPATH_LENGTH_RECORD || segment.selector == AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD) {
                if (currentPath != null) {
                    if (currentPathLength != currentPath.size()) {
                        throw new IIOException(String.format("Bad path, expected %d segments, found only %d", currentPathLength, currentPath.size()));
                    }
                    subPaths.add(currentPath);
                }

                currentPath = new ArrayList<>(segment.length);
                currentPathLength = segment.length;
            }
            else if (segment.selector == AdobePathSegment.OPEN_SUBPATH_BEZIER_LINKED
                    || segment.selector == AdobePathSegment.OPEN_SUBPATH_BEZIER_UNLINKED
                    || segment.selector == AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED
                    || segment.selector == AdobePathSegment.CLOSED_SUBPATH_BEZIER_UNLINKED) {
                if (currentPath == null) {
                    throw new IIOException("Bad path, missing subpath length record");
                }
                if (currentPath.size() >= currentPathLength) {
                    throw new IIOException(String.format("Bad path, expected %d segments, found%d", currentPathLength, currentPath.size()));
                }

                currentPath.add(segment);
            }
        }

        // now add the last one
        if (currentPath != null) {
            if (currentPathLength != currentPath.size()) {
                throw new IIOException(String.format("Bad path, expected %d segments, found only %d", currentPathLength, currentPath.size()));
            }

            subPaths.add(currentPath);
        }

        // We have collected the Path points, now create a Shape
        return pathToShape(subPaths);
    }

    /**
     * The Correct Order... P1, P2, P3, P4, P5, P6 (Closed) moveTo(P1)
     * curveTo(P1.cpl, P2.cpp, P2.ap); curveTo(P2.cpl, P3.cpp, P3.ap);
     * curveTo(P3.cpl, P4.cpp, P4.ap); curveTo(P4.cpl, P5.cpp, P5.ap);
     * curveTo(P5.cpl, P6.cpp, P6.ap); curveTo(P6.cpl, P1.cpp, P1.ap);
     * closePath()
     */
    private Path2D pathToShape(final List<List<AdobePathSegment>> paths) {
        GeneralPath path = new GeneralPath(Path2D.WIND_EVEN_ODD, paths.size());
        GeneralPath subpath = null;

        for (List<AdobePathSegment> points : paths) {
            int length = points.size();

            for (int i = 0; i < points.size(); i++) {
                AdobePathSegment current = points.get(i);

                int step = i == 0 ? 0 : i == length - 1 ? 2 : 1;

                switch (step) {
                    // Begin
                    case 0: {
                        subpath = new GeneralPath(Path2D.WIND_EVEN_ODD, length);
                        subpath.moveTo(current.apx, current.apy);

                        if (length > 1) {
                            AdobePathSegment next = points.get((i + 1));
                            subpath.curveTo(current.cplx, current.cply, next.cppx, next.cppy, next.apx, next.apy);
                        }
                        else {
                            subpath.lineTo(current.apx, current.apy);
                        }

                        break;
                    }
                    // Middle
                    case 1: {
                        AdobePathSegment next = points.get((i + 1)); // We are always guaranteed one more.
                        subpath.curveTo(current.cplx, current.cply, next.cppx, next.cppy, next.apx, next.apy);

                        break;
                    }
                    // End
                    case 2: {
                        AdobePathSegment first = points.get(0);

                        if (first.selector == AdobePathSegment.CLOSED_SUBPATH_BEZIER_LINKED || first.selector == AdobePathSegment.CLOSED_SUBPATH_BEZIER_UNLINKED) {
                            subpath.curveTo(current.cplx, current.cply, first.cppx, first.cppy, first.apx, first.apy);
                            subpath.closePath();
                            path.append(subpath, false);
                        }
                        else {
                            subpath.lineTo(current.apx, current.apy);
                            path.append(subpath, true);
                        }

                        break;
                    }
                    default:
                        throw new AssertionError();
                }
            }
        }

        return path;
    }

    private AdobePathSegment nextSegment() throws IOException {
        // Each segment is 26 bytes
        int selector;
        try {
            selector = data.readUnsignedShort();
        }
        catch (EOFException eof) {
            // No more data, we're done
            return null;
        }

        switch (selector) {
            case AdobePathSegment.INITIAL_FILL_RULE_RECORD:
            case AdobePathSegment.PATH_FILL_RULE_RECORD:
                // Spec says Fill rule is ignored by Photoshop
                data.skipBytes(24);
                return new AdobePathSegment(selector);
            case AdobePathSegment.CLOSED_SUBPATH_LENGTH_RECORD:
            case AdobePathSegment.OPEN_SUBPATH_LENGTH_RECORD:
                int size = data.readUnsignedShort();
                data.skipBytes(22);
                return new AdobePathSegment(selector, size);
            default:
                return new AdobePathSegment(
                        selector,
                        toFixedPoint(data.readInt()),
                        toFixedPoint(data.readInt()),
                        toFixedPoint(data.readInt()),
                        toFixedPoint(data.readInt()),
                        toFixedPoint(data.readInt()),
                        toFixedPoint(data.readInt())
                );
        }
    }

    // TODO: Move to AdobePathSegment
    private static double toFixedPoint(final int fixed) {
        return ((double) fixed / 0x1000000);
    }
}
