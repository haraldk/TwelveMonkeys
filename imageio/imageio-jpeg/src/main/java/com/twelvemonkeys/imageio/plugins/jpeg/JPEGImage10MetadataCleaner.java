package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegment;
import com.twelvemonkeys.xml.XMLSerializer;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.color.ICC_Profile;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * JPEGImage10MetadataCleaner
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImage10MetadataCleaner.java,v 1.0 22.10.13 14:41 haraldk Exp$
 */
final class JPEGImage10MetadataCleaner {

    /**
     * Native metadata format name
     */
    static final String JAVAX_IMAGEIO_JPEG_IMAGE_1_0 = "javax_imageio_jpeg_image_1.0";

    private final JPEGImageReader reader;

    JPEGImage10MetadataCleaner(final JPEGImageReader reader) {
        this.reader = reader;
    }

    IIOMetadata cleanMetadata(final IIOMetadata imageMetadata) throws IOException {
        // We filter out pretty much everything from the stream..
        // Meaning we have to read get *all APP segments* and re-insert into metadata.
        List<JPEGSegment> appSegments = reader.getAppSegments(JPEGImageReader.ALL_APP_MARKERS, null);

        // NOTE: There's a bug in the merging code in JPEGMetadata mergeUnknownNode that makes sure all "unknown" nodes are added twice in certain conditions.... ARGHBL...
        // DONE: 1: Work around
        // TODO: 2: REPORT BUG!
        // TODO: Report dht inconsistency bug (reads any amount of tables but only allows setting 4 tables)

        // TODO: Allow EXIF (as app1EXIF) in the JPEGvariety (sic) node. Need new format, might as well create a completely new format...
        // As EXIF is (a subset of) TIFF, (and the EXIF data is a valid TIFF stream) probably use something like:
        // http://download.java.net/media/jai-imageio/javadoc/1.1/com/sun/media/imageio/plugins/tiff/package-summary.html#ImageMetadata
        /*
        from: http://docs.oracle.com/javase/6/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html

        In future versions of the JPEG metadata format, other varieties of JPEG metadata may be supported (e.g. Exif)
        by defining other types of nodes which may appear as a child of the JPEGvariety node.

        (Note that an application wishing to interpret Exif metadata given a metadata tree structure in the
        javax_imageio_jpeg_image_1.0 format must check for an unknown marker segment with a tag indicating an
        APP1 marker and containing data identifying it as an Exif marker segment. Then it may use application-specific
        code to interpret the data in the marker segment. If such an application were to encounter a metadata tree
        formatted according to a future version of the JPEG metadata format, the Exif marker segment might not be
        unknown in that format - it might be structured as a child node of the JPEGvariety node.

        Thus, it is important for an application to specify which version to use by passing the string identifying
        the version to the method/constructor used to obtain an IIOMetadata object.)
         */

        IIOMetadataNode tree = (IIOMetadataNode) imageMetadata.getAsTree(JAVAX_IMAGEIO_JPEG_IMAGE_1_0);
        IIOMetadataNode jpegVariety = (IIOMetadataNode) tree.getElementsByTagName("JPEGvariety").item(0);
        IIOMetadataNode markerSequence = (IIOMetadataNode) tree.getElementsByTagName("markerSequence").item(0);

        JFIFSegment jfifSegment = reader.getJFIF();
        JFXXSegment jfxxSegment = reader.getJFXX();
        AdobeDCTSegment adobeDCT = reader.getAdobeDCT();
        ICC_Profile embeddedICCProfile = reader.getEmbeddedICCProfile(true);
        SOFSegment sof = reader.getSOF();

        boolean hasRealJFIF = false;
        boolean hasRealJFXX = false;
        boolean hasRealICC = false;

        if (jfifSegment != null) {
            // Normal case, conformant JFIF with 1 or 3 components
            // TODO: Test if we have CMY or other non-JFIF color space?
            if (sof.componentsInFrame() == 1 || sof.componentsInFrame() == 3) {
                IIOMetadataNode jfif = new IIOMetadataNode("app0JFIF");
                jfif.setAttribute("majorVersion", String.valueOf(jfifSegment.majorVersion));
                jfif.setAttribute("minorVersion", String.valueOf(jfifSegment.minorVersion));
                jfif.setAttribute("resUnits", String.valueOf(jfifSegment.units));
                jfif.setAttribute("Xdensity", String.valueOf(Math.max(1, jfifSegment.xDensity))); // Avoid 0 density
                jfif.setAttribute("Ydensity", String.valueOf(Math.max(1,jfifSegment.yDensity)));
                jfif.setAttribute("thumbWidth", String.valueOf(jfifSegment.xThumbnail));
                jfif.setAttribute("thumbHeight", String.valueOf(jfifSegment.yThumbnail));

                jpegVariety.appendChild(jfif);
                hasRealJFIF = true;

                // Add app2ICC and JFXX as proper nodes
                if (embeddedICCProfile != null) {
                    IIOMetadataNode app2ICC = new IIOMetadataNode("app2ICC");
                    app2ICC.setUserObject(embeddedICCProfile);
                    jfif.appendChild(app2ICC);
                    hasRealICC = true;
                }

                if (jfxxSegment != null) {
                    IIOMetadataNode JFXX = new IIOMetadataNode("JFXX");
                    jfif.appendChild(JFXX);
                    IIOMetadataNode app0JFXX = new IIOMetadataNode("app0JFXX");
                    app0JFXX.setAttribute("extensionCode", String.valueOf(jfxxSegment.extensionCode));

                    JFXXThumbnailReader thumbnailReader = new JFXXThumbnailReader(null, reader.getThumbnailReader(), 0, 0, jfxxSegment);
                    IIOMetadataNode jfifThumb;

                    switch (jfxxSegment.extensionCode) {
                        case JFXXSegment.JPEG:
                            jfifThumb = new IIOMetadataNode("JFIFthumbJPEG");
                            // Contains it's own "markerSequence" with full DHT, DQT, SOF etc...
                            IIOMetadata thumbMeta = thumbnailReader.readMetadata();
                            Node thumbTree = thumbMeta.getAsTree(JAVAX_IMAGEIO_JPEG_IMAGE_1_0);
                            jfifThumb.appendChild(thumbTree.getLastChild());
                            app0JFXX.appendChild(jfifThumb);
                            break;

                        case JFXXSegment.INDEXED:
                            jfifThumb = new IIOMetadataNode("JFIFthumbPalette");
                            jfifThumb.setAttribute("thumbWidth", String.valueOf(thumbnailReader.getWidth()));
                            jfifThumb.setAttribute("thumbHeight", String.valueOf(thumbnailReader.getHeight()));
                            app0JFXX.appendChild(jfifThumb);
                            break;

                        case JFXXSegment.RGB:
                            jfifThumb = new IIOMetadataNode("JFIFthumbRGB");
                            jfifThumb.setAttribute("thumbWidth", String.valueOf(thumbnailReader.getWidth()));
                            jfifThumb.setAttribute("thumbHeight", String.valueOf(thumbnailReader.getHeight()));
                            app0JFXX.appendChild(jfifThumb);
                            break;

                        default:
                            reader.processWarningOccurred(String.format("Unknown JFXX extension code: %d", jfxxSegment.extensionCode));
                    }

                    JFXX.appendChild(app0JFXX);
                    hasRealJFXX = true;
                }
            }
            else {
                // Typically CMYK JPEG with JFIF segment (Adobe or similar).
                reader.processWarningOccurred(String.format(
                        "Incompatible JFIF marker segment in stream. " +
                                "SOF%d has %d color components, JFIF allows only 1 or 3 components. Ignoring JFIF marker.",
                        sof.marker & 0xf, sof.componentsInFrame()
                ));
            }
        }

        // Special case: Broken AdobeDCT segment, inconsistent with SOF, use values from SOF
        if (adobeDCT != null && adobeDCT.getTransform() == AdobeDCTSegment.YCCK && sof.componentsInFrame() < 4) {
            reader.processWarningOccurred(String.format(
                    "Invalid Adobe App14 marker. Indicates YCCK/CMYK data, but SOF%d has %d color components. " +
                            "Ignoring Adobe App14 marker.",
                    sof.marker & 0xf, sof.componentsInFrame()
            ));

            // Remove bad AdobeDCT
            NodeList app14Adobe = tree.getElementsByTagName("app14Adobe");
            for (int i = app14Adobe.getLength() - 1; i >= 0; i--) {
                Node item = app14Adobe.item(i);
                item.getParentNode().removeChild(item);
            }

            // We don't add this as unknown marker, as we are certain it's bogus by now
        }

        Node next = null;
        for (JPEGSegment segment : appSegments) {
            // Except real app0JFIF, app0JFXX, app2ICC and app14Adobe, add all the app segments that we filtered away as "unknown" markers
            if (segment.marker() == JPEG.APP0 && "JFIF".equals(segment.identifier()) && hasRealJFIF) {
                continue;
            }
            else if (segment.marker() == JPEG.APP0 && "JFXX".equals(segment.identifier()) && hasRealJFXX) {
                continue;
            }
            else if (segment.marker() == JPEG.APP1 && "Exif".equals(segment.identifier()) /* always inserted */) {
                continue;
            }
            else if (segment.marker() == JPEG.APP2 && "ICC_PROFILE".equals(segment.identifier()) && hasRealICC) {
                continue;
            }
            else if (segment.marker() == JPEG.APP14 && "Adobe".equals(segment.identifier()) /* always inserted */) {
                continue;
            }

            IIOMetadataNode unknown = new IIOMetadataNode("unknown");
            unknown.setAttribute("MarkerTag", Integer.toString(segment.marker() & 0xff));

            DataInputStream stream = new DataInputStream(segment.data());

            try {
                String identifier = segment.identifier();
                int off = identifier != null ? identifier.length() + 1 : 0;

                byte[] data = new byte[off + segment.length()];

                if (identifier != null) {
                    System.arraycopy(identifier.getBytes(Charset.forName("ASCII")), 0, data, 0, identifier.length());
                }

                stream.readFully(data, off, segment.length());

                unknown.setUserObject(data);
            }
            finally {
                stream.close();
            }

            if (next == null) {
                // To be semi-compatible with the functionality in mergeTree,
                // let's insert after the last unknown tag, or before any other tag if no unknown tag exists
                NodeList unknowns = markerSequence.getElementsByTagName("unknown");

                if (unknowns.getLength() > 0) {
                    next = unknowns.item(unknowns.getLength() - 1).getNextSibling();
                }
                else {
                    next = markerSequence.getFirstChild();
                }
            }

            markerSequence.insertBefore(unknown, next);
        }

        // Inconsistency issue in the com.sun classes, it can read metadata with dht containing
        // more than 4 children, but will not allow setting such a tree...
        // We'll split AC/DC tables into separate dht nodes.
        NodeList dhts = markerSequence.getElementsByTagName("dht");
        for (int j = 0; j < dhts.getLength(); j++) {
            Node dht = dhts.item(j);
            NodeList dhtables = dht.getChildNodes();

            if (dhtables.getLength() > 4) {
                IIOMetadataNode acTables = new IIOMetadataNode("dht");
                dht.getParentNode().insertBefore(acTables, dht.getNextSibling());

                // Split into 2 dht nodes, one for AC and one for DC
                for (int i = 0; i < dhtables.getLength(); i++) {
                    Element dhtable = (Element) dhtables.item(i);
                    String tableClass = dhtable.getAttribute("class");
                    if ("1".equals(tableClass)) {
                        dht.removeChild(dhtable);
                        acTables.appendChild(dhtable);
                    }
                }
            }
        }

        try {
            imageMetadata.setFromTree(JAVAX_IMAGEIO_JPEG_IMAGE_1_0, tree);
        }
        catch (IIOInvalidTreeException e) {
            if (JPEGImageReader.DEBUG) {
                new XMLSerializer(System.out, System.getProperty("file.encoding")).serialize(tree, false);
            }

            throw e;
        }

        return imageMetadata;
    }
}
