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

package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.metadata.Directory;
import org.w3c.dom.Document;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * PSDMetadataFormat
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDMetadataFormat.java,v 1.0 Nov 4, 2009 5:27:53 PM haraldk Exp$
 */
public final class PSDMetadataFormat extends IIOMetadataFormatImpl {

    static final List<String> PSD_BLEND_MODES = Arrays.asList(
            PSDUtil.intToStr(PSD.BLEND_PASS), PSDUtil.intToStr(PSD.BLEND_NORM), PSDUtil.intToStr(PSD.BLEND_DISS),
            PSDUtil.intToStr(PSD.BLEND_DARK), PSDUtil.intToStr(PSD.BLEND_MUL), PSDUtil.intToStr(PSD.BLEND_IDIV),
            PSDUtil.intToStr(PSD.BLEND_LBRN), PSDUtil.intToStr(PSD.BLEND_DKCL), PSDUtil.intToStr(PSD.BLEND_LITE),
            PSDUtil.intToStr(PSD.BLEND_SCRN), PSDUtil.intToStr(PSD.BLEND_DIV), PSDUtil.intToStr(PSD.BLEND_LDDG),
            PSDUtil.intToStr(PSD.BLEND_LGCL), PSDUtil.intToStr(PSD.BLEND_OVER), PSDUtil.intToStr(PSD.BLEND_SLIT),
            PSDUtil.intToStr(PSD.BLEND_HLIT), PSDUtil.intToStr(PSD.BLEND_VLIT), PSDUtil.intToStr(PSD.BLEND_LLIT),
            PSDUtil.intToStr(PSD.BLEND_PLIT), PSDUtil.intToStr(PSD.BLEND_HMIX), PSDUtil.intToStr(PSD.BLEND_DIFF),
            PSDUtil.intToStr(PSD.BLEND_SMUD), PSDUtil.intToStr(PSD.BLEND_FSUB), PSDUtil.intToStr(PSD.BLEND_FDIV),
            PSDUtil.intToStr(PSD.BLEND_HUE), PSDUtil.intToStr(PSD.BLEND_SAT), PSDUtil.intToStr(PSD.BLEND_COLR),
            PSDUtil.intToStr(PSD.BLEND_LUM)
    );

    private static final PSDMetadataFormat instance = new PSDMetadataFormat();

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
        addElement("Header", PSDMetadata.NATIVE_METADATA_FORMAT_NAME, CHILD_POLICY_EMPTY);

        addAttribute("Header", "type", DATATYPE_STRING, false, "PSD", Arrays.asList("PSD", "PSB"));
        addAttribute("Header", "version", DATATYPE_INTEGER, false, "1", Collections.singletonList("1"));

        addAttribute("Header", "channels", DATATYPE_INTEGER, true, null, "1", "24", true, true);
        // rows?
        addAttribute("Header", "height", DATATYPE_INTEGER, true, null, "1", "30000", true, true);
        // columns?
        addAttribute("Header", "width", DATATYPE_INTEGER, true, null, "1", "30000", true, true);
        addAttribute("Header", "bits", DATATYPE_INTEGER, true, null, Arrays.asList("1", "8", "16"));

        addAttribute("Header", "mode", DATATYPE_STRING, true, null, asListNoNulls(PSDMetadata.COLOR_MODES));

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
        addAttribute("Name", "value", DATATYPE_STRING, true, null);

        // root -> ImageResources -> DisplayInfo
        addElement("DisplayInfo", "ImageResources", CHILD_POLICY_EMPTY);
        // TODO: Consider using human readable strings
        // TODO: Limit values (0-8, 10, 11, 3000)
        addAttribute("DisplayInfo", "colorSpace", DATATYPE_STRING, true, null, asListNoNulls(PSDMetadata.DISPLAY_INFO_CS));
        addAttribute("DisplayInfo", "colors", DATATYPE_INTEGER, true, 4, 4);
        addAttribute("DisplayInfo", "opacity", DATATYPE_INTEGER, true, null, "0", "100", true, true);
        // TODO: Consider using human readable strings
        addAttribute("DisplayInfo", "kind", DATATYPE_STRING, true, null, Arrays.asList(PSDMetadata.DISPLAY_INFO_KINDS));

        // root -> ImageResources -> EXIF
        addElement("EXIF", "ImageResources", CHILD_POLICY_EMPTY);
        addObjectValue("EXIF", Directory.class, true, null);
        // TODO: Incorporate EXIF / TIFF metadata here somehow... (or treat as opaque bytes?)

        // root -> ImageResources -> GridAndGuideInfo
        addElement("GridAndGuideInfo", "ImageResources", 0, Integer.MAX_VALUE);
        addAttribute("GridAndGuideInfo", "version", DATATYPE_INTEGER, false, "1");
        addAttribute("GridAndGuideInfo", "verticalGridCycle", DATATYPE_INTEGER, false, "576");
        addAttribute("GridAndGuideInfo", "horizontalGridCycle", DATATYPE_INTEGER, false, "576");
        addElement("Guide", "GridAndGuideInfo", CHILD_POLICY_EMPTY);
        addAttribute("Guide", "location", DATATYPE_INTEGER, true, null, "0", Integer.toString(Integer.MAX_VALUE), true, true);
        addAttribute("Guide", "orientation", DATATYPE_STRING, true, null, Arrays.asList(PSDMetadata.GUIDE_ORIENTATIONS));

        // root -> ImageResources -> ICCProfile
        addElement("ICCProfile", "ImageResources", CHILD_POLICY_EMPTY);
        addAttribute("ICCProfile", "colorSpaceType", DATATYPE_STRING, true, null, Arrays.asList(PSDMetadata.JAVA_CS));

        // root -> ImageResources -> IPTC
        addElement("IPTC", "ImageResources", CHILD_POLICY_EMPTY);
        addObjectValue("IPTC", Directory.class, true, null);
        // TODO: Incorporate IPTC metadata here somehow... (or treat as opaque bytes?)

        // root -> ImageResources -> PixelAspectRatio
        addElement("PixelAspectRatio", "ImageResources", CHILD_POLICY_EMPTY);
        addAttribute("PixelAspectRatio", "version", DATATYPE_STRING, false, "1");
        addAttribute("PixelAspectRatio", "aspectRatio", DATATYPE_DOUBLE, true, null, "0", Double.toString(Double.POSITIVE_INFINITY), true, false);

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
        addAttribute("PrintFlagsInformation", "version", DATATYPE_INTEGER, false, "1");
        addBooleanAttribute("PrintFlagsInformation", "cropMarks", false, false);
        addAttribute("PrintFlagsInformation", "field", DATATYPE_INTEGER, true, "0");
        addAttribute("PrintFlagsInformation", "bleedWidth", DATATYPE_INTEGER, true, null, "0", String.valueOf(Long.MAX_VALUE), true, true); // TODO: LONG??!
        addAttribute("PrintFlagsInformation", "bleedScale", DATATYPE_INTEGER, true, null, "0", String.valueOf(Integer.MAX_VALUE), true, true);

        // root -> ImageResources -> PrintScale
        addElement("PrintScale", "ImageResources", CHILD_POLICY_EMPTY);
        addAttribute("PrintScale", "style", DATATYPE_STRING, false, null, Arrays.asList(PSDMetadata.PRINT_SCALE_STYLES));
        addAttribute("PrintScale", "xLocation", DATATYPE_FLOAT, true, null);
        addAttribute("PrintScale", "yLocation", DATATYPE_FLOAT, true, null);
        addAttribute("PrintScale", "scale", DATATYPE_FLOAT, true, null);

        // root -> ImageResources -> ResolutionInfo
        addElement("ResolutionInfo", "ImageResources", CHILD_POLICY_EMPTY);
        addAttribute("ResolutionInfo", "hRes", DATATYPE_FLOAT, true, null);
        addAttribute("ResolutionInfo", "hResUnit", DATATYPE_STRING, true, null, asListNoNulls(PSDMetadata.RESOLUTION_UNITS));
        addAttribute("ResolutionInfo", "widthUnit", DATATYPE_STRING, true, null, asListNoNulls(PSDMetadata.DIMENSION_UNITS));
        addAttribute("ResolutionInfo", "vRes", DATATYPE_FLOAT, true, null);
        addAttribute("ResolutionInfo", "vResUnit", DATATYPE_STRING, true, null, asListNoNulls(PSDMetadata.RESOLUTION_UNITS));
        addAttribute("ResolutionInfo", "heightUnit", DATATYPE_STRING, true, null, asListNoNulls(PSDMetadata.DIMENSION_UNITS));

        // root -> ImageResources -> UnicodeAlphaNames
        addElement("UnicodeAlphaNames", "ImageResources", 0, Integer.MAX_VALUE);
        addChildElement("UnicodeAlphaNames", "Name"); // TODO: Does this really work?

        // root -> ImageResources -> VersionInfo
        addElement("VersionInfo", "ImageResources", CHILD_POLICY_EMPTY);
        addAttribute("VersionInfo", "version", DATATYPE_INTEGER, false, "1");
        addBooleanAttribute("VersionInfo", "hasRealMergedData", false, false);
        addAttribute("VersionInfo", "writer", DATATYPE_STRING, true, null);
        addAttribute("VersionInfo", "reader", DATATYPE_STRING, true, null);
        addAttribute("VersionInfo", "fileVersion", DATATYPE_INTEGER, true, "1");

        // root -> ImageResources -> Thumbnail
        addElement("Thumbnail", "ImageResources", CHILD_POLICY_EMPTY);
        addObjectValue("Thumbnail", BufferedImage.class, true, null);

        // root -> ImageResources -> UnicodeAlphaName
        addElement("UnicodeAlphaName", "ImageResources", CHILD_POLICY_EMPTY);
        addAttribute("UnicodeAlphaName", "value", DATATYPE_STRING, true, null);

        // root -> ImageResources -> XMP
        addElement("XMP", "ImageResources", CHILD_POLICY_CHOICE);
        // TODO: Incorporate XMP metadata here somehow (or treat as opaque bytes?)
        addObjectValue("XMP", Document.class, true, null);

        // root -> Layers
        addElement("Layers", PSDMetadata.NATIVE_METADATA_FORMAT_NAME, 0, Short.MAX_VALUE);

        // root -> Layers -> LayerInfo
        addElement("LayerInfo", "Layers", CHILD_POLICY_EMPTY);
        addAttribute("LayerInfo", "name", DATATYPE_STRING, false, "");
        addAttribute("LayerInfo", "top", DATATYPE_INTEGER, false, "0");
        addAttribute("LayerInfo", "left", DATATYPE_INTEGER, false, "0");
        addAttribute("LayerInfo", "bottom", DATATYPE_INTEGER, false, "0");
        addAttribute("LayerInfo", "right", DATATYPE_INTEGER, false, "0");
        addAttribute("LayerInfo", "blendmode", DATATYPE_STRING, false, PSDUtil.intToStr(PSD.BLEND_NORM), PSD_BLEND_MODES);
        addAttribute("LayerInfo", "opacity", DATATYPE_INTEGER, false, "0");
        addAttribute("LayerInfo", "clipping", DATATYPE_STRING, false, "base", Arrays.asList("base", "non-base"));
        addAttribute("LayerInfo", "flags", DATATYPE_INTEGER, false, "0");

        // Redundant (derived from flags)
        addAttribute("LayerInfo", "transparencyProtected", DATATYPE_BOOLEAN, false, "false");
        addAttribute("LayerInfo", "visible", DATATYPE_BOOLEAN, false, "false");
        addAttribute("LayerInfo", "obsolete", DATATYPE_BOOLEAN, false, "false"); // Spec doesn't say if the flag is obsolete, or if this means the layer is obsolete...?
        addAttribute("LayerInfo", "pixelDataIrrelevant", DATATYPE_BOOLEAN, false, "false");


        // root -> GlobalLayerMask
        addElement("GlobalLayerMask", PSDMetadata.NATIVE_METADATA_FORMAT_NAME, CHILD_POLICY_EMPTY);
        addAttribute("GlobalLayerMask", "colorSpace", DATATYPE_INTEGER, false, "0");
        addAttribute("GlobalLayerMask", "colors", DATATYPE_INTEGER, false, 4, 4);
        addAttribute("GlobalLayerMask", "opacity", DATATYPE_INTEGER, false, "0");
        addAttribute("GlobalLayerMask", "kind", DATATYPE_STRING, false, "layer", Arrays.asList("selected", "protected", "layer"));
    }

    private static <T> List<T> asListNoNulls(final T[] values) {
        List<T> list = new ArrayList<>(values.length);

        for (T value : values) {
            if (value != null) {
                list.add(value);
            }
        }

        return list;
    }

    @Override
    public boolean canNodeAppear(final String elementName, final ImageTypeSpecifier imageType) {
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
        return instance;
    }
}
