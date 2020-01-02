package com.twelvemonkeys.imageio.path;

import com.twelvemonkeys.imageio.metadata.psd.PSD;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.twelvemonkeys.imageio.path.AdobePathSegment.*;
import static com.twelvemonkeys.lang.Validate.isTrue;
import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * AdobePathWriter
 */
public final class AdobePathWriter {

    private final List<AdobePathSegment> segments;

    /**
     * Creates an AdobePathWriter for the given path.
     * <p>
 *     NOTE: Photoshop paths are stored with the coordinates
     *     (0,0) representing the top left corner of the image,
     *     and (1,1) representing the bottom right corner,
     *     regardless of image dimensions.
     * </p>
     *
     * @param path A {@code Path2D} instance that has {@link Path2D#WIND_EVEN_ODD WIND_EVEN_ODD} rule
     *             and is contained within the rectangle [x=0.0,y=0.0,w=1.0,h=1.0].
     * @throws IllegalArgumentException if {@code path} is {@code null},
     *                                  the paths winding rule is not @link Path2D#WIND_EVEN_ODD} or
     *                                  the paths bounding box is outside [x=0.0,y=0.0,w=1.0,h=1.0].
     */
    public AdobePathWriter(final Path2D path) {
        notNull(path, "path");
        // TODO: Test if PS really ignores winding rule as documented... Otherwise we could support writing non-zero too.
        isTrue(path.getWindingRule() == Path2D.WIND_EVEN_ODD, path.getWindingRule(), "Only even/odd winding rule supported: %d");
        isTrue(new Rectangle(0, 0, 1, 1).contains(path.getBounds2D()), path.getBounds2D(), "Path bounds must be within [x=0,y=0,w=1,h=1]: %s");

        segments = pathToSegments(path.getPathIterator(null));
    }

    // TODO: Look at the API so that conversion both ways are aligned. The read part builds a path from List<List<AdobePathSegment>...
    private static List<AdobePathSegment> pathToSegments(final PathIterator pathIterator) {
        double[] coords = new double[6];
        AdobePathSegment prev = new AdobePathSegment(CLOSED_SUBPATH_BEZIER_LINKED, 0, 0, 0,0, 0, 0);

        List<AdobePathSegment> subpath = new ArrayList<>();
        List<AdobePathSegment> segments = new ArrayList<>();
        segments.add(new AdobePathSegment(PATH_FILL_RULE_RECORD));

        while (!pathIterator.isDone()) {
            int segmentType = pathIterator.currentSegment(coords);
            System.out.println("segmentType: " + segmentType);
            System.err.println("coords: " + Arrays.toString(coords));

            switch (segmentType) {
                case PathIterator.SEG_MOVETO:
                    // TODO: What if we didn't close before the moveto? Start new segment here?

                    // Dummy starting point, will be updated later
                    prev = new AdobePathSegment(CLOSED_SUBPATH_BEZIER_LINKED, 0, 0, coords[1], coords[0], 0, 0);
                    break;

                case PathIterator.SEG_LINETO:
                    subpath.add(new AdobePathSegment(CLOSED_SUBPATH_BEZIER_LINKED, prev.cppy, prev.cppx, prev.apy, prev.apx, coords[1], coords[0]));
                    prev = new AdobePathSegment(CLOSED_SUBPATH_BEZIER_LINKED, coords[1], coords[0], coords[1], coords[0], 0, 0);
                    break;

                case PathIterator.SEG_QUADTO:
                    subpath.add(new AdobePathSegment(CLOSED_SUBPATH_BEZIER_LINKED, prev.cppy, prev.cppx, prev.apy, prev.apx, coords[1], coords[0]));
                    prev = new AdobePathSegment(CLOSED_SUBPATH_BEZIER_LINKED, coords[3], coords[2], coords[3], coords[2], 0, 0);
                    break;

                case PathIterator.SEG_CUBICTO:
                    subpath.add(new AdobePathSegment(CLOSED_SUBPATH_BEZIER_LINKED, prev.cppy, prev.cppx, prev.apy, prev.apx, coords[1], coords[0]));
                    prev = new AdobePathSegment(CLOSED_SUBPATH_BEZIER_LINKED, coords[3], coords[2], coords[5], coords[4], 0, 0);
                    break;

                case PathIterator.SEG_CLOSE:
                    // Replace initial point.
                    AdobePathSegment initial = subpath.get(0);
                    if (initial.apx != prev.apx || initial.apy != prev.apy) {
                        // TODO: Line back to initial if last anchor point does not equal initial anchor?
//                        subpath.add(new AdobePathSegment(CLOSED_SUBPATH_BEZIER_LINKED, prev.cppy, prev.cppx, prev.apy, prev.apx, 0, 0));
                        throw new AssertionError("Not a closed path");
                    }
                    subpath.set(0, new AdobePathSegment(CLOSED_SUBPATH_BEZIER_LINKED, prev.cppy, prev.cppx, initial.apy, initial.apx, initial.cply, initial.cplx));

                    // Add to full path
                    segments.add(new AdobePathSegment(CLOSED_SUBPATH_LENGTH_RECORD, subpath.size()));
                    segments.addAll(subpath);

                    subpath.clear();

                    break;
            }

            pathIterator.next();
        }

        return segments;
    }

    public void writePath(final DataOutput output) throws IOException {
        System.err.println("segments: " + segments.size());

        output.writeInt(PSD.RESOURCE_TYPE);
        output.writeShort(PSD.RES_CLIPPING_PATH);
        output.writeShort(0); // Path name (Pascal string) empty + pad
        output.writeInt(segments.size() * 26); // Resource size


        for (AdobePathSegment segment : segments) {
            System.err.println(segment);
            switch (segment.selector) {
                case PATH_FILL_RULE_RECORD:
                case INITIAL_FILL_RULE_RECORD:
                    // The first 26-byte path record contains a selector value of 6, path fill rule record.
                    // The remaining 24 bytes of the first record are zeroes. Paths use even/odd ruling.
                    output.writeShort(segment.selector);
                    output.write(new byte[24]);
                    break;
                case OPEN_SUBPATH_LENGTH_RECORD:
                case CLOSED_SUBPATH_LENGTH_RECORD:
                    output.writeShort(segment.selector);
                    output.writeShort(segment.length); // Subpath length
                    output.write(new byte[22]);
                    break;
                default:
                    output.writeShort(segment.selector);
                    output.writeInt(toFixedPoint(segment.cppy));
                    output.writeInt(toFixedPoint(segment.cppx));
                    output.writeInt(toFixedPoint(segment.apy));
                    output.writeInt(toFixedPoint(segment.apx));
                    output.writeInt(toFixedPoint(segment.cply));
                    output.writeInt(toFixedPoint(segment.cplx));
                    break;
            }
        }
    }

    public byte[] createPath() {
        // TODO: Do we need to care about endianness for TIFF files?
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (DataOutputStream stream = new DataOutputStream(bytes)) {
            writePath(stream);
        } catch (IOException e) {
            throw new AssertionError("Should never.. uh.. Oh well. It happened.", e);
        }

        return bytes.toByteArray();
    }

    // TODO: Move to AdobePathSegment
    private static int toFixedPoint(final double value) {
        return (int) Math.round(value * 0x1000000);
    }
}
