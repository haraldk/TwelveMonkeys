package com.twelvemonkeys.imageio.plugins.tiff;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormatImpl;

/**
 * TIFFMedataFormat.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: TIFFMedataFormat.java,v 1.0 17/04/15 harald.kuhr Exp$
 */
public final class TIFFMedataFormat extends IIOMetadataFormatImpl {
    private static final TIFFMedataFormat INSTANCE = new TIFFMedataFormat();

    // We'll reuse the metadata formats defined for JAI
    public static final String SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME = "com_sun_media_imageio_plugins_tiff_image_1.0";
    public static final String SUN_NATIVE_STREAM_METADATA_FORMAT_NAME = "com_sun_media_imageio_plugins_tiff_stream_1.0";

    public TIFFMedataFormat() {
        super(SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME, CHILD_POLICY_SOME);
    }

    @Override
    public boolean canNodeAppear(String elementName, ImageTypeSpecifier imageType) {
        return false;
    }

    public static TIFFMedataFormat getInstance() {
        return INSTANCE;
    }
}
