/*
 * Copyright (c) 2016, Harald Kuhr
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

import com.twelvemonkeys.imageio.AbstractMetadata;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import org.w3c.dom.Node;

import javax.imageio.metadata.IIOMetadataNode;
import java.util.List;

/**
 * JPEGImage10Metadata.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: JPEGImage10Metadata.java,v 1.0 10/08/16 harald.kuhr Exp$
 */
class JPEGImage10Metadata extends AbstractMetadata {

    // TODO: Clean up. Consider just making the meta data classes we were trying to avoid in the first place....

    private final List<Segment> segments;

    JPEGImage10Metadata(List<Segment> segments) {
        super(true, JPEGImage10MetadataCleaner.JAVAX_IMAGEIO_JPEG_IMAGE_1_0, null, null, null);

        this.segments = segments;
    }

    @Override
    protected Node getNativeTree() {
        IIOMetadataNode root = new IIOMetadataNode(JPEGImage10MetadataCleaner.JAVAX_IMAGEIO_JPEG_IMAGE_1_0);

        IIOMetadataNode jpegVariety = new IIOMetadataNode("JPEGvariety");
        root.appendChild(jpegVariety);
        // TODO: If we have JFIF, append in JPEGvariety, but can't happen for lossless

        IIOMetadataNode markerSequence = new IIOMetadataNode("markerSequence");
        root.appendChild(markerSequence);

        for (Segment segment : segments)
            switch (segment.marker) {
                // SOF3 is the only one supported by now
                case JPEG.SOF3:
                    Frame sofSegment = (Frame) segment;

                    IIOMetadataNode sof = new IIOMetadataNode("sof");
                    sof.setAttribute("process", String.valueOf(sofSegment.marker & 0xf));
                    sof.setAttribute("samplePrecision", String.valueOf(sofSegment.samplePrecision));
                    sof.setAttribute("numLines", String.valueOf(sofSegment.lines));
                    sof.setAttribute("samplesPerLine", String.valueOf(sofSegment.samplesPerLine));
                    sof.setAttribute("numFrameComponents", String.valueOf(sofSegment.componentsInFrame()));

                    for (Frame.Component component : sofSegment.components) {
                        IIOMetadataNode componentSpec = new IIOMetadataNode("componentSpec");
                        componentSpec.setAttribute("componentId", String.valueOf(component.id));
                        componentSpec.setAttribute("HsamplingFactor", String.valueOf(component.hSub));
                        componentSpec.setAttribute("VsamplingFactor", String.valueOf(component.vSub));
                        componentSpec.setAttribute("QtableSelector", String.valueOf(component.qtSel));

                        sof.appendChild(componentSpec);
                    }

                    markerSequence.appendChild(sof);
                    break;

                case JPEG.DHT:
                    HuffmanTable huffmanTable = (HuffmanTable) segment;
                    IIOMetadataNode dht = new IIOMetadataNode("dht");

                    // Uses fixed tables...
                    for (int i = 0; i < 4; i++) {
                        for (int j = 0; j < 2; j++) {
                            if (huffmanTable.tc[i][j] != 0) {
                                IIOMetadataNode dhtable = new IIOMetadataNode("dhtable");
                                dhtable.setAttribute("class", String.valueOf(j));
                                dhtable.setAttribute("htableId", String.valueOf(i));
                                dht.appendChild(dhtable);
                            }
                        }
                    }

                    markerSequence.appendChild(dht);
                    break;

                case JPEG.DQT:
                    markerSequence.appendChild(new IIOMetadataNode("dqt"));
                    // TODO:
                    break;

                case JPEG.SOS:
                    Scan scan = (Scan) segment;
                    IIOMetadataNode sos = new IIOMetadataNode("sos");
                    sos.setAttribute("numScanComponents", String.valueOf(scan.components.length));
                    sos.setAttribute("startSpectralSelection", String.valueOf(scan.spectralSelStart));
                    sos.setAttribute("endSpectralSelection", String.valueOf(scan.spectralSelEnd));
                    sos.setAttribute("approxHigh", String.valueOf(scan.approxHigh));
                    sos.setAttribute("approxLow", String.valueOf(scan.approxLow));

                    for (Scan.Component component : scan.components) {
                        IIOMetadataNode spec = new IIOMetadataNode("scanComponentSpec");
                        spec.setAttribute("componentSelector", String.valueOf(component.scanCompSel));
                        spec.setAttribute("dcHuffTable", String.valueOf(component.dcTabSel));
                        spec.setAttribute("acHuffTable", String.valueOf(component.acTabSel));
                        sos.appendChild(spec);
                    }

                    markerSequence.appendChild(sos);
                    break;

                case JPEG.COM:
                    IIOMetadataNode com = new IIOMetadataNode("com");
                    com.setAttribute("comment", ((Comment) segment).comment);

                    markerSequence.appendChild(com);

                    break;

                case JPEG.APP14:
                    if (segment instanceof AdobeDCT) {
                        AdobeDCT adobe = (AdobeDCT) segment;
                        IIOMetadataNode app14Adobe = new IIOMetadataNode("app14Adobe");
                        app14Adobe.setAttribute("version", String.valueOf(adobe.version));
                        app14Adobe.setAttribute("flags0", String.valueOf(adobe.flags0));
                        app14Adobe.setAttribute("flags1", String.valueOf(adobe.flags1));
                        app14Adobe.setAttribute("transform", String.valueOf(adobe.transform));
                        markerSequence.appendChild(app14Adobe);
                        break;
                    }
                    // Else, fall through to unknown segment

                default:
                    IIOMetadataNode unknown = new IIOMetadataNode("unknown");
                    unknown.setAttribute("MarkerTag", String.valueOf(segment.marker & 0xFF));
                    unknown.setUserObject(((Application) segment).data);
                    markerSequence.appendChild(unknown);

                    break;
            }

        return root;
    }

    @Override
    protected IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma = new IIOMetadataNode("Chroma");

        for (Segment segment : segments) {
            if (segment instanceof Frame) {
                Frame sofSegment = (Frame) segment;
                IIOMetadataNode colorSpaceType = new IIOMetadataNode("ColorSpaceType");
                colorSpaceType.setAttribute("name", sofSegment.componentsInFrame() == 1 ? "GRAY" : "RGB"); // TODO YCC, YCCK, CMYK etc
                chroma.appendChild(colorSpaceType);

                IIOMetadataNode numChannels = new IIOMetadataNode("NumChannels");
                numChannels.setAttribute("value", String.valueOf(sofSegment.componentsInFrame()));
                chroma.appendChild(numChannels);

                break;
            }
        }

        return chroma;
    }

    @Override
    protected IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode compression = new IIOMetadataNode("Compression");

        IIOMetadataNode compressionTypeName = new IIOMetadataNode("CompressionTypeName");
        compressionTypeName.setAttribute("value", "JPEG"); // ...or "JPEG-LOSSLESS" (which is the name used by the JAI JPEGImageWriter for it's compression name)?
        compression.appendChild(compressionTypeName);

        IIOMetadataNode lossless = new IIOMetadataNode("Lossless");
        lossless.setAttribute("value", "TRUE"); // TODO: For lossless only
        compression.appendChild(lossless);

        IIOMetadataNode numProgressiveScans = new IIOMetadataNode("NumProgressiveScans");
        numProgressiveScans.setAttribute("value", "1"); // TODO!
        compression.appendChild(numProgressiveScans);

        return compression;
    }

    @Override
    protected IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimension = new IIOMetadataNode("Dimension");

        IIOMetadataNode imageOrientation = new IIOMetadataNode("ImageOrientation");
        imageOrientation.setAttribute("value", "normal"); // TODO
        dimension.appendChild(imageOrientation);

        return dimension;
    }

    @Override
    protected IIOMetadataNode getStandardTextNode() {
        IIOMetadataNode text = new IIOMetadataNode("Text");

        for (Segment segment : segments) {
            if (segment instanceof Comment) {
                IIOMetadataNode com = new IIOMetadataNode("TextEntry");
                com.setAttribute("keyword", "comment");
                com.setAttribute("value", ((Comment) segment).comment);

                text.appendChild(com);
            }
        }

        return text.hasChildNodes() ? text : null;
    }
}
