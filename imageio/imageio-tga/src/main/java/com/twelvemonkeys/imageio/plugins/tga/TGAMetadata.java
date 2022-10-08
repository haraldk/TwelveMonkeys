package com.twelvemonkeys.imageio.plugins.tga;

import com.twelvemonkeys.imageio.StandardImageMetadataSupport;

import javax.imageio.ImageTypeSpecifier;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

final class TGAMetadata extends StandardImageMetadataSupport {
    TGAMetadata(ImageTypeSpecifier type, TGAHeader header, TGAExtensions extensions) {
        super(builder(type)
                      .withCompressionTypeName(compressionName(header))
                      .withPixelAspectRatio(pixelAspectRatio(extensions))
                      .withOrientation(orientation(header))
                      .withFormatVersion(extensions == null ? "1.0" : "2.0")
                      .withDocumentCreationTime(documentCreationTime(extensions))
                      .withTextEntries(textEntries(header, extensions))
        );
    }

    private static String compressionName(TGAHeader header) {
        switch (header.getImageType()) {
            case TGA.IMAGETYPE_NONE:
            case TGA.IMAGETYPE_COLORMAPPED:
            case TGA.IMAGETYPE_TRUECOLOR:
            case TGA.IMAGETYPE_MONOCHROME:
                return "None";
            case TGA.IMAGETYPE_COLORMAPPED_RLE:
            case TGA.IMAGETYPE_TRUECOLOR_RLE:
            case TGA.IMAGETYPE_MONOCHROME_RLE:
                return "RLE";
            case TGA.IMAGETYPE_COLORMAPPED_HUFFMAN:
            case TGA.IMAGETYPE_COLORMAPPED_HUFFMAN_QUADTREE:
            default:
                return "Unknown";
        }
    }

    private static double pixelAspectRatio(TGAExtensions extensions) {
        return extensions != null ? extensions.getPixelAspectRatio() : 1f;
    }

    private static ImageOrientation orientation(TGAHeader header) {
        switch (header.origin) {
            case TGA.ORIGIN_LOWER_LEFT:
                return ImageOrientation.FlipH;
            case TGA.ORIGIN_LOWER_RIGHT:
                return ImageOrientation.Rotate180;
            case TGA.ORIGIN_UPPER_LEFT:
                return ImageOrientation.Normal;
            case TGA.ORIGIN_UPPER_RIGHT:
                return ImageOrientation.FlipV;
            default:
                throw new IllegalArgumentException("Unknown orientation: " + header.origin);
        }
    }

    private static Calendar documentCreationTime(TGAExtensions extensions) {
        return extensions != null ? extensions.creationDate : null;
    }

    private static Map<String, String> textEntries(TGAHeader header, TGAExtensions extensions) {
        LinkedHashMap<String, String> textEntries = new LinkedHashMap<>();

        // NOTE: Keywords follow TIFF standard naming
        putIfValue(textEntries, "DocumentName", header.getIdentification());

        if (extensions != null) {
            putIfValue(textEntries, "Software", extensions.getSoftwareVersion() == null ? extensions.getSoftware() : (extensions.getSoftware() + " " + extensions.getSoftwareVersion()));
            putIfValue(textEntries, "Artist", extensions.getAuthorName());
            putIfValue(textEntries, "UserComment", extensions.getAuthorComments());
        }

        return textEntries;
    }

    private static void putIfValue(final Map<String, String> textEntries, final String keyword, final String value) {
        if (value != null && !value.isEmpty()) {
            textEntries.put(keyword, value);
        }
    }
}
