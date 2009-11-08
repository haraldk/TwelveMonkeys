package com.twelvemonkeys.imageio.plugins.psd;

import org.w3c.dom.Document;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import java.util.Arrays;

/**
 * PSDMetadataFormat
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDMetadataFormat.java,v 1.0 Nov 4, 2009 5:27:53 PM haraldk Exp$
 */
public final class PSDMetadataFormat extends IIOMetadataFormatImpl {

    private final static PSDMetadataFormat sInstance = new PSDMetadataFormat();

    /**
     * Private constructor.
     * <p/>
     * The {@link javax.imageio.metadata.IIOMetadata} class will instantiate this class
     * by reflection, invoking the static {@code getInstance()} method.
     *
     * @see javax.imageio.metadata.IIOMetadata#getMetadataFormat
     * @see #getInstance()
     */
    private PSDMetadataFormat() {
        // Defines the root element
        super(PSDMetadata.NATIVE_METADATA_FORMAT_NAME, CHILD_POLICY_SOME);

        // root -> PSDHeader
        // TODO: How do I specify that the header is required?
        addElement("PSDHeader", PSDMetadata.NATIVE_METADATA_FORMAT_NAME, CHILD_POLICY_EMPTY);

        // TODO: Do the first two make sense?
//        addAttribute("PSDHeader", "signature", DATATYPE_STRING, false, "8BPS", Arrays.asList("8BPS"));
        addAttribute("PSDHeader", "version", DATATYPE_INTEGER, false, "1", Arrays.asList("1"));

        addAttribute("PSDHeader", "channels", DATATYPE_INTEGER, true, null, "1", "24", true, true);
        // rows?
        addAttribute("PSDHeader", "height", DATATYPE_INTEGER, true, null, "1", "30000", true, true);
        // columns?
        addAttribute("PSDHeader", "width", DATATYPE_INTEGER, true, null, "1", "30000", true, true);
        addAttribute("PSDHeader", "bits", DATATYPE_INTEGER, true, null, Arrays.asList("1", "8", "16"));
        // TODO: Consider using more readable names?!
        addAttribute("PSDHeader", "mode", DATATYPE_INTEGER, true, null, Arrays.asList(PSDMetadata.COLOR_MODES));

        /*
        Contains the required data to define the color mode.

        For indexed color images, the count will be equal to 768, and the mode data
        will contain the color table for the image, in non-interleaved order.

        For duotone images, the mode data will contain the duotone specification,
        the format of which is not documented.  Non-Photoshop readers can treat
        the duotone image as a grayscale image, and keep the duotone specification
        around as a black box for use when saving the file.
         */
        // root -> Palette
        // Color map for indexed, optional
        // NOTE: Palette, PaletteEntry naming taken from the standard format, native PSD naming is ColorModeData
        // NOTE: PSD stores these as 256 Red, 256 Green, 256 Blue.. Should we do the same in the meta data?
        addElement("Palette", PSDMetadata.NATIVE_METADATA_FORMAT_NAME, 256, 256); // 768 = 256 * 3
        addElement("PaletteEntry", "Palette", CHILD_POLICY_EMPTY);
        addAttribute("PaletteEntry", "index", DATATYPE_INTEGER, true, null, "0", "255", true, true);
        addAttribute("PaletteEntry", "red", DATATYPE_INTEGER, true, null, "0", "255", true, true);
        addAttribute("PaletteEntry", "green", DATATYPE_INTEGER, true, null, "0", "255", true, true);
        addAttribute("PaletteEntry", "blue", DATATYPE_INTEGER, true, null, "0", "255", true, true);
        // No alpha allowed in indexed color PSD

        // TODO: Duotone spec, optional (use same element as palette?)
        // Or use object or raw bytes..

        // root -> ImageResources
        // Image resources, optional
        addElement("ImageResources", PSDMetadata.NATIVE_METADATA_FORMAT_NAME, CHILD_POLICY_SEQUENCE); // SOME?

        // root -> ImageResources -> ImageResource
        // Generic resource
        addElement("ImageResource", "ImageResources", CHILD_POLICY_ALL);
        // TODO: Allow arbitrary values to be added as a generic resource...

        // root -> ImageResources -> AlphaChannelInfo
        addElement("AlphaChannelInfo", "ImageResources", 0, Integer.MAX_VALUE); // The format probably does not support that many layers..
        addElement("Name", "AlphaChannelInfo", CHILD_POLICY_EMPTY);
        addAttribute("Name", "value", DATATYPE_STRING, true, 0, Integer.MAX_VALUE);

        // root -> ImageResources -> DisplayInfo
        addElement("DisplayInfo", "ImageResources", CHILD_POLICY_EMPTY);
        // TODO: Consider using human readable strings
        // TODO: Limit values (0-8, 10, 11, 3000)
        addAttribute("DisplayInfo", "colorSpace", DATATYPE_INTEGER, true, null);
        addAttribute("DisplayInfo", "colors", DATATYPE_INTEGER, true, 4, 4);
        addAttribute("DisplayInfo", "opacity", DATATYPE_INTEGER, true, null, "0", "100", true, true);
        // TODO: Consider using human readable strings
        addAttribute("DisplayInfo", "kind", DATATYPE_INTEGER, true, null, Arrays.asList(PSDMetadata.DISPLAY_INFO_KINDS));

        // root -> ImageResources -> EXIF1Data
        addElement("EXIF1Data", "ImageResources", CHILD_POLICY_ALL);
        // TODO: Incorporate EXIF / TIFF metadata here somehow... (or treat as opaque bytes?)

        // root -> ImageResources -> PrintFlags
        addElement("PrintFlags", "ImageResources", CHILD_POLICY_EMPTY);
        addBooleanAttribute("PrintFlags", "labels", false, false);
        addBooleanAttribute("PrintFlags", "cropMasks", false, false);
        addBooleanAttribute("PrintFlags", "colorBars", false, false);
        addBooleanAttribute("PrintFlags", "registrationMarks", false, false);
        addBooleanAttribute("PrintFlags", "negative", false, false);
        addBooleanAttribute("PrintFlags", "flip", false, false);
        addBooleanAttribute("PrintFlags", "interpolate", false, false);
        addBooleanAttribute("PrintFlags", "caption", false, false);

        // root -> ImageResources -> PrintFlagsInformation
        addElement("PrintFlagsInformation", "ImageResources", CHILD_POLICY_EMPTY);
        addAttribute("PrintFlagsInformation", "version", DATATYPE_INTEGER, true, null);
        addBooleanAttribute("PrintFlagsInformation", "cropMarks", false, false);
        addAttribute("PrintFlagsInformation", "field", DATATYPE_INTEGER, true, null);
        addAttribute("PrintFlagsInformation", "bleedWidth", DATATYPE_INTEGER, true, null, "0", String.valueOf(Long.MAX_VALUE), true, true); // TODO: LONG??!
        addAttribute("PrintFlagsInformation", "bleedScale", DATATYPE_INTEGER, true, null, "0", String.valueOf(Integer.MAX_VALUE), true, true);

        // root -> ImageResources -> ResolutionInfo
        addElement("ResolutionInfo", "ImageResources", CHILD_POLICY_EMPTY);
        addAttribute("ResolutionInfo", "hRes", DATATYPE_FLOAT, true, null);
        // TODO: Or use string and more friendly names? "pixels/inch"/"pixels/cm" and "inch"/"cm"/"pt"/"pica"/"column"
        addAttribute("ResolutionInfo", "hResUnit", DATATYPE_INTEGER, true, null, Arrays.asList("1", "2"));
        addAttribute("ResolutionInfo", "widthUnit", DATATYPE_INTEGER, true, null, Arrays.asList("1", "2", "3", "4", "5"));
        addAttribute("ResolutionInfo", "vRes", DATATYPE_FLOAT, true, null);
        // TODO: Or use more friendly names?
        addAttribute("ResolutionInfo", "vResUnit", DATATYPE_INTEGER, true, null, Arrays.asList("1", "2"));
        addAttribute("ResolutionInfo", "heightUnit", DATATYPE_INTEGER, true, null, Arrays.asList("1", "2", "3", "4", "5"));

        // ??? addElement("Thumbnail", "ImageResources", CHILD_POLICY_CHOICE);

        // root -> ImageResources -> XMPData
        addElement("XMPData", "ImageResources", CHILD_POLICY_CHOICE);
        // TODO: Incorporate XMP metadata here somehow (or treat as opaque bytes?)
        addObjectValue("XMPData", Document.class, true, null);

        // TODO: Layers
        //addElement("ChannelSourceDestinationRange", "LayerSomething", CHILD_POLICY_CHOICE);

        // TODO: Global layer mask info
    }



    @Override
    public boolean canNodeAppear(final String pElementName, final ImageTypeSpecifier pImageType) {
        // TODO: PSDColorData and PaletteEntry only for indexed color model
        throw new UnsupportedOperationException("Method canNodeAppear not implemented"); // TODO: Implement
    }

    /**
     * Returns the shared instance of the {@code PSDMetadataFormat}.
     *
     * @return the shared instance.
     * @see javax.imageio.metadata.IIOMetadata#getMetadataFormat
     */
    public static PSDMetadataFormat getInstance() {
        return sInstance;
    }
}
