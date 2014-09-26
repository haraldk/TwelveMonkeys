/*
Copyright (c) 2008, Harald Kuhr
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name "TwelveMonkeys" nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


Parts of this software is based on JVG/JIS.
See http://www.cs.hut.fi/~framling/JVG/index.html for more information.
Redistribution under BSD authorized by Kary Främling:

Copyright (c) 2003, Kary Främling
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the JIS/JVG nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.io.enc.Decoder;
import com.twelvemonkeys.io.enc.DecoderStream;
import com.twelvemonkeys.io.enc.PackBits16Decoder;
import com.twelvemonkeys.io.enc.PackBitsDecoder;

import javax.imageio.*;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Reader for Apple Mac Paint Picture (PICT) format.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author <a href="http://www.cs.hut.fi/~framling/JVG/">Kary Främling</a> (original PICT/QuickDraw parsing)
 * @author <a href="mailto:matthias.wiesmann@a3.epfl.ch">Matthias Wiesmann</a> (original embedded QuickTime parsing)
 * @version $Id: PICTReader.java,v 1.0 05.apr.2006 15:20:48 haku Exp$
 */
/*
 * @todo New paint strategy: Need to have a PEN and a PEN MODE, in addition to BG and PATTERN and PATTERN MODE.
 *       - These must be set before each frame/paint/invert/erase/fill operation.
 *         This is because there isn't a one-to-one mapping, between Java and PICT drawing.
 *        - Subclass Graphics?
 *        - Or create a QuickDrawContext that converts from PICT to AWT Graphics?
 *        - Or Methods like setupFrame(pen, penmode, penwidth?), setupPaint(pen, penmode), setupFill(patter, patMode), etc?
 *        - Or methods like frameRect(pen, penmode, penwidth, rect), frameOval(pen, penmode, penwidth, rect), etc?
 *        - Or methods like frameShape(pen, penmode, penwidth, shape), paintShape(pen, penmode, shape) etc??
 *      QuickDrawContext that wraps an AWT Grpahics, and with methods macthing opcodes, seems like the best fit ATM
 * @todo Remove null-checks for Graphics, as null-graphics makes no sense. 
 * @todo Some MAJOR clean up
 * @todo Object orientation of different opcodes?
 * @todo As we now have Graphics2D with more options, support more of the format?
 * @todo Support for some other compression (packType 3) that seems to be common...
 */
public class PICTImageReader extends ImageReaderBase {

    static boolean DEBUG = false;

    // Private fields
    private QuickDrawContext context;
    private Rectangle frame;

    private int version;

    // Variables for storing draw status
    private Point penPosition = new Point(0, 0);
    private Rectangle lastRectangle = new Rectangle(0, 0);

    // Ratio between the screen resolution and the image resolution
    private double screenImageXRatio;
    private double screenImageYRatio;

    // List of images created during image import
    private List<BufferedImage> images = new ArrayList<BufferedImage>();
    private long imageStartStreamPos;
    protected int picSize;

    public PICTImageReader() {
        this(null);
    }

    protected PICTImageReader(final ImageReaderSpi pProvider) {
        super(pProvider);
    }

    protected void resetMembers() {
        context = null;
        frame = null;
        images.clear();
    }

    /**
     * Open and read the frame size of the PICT file.
     *
     * @return return the PICT frame
     * @throws IOException if an I/O error occurs while reading the image.
     */
    private Rectangle getPICTFrame() throws IOException {
        if (frame == null) {
            // Read in header information
            readPICTHeader(imageInput);

            if (DEBUG) {
                System.out.println("Done reading PICT header!");
            }
        }

        return frame;
    }

    /**
     * Read the PICT header. The information read is shown on stdout if "DEBUG" is true.
     *
     * @param pStream the stream to read from
     *
     * @throws IOException if an I/O error occurs while reading the image.
     */
    private void readPICTHeader(final ImageInputStream pStream) throws IOException {
        pStream.seek(0l);

        try {
            readPICTHeader0(pStream);
        }
        catch (IIOException e) {
            // Rest and try again
            pStream.seek(0l);

            // Skip first 512 bytes
            skipNullHeader(pStream);
            readPICTHeader0(pStream);
        }
    }
    
    private void readPICTHeader0(final ImageInputStream pStream) throws IOException {
        // Get size
        picSize = pStream.readUnsignedShort();

        if (DEBUG) {
            System.out.println("picSize: " + picSize);
        }

        // Get frame at 72 dpi
        // NOTE: These are not pixel sizes!
        // Need to be multiplied with hRes/screenResolution and vRes/screenResolution
        int y = pStream.readUnsignedShort();
        int x = pStream.readUnsignedShort();
        int h = pStream.readUnsignedShort();
        int w = pStream.readUnsignedShort();

        frame = new Rectangle(x, y, w - x, h - y);
        if (frame.width < 0 || frame.height < 0) {
            throw new IIOException("Error in PICT header: Invalid frame " + frame);
        }
        if (DEBUG) {
            System.out.println("frame: " + frame);
        }

        // Set default display ratios. 72 dpi is the standard Macintosh resolution.
        screenImageXRatio = 1.0;
        screenImageYRatio = 1.0;

        // Get the version, since the way of reading the rest depends on it
        boolean isExtendedV2 = false;
        int version = pStream.readShort();
        if (DEBUG) {
            System.out.println(String.format("PICT version: 0x%04x", version));
        }

        if (version == (PICT.OP_VERSION << 8) + 0x01) {
            this.version = 1;
        }
        else if (version == PICT.OP_VERSION && pStream.readShort() == PICT.OP_VERSION_2) {
            this.version = 2;

            // Read in version 2 header op and test that it is valid: HeaderOp 0x0C00
            if (pStream.readShort() != PICT.OP_HEADER_OP) {
                throw new IIOException("Error in PICT header: Invalid HeaderOp, expected 0x0c00");
            }

            int headerVersion = pStream.readInt();
            if (DEBUG) {
                System.out.println(String.format("headerVersion: 0x%04x", headerVersion));
            }

            // TODO: This (headerVersion) should be picture size (bytes) for non-V2-EXT...?
            //       - but.. We should take care to make sure we don't mis-interpret non-PICT data...
            //if (headerVersion == PICT.HEADER_V2) {
            if ((headerVersion & 0xffff0000) != PICT.HEADER_V2_EXT) {
                // TODO: Test this.. Looks dodgy to me..
                // Get the image resolution and calculate the ratio between
                // the default Mac screen resolution and the image resolution

                // int y (fixed point)
                double y2 = PICTUtil.readFixedPoint(pStream);
                // int x (fixed point)
                double x2 = PICTUtil.readFixedPoint(pStream);
                // int w (fixed point)
                double w2 = PICTUtil.readFixedPoint(pStream); // ?!
                // int h (fixed point)
                double h2 = PICTUtil.readFixedPoint(pStream);

                screenImageXRatio = (w - x) / (w2 - x2);
                screenImageYRatio = (h - y) / (h2 - y2);

                if (screenImageXRatio < 0 || screenImageYRatio < 0) {
                    throw new IIOException("Error in PICT header: Invalid bounds " + new Rectangle.Double(x2, y2, w2 - x2, h2 - y2));
                }
                if (DEBUG) {
                    System.out.println("bounding rect: " + new Rectangle.Double(x2, y2, w2 - x2, h2 - y2));
                }

                // int reserved
                pStream.skipBytes(4);
            }
            else /*if ((headerVersion & 0xffff0000) == PICT.HEADER_V2_EXT)*/ {
                isExtendedV2 = true;
                // Get the image resolution
                // Not sure if they are useful for anything...

                // int horizontal res (fixed point)
                double xRes = PICTUtil.readFixedPoint(pStream);
                // int vertical res (fixed point)
                double yRes = PICTUtil.readFixedPoint(pStream);

                if (DEBUG) {
                    System.out.println("xResolution: " + xRes);
                    System.out.println("yResolution: " + yRes);
                }

                // Get the image resolution and calculate the ratio between
                // the default Mac screen resolution and the image resolution
                // short y
                short y2 = pStream.readShort();
                // short x
                short x2 = pStream.readShort();
                // short h
                short h2 = pStream.readShort();
                // short w
                short w2 = pStream.readShort();

                screenImageXRatio = (w - x) / (double) (w2 - x2);
                screenImageYRatio = (h - y) / (double) (h2 - y2);

                if (screenImageXRatio < 0 || screenImageYRatio < 0) {
                    throw new IIOException("Error in PICT header: Invalid bounds " + new Rectangle.Double(x2, y2, w2 - x2, h2 - y2));
                }
                if (DEBUG) {
                    System.out.println("bounding rect: " + new Rectangle(x2, y2, w2 - x2, h2 - y2));
                }

                // long reserved
                pStream.skipBytes(4);
            }

            if (DEBUG) {
                System.out.println("screenImageXRatio: " + screenImageXRatio);
                System.out.println("screenImageYRatio: " + screenImageYRatio);
            }
        }
        else {
            // No version information, return straight away
            throw new IIOException("Error in PICT header: Missing or unknown version information");
        }

        if (DEBUG) {
            System.out.println("Version: " + this.version + (isExtendedV2 ? " extended" : ""));
        }

        imageStartStreamPos = pStream.getStreamPosition();

        // Won't need header data again (NOTE: We'll only get here if no exception is thrown)
        pStream.flushBefore(imageStartStreamPos);
    }

    static void skipNullHeader(final ImageInputStream pStream) throws IOException {
        // NOTE: Only skip if FILE FORMAT, not needed for Mac OS DnD
        // Spec says "platofrm dependent", may not be all nulls..
        pStream.skipBytes(PICT.PICT_NULL_HEADER_SIZE);
    }

    /**
     * Reads the PICT stream.
     * The contents of the stream will be drawn onto the supplied graphics 
     * object.
     * <p/>
     * If "DEBUG" is true, the elements read are listed on stdout.
     *
     * @param pGraphics the graphics object to draw onto.
     *
     * @throws javax.imageio.IIOException if the data can not be read.
     * @throws IOException if an I/O error occurs while reading the image.
     */
    private void drawOnto(Graphics2D pGraphics) throws IOException {
        context = new QuickDrawContext(pGraphics);

        readPICTopcodes(imageInput);
        if (DEBUG) {
            System.out.println("Done reading PICT body!");
        }
    }

    /**
     * Parse PICT opcodes in a PICT file. The input stream must be
     * positioned at the beginning of the opcodes, after picframe.
     * If we have a non-null graphics, we try to draw the elements.
     *
     * @param pStream the stream to read from
     *
     * @throws javax.imageio.IIOException if the data can not be read. 
     * @throws java.io.IOException if an I/O error occurs while reading the image.
     */
    private void readPICTopcodes(ImageInputStream pStream) throws IOException {
        pStream.seek(imageStartStreamPos);

        int opCode, dh, dv, dataLength;
        byte[] colorBuffer = new byte[3 * PICT.COLOR_COMP_SIZE];


        Pattern fill = QuickDraw.BLACK;
        Pattern bg;
        Pattern pen;
        Paint foreground;
        Paint background;
        Color hilight = Color.RED;

        Point origin, dh_dv;
        Point ovSize = new Point();
        Point arcAngles = new Point();
        String text;
        Rectangle bounds = new Rectangle();
        Polygon polygon = new Polygon();
        Polygon region = new Polygon();
        int pixmapCount = 0;

        try {
            // Read from file until we read the end of picture opcode
            do {
                // Read opcode, version 1: byte, version 2: short
                if (version == 1) {
                    opCode = pStream.readUnsignedByte();
                }
                else {
                    // Always word-aligned for version 2
                    if ((pStream.getStreamPosition() & 1) > 0) {
                        pStream.readByte();
                    }
                    opCode = pStream.readUnsignedShort();
                }

                // See what we got and react in consequence
                switch (opCode) {
                    case PICT.NOP:
                        // Just go on
                        if (DEBUG) {
                            System.out.println("NOP");
                        }
                        break;

                    case PICT.OP_CLIP_RGN:// OK for RECTS, not for regions yet
                        // Read the region
                        if ((region = readRegion(pStream, bounds)) == null) {
                            throw new IIOException("Could not read region");
                        }
                        // Set clip rect or clip region

                        //if (mGraphics != null) {
                        //    if (region.npoints == 0) {
                        //        // TODO: Read what the specs says about this...
                        //        if (bounds.width > 0 && bounds.height > 0) {
                        //            mGraphics.setClip(bounds.x, bounds.y, bounds.width, bounds.height);
                        //        }
                        //    }
                        //    else {
                        //        mGraphics.setClip(region);
                        //    }
                        //}
                        if (DEBUG) {
                            verboseRegionCmd("clipRgn", bounds, region);
                        }
                        break;

                    case PICT.OP_BK_PAT:
                        // Get the data
                        context.setBackgroundPattern(PICTUtil.readPattern(pStream));
                        if (DEBUG) {
                            System.out.println("bkPat");
                        }
                        break;

                    case PICT.OP_TX_FONT:// DIFFICULT TO KNOW THE FONT???
                        // Get the data
                        pStream.readFully(new byte[2], 0, 2);
                        // TODO: Font family id, 0 - System font, 1 - Application font.
                        // But how can we get these mappings?
                        if (DEBUG) {
                            System.out.println("txFont");
                        }
                        break;

                    case PICT.OP_TX_FACE:// SEE IF IT IS TO BE IMPLEMENTED FOR NOW?
                        // Get the data
                        byte txFace = pStream.readByte();

                        //// Construct text face mask
//                        currentFont = mGraphics.getFont();
                        //int awt_face_mask = 0;
                        //if ((txFace & (byte) QuickDraw.TX_BOLD_MASK) > 0) {
                        //    awt_face_mask |= Font.BOLD;
                        //}
                        //if ((txFace & (byte) QuickDraw.TX_ITALIC_MASK) > 0) {
                        //    awt_face_mask |= Font.ITALIC;
                        //}
                        //
                        //// Set the font
                        //mGraphics.setFont(new Font(currentFont.getName(), awt_face_mask, currentFont.getSize()));

                        if (DEBUG) {
                            System.out.println("txFace: " + txFace);
                        }
                        break;

                    case PICT.OP_TX_MODE:// SEE IF IT IS TO BE IMPLEMENTED FOR NOW?
                        // Get the data
                        byte[] mode_buf = new byte[2];
                        pStream.readFully(mode_buf, 0, mode_buf.length);
                        if (DEBUG) {
                            System.out.println("txMode: " + mode_buf[0] + ", " + mode_buf[1]);
                        }
                        break;

                    case PICT.OP_SP_EXTRA:// WONDER WHAT IT IS?
                        // Get the data
                        pStream.readFully(new byte[4], 0, 4);
                        if (DEBUG) {
                            System.out.println("spExtra");
                        }
                        break;

                    case PICT.OP_PN_SIZE:
                        // Get the two words
                        // NOTE: This is out of order, compared to other Points
                        Dimension pnsize = new Dimension(pStream.readUnsignedShort(), pStream.readUnsignedShort());
                        context.setPenSize(pnsize);
                        if (DEBUG) {
                            System.out.println("pnsize: " + pnsize);
                        }
                        break;

                    case PICT.OP_PN_MODE:// TRY EMULATING WITH SETXORMODE ETC
                        // Get the data
                        int mode = pStream.readUnsignedShort();
                        if (DEBUG) {
                            System.out.println("pnMode: " + mode);
                        }

                        context.setPenMode(mode);

                        break;

                    case PICT.OP_PN_PAT:
                        context.setPenPattern(PICTUtil.readPattern(pStream));
                        if (DEBUG) {
                            System.out.println("pnPat");
                        }
                        break;

                    case PICT.OP_FILL_PAT:
                        fill = PICTUtil.readPattern(pStream);
                        if (DEBUG) {
                            System.out.println("fillPat");
                        }
                        break;

                    case PICT.OP_OV_SIZE:// OK, we use this for rounded rectangle corners
                        // Get the two words
                        int y = getYPtCoord(pStream.readUnsignedShort());
                        int x = getXPtCoord(pStream.readUnsignedShort());

                        ovSize.setLocation(x, y);
                        /*
                        ovSize.x *= 2;// Don't know why, but has to be multiplied by 2
                        
                        ovSize.y *= 2;
                        */
                        if (DEBUG) {
                            System.out.println("ovSize: " + ovSize);
                        }
                        break;

                    case PICT.OP_ORIGIN:// PROBABLY OK
                        // Get the two words
                        y = getYPtCoord(pStream.readUnsignedShort());
                        x = getXPtCoord(pStream.readUnsignedShort());
                        origin = new Point(x, y);
                        //if (mGraphics != null) {
                        //    mGraphics.translate(origin.x, origin.y);
                        //}
                        if (DEBUG) {
                            System.out.println("Origin: " + origin);
                        }
                        break;

                    case PICT.OP_TX_SIZE:// OK
                        // Get the text size
                        int tx_size = getYPtCoord(pStream.readUnsignedShort());
                        //if (mGraphics != null) {
                        //    currentFont = mGraphics.getFont();
                        //    mGraphics.setFont(new Font(currentFont.getName(), currentFont.getStyle(), tx_size));
                        //}
                        context.setTextSize(tx_size);
                        if (DEBUG) {
                            System.out.println("txSize: " + tx_size);
                        }
                        break;

                    case PICT.OP_FG_COLOR:// TO BE DONE IF POSSIBLE
                        // TODO!
                        // Get the data
                        pStream.readInt();
                        if (DEBUG) {
                            System.out.println("fgColor");
                        }
                        break;

                    case PICT.OP_BK_COLOR:// TO BE DONE IF POSSIBLE
                        // TODO!
                        // Get the data
                        pStream.readInt();
                        if (DEBUG) {
                            System.out.println("bgColor");
                        }
                        break;

                    case PICT.OP_TX_RATIO:// SEE IF WE HAVE THIS???
                        // Get the data
                        pStream.readFully(new byte[8], 0, 8);
                        if (DEBUG) {
                            System.out.println("txRatio");
                        }
                        break;

                    case PICT.OP_VERSION:// OK, ignored since we should already have it
                        // Get the data
                        pStream.readFully(new byte[1], 0, 1);
                        if (DEBUG) {
                            System.out.println("opVersion");
                        }
                        break;

                    case 0x0012: // BkPixPat
                        bg = PICTUtil.readColorPattern(pStream);
                        context.setBackgroundPattern(bg);
                        break;
                    case 0x0013: // PnPixPat
                        pen = PICTUtil.readColorPattern(pStream);
                        context.setBackgroundPattern(pen);
                        break;
                    case 0x0014: // FillPixPat
                        fill = PICTUtil.readColorPattern(pStream);
                        context.setBackgroundPattern(fill);
                        break;

                    case PICT.OP_PN_LOC_H_FRAC:// TO BE DONE???
                        // Get the data
                        pStream.readFully(new byte[2], 0, 2);
                        if (DEBUG) {
                            System.out.println("opPnLocHFrac");
                        }
                        break;

                    case PICT.OP_CH_EXTRA:// TO BE DONE???
                        // Get the data
                        pStream.readFully(new byte[2], 0, 2);
                        if (DEBUG) {
                            System.out.println("opChExtra");
                        }
                        break;

                    case PICT.OP_RGB_FG_COL:// OK
                        // Get the color
                        pStream.readFully(colorBuffer, 0, colorBuffer.length);
                        foreground = new Color((colorBuffer[0] & 0xFF), (colorBuffer[2] & 0xFF), (colorBuffer[4] & 0xFF));
                        //if (mGraphics != null) {
                        //    mGraphics.setColor(foreground);
                        //}
                        if (DEBUG) {
                            System.out.println("rgbFgColor: " + foreground);
                        }
                        break;

                    case PICT.OP_RGB_BK_COL:// OK
                        // Get the color
                        pStream.readFully(colorBuffer, 0, colorBuffer.length);
                        // TODO: The color might be 16 bit per component..
                        background = new Color((colorBuffer[0] & 0xFF), (colorBuffer[2] & 0xFF), (colorBuffer[4] & 0xFF));
                        if (DEBUG) {
                            System.out.println("rgbBgColor: " + background);
                        }
                        break;

                    case PICT.OP_HILITE_MODE:
                        // Change color to hilite color
                        context.setPenPattern(new BitMapPattern(hilight));
                        if (DEBUG) {
                            System.out.println("opHiliteMode");
                        }
                        break;

                    case PICT.OP_HILITE_COLOR:// OK
                        // Get the color
                        pStream.readFully(colorBuffer, 0, colorBuffer.length);
                        // TODO: The color might be 16 bit per component..
                        hilight = new Color((colorBuffer[0] & 0xFF), (colorBuffer[2] & 0xFF), (colorBuffer[4] & 0xFF));
                        if (DEBUG) {
                            System.out.println("opHiliteColor: " + hilight);
                        }
                        break;

                    case PICT.OP_DEF_HILITE:// Macintosh internal, ignored?
                        // Nothing to do
                        hilight = Color.red; // TODO: My guess it's a reset, verify!
                        if (DEBUG) {
                            System.out.println("opDefHilite");
                        }
                        break;

                    case PICT.OP_OP_COLOR:// To be done once I know what it means
                        // TODO: Is this the mask? Scale value for RGB colors?
                        // Get the color
                        pStream.readFully(colorBuffer, 0, colorBuffer.length);
                        if (DEBUG) {
                            System.out.println("opOpColor");
                        }
                        break;

                    case PICT.OP_LINE:// OK, not tested
                        // Get the data (two points)
                        y = getYPtCoord(pStream.readUnsignedShort());
                        x = getXPtCoord(pStream.readUnsignedShort());
                        origin = new Point(x, y);

                        y = getYPtCoord(pStream.readUnsignedShort());
                        x = getXPtCoord(pStream.readUnsignedShort());
                        penPosition.setLocation(x, y);

                        // Move pen to new position, draw line
                        context.moveTo(origin);
                        context.lineTo(penPosition);

                        if (DEBUG) {
                            System.out.println("line from: " + origin + " to: " + penPosition);
                        }
                        break;

                    case PICT.OP_LINE_FROM:// OK, not tested
                        // Get the point
                        y = getYPtCoord(pStream.readUnsignedShort());
                        x = getXPtCoord(pStream.readUnsignedShort());

                        // Draw line
                        context.line(x, y);

                        if (DEBUG) {
                            System.out.println("lineFrom to: " + penPosition);
                        }
                        break;

                    case PICT.OP_SHORT_LINE:// OK
                        // Get origin and dh, dv
                        y = getYPtCoord(pStream.readUnsignedShort());
                        x = getXPtCoord(pStream.readUnsignedShort());
                        origin = new Point(x, y);

                        y = getYPtCoord(pStream.readByte());
                        x = getXPtCoord(pStream.readByte());
                        dh_dv = new Point(x, y);

                        // Move pen to new position, draw line if we have a graphics
                        penPosition.setLocation(origin.x + dh_dv.x, origin.y + dh_dv.y);
                        context.lineTo(penPosition);

                        if (DEBUG) {
                            System.out.println("Short line origin: " + origin + ", dh,dv: " + dh_dv);
                        }
                        break;

                    case PICT.OP_SHORT_LINE_FROM:// OK
                        // Get dh, dv
                        y = getYPtCoord(pStream.readByte());
                        x = getXPtCoord(pStream.readByte());

                        // Draw line
                        context.line(x, y);

                        if (DEBUG) {
                            System.out.println("Short line from dh,dv: " + x + "," + y);
                        }
                        break;

                    case 0x24:
                    case 0x25:
                    case 0x26:
                    case 0x27:
                        // Apple reserved
                        dataLength = pStream.readUnsignedShort();

                        pStream.readFully(new byte[dataLength], 0, dataLength);
                        if (DEBUG) {
                            System.out.println(String.format("%s: 0x%04x", PICT.APPLE_USE_RESERVED_FIELD, opCode));
                        }
                        break;

                    case PICT.OP_LONG_TEXT:// OK
                        // Get the data
                        y = getYPtCoord(pStream.readUnsignedShort());
                        x = getXPtCoord(pStream.readUnsignedShort());
                        origin = new Point(x, y);
                        penPosition = origin;
                        context.moveTo(penPosition);
                        text = PICTUtil.readPascalString(pStream);
                        // TODO
                        //if (mGraphics != null) {
                        //    mGraphics.drawString(text, penPosition.x, penPosition.y);
                        //}
                        context.drawString(text);
                        if (DEBUG) {
                            System.out.println("longText origin: " + penPosition + ", text:" + text);
                        }
                        break;

                    case PICT.OP_DH_TEXT:// OK, not tested
                        // Get dh
                        dh = getXPtCoord(pStream.readByte());
                        penPosition.translate(dh, 0);
                        context.moveTo(penPosition);
                        text = PICTUtil.readPascalString(pStream);
                        // TODO
//                        if (mGraphics != null) {
//                            mGraphics.drawString(text, penPosition.x, penPosition.y);
//                        }
                        context.drawString(text);
                        if (DEBUG) {
                            System.out.println("DHText dh: " + dh + ", text:" + text);
                        }
                        break;

                    case PICT.OP_DV_TEXT:// OK, not tested
                        // Get dh
                        dv = getYPtCoord(pStream.readByte());
                        penPosition.translate(0, dv);
                        context.moveTo(penPosition);
                        text = PICTUtil.readPascalString(pStream);
                        // TODO
                        //if (mGraphics != null) {
                        //    mGraphics.drawString(text, penPosition.x, penPosition.y);
                        //}
                        context.drawString(text);
                        if (DEBUG) {
                            System.out.println("DVText dv: " + dv + ", text:" + text);
                        }
                        break;

                    case PICT.OP_DHDV_TEXT:// OK, not tested
                        // Get dh, dv
                        y = getYPtCoord(pStream.readByte());
                        x = getXPtCoord(pStream.readByte());
                        penPosition.translate(x, y);
                        context.moveTo(penPosition);
                        text = PICTUtil.readPascalString(pStream);
                        // TODO
                        //if (mGraphics != null) {
                        //    mGraphics.drawString(text, penPosition.x, penPosition.y);
                        //}
                        context.drawString(text);
                        if (DEBUG) {
                            System.out.println("DHDVText penPosition: " + penPosition + ", text:" + text);
                        }
                        break;

                    case PICT.OP_FONT_NAME:// OK, not tested
                        // Get data length
                        /*data_len = */
                        pStream.readShort();

                        // Get old font ID, ignored
//                        pStream.readInt();
                        pStream.readUnsignedShort();

                        // Get font name and set the new font if we have one
                        text = PICTUtil.readPascalString(pStream);
                        // TODO
                        //if (mGraphics != null) {
                        //    mGraphics.setFont(Font.decode(text)
                        //            .deriveFont(currentFont.getStyle(), currentFont.getSize()));
                        //}
                        context.drawString(text);
                        if (DEBUG) {
                            System.out.println("fontName: \"" + text +"\"");
                        }
                        break;

                    case PICT.OP_LINE_JUSTIFY:// TO BE DONE???
                        // Get data
                        pStream.readFully(new byte[10], 0, 10);
                        if (DEBUG) {
                            System.out.println("opLineJustify");
                        }
                        break;

                    case PICT.OP_GLYPH_STATE:// TODO: NOT SUPPORTED IN AWT GRAPHICS YET?
                        // Get data
                        pStream.readFully(new byte[6], 0, 6);
                        if (DEBUG) {
                            System.out.println("glyphState");
                        }
                        break;

                    case 0x2F:
                        dataLength = pStream.readUnsignedShort();
                        pStream.readFully(new byte[dataLength], 0, dataLength);
                        if (DEBUG) {
                            System.out.println(String.format("%s: 0x%04x", PICT.APPLE_USE_RESERVED_FIELD, opCode));
                        }
                        break;

                        //--------------------------------------------------------------------------------
                        // Rect treatments
                        //--------------------------------------------------------------------------------
                    case PICT.OP_FRAME_RECT:// OK
                    case PICT.OP_PAINT_RECT:// OK
                    case PICT.OP_ERASE_RECT:// OK, not tested
                    case PICT.OP_INVERT_RECT:// OK, not tested
                    case PICT.OP_FILL_RECT:// OK, not tested
                        // Get the frame rectangle
                        readRectangle(pStream, lastRectangle);

                    case PICT.OP_FRAME_SAME_RECT:// OK, not tested
                    case PICT.OP_PAINT_SAME_RECT:// OK, not tested
                    case PICT.OP_ERASE_SAME_RECT:// OK, not tested
                    case PICT.OP_INVERT_SAME_RECT:// OK, not tested
                    case PICT.OP_FILL_SAME_RECT:// OK, not tested
                        // Draw
                        switch (opCode) {
                            case PICT.OP_FRAME_RECT:
                            case PICT.OP_FRAME_SAME_RECT:
                                context.frameRect(lastRectangle);
                                break;
                            case PICT.OP_PAINT_RECT:
                            case PICT.OP_PAINT_SAME_RECT:
                                context.paintRect(lastRectangle);
                                break;
                            case PICT.OP_ERASE_RECT:
                            case PICT.OP_ERASE_SAME_RECT:
                                context.eraseRect(lastRectangle);
                                break;
                            case PICT.OP_INVERT_RECT:
                            case PICT.OP_INVERT_SAME_RECT:
                                context.invertRect(lastRectangle);
                                break;
                            case PICT.OP_FILL_RECT:
                            case PICT.OP_FILL_SAME_RECT:
                                context.fillRect(lastRectangle, fill);
                                break;
                        }

                        // Do verbose mode output
                        if (DEBUG) {
                            switch (opCode) {
                                case PICT.OP_FRAME_RECT:
                                    System.out.println("frameRect: " + lastRectangle);
                                    break;
                                case PICT.OP_PAINT_RECT:
                                    System.out.println("paintRect: " + lastRectangle);
                                    break;
                                case PICT.OP_ERASE_RECT:
                                    System.out.println("eraseRect: " + lastRectangle);
                                    break;
                                case PICT.OP_INVERT_RECT:
                                    System.out.println("invertRect: " + lastRectangle);
                                    break;
                                case PICT.OP_FILL_RECT:
                                    System.out.println("fillRect: " + lastRectangle);
                                    break;
                                case PICT.OP_FRAME_SAME_RECT:
                                    System.out.println("frameSameRect: " + lastRectangle);
                                    break;
                                case PICT.OP_PAINT_SAME_RECT:
                                    System.out.println("paintSameRect: " + lastRectangle);
                                    break;
                                case PICT.OP_ERASE_SAME_RECT:
                                    System.out.println("eraseSameRect: " + lastRectangle);
                                    break;
                                case PICT.OP_INVERT_SAME_RECT:
                                    System.out.println("invertSameRect: " + lastRectangle);
                                    break;
                                case PICT.OP_FILL_SAME_RECT:
                                    System.out.println("fillSameRect: " + lastRectangle);
                                    break;
                            }
                        }

                        // Rect treatments finished
                        break;

                    case 0x003d:
                    case 0x003e:
                    case 0x003f:
                        if (DEBUG) {
                            System.out.println(String.format("%s: 0x%04x", PICT.APPLE_USE_RESERVED_FIELD, opCode));
                        }
                        break;

                        //--------------------------------------------------------------------------------
                        // Round Rect treatments
                        //--------------------------------------------------------------------------------
                    case PICT.OP_FRAME_R_RECT:// OK
                    case PICT.OP_PAINT_R_RECT:// OK, not tested
                    case PICT.OP_ERASE_R_RECT:// OK, not tested
                    case PICT.OP_INVERT_R_RECT:// OK, not tested
                    case PICT.OP_FILL_R_RECT:// OK, not tested
                        // Get the frame rectangle
                        readRectangle(pStream, lastRectangle);

                    case PICT.OP_FRAME_SAME_R_RECT:// OK, not tested
                    case PICT.OP_PAINT_SAME_R_RECT:// OK, not tested
                    case PICT.OP_ERASE_SAME_R_RECT:// OK, not tested
                    case PICT.OP_INVERT_SAME_R_RECT:// OK, not tested
                    case PICT.OP_FILL_SAME_R_RECT:// OK, not tested
                        // Draw
                        switch (opCode) {
                            case PICT.OP_FRAME_R_RECT:
                            case PICT.OP_FRAME_SAME_R_RECT:
                                context.frameRoundRect(lastRectangle, ovSize.x, ovSize.y);
                                break;
                            case PICT.OP_PAINT_R_RECT:
                            case PICT.OP_PAINT_SAME_R_RECT:
                                context.paintRoundRect(lastRectangle, ovSize.x, ovSize.y);
                                break;
                            case PICT.OP_ERASE_R_RECT:
                            case PICT.OP_ERASE_SAME_R_RECT:
                                context.eraseRoundRect(lastRectangle, ovSize.x, ovSize.y);
                                break;
                            case PICT.OP_INVERT_R_RECT:
                            case PICT.OP_INVERT_SAME_R_RECT:
                                context.invertRoundRect(lastRectangle, ovSize.x, ovSize.y);
                                break;
                            case PICT.OP_FILL_R_RECT:
                            case PICT.OP_FILL_SAME_R_RECT:
                                context.fillRoundRect(lastRectangle, ovSize.x, ovSize.y, fill);
                                break;
                        }

                        // Do verbose mode output
                        if (DEBUG) {
                            switch (opCode) {
                                case PICT.OP_FRAME_R_RECT:
                                    System.out.println("frameRRect: " + lastRectangle);
                                    break;
                                case PICT.OP_PAINT_R_RECT:
                                    System.out.println("paintRRect: " + lastRectangle);
                                    break;
                                case PICT.OP_ERASE_R_RECT:
                                    System.out.println("eraseRRect: " + lastRectangle);
                                    break;
                                case PICT.OP_INVERT_R_RECT:
                                    System.out.println("invertRRect: " + lastRectangle);
                                    break;
                                case PICT.OP_FILL_R_RECT:
                                    System.out.println("fillRRect: " + lastRectangle);
                                    break;
                                case PICT.OP_FRAME_SAME_R_RECT:
                                    System.out.println("frameSameRRect: " + lastRectangle);
                                    break;
                                case PICT.OP_PAINT_SAME_R_RECT:
                                    System.out.println("paintSameRRect: " + lastRectangle);
                                    break;
                                case PICT.OP_ERASE_SAME_R_RECT:
                                    System.out.println("eraseSameRRect: " + lastRectangle);
                                    break;
                                case PICT.OP_INVERT_SAME_R_RECT:
                                    System.out.println("invertSameRRect: " + lastRectangle);
                                    break;
                                case PICT.OP_FILL_SAME_R_RECT:
                                    System.out.println("fillSameRRect: " + lastRectangle);
                                    break;
                            }
                        }

                        // RoundRect treatments finished
                        break;

                        //--------------------------------------------------------------------------------
                        // Oval treatments
                        //--------------------------------------------------------------------------------
                    case PICT.OP_FRAME_OVAL:// OK
                    case PICT.OP_PAINT_OVAL:// OK, not tested
                    case PICT.OP_ERASE_OVAL:// OK, not tested
                    case PICT.OP_INVERT_OVAL:// OK, not tested
                    case PICT.OP_FILL_OVAL:// OK, not tested
                        // Get the frame rectangle
                        readRectangle(pStream, lastRectangle);
                    case PICT.OP_FRAME_SAME_OVAL:// OK, not tested
                    case PICT.OP_PAINT_SAME_OVAL:// OK, not tested
                    case PICT.OP_ERASE_SAME_OVAL:// OK, not tested
                    case PICT.OP_INVERT_SAME_OVAL:// OK, not tested
                    case PICT.OP_FILL_SAME_OVAL:// OK, not tested
                        // Draw
                        switch (opCode) {
                            case PICT.OP_FRAME_OVAL:
                            case PICT.OP_FRAME_SAME_OVAL:
                                context.frameOval(lastRectangle);
                                break;
                            case PICT.OP_PAINT_OVAL:
                            case PICT.OP_PAINT_SAME_OVAL:
                                context.paintOval(lastRectangle);
                                break;
                            case PICT.OP_ERASE_OVAL:
                            case PICT.OP_ERASE_SAME_OVAL:
                                context.eraseOval(lastRectangle);
                                break;
                            case PICT.OP_INVERT_OVAL:
                            case PICT.OP_INVERT_SAME_OVAL:
                                context.invertOval(lastRectangle);
                                break;
                            case PICT.OP_FILL_OVAL:
                            case PICT.OP_FILL_SAME_OVAL:
                                context.fillOval(lastRectangle, fill);
                                break;
                        }

                        // Do verbose mode output
                        if (DEBUG) {
                            switch (opCode) {
                                case PICT.OP_FRAME_OVAL:
                                    System.out.println("frameOval: " + lastRectangle);
                                    break;
                                case PICT.OP_PAINT_OVAL:
                                    System.out.println("paintOval: " + lastRectangle);
                                    break;
                                case PICT.OP_ERASE_OVAL:
                                    System.out.println("eraseOval: " + lastRectangle);
                                    break;
                                case PICT.OP_INVERT_OVAL:
                                    System.out.println("invertOval: " + lastRectangle);
                                    break;
                                case PICT.OP_FILL_OVAL:
                                    System.out.println("fillOval: " + lastRectangle);
                                    break;
                                case PICT.OP_FRAME_SAME_OVAL:
                                    System.out.println("frameSameOval: " + lastRectangle);
                                    break;
                                case PICT.OP_PAINT_SAME_OVAL:
                                    System.out.println("paintSameOval: " + lastRectangle);
                                    break;
                                case PICT.OP_ERASE_SAME_OVAL:
                                    System.out.println("eraseSameOval: " + lastRectangle);
                                    break;
                                case PICT.OP_INVERT_SAME_OVAL:
                                    System.out.println("invertSameOval: " + lastRectangle);
                                    break;
                                case PICT.OP_FILL_SAME_OVAL:
                                    System.out.println("fillSameOval: " + lastRectangle);
                                    break;
                            }
                        }

                        // Oval treatments finished
                        break;

                    case 0x35:
                    case 0x36:
                    case 0x37:
                    case 0x45:
                    case 0x46:
                    case 0x47:
                    case 0x55:
                    case 0x56:
                    case 0x57:
                        pStream.readFully(new byte[8], 0, 8);
                        if (DEBUG) {
                            System.out.println(String.format("%s: 0x%04x", PICT.APPLE_USE_RESERVED_FIELD, opCode));
                        }
                        break;

                        //--------------------------------------------------------------------------------
                        // Arc treatments
                        //--------------------------------------------------------------------------------
                    case PICT.OP_FRAME_ARC:// OK, not tested
                    case PICT.OP_PAINT_ARC:// OK, not tested
                    case PICT.OP_ERASE_ARC:// OK, not tested
                    case PICT.OP_INVERT_ARC:// OK, not tested
                    case PICT.OP_FILL_ARC:// OK, not tested
                        // Get the frame rectangle
                        readRectangle(pStream, lastRectangle);
                    case PICT.OP_FRAME_SAME_ARC:// OK, not tested
                    case PICT.OP_PAINT_SAME_ARC:// OK, not tested
                    case PICT.OP_ERASE_SAME_ARC:// OK, not tested
                    case PICT.OP_INVERT_SAME_ARC:// OK, not tested
                    case PICT.OP_FILL_SAME_ARC:// OK, not tested
                        // NOTE: These are inlcuded even if SAME
                        // Get start and end angles
                        //x = getXPtCoord(pStream.readUnsignedShort());
                        //y = getYPtCoord(pStream.readUnsignedShort());
                        x = pStream.readUnsignedShort();
                        y = pStream.readUnsignedShort();
                        arcAngles.setLocation(x, y);

                        // Draw
                        switch (opCode) {
                            case PICT.OP_FRAME_ARC:
                            case PICT.OP_FRAME_SAME_ARC:
                                context.frameArc(lastRectangle, arcAngles.x, arcAngles.y);
                                break;
                            case PICT.OP_PAINT_ARC:
                            case PICT.OP_PAINT_SAME_ARC:
                                context.paintArc(lastRectangle, arcAngles.x, arcAngles.y);
                                break;
                            case PICT.OP_ERASE_ARC:
                            case PICT.OP_ERASE_SAME_ARC:
                                context.eraseArc(lastRectangle, arcAngles.x, arcAngles.y);
                                break;
                            case PICT.OP_INVERT_ARC:
                            case PICT.OP_INVERT_SAME_ARC:
                                context.invertArc(lastRectangle, arcAngles.x, arcAngles.y);
                                break;
                            case PICT.OP_FILL_ARC:
                            case PICT.OP_FILL_SAME_ARC:
                                context.fillArc(lastRectangle, arcAngles.x, arcAngles.y, fill);
                                break;
                        }

                        // Do verbose mode output
                        if (DEBUG) {
                            switch (opCode) {
                                case PICT.OP_FRAME_ARC:
                                    System.out.println("frameArc: " + lastRectangle + ", angles:" + arcAngles);
                                    break;
                                case PICT.OP_PAINT_ARC:
                                    System.out.println("paintArc: " + lastRectangle + ", angles:" + arcAngles);
                                    break;
                                case PICT.OP_ERASE_ARC:
                                    System.out.println("eraseArc: " + lastRectangle + ", angles:" + arcAngles);
                                    break;
                                case PICT.OP_INVERT_ARC:
                                    System.out.println("invertArc: " + lastRectangle + ", angles:" + arcAngles);
                                    break;
                                case PICT.OP_FILL_ARC:
                                    System.out.println("fillArc: " + lastRectangle + ", angles:" + arcAngles);
                                    break;
                                case PICT.OP_FRAME_SAME_ARC:
                                    System.out.println("frameSameArc: " + lastRectangle + ", angles:" + arcAngles);
                                    break;
                                case PICT.OP_PAINT_SAME_ARC:
                                    System.out.println("paintSameArc: " + lastRectangle + ", angles:" + arcAngles);
                                    break;
                                case PICT.OP_ERASE_SAME_ARC:
                                    System.out.println("eraseSameArc: " + lastRectangle + ", angles:" + arcAngles);
                                    break;
                                case PICT.OP_INVERT_SAME_ARC:
                                    System.out.println("invertSameArc: " + lastRectangle + ", angles:" + arcAngles);
                                    break;
                                case PICT.OP_FILL_SAME_ARC:
                                    System.out.println("fillSameArc: " + lastRectangle + ", angles:" + arcAngles);
                                    break;
                            }
                        }

                        // Arc treatments finished
                        break;

                    case 0x65:
                    case 0x66:
                    case 0x67:
                        pStream.readFully(new byte[12], 0, 12);
                        if (DEBUG) {
                            System.out.println(String.format("%s: 0x%04x", PICT.APPLE_USE_RESERVED_FIELD, opCode));
                        }
                        break;
                    case 0x6d:
                    case 0x6e:
                    case 0x6f:
                        pStream.readFully(new byte[4], 0, 4);
                        if (DEBUG) {
                            System.out.println(String.format("%s: 0x%04x", PICT.APPLE_USE_RESERVED_FIELD, opCode));
                        }
                        break;

                        //--------------------------------------------------------------------------------
                        // Polygon treatments
                        //--------------------------------------------------------------------------------
                    case PICT.OP_FRAME_POLY:// OK
                    case PICT.OP_PAINT_POLY:// OK
                    case PICT.OP_ERASE_POLY:// OK, not tested
                    case PICT.OP_INVERT_POLY:// OK, not tested
                    case PICT.OP_FILL_POLY:// OK, not tested
                        // Read the polygon
                        polygon = readPoly(pStream, bounds);

                    case PICT.OP_FRAME_SAME_POLY:// OK, not tested
                    case PICT.OP_PAINT_SAME_POLY:// OK, not tested
                    case PICT.OP_ERASE_SAME_POLY:// OK, not tested
                    case PICT.OP_INVERT_SAME_POLY:// OK, not tested
                    case PICT.OP_FILL_SAME_POLY:// OK, not tested

                        // Draw
                        switch (opCode) {
                            case PICT.OP_FRAME_POLY:
                            case PICT.OP_FRAME_SAME_POLY:
                                context.framePoly(polygon);
                                break;
                            case PICT.OP_PAINT_POLY:
                            case PICT.OP_PAINT_SAME_POLY:
                                context.paintPoly(polygon);
                                break;
                            case PICT.OP_ERASE_POLY:
                            case PICT.OP_ERASE_SAME_POLY:
                                context.erasePoly(polygon);
                                break;
                            case PICT.OP_INVERT_POLY:
                            case PICT.OP_INVERT_SAME_POLY:
                                context.invertPoly(polygon);
                                break;
                            case PICT.OP_FILL_POLY:
                            case PICT.OP_FILL_SAME_POLY:
                                context.fillPoly(polygon, fill);
                                break;
                        }

                        // Do verbose mode output
                        if (DEBUG) {
                            switch (opCode) {
                                case PICT.OP_FRAME_POLY:
                                    verbosePolyCmd("framePoly", bounds, polygon);
                                    break;
                                case PICT.OP_PAINT_POLY:
                                    verbosePolyCmd("paintPoly", bounds, polygon);
                                    break;
                                case PICT.OP_ERASE_POLY:
                                    verbosePolyCmd("erasePoly", bounds, polygon);
                                    break;
                                case PICT.OP_INVERT_POLY:
                                    verbosePolyCmd("invertPoly", bounds, polygon);
                                    break;
                                case PICT.OP_FILL_POLY:
                                    verbosePolyCmd("fillPoly", bounds, polygon);
                                    break;
                                case PICT.OP_FRAME_SAME_POLY:
                                    verbosePolyCmd("frameSamePoly", bounds, polygon);
                                    break;
                                case PICT.OP_PAINT_SAME_POLY:
                                    verbosePolyCmd("paintSamePoly", bounds, polygon);
                                    break;
                                case PICT.OP_ERASE_SAME_POLY:
                                    verbosePolyCmd("eraseSamePoly", bounds, polygon);
                                    break;
                                case PICT.OP_INVERT_SAME_POLY:
                                    verbosePolyCmd("invertSamePoly", bounds, polygon);
                                    break;
                                case PICT.OP_FILL_SAME_POLY:
                                    verbosePolyCmd("fillSamePoly", bounds, polygon);
                                    break;
                            }
                        }

                        // Polygon treatments finished
                        break;

                    case 0x75:
                    case 0x76:
                    case 0x77:
                        // Read the polygon
                        polygon = readPoly(pStream, bounds);
                        if (DEBUG) {
                            System.out.println(String.format("%s: 0x%04x", PICT.APPLE_USE_RESERVED_FIELD, opCode));
                        }
                        break;

                        //--------------------------------------------------------------------------------
                        // Region treatments
                        //--------------------------------------------------------------------------------
                    case PICT.OP_FRAME_RGN:// OK, not tested
                    case PICT.OP_PAINT_RGN:// OK, not tested
                    case PICT.OP_ERASE_RGN:// OK, not tested
                    case PICT.OP_INVERT_RGN:// OK, not tested
                    case PICT.OP_FILL_RGN:// OK, not tested
                        // Read the region
                        region = readRegion(pStream, bounds);

                    case PICT.OP_FRAME_SAME_RGN:// OK, not tested
                    case PICT.OP_PAINT_SAME_RGN:// OK, not tested
                    case PICT.OP_ERASE_SAME_RGN:// OK, not tested
                    case PICT.OP_INVERT_SAME_RGN:// OK, not tested
                    case PICT.OP_FILL_SAME_RGN:// OK, not tested
                        // Draw
                        if (region != null && region.npoints > 1) {
                            switch (opCode) {
                                case PICT.OP_FRAME_RGN:
                                case PICT.OP_FRAME_SAME_RGN:
                                    context.frameRegion(new Area(region));
                                    break;
                                case PICT.OP_PAINT_RGN:
                                case PICT.OP_PAINT_SAME_RGN:
                                    context.paintRegion(new Area(region));
                                    break;
                                case PICT.OP_ERASE_RGN:
                                case PICT.OP_ERASE_SAME_RGN:
                                    context.eraseRegion(new Area(region));
                                    break;
                                case PICT.OP_INVERT_RGN:
                                case PICT.OP_INVERT_SAME_RGN:
                                    context.invertRegion(new Area(region));
                                    break;
                                case PICT.OP_FILL_RGN:
                                case PICT.OP_FILL_SAME_RGN:
                                    context.fillRegion(new Area(region), fill);
                                    break;
                            }
                        }

                        // Do verbose mode output
                        if (DEBUG) {
                            switch (opCode) {
                                case PICT.OP_FRAME_RGN:
                                    verboseRegionCmd("frameRgn", bounds, region);
                                    break;
                                case PICT.OP_PAINT_RGN:
                                    verboseRegionCmd("paintRgn", bounds, region);
                                    break;
                                case PICT.OP_ERASE_RGN:
                                    verboseRegionCmd("eraseRgn", bounds, region);
                                    break;
                                case PICT.OP_INVERT_RGN:
                                    verboseRegionCmd("invertRgn", bounds, region);
                                    break;
                                case PICT.OP_FILL_RGN:
                                    verboseRegionCmd("fillRgn", bounds, region);
                                    break;
                                case PICT.OP_FRAME_SAME_RGN:
                                    verboseRegionCmd("frameSameRgn", bounds, region);
                                    break;
                                case PICT.OP_PAINT_SAME_RGN:
                                    verboseRegionCmd("paintSameRgn", bounds, region);
                                    break;
                                case PICT.OP_ERASE_SAME_RGN:
                                    verboseRegionCmd("eraseSameRgn", bounds, region);
                                    break;
                                case PICT.OP_INVERT_SAME_RGN:
                                    verboseRegionCmd("invertSameRgn", bounds, region);
                                    break;
                                case PICT.OP_FILL_SAME_RGN:
                                    verboseRegionCmd("fillSameRgn", bounds, region);
                                    break;
                            }
                        }

                        // Region treatments finished
                        break;

                    case 0x85:
                    case 0x86:
                    case 0x87:
                        // Read the region
                        region = readRegion(pStream, bounds);
                        if (DEBUG) {
                            System.out.println(String.format("%s: 0x%04x", PICT.APPLE_USE_RESERVED_FIELD, opCode));
                        }
                        break;

                    case PICT.OP_BITS_RECT:
                        // [4] Four opcodes ($0090, $0091, $0098, $0099) are modifications of version 1 opcodes.
                        // The first word following the opcode is rowBytes. If the high bit of rowBytes is set,
                        // then it is a pixel map containing multiple bits per pixel; if it is not set, it is a
                        // bitmap containing 1 bit per pixel.
                        // In general, the difference between version 2 and version 1 formats is that the pixel
                        // map replaces the bitmap, a color table has been added, and pixData replaces bitData.
                        // [5] For opcodes $0090 (BitsRect) and $0091 (BitsRgn), the data is unpacked. These
                        // opcodes can be used only when rowBytes is less than 8.
                        /*
                           PixMap:     PixMap;     {pixel map}
                           ColorTable: ColorTable; {ColorTable record}
                           srcRect:    Rect;       {source rectangle}
                           dstRect:    Rect;       {destination rectangle}
                           mode:       Word;       {transfer mode (may include }
                                                   { new transfer modes)}
                           PixData:    PixData;
                         */

                        int rowBytesRaw = pStream.readUnsignedShort();
                        int rowBytes = rowBytesRaw & 0x3FFF;

                        // TODO: Use rowBytes to determine size of PixMap/ColorTable?
                        if ((rowBytesRaw & 0x8000) > 0) {
                            // Do stuff...
                        }

                        // Get bounds rectangle. THIS IS NOT TO BE SCALED BY THE RESOLUTION! TODO: ?!
                        bounds = new Rectangle();
                        y = pStream.readUnsignedShort();
                        x = pStream.readUnsignedShort();
                        bounds.setLocation(x, y);

                        y = pStream.readUnsignedShort();
                        x = pStream.readUnsignedShort();
                        bounds.setSize(x - bounds.x,
                                        y - bounds.y);

                        Rectangle srcRect = new Rectangle();
                        readRectangle(pStream, srcRect);

                        Rectangle dstRect = new Rectangle();
                        readRectangle(pStream, dstRect);

                        mode = pStream.readUnsignedShort();
                        context.setPenMode(mode); // TODO: Or parameter?

                        if (DEBUG) {
                            System.out.print("bitsRect, rowBytes: " + rowBytes);
                            if ((rowBytesRaw & 0x8000) > 0) {
                                System.out.print(", it is a PixMap");
                            }
                            else {
                                System.out.print(", it is a BitMap");
                            }
                            System.out.print(", bounds: " + bounds);
                            System.out.print(", srcRect: " + srcRect);
                            System.out.print(", dstRect: " + dstRect);
                            System.out.print(", mode: " + mode);
                            System.out.println();
                        }

                        BufferedImage image = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_BYTE_BINARY);
                        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

                        // Read pixel data
                        int width = bounds.width / 8;
                        for (int i = 0; i < bounds.height; i++) {
                            pStream.readFully(data, i * width, width);
                            pStream.skipBytes(rowBytes - width);
                        }

                        // Draw pixel data
                        Rectangle rect = new Rectangle(srcRect);
                        rect.translate(-bounds.x, -bounds.y);
                        context.copyBits(image, rect, dstRect, mode, null);
                        //mGraphics.drawImage(image,
                        //                    dstRect.x,  dstRect.y,
                        //                    dstRect.x + dstRect.width, dstRect.y + dstRect.height,
                        //                    srcRect.x - bounds.x, srcRect.y - bounds.y,
                        //                    srcRect.x - bounds.x + srcRect.width, srcRect.y - bounds.y + srcRect.height,
                        //                    null);
                        //
                        break;

                    case PICT.OP_BITS_RGN:
                        // TODO: As OP_BITS_RECT but with clip
                        /*
                           pixMap:     PixMap;
                           colorTable: ColorTable;
                           srcRect:    Rect;          {source rectangle}
                           dstRect:    Rect;          {destination rectangle}
                           mode:       Word;          {transfer mode (may }
                                                      { include new modes)}
                           maskRgn:    Rgn;           {region for masking}
                           pixData:    PixData;
                         */
                        if (DEBUG) {
                            System.out.println("bitsRgn");
                        }
                        break;

                    case 0x92:
                    case 0x93:
                    case 0x94:
                    case 0x95:
                    case 0x96:
                    case 0x97:
                        dataLength = pStream.readUnsignedShort();
                        pStream.readFully(new byte[dataLength], 0, dataLength);
                        if (DEBUG) {
                            System.out.println(String.format("%s: 0x%04x", PICT.APPLE_USE_RESERVED_FIELD, opCode));
                        }
                        break;

                    case PICT.OP_PACK_BITS_RECT:
                        readOpPackBitsRect(pStream, bounds, pixmapCount++);
                        if (DEBUG) {
                            System.out.println("packBitsRect - TODO");
                        }
                        break;

                    case PICT.OP_PACK_BITS_RGN:
                        // TODO: As OP_PACK_BITS_RECT but with clip
                        // TODO: Read/Skip data
                        if (DEBUG) {
                            System.out.println("packBitsRgn - TODO");
                        }
                        break;

                    case PICT.OP_DIRECT_BITS_RECT:
                        readOpDirectBitsRect(pStream, bounds, pixmapCount++);
                        break;

                    case PICT.OP_DIRECT_BITS_RGN:
                        // TODO: As OP_DIRECT_BITS_RECT but with clip
                        // TODO: Read/Skip data
                        if (DEBUG) {
                            System.out.println("directBitsRgn - TODO");
                        }
                        break;

                    case 0x9C:
                    case 0x9D:
                    case 0x9E:
                    case 0x9F:
                        // TODO: Move to special Apple Reserved handling?
                        dataLength = pStream.readUnsignedShort();
                        pStream.readFully(new byte[dataLength], 0, dataLength);
                        if (DEBUG) {
                            System.out.println(String.format("%s: 0x%04x", PICT.APPLE_USE_RESERVED_FIELD, opCode));
                        }
                        break;

                    case PICT.OP_SHORT_COMMENT:// NOTHING TO DO, JUST JUMP OVER
                        pStream.readFully(new byte[2], 0, 2);
                        if (DEBUG) {
                            System.out.println("Short comment");
                        }
                        break;

                    case PICT.OP_LONG_COMMENT:// NOTHING TO DO, JUST JUMP OVER
                        readLongComment(pStream);
                        if (DEBUG) {
                            System.out.println("Long comment");
                        }
                        break;

                    case PICT.OP_END_OF_PICTURE:// OK
                        break;

                        // WE DON'T CARE ABOUT CODES 0x100 to 0x2FE, even if it might be needed

                        // WE DON'T CARE ABOUT CODES 0x300 to 0xBFF, even if it might be needed

                        // WE DON'T CARE ABOUT CODES 0xC01 to 0x81FF, even if it might be needed

                    case PICT.OP_COMPRESSED_QUICKTIME:
                        // $8200: CompressedQuickTime Data length (Long), data (private to QuickTime) 4 + data length
                        if (DEBUG) {
                            System.out.println("compressedQuickTime");
                        }

                        readCompressedQT(pStream);

                        break;

                    case PICT.OP_UNCOMPRESSED_QUICKTIME:// JUST JUMP OVER
                        // $8201: UncompressedQuickTime Data length (Long), data (private to QuickTime) 4 + data length
                        // TODO: Read this as well, need test data
                        dataLength = pStream.readInt();
                        pStream.readFully(new byte[dataLength], 0, dataLength);
                        if (DEBUG) {
                            System.out.println("unCompressedQuickTime");
                        }
                        break;

                    case 0xFFFF:// JUST JUMP OVER
                        dataLength = pStream.readInt();
                        pStream.readFully(new byte[dataLength], 0, dataLength);
                        if (DEBUG) {
                            System.out.println(String.format("%s: 0x%04x - length: %s", PICT.APPLE_USE_RESERVED_FIELD, opCode, dataLength));
                        }
                        break;

                    default:
                        // See: http://developer.apple.com/DOCUMENTATION/mac/QuickDraw/QuickDraw-461.html
                        if (opCode >= 0x00a0 && opCode <= 0x00af) {
                            dataLength = pStream.readUnsignedShort();
                            pStream.readFully(new byte[dataLength], 0, dataLength);
                        }
                        else if (opCode >= 0x00b0 && opCode <= 0x00cf) {
                            // Zero-length
                            dataLength = 0;
                        }
                        else if (opCode >= 0x00d0 && opCode <= 0x00fe) {
                            dataLength = pStream.readInt();
                            pStream.readFully(new byte[dataLength], 0, dataLength);
                        }
                        else if (opCode >= 0x0100 && opCode <= 0x7fff) {
                            // For opcodes $0100-$7FFF, the amount of data for
                            // opcode $nnXX = 2 times nn bytes.
                            dataLength = ((opCode & 0xff00) >> 8) * 2;
                            pStream.skipBytes(dataLength);  
                        }
                        else if (opCode >= 0x8000 && opCode <= 0x80ff) {
                            // Zero-length
                            dataLength = 0;
                        }
                        else if (opCode >= 0x8100 && opCode <= 0x81ff) {
                            dataLength = pStream.readInt();
                            pStream.readFully(new byte[dataLength], 0, dataLength);
                        }
                        else {
                            throw new IIOException(String.format("Found unknown opcode: 0x%04x", opCode));
                        }

                        if (DEBUG) {
                            System.out.println(String.format("%s: 0x%04x - length: %s", PICT.APPLE_USE_RESERVED_FIELD, opCode, dataLength));
                        }
                }
            }
            while (opCode != PICT.OP_END_OF_PICTURE);
        }
        catch (IIOException e) {
            throw e;
        }
        catch (EOFException e) {
            String pos;
            try {
                pos = String.format("position %d", imageInput.getStreamPosition());
            }
            catch (IOException ignore) {
                pos = "unknown position";
            }

            throw new IIOException(String.format("Error in PICT format: Unexpected end of File at %s", pos), e);
        }
        catch (IOException e) {
            throw new IIOException(String.format("Error in PICT format: %s", e.getMessage()), e);
        }
    }

    /*
    http://devworld.apple.com/documentation/QuickTime/RM/CompressDecompress/ImageComprMgr/B-Chapter/chapter_1000_section_5.html:

    Field name      Description                             Data size (in bytes)

    Opcode          Compressed picture data                 2
    Size            Size in bytes of data for this opcode   4
    Version         Version of this opcode                  2
    Matrix          3 by 3 fixed transformation matrix      36
    MatteSize       Size of matte data in bytes             4
    MatteRect       Rectangle for matte data                8
    Mode            Transfer mode                           2
    SrcRect         Rectangle for source                    8
    Accuracy        Preferred accuracy                      4
    MaskSize        Size of mask region in bytes            4
     */
    private void readCompressedQT(final ImageInputStream pStream) throws IOException {
        int dataLength = pStream.readInt();
        long pos = pStream.getStreamPosition();

        if (DEBUG) {
            System.out.println("QT data length: " + dataLength);
        }

        // TODO: Need to figure out what the skipped data is?
        for (int i = 0; i < 13; i++) {
            int value = pStream.readInt();
            if (DEBUG) {
                System.out.println(String.format("%2d: 0x%08x", i * 4, value));
            }
        }

        // Read the destination rectangle
        Rectangle destination = new Rectangle();
        readRectangle(pStream, destination);

        if (DEBUG) {
            System.out.println("...");
        }

        for (int i = 0; i < 2; i++) {
            int value = pStream.readInt();
            if (DEBUG) {
                System.out.println(String.format("%2d: 0x%08x", (i + 15) * 4, value));
            }
        }

        BufferedImage image = QuickTime.decompress(pStream);

        if (image != null) {
            context.copyBits(image, new Rectangle(image.getWidth(), image.getHeight()), destination, QuickDraw.SRC_COPY, null);

            pStream.seek(pos + dataLength); // Might be word-align mismatch here

            // Skip "QuickTime? and a ... decompressor required" text
            // TODO: Verify that this is correct. It works with all my test data, but the algorithm is
            // reverse-engineered by looking at the input data and not from any spec I've seen...
            int penSizeMagic = pStream.readInt();
            if (penSizeMagic == 0x000700ae) {           // OP_PN_SIZE + bogus x value..?
                int skip = pStream.readUnsignedShort(); // bogus y value is the number of bytes to skip
                pStream.skipBytes(skip);                // Following opcode should be a OP_PN_SIZE with real values
            }
            else {
                pStream.seek(pos + dataLength);
            }
        }
        else {
            pStream.seek(pos + dataLength);
        }
    }

    /*
    http://devworld.apple.com/documentation/QuickTime/RM/CompressDecompress/ImageComprMgr/B-Chapter/chapter_1000_section_5.html:

    Field name      Description                             Data size (in bytes)

    Opcode          Uncompressed picture data               2
    Size            Size in bytes of data for this opcode   4
    Version         Version of this opcode                  2
    Matrix          3 by 3 fixed transformation matrix      36
    MatteSize       Size of matte data in bytes             4
    MatteRect       Rectangle for matte data                8
     */


    private void readOpPackBitsRect(ImageInputStream pStream, Rectangle pBounds, int pPixmapCount) throws IOException {
        if (DEBUG) {
            System.out.println("packBitsRect");
        }

        // Skip PixMap pointer (always 0x000000FF);
//        pStream.skipBytes(4);
//        int pixmapPointer = pStream.readInt();
//        System.out.println(String.format("%08d: 0x%08x", pStream.getStreamPosition(), pixmapPointer));

        // Get rowBytes
        int rowBytesRaw = pStream.readUnsignedShort();
//        System.out.println(String.format("%08d: 0x%04x", pStream.getStreamPosition(), rowBytesRaw));
        int rowBytes = rowBytesRaw & 0x3FFF;
        if (DEBUG) {
            System.out.print("packBitsRect, rowBytes: " + rowBytes);
            if ((rowBytesRaw & 0x8000) > 0) {
                System.out.print(", it is a PixMap");
            }
            else {
                System.out.print(", it is a BitMap");
            }
        }

        // Get bounds rectangle. THIS IS NOT TO BE SCALED BY THE RESOLUTION!
        int y = pStream.readUnsignedShort();
        int x = pStream.readUnsignedShort();
        pBounds.setLocation(x, y);

        y = pStream.readUnsignedShort();
        x = pStream.readUnsignedShort();
        pBounds.setSize(x - pBounds.x, y - pBounds.y);
        if (DEBUG) {
            System.out.print(", bounds: " + pBounds);
        }

        // Get PixMap record version number
        int pmVersion = pStream.readUnsignedShort() & 0xFFFF;
        if (DEBUG) {
            System.out.print(", pmVersion: " + pmVersion);
        }

        // Get packing format
        int packType = pStream.readUnsignedShort() & 0xFFFF;
        if (DEBUG) {
            System.out.print(", packType: " + packType);
        }

        // Get size of packed data (not used for v2)
        int packSize = pStream.readInt();
        if (DEBUG) {
            System.out.println(", packSize: " + packSize);
        }

        // Get resolution info
        double hRes = PICTUtil.readFixedPoint(pStream);
        double vRes = PICTUtil.readFixedPoint(pStream);
        if (DEBUG) {
            System.out.print("hRes: " + hRes + ", vRes: " + vRes);
        }

        // Get pixel type
        int pixelType = pStream.readUnsignedShort();
        if (DEBUG) {
            if (pixelType == 0) {
                System.out.print(", indexed pixels");
            }
            else {
                System.out.print(", RGBDirect");
            }
        }

        // Get pixel size
        int pixelSize = pStream.readUnsignedShort();
        if (DEBUG) {
            System.out.print(", pixelSize:" + pixelSize);
        }

        // Get pixel component count
        int cmpCount = pStream.readUnsignedShort();
        if (DEBUG) {
            System.out.print(", cmpCount:" + cmpCount);
        }

        // Get pixel component size
        int cmpSize = pStream.readUnsignedShort();
        if (DEBUG) {
            System.out.print(", cmpSize:" + cmpSize);
        }

        // planeBytes (ignored)
        int planeBytes = pStream.readInt();
        if (DEBUG) {
            System.out.print(", planeBytes:" + planeBytes);
        }

        // Handle to ColorTable record, there should be none for direct
        // bits so this should be 0, just skip
        int clutId = pStream.readInt();
        if (DEBUG) {
            System.out.println(", clutId:" + clutId);
        }

        // Reserved
        pStream.readInt();

        // Color table
        ColorModel colorModel;
        if (pixelType == 0) {
            colorModel = PICTUtil.readColorTable(pStream, pixelSize);
        }
        else {
            throw new IIOException("Unsupported pixel type: " + pixelType);
        }

        // Get source rectangle. We DO NOT scale the coordinates by the
        // resolution info, since we are in pixmap coordinates here
        Rectangle srcRect = new Rectangle();
        y = pStream.readUnsignedShort();
        x = pStream.readUnsignedShort();
        srcRect.setLocation(x, y);

        y = pStream.readUnsignedShort();
        x = pStream.readUnsignedShort();
        srcRect.setSize(x - srcRect.x, y - srcRect.y);

        if (DEBUG) {
            System.out.print("opPackBitsRect, srcRect:" + srcRect);
        }

        // TODO: FixMe...
        // Get destination rectangle. We DO scale the coordinates according to
        // the image resolution, since we are working in display coordinates
        Rectangle dstRect = new Rectangle();
        readRectangle(pStream, dstRect);
        if (DEBUG) {
            System.out.print(", dstRect:" + dstRect);
        }

        // Get transfer mode
        int transferMode = pStream.readUnsignedShort();
        if (DEBUG) {
            System.out.print(", mode: " + transferMode);
        }

        // Set up pixel buffer for the RGB values

        // TODO: Seems to be packType 0 all the time?
        // packType = 0 means default....


        // Read in the RGB arrays
        byte[] dstBytes;
        /*
        if (packType == 1 || rowBytes < 8) {
            // TODO: Verify this...
            dstBytes = new byte[rowBytes];
        }
        else if (packType == 2) {
            // TODO: Verify this...
            dstBytes = new byte[rowBytes * 3 / 4];
        }
        else if (packType == 3) {
            dstBytes = new byte[2 * pBounds.width];
        }
        else if (packType == 4) {
            dstBytes = new byte[cmpCount * pBounds.width];
        }
        else {
            throw new IIOException("Unknown pack type: " + packType);
        }
        */
        if (packType == 0) {
            dstBytes = new byte[cmpCount * pBounds.width];
        }
        else {
            throw new IIOException("Unknown pack type: " + packType);
        }

//        int[] pixArray = new int[pBounds.height * pBounds.width];
        byte[] pixArray = new byte[pBounds.height * pBounds.width];
        int pixBufOffset = 0;

        int packedBytesCount;
        for (int scanline = 0; scanline < pBounds.height; scanline++) {
            // Get byteCount of the scanline
            if (rowBytes > 250) {
                packedBytesCount = pStream.readUnsignedShort();
            }
            else {
                packedBytesCount = pStream.readUnsignedByte();
            }
            if (DEBUG) {
                System.out.println();
                System.out.print("Line " + scanline + ", byteCount: " + packedBytesCount);
                System.out.print(" dstBytes: " + dstBytes.length);
            }

            // Read in the scanline
            /*if (packType > 2) {
                // Unpack them all*/
                Decoder decoder;/*
                if (packType == 3) {
                    decoder = new PackBits16Decoder();
                }
                else {*/
                    decoder = new PackBitsDecoder();
                /*}*/
            DataInput unPackBits = new DataInputStream(new DecoderStream(IIOUtil.createStreamAdapter(pStream, packedBytesCount), decoder));
//                unPackBits.readFully(dstBytes);
            unPackBits.readFully(pixArray, pixBufOffset, pBounds.width);
            /*}
            else {
                imageInput.readFully(dstBytes);
            }*/

            // TODO: Use TYPE_USHORT_555_RGB for 16 bit
            /*
            if (packType == 3) {
                for (int i = 0; i < pBounds.width; i++) {
                    // Set alpha values to all opaque
                    pixArray[pixBufOffset + i] = 0xFF000000;

                    // Get red values
                    int red = 8 * ((dstBytes[2 * i] & 0x7C) >> 2);
                    pixArray[pixBufOffset + i] |= red << 16;
                    // Get green values
                    int green = 8 * (((dstBytes[2 * i] & 0x07) << 3) + ((dstBytes[2 * i + 1] & 0xE0) >> 5));
                    pixArray[pixBufOffset + i] |= green << 8;
                    // Get blue values
                    int blue = 8 * ((dstBytes[2 * i + 1] & 0x1F));
                    pixArray[pixBufOffset + i] |= blue;
                }
            }
            else {
                if (cmpCount == 3) {
                    for (int i = 0; i < pBounds.width; i++) {
                        // Set alpha values to all opaque
                        pixArray[pixBufOffset + i] = 0xFF000000;
                        // Get red values
                        pixArray[pixBufOffset + i] |= (dstBytes[i] & 0xFF) << 16;
                        // Get green values
                        pixArray[pixBufOffset + i] |= (dstBytes[pBounds.width + i] & 0xFF) << 8;
                        // Get blue values
                        pixArray[pixBufOffset + i] |= (dstBytes[2 * pBounds.width + i] & 0xFF);
                    }
                }
                else {
                    for (int i = 0; i < pBounds.width; i++) {
//                        // Get alpha values
//                        pixArray[pixBufOffset + i] = (dstBytes[i] & 0xFF) << 24;
//                        // Get red values
//                        pixArray[pixBufOffset + i] |= (dstBytes[pBounds.width + i] & 0xFF) << 16;
//                        // Get green values
//                        pixArray[pixBufOffset + i] |= (dstBytes[2 * pBounds.width + i] & 0xFF) << 8;
//                        // Get blue values
//                        pixArray[pixBufOffset + i] |= (dstBytes[3 * pBounds.width + i] & 0xFF);

                        // TODO: Fake it for now... Should ideally just use byte array and use the ICM
//                        pixArray[pixBufOffset + i] = 0xFF << 24;
//                        pixArray[pixBufOffset + i] |= colorModel.getRed(dstBytes[i] & 0xFF) << 16;
//                        pixArray[pixBufOffset + i] |= colorModel.getGreen(dstBytes[i] & 0xFF) << 8;
//                        pixArray[pixBufOffset + i] |= colorModel.getBlue(dstBytes[i] & 0xFF);

                        pixArray[pixBufOffset + i] = dstBytes[i];
                    }
//                }
//            }
*/

            // Increment pixel buffer offset
            pixBufOffset += pBounds.width;

            ////////////////////////////////////////////////////
            // TODO: This works for single image PICTs only...
            // However, this is the most common case. Ok for now
            processImageProgress(scanline * 100 / pBounds.height);
            if (abortRequested()) {
                processReadAborted();

                // Skip rest of image data
                for (int skip = scanline + 1; skip < pBounds.height; skip++) {
                    // Get byteCount of the scanline
                    if (rowBytes > 250) {
                        packedBytesCount = pStream.readUnsignedShort();
                    }
                    else {
                        packedBytesCount = pStream.readUnsignedByte();
                    }
                    pStream.readFully(new byte[packedBytesCount], 0, packedBytesCount);

                    if (DEBUG) {
                        System.out.println();
                        System.out.print("Skip " + skip + ", byteCount: " + packedBytesCount);
                    }
                }

                break;
            }
            ////////////////////////////////////////////////////
        }

        // We add all new images to it. If we are just replaying, then
        // "pPixmapCount" will never be greater than the size of the vector
        if (images.size() <= pPixmapCount) {
            // Create BufferedImage and add buffer it for multiple reads
//            DirectColorModel cm = (DirectColorModel) ColorModel.getRGBdefault();
//            DataBuffer db = new DataBufferInt(pixArray, pixArray.length);
//            WritableRaster raster = Raster.createPackedRaster(db, pBounds.width, pBounds.height, pBounds.width, cm.getMasks(), null);
            DataBuffer db = new DataBufferByte(pixArray, pixArray.length);
            WritableRaster raster = Raster.createPackedRaster(db, pBounds.width, pBounds.height, cmpSize, null); // TODO: last param should ideally be srcRect.getLocation()
            BufferedImage img = new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);

            images.add(img);
        }

        // Draw the image
        BufferedImage img = images.get(pPixmapCount);
        if (img != null) {
            // TODO: FixMe.. Seems impossible to create a bufferedImage with a raster not starting at 0,0
            srcRect.setLocation(0, 0); // should not require this line..
            context.copyBits(img, srcRect, dstRect, transferMode, null);
        }

        // Line break at the end
        if (DEBUG) {
            System.out.println();
        }
    }

    /**
     * Reads the data following a {@code directBitsRect} opcode.
     *
     * @param pStream the stream to read from
     * @param pBounds the bounding rectangle
     * @param pPixmapCount the index of the bitmap in the PICT file, used for
     *        cahcing.
     *
     * @throws javax.imageio.IIOException if the data can not be read.
     * @throws IOException if an I/O error occurs while reading the image.
     */
    private void readOpDirectBitsRect(ImageInputStream pStream, Rectangle pBounds, int pPixmapCount) throws IOException {
        if (DEBUG) {
            System.out.println("directBitsRect");
        }

        // Skip PixMap pointer (always 0x000000FF);
        pStream.skipBytes(4);

        // Get rowBytes
        int rowBytesRaw = pStream.readUnsignedShort();
        int rowBytes = rowBytesRaw & 0x3FFF;
        if (DEBUG) {
            System.out.print("directBitsRect, rowBytes: " + rowBytes);
            if ((rowBytesRaw & 0x8000) > 0) {
                System.out.print(", it is a PixMap");
            }
            else {
                System.out.print(", it is a BitMap");
            }
        }

        // Get bounds rectangle. THIS IS NOT TO BE SCALED BY THE RESOLUTION!
        int y = pStream.readUnsignedShort();
        int x = pStream.readUnsignedShort();
        pBounds.setLocation(x, y);

        y = pStream.readUnsignedShort();
        x = pStream.readUnsignedShort();
        pBounds.setSize(x - pBounds.x, y - pBounds.y);
        if (DEBUG) {
            System.out.print(", bounds: " + pBounds);
        }

        // Get PixMap record version number
        int pmVersion = pStream.readUnsignedShort() & 0xFFFF;
        if (DEBUG) {
            System.out.print(", pmVersion: " + pmVersion);
        }

        // Get packing format
        int packType = pStream.readUnsignedShort() & 0xFFFF;
        if (DEBUG) {
            System.out.print(", packType: " + packType);
        }

        // Get size of packed data (not used for v2)
        int packSize = pStream.readInt();
        if (DEBUG) {
            System.out.println(", packSize: " + packSize);
        }

        // Get resolution info
        double hRes = PICTUtil.readFixedPoint(pStream);
        double vRes = PICTUtil.readFixedPoint(pStream);
        if (DEBUG) {
            System.out.print("hRes: " + hRes + ", vRes: " + vRes);
        }

        // Get pixel type
        int pixelType = pStream.readUnsignedShort();
        if (DEBUG) {
            if (pixelType == 0) {
                System.out.print(", indexed pixels");
            }
            else {
                System.out.print(", RGBDirect");
            }
        }

        // Get pixel size
        int pixelSize = pStream.readUnsignedShort();
        if (DEBUG) {
            System.out.print(", pixelSize:" + pixelSize);
        }

        // Get pixel component count
        int cmpCount = pStream.readUnsignedShort();
        if (DEBUG) {
            System.out.print(", cmpCount:" + cmpCount);
        }

        // Get pixel component size
        int cmpSize = pStream.readUnsignedShort();
        if (DEBUG) {
            System.out.println(", cmpSize:" + cmpSize);
        }

        // planeBytes (ignored)
        pStream.readInt();

        // Handle to ColorTable record, there should be none for direct
        // bits so this should be 0, just skip
        pStream.readInt();

        // Reserved
        pStream.readInt();

        // Get source rectangle. We DO NOT scale the coordinates by the
        // resolution info, since we are in pixmap coordinates here
        Rectangle srcRect = new Rectangle();
        y = pStream.readUnsignedShort();
        x = pStream.readUnsignedShort();
        srcRect.setLocation(x, y);

        y = pStream.readUnsignedShort();
        x = pStream.readUnsignedShort();
        srcRect.setSize(x - srcRect.x, y - srcRect.y);

        if (DEBUG) {
            System.out.print("opDirectBitsRect, srcRect:" + srcRect);
        }

        // TODO: FixMe...
        // Get destination rectangle. We DO scale the coordinates according to
        // the image resolution, since we are working in display coordinates
        Rectangle dstRect = new Rectangle();
        readRectangle(pStream, dstRect);
        if (DEBUG) {
            System.out.print(", dstRect:" + dstRect);
        }

        // Get transfer mode
        int transferMode = pStream.readUnsignedShort();
        if (DEBUG) {
            System.out.print(", mode: " + transferMode);
        }

        // Set up pixel buffer for the RGB values

        // Read in the RGB arrays
        byte[] dstBytes;
        if (packType == 1 || rowBytes < 8) {
            // TODO: Verify this...
            dstBytes = new byte[rowBytes];
        }
        else if (packType == 2) {
            // TODO: Verify this...
            dstBytes = new byte[rowBytes * 3 / 4];
        }
        else if (packType == 3) {
            dstBytes = new byte[2 * pBounds.width];
        }
        else if (packType == 4) {
            dstBytes = new byte[cmpCount * pBounds.width];
        }
        else {
            throw new IIOException("Unknown pack type: " + packType);
        }

        int[] pixArray = null;
        short[] shortArray = null;
        if (packType == 3) {
            shortArray = new short[pBounds.height * pBounds.width];
        }
        else {
            pixArray = new int[pBounds.height * pBounds.width];
        }

        int pixBufOffset = 0;

        int packedBytesCount;
        for (int scanline = 0; scanline < pBounds.height; scanline++) {
            // Get byteCount of the scanline
            if (rowBytes > 250) {
                packedBytesCount = pStream.readUnsignedShort();
            }
            else {
                packedBytesCount = pStream.readUnsignedByte();
            }
            if (DEBUG) {
                System.out.println();
                System.out.print("Line " + scanline + ", byteCount: " + packedBytesCount);
                System.out.print(" dstBytes: " + dstBytes.length);
            }

            // Read in the scanline
            if (packType > 2) {
                // Unpack them all
                Decoder decoder;
                if (packType == 3) {
                    decoder = new PackBits16Decoder();
                }
                else {
                    decoder = new PackBitsDecoder();
                }
                DataInput unPackBits = new DataInputStream(new DecoderStream(IIOUtil.createStreamAdapter(pStream, packedBytesCount), decoder));
                unPackBits.readFully(dstBytes);
            }
            else {
                imageInput.readFully(dstBytes);
            }

            if (packType == 3) {
                // TYPE_USHORT_555_RGB for 16 bit
                for (int i = 0; i < pBounds.width; i++) {
                    shortArray[pixBufOffset + i] = (short) (((0xff & dstBytes[2 * i]) << 8) | (0xff & dstBytes[2 * i + 1]));
//                    // Set alpha values to all opaque
//                    pixArray[pixBufOffset + i] = 0xFF000000;
//
//                    // Get red values
//                    int red = 8 * ((dstBytes[2 * i] & 0x7C) >> 2);
//                    pixArray[pixBufOffset + i] |= red << 16;
//                    // Get green values
//                    int green = 8 * (((dstBytes[2 * i] & 0x07) << 3) + ((dstBytes[2 * i + 1] & 0xE0) >> 5));
//                    pixArray[pixBufOffset + i] |= green << 8;
//                    // Get blue values
//                    int blue = 8 * ((dstBytes[2 * i + 1] & 0x1F));
//                    pixArray[pixBufOffset + i] |= blue;
                }
            }
            else {
                if (cmpCount == 3) {
                    // RGB
                    for (int i = 0; i < pBounds.width; i++) {
                        // Set alpha values to all opaque
                        pixArray[pixBufOffset + i] = 0xFF000000;
                        // Get red values
                        pixArray[pixBufOffset + i] |= (dstBytes[i] & 0xFF) << 16;
                        // Get green values
                        pixArray[pixBufOffset + i] |= (dstBytes[pBounds.width + i] & 0xFF) << 8;
                        // Get blue values
                        pixArray[pixBufOffset + i] |= (dstBytes[2 * pBounds.width + i] & 0xFF);
                    }
                }
                else {
                    // ARGB
                    for (int i = 0; i < pBounds.width; i++) {
                        // Get alpha values
                        pixArray[pixBufOffset + i] = (dstBytes[i] & 0xFF) << 24;
                        // Get red values
                        pixArray[pixBufOffset + i] |= (dstBytes[pBounds.width + i] & 0xFF) << 16;
                        // Get green values
                        pixArray[pixBufOffset + i] |= (dstBytes[2 * pBounds.width + i] & 0xFF) << 8;
                        // Get blue values
                        pixArray[pixBufOffset + i] |= (dstBytes[3 * pBounds.width + i] & 0xFF);
                    }
                }
            }

            // Increment pixel buffer offset
            pixBufOffset += pBounds.width;

            ////////////////////////////////////////////////////
            // TODO: This works for single image PICTs only...
            // However, this is the most common case. Ok for now
            processImageProgress(scanline * 100 / pBounds.height);
            if (abortRequested()) {
                processReadAborted();

                // Skip rest of image data
                for (int skip = scanline + 1; skip < pBounds.height; skip++) {
                    // Get byteCount of the scanline
                    if (rowBytes > 250) {
                        packedBytesCount = pStream.readUnsignedShort();
                    }
                    else {
                        packedBytesCount = pStream.readUnsignedByte();
                    }
                    pStream.readFully(new byte[packedBytesCount], 0, packedBytesCount);

                    if (DEBUG) {
                        System.out.println();
                        System.out.print("Skip " + skip + ", byteCount: " + packedBytesCount);
                    }
                }

                break;
            }
            ////////////////////////////////////////////////////
        }

        // We add all new images to it. If we are just replaying, then
        // "pPixmapCount" will never be greater than the size of the vector
        if (images.size() <= pPixmapCount) {
            // Create BufferedImage and add buffer it for multiple reads
            DirectColorModel cm;
            WritableRaster raster;

            if (packType == 3) {
                cm = new DirectColorModel(15, 0x7C00, 0x03E0, 0x001F); // See BufferedImage TYPE_USHORT_555_RGB
                DataBuffer db = new DataBufferUShort(shortArray, shortArray.length);
                raster = Raster.createPackedRaster(db, pBounds.width, pBounds.height, pBounds.width, cm.getMasks(), null);  // TODO: last param should ideally be srcRect.getLocation()
            }
            else {
                cm = (DirectColorModel) ColorModel.getRGBdefault();
                DataBuffer db = new DataBufferInt(pixArray, pixArray.length);
                raster = Raster.createPackedRaster(db, pBounds.width, pBounds.height, pBounds.width, cm.getMasks(), null);  // TODO: last param should ideally be srcRect.getLocation()
            }

            BufferedImage img = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);

            images.add(img);
        }

        // Draw the image
        BufferedImage img = images.get(pPixmapCount);
        if (img != null) {
            // TODO: FixMe.. Something wrong here, might be the copyBits methods.
            srcRect.setLocation(0, 0); // should not require this line..
            context.copyBits(img, srcRect, dstRect, transferMode, null);
        }

        // Line break at the end
        if (DEBUG) {
            System.out.println();
        }

    }

    /**
     * Reads the rectangle location and size from an 8-byte rectangle stream.
     *
     * @param pStream the stream to read from
     * @param pDestRect the rectangle to read into
     *
     * @throws NullPointerException if {@code pDestRect} is {@code null}
     * @throws IOException if an I/O error occurs while reading the image.
     */
    private void readRectangle(DataInput pStream, Rectangle pDestRect) throws IOException {
        int y = pStream.readUnsignedShort();
        int x = pStream.readUnsignedShort();
        int h = pStream.readUnsignedShort();
        int w = pStream.readUnsignedShort();

        pDestRect.setLocation(getXPtCoord(x), getYPtCoord(y));
        pDestRect.setSize(getXPtCoord(w - x), getYPtCoord(h - y));

    }

    /**
     * Read in a region. The inputstream should be positioned at the first byte
     * of the region. {@code pBoundsRect} is a rectangle that will be set to the
     * region bounds.
     * The point array may therefore be empty if the region is just a rectangle.
     *
     * @param pStream the stream to read from
     * @param pBoundsRect the bounds rectangle to read into
     *
     * @return the polygon containing the region, or an empty polygon if the
     * region is a rectanlge.
     *
     * @throws IOException if an I/O error occurs while reading the image.
     */
    private Polygon readRegion(DataInput pStream, Rectangle pBoundsRect) throws IOException {
        // Get minimal region

        // Get region data size
        int size = pStream.readUnsignedShort();

        // Get region bounds
        int y = getYPtCoord(pStream.readUnsignedShort());
        int x = getXPtCoord(pStream.readUnsignedShort());
        pBoundsRect.setLocation(x, y);

        y = getYPtCoord(pStream.readShort()) - pBoundsRect.getLocation().y;
        x = getXPtCoord(pStream.readShort()) - pBoundsRect.getLocation().x;
        pBoundsRect.setSize(x, y);

        // Initialize the point array to the right size
        int points = (size - 10) / (2 * 2);

        // Get the rest of the polygon points
        Polygon polygon = new Polygon();
        for (int i = 0; i < points; i++) {
            x = getXPtCoord(pStream.readShort());
            y = getYPtCoord(pStream.readShort());

            polygon.addPoint(x, y);
        }

        return polygon;
    }

    /*
     * Read in a polygon. The inputstream should be positioned at the first byte
     * of the polygon.
     */
    private Polygon readPoly(DataInput pStream, Rectangle pBoundsRect) throws IOException {
        // Get polygon data size
        int size = pStream.readUnsignedShort();

        // Get poly bounds
        int y = getYPtCoord(pStream.readShort());
        int x = getXPtCoord(pStream.readShort());
        pBoundsRect.setLocation(x, y);

        y = getYPtCoord(pStream.readShort()) - pBoundsRect.getLocation().y;
        x = getXPtCoord(pStream.readShort()) - pBoundsRect.getLocation().x;
        pBoundsRect.setSize(x, y);

        // Initialize the point array to the right size
        int points = (size - 10) / (2 * 2);

        // Get the rest of the polygon points
        Polygon polygon = new Polygon();

        for (int i = 0; i < points; i++) {
            y = getYPtCoord(pStream.readShort());
            x = getXPtCoord(pStream.readShort());
            polygon.addPoint(x, y);
        }

        return polygon;
    }

    // TODO: Support color pixel patterns!
    /*
     * Read PixPat. Read a PixPat data structure from the stream. Just returns
     * void for the moment since not used in AWT graphics. NOT IMPLEMENTED YET!
     */
    /*
    private void readPixPat(DataInput pStream) {
        byte[]	count = new byte[1];
        byte[]	text_bytes;

        try {
            // Comment kind and data byte count
            pStream.readFully(count, 0, count.length);

            // Get as many bytes as indicated by byte count
            int text_byte_count = count[0];
            text_bytes = new byte[text_byte_count];
            pStream.readFully(text_bytes, 0, text_byte_count);
        } catch ( IOException e ) { return null; }
        return new String(text_bytes);
    }
    */

    /*
     * Read a long comment from the stream.
     */
    private void readLongComment(final DataInput pStream) throws IOException {
        // Comment kind and data byte count
        pStream.readShort();

        // Get as many bytes as indicated by byte count
        int length = pStream.readUnsignedShort();
        pStream.readFully(new byte[length], 0, length);
    }

    /*
     * Return the X coordinate value in display coordinates for the given
     * coordinate value. This means multiplying it with the screen resolution/
     * image resolution ratio.
     */
    private int getXPtCoord(int pPoint) {
        return (int) (pPoint / screenImageXRatio);
    }

    /*
     * Return the Y coordinate value in display coordinates for the given
     * coordinate value. This means multiplying it with the screen resolution/
     * image resolution ratio.
     */
    private int getYPtCoord(int pPoint) {
        return (int) (pPoint / screenImageYRatio);
    }

    /*
     * Write out polygon command, bounds and points.
     */
    private void verbosePolyCmd(String pCmd, Rectangle pBounds, Polygon pPolygon) {
        int i;

        System.out.println(pCmd + ": " + new Rectangle(pBounds.x, pBounds.y, pBounds.width, pBounds.height));
        System.out.print("Polygon points: ");
        for (i = 0; pPolygon != null && i < pPolygon.npoints - 1; i++) {
            System.out.print("(" + pPolygon.xpoints[i] + "," + pPolygon.ypoints[i] + "), ");
        }
        if (pPolygon != null && pPolygon.npoints > 0) {
            System.out.print("(" + pPolygon.xpoints[i] + "," + pPolygon.ypoints[i] + ")");
        }
        System.out.println();
    }

    /*
     * Write out region command, bounds and points.
     */
    private void verboseRegionCmd(String pCmd, Rectangle pBounds, Polygon pPolygon) {
        int i;

        System.out.println(pCmd + ": " + new Rectangle(pBounds.x, pBounds.y, pBounds.width, pBounds.height));
        System.out.print("Region points: ");
        for (i = 0; pPolygon != null && i < pPolygon.npoints - 1; i++) {
            System.out.print("(" + pPolygon.xpoints[i] + "," + pPolygon.ypoints[i] + "), ");
        }
        if (pPolygon != null && pPolygon.npoints > 0) {
            System.out.print("(" + pPolygon.xpoints[i] + "," + pPolygon.ypoints[i] + ")");
        }
        System.out.println();
    }

    @Override
    public BufferedImage read(final int pIndex, final ImageReadParam pParam) throws IOException {
        checkBounds(pIndex);

        processImageStarted(pIndex);

        // TODO: Param handling
        // TODO: Real subsampling for bit/pixmap/QT stills
        final int subX, subY;
        if (pParam != null) {
            subX = pParam.getSourceXSubsampling();
            subY = pParam.getSourceYSubsampling();
        }
        else {
            subX = 1;
            subY = 1;
        }

        Rectangle frame = getPICTFrame();
        BufferedImage image = getDestination(pParam, getImageTypes(pIndex), getXPtCoord(frame.width), getYPtCoord(frame.height));
        Graphics2D g = image.createGraphics();
        try {
            // TODO: Might need to clear background
            AffineTransform instance = new AffineTransform();
            if (pParam != null && pParam.getSourceRegion() != null) {
                Rectangle rectangle = pParam.getSourceRegion();
                instance.translate(-rectangle.x, -rectangle.y);
            }
            instance.scale(screenImageXRatio / subX, screenImageYRatio / subY);
            g.setTransform(instance);
//            try {
                drawOnto(g);
//            }
//            catch (IOException e) {
//                e.printStackTrace();
//            }
        }
        finally {
            g.dispose();
        }

        processImageComplete();

        return image;
    }

    public int getWidth(int pIndex) throws IOException {
        checkBounds(pIndex);
        return getXPtCoord(getPICTFrame().width);
    }

    public int getHeight(int pIndex) throws IOException {
        checkBounds(pIndex);
        return getYPtCoord(getPICTFrame().height);
    }

    public Iterator<ImageTypeSpecifier> getImageTypes(int pIndex) throws IOException {
        // TODO: The images look slightly different in Preview.. Could indicate the color space is wrong...
        return Arrays.asList(
                ImageTypeSpecifier.createPacked(
                        ColorSpace.getInstance(ColorSpace.CS_sRGB),
                        0xff0000, 0xff00, 0xff, 0xff000000, DataBuffer.TYPE_INT, false
                )
        ).iterator();
    }

    public static void main(String[] pArgs) throws IOException {
        ImageReader reader = new PICTImageReader(new PICTImageReaderSpi());

        ImageInputStream input;
        String title;
        if (pArgs.length >= 1) {
            File file = new File(pArgs[0]);
            input = ImageIO.createImageInputStream(file);
            title = file.getName();
        }
        else {
            input = ImageIO.createImageInputStream(new ByteArrayInputStream(DATA_V1_OVERPAINTED_ARC));
            title = "PICT test data";
        }

        System.out.println("canRead: " + reader.getOriginatingProvider().canDecodeInput(input));

        reader.setInput(input);
        long start = System.currentTimeMillis();
        BufferedImage image = reader.read(0);

        System.out.println("time: " + (System.currentTimeMillis() - start));

        showIt(image, title);

        System.out.println("image = " + image);
    }

    // Sample data from http://developer.apple.com/documentation/mac/QuickDraw/QuickDraw-458.html
    // TODO: Create test case(s)!
    private static final byte[] DATA_EXT_V2 = {
            0x00, 0x78, /* picture size; don't use this value for picture size */
            0x00, 0x00, 0x00, 0x00, 0x00, 0x6C, 0x00, (byte) 0xA8, /* bounding rectangle of picture at 72 dpi */
            0x00, 0x11, /* VersionOp opcode; always $0011 for extended version 2 */
            0x02, (byte) 0xFF, /* Version opcode; always $02FF for extended version 2 */
            0x0C, 0x00, /* HeaderOp opcode; always $0C00 for extended version 2 */
            /* next 24 bytes contain header information */
            (byte) 0xFF, (byte) 0xFE, /* version; always -2 for extended version 2 */
            0x00, 0x00, /* reserved */
            0x00, 0x48, 0x00, 0x00, /* best horizontal resolution: 72 dpi */
            0x00, 0x48, 0x00, 0x00, /* best vertical resolution: 72 dpi */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* optimal source rectangle for 72 dpi horizontal
                              and 72 dpi vertical resolutions */
            0x00, 0x00, /* reserved */
            0x00, 0x1E, /* DefHilite opcode to use default hilite color */
            0x00, 0x01, /* Clip opcode to define clipping region for picture */
            0x00, 0x0A, /* region size */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* bounding rectangle for clipping region */
            0x00, 0x0A, /* FillPat opcode; fill pattern specified in next 8 bytes */
            0x77, (byte) 0xDD, 0x77, (byte) 0xDD, 0x77, (byte) 0xDD, 0x77, (byte) 0xDD, /* fill pattern */
            0x00, 0x34, /* fillRect opcode; rectangle specified in next 8 bytes */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* rectangle to fill */
            0x00, 0x0A, /* FillPat opcode; fill pattern specified in next 8 bytes */
            (byte) 0x88, 0x22, (byte) 0x88, 0x22, (byte) 0x88, 0x22, (byte) 0x88, 0x22, /* fill pattern */
            0x00, 0x5C, /* fillSameOval opcode */
            0x00, 0x08, /* PnMode opcode */
            0x00, 0x08, /* pen mode data */
            0x00, 0x71, /* paintPoly opcode */
            0x00, 0x1A, /* size of polygon */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* bounding rectangle for polygon */
            0x00, 0x6E, 0x00, 0x02, 0x00, 0x02, 0x00, 0x54, 0x00, 0x6E, 0x00, (byte) 0xAA, 0x00, 0x6E, 0x00, 0x02, /* polygon points */
            0x00, (byte) 0xFF, /* OpEndPic opcode; end of picture */
    };

    private static final byte[] DATA_V2 = {
            0x00, 0x78, /* picture size; don't use this value for picture size */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* bounding rectangle of picture */
            0x00, 0x11, /* VersionOp opcode; always $0x00, 0x11, for version 2 */
            0x02, (byte) 0xFF, /* Version opcode; always $0x02, 0xFF, for version 2 */
            0x0C, 0x00, /* HeaderOp opcode; always $0C00 for version 2 */
            /* next 24 bytes contain header information */
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, /* version; always -1 (long) for version 2 */
            0x00, 0x02, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, (byte) 0xAA, 0x00, 0x00, 0x00, 0x6E, 0x00, 0x00, /* fixed-point bounding
                                                   rectangle for picture */
            0x00, 0x00, 0x00, 0x00, /* reserved */
            0x00, 0x1E, /* DefHilite opcode to use default hilite color */
            0x00, 0x01, /* Clip opcode to define clipping region for picture */
            0x00, 0x0A, /* region size */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* bounding rectangle for clipping region */
            0x00, 0x0A, /* FillPat opcode; fill pattern specifed in next 8 bytes */
            0x77, (byte) 0xDD, 0x77, (byte) 0xDD, 0x77, (byte) 0xDD, 0x77, (byte) 0xDD, /* fill pattern */
            0x00, 0x34, /* fillRect opcode; rectangle specified in next 8 bytes */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* rectangle to fill */
            0x00, 0x0A, /* FillPat opcode; fill pattern specified in next 8 bytes */
            (byte) 0x88, 0x22, (byte) 0x88, 0x22, (byte) 0x88, 0x22, (byte) 0x88, 0x22, /* fill pattern */
            0x00, 0x5C, /* fillSameOval opcode */
            0x00, 0x08, /* PnMode opcode */
            0x00, 0x08, /* pen mode data */
            0x00, 0x71, /* paintPoly opcode */
            0x00, 0x1A, /* size of polygon */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* bounding rectangle for polygon */
            0x00, 0x6E, 0x00, 0x02, 0x00, 0x02, 0x00, 0x54, 0x00, 0x6E, 0x00, (byte) 0xAA, 0x00, 0x6E, 0x00, 0x02, /* polygon points */
            0x00, (byte) 0xFF, /* OpEndPic opcode; end of picture */
    };

    private static final byte[] DATA_V1 = {
            0x00, 0x4F, /* picture size; this value is reliable for version 1 pictures */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* bounding rectangle of picture */
            0x11, /* picVersion opcode for version 1 */
            0x01, /* version number 1 */
            0x01, /* ClipRgn opcode to define clipping region for picture */
            0x00, 0x0A, /* region size */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* bounding rectangle for region */
            0x0A, /* FillPat opcode; fill pattern specified in next 8 bytes */
            0x77, (byte) 0xDD, 0x77, (byte) 0xDD, 0x77, (byte) 0xDD, 0x77, (byte) 0xDD, /* fill pattern */
            0x34, /* fillRect opcode; rectangle specified in next 8 bytes */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* rectangle to fill */
            0x0A, /* FillPat opcode; fill pattern specified in next 8 bytes */
            (byte) 0x88, 0x22, (byte) 0x88, 0x22, (byte) 0x88, 0x22, (byte) 0x88, 0x22, /* fill pattern */
            0x5C, /* fillSameOval opcode */
            0x71, /* paintPoly opcode */
            0x00, 0x1A, /* size of polygon */
            0x00, 0x02, 0x00, 0x02, 0x00, 0x6E, 0x00, (byte) 0xAA, /* bounding rectangle for polygon */
            0x00, 0x6E, 0x00, 0x02, 0x00, 0x02, 0x00, 0x54, 0x00, 0x6E, 0x00, (byte) 0xAA, 0x00, 0x6E, 0x00, 0x02, /* polygon points */
            (byte) 0xFF, /* EndOfPicture opcode; end of picture */
    };

    // Examples from http://developer.apple.com/technotes/qd/qd_14.html
    private static final byte[] DATA_V1_OVAL_RECT = {
            0x00, 0x26, /*size */
            0x00, 0x0A, 0x00, 0x14, 0x00, (byte) 0xAF, 0x00, 0x78, /* picFrame */
            0x11, 0x01, /* version 1 */
            0x01, 0x00, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xFA, 0x01, (byte) 0x90, /* clipRgn -- 10 byte region */
            0x0B, 0x00, 0x04, 0x00, 0x05, /* ovSize point */
            0x40, 0x00, 0x0A, 0x00, 0x14, 0x00, (byte) 0xAF, 0x00, 0x78, /* frameRRect rectangle */
            (byte) 0xFF, /* fin */
    };

    private static final byte[] DATA_V1_OVERPAINTED_ARC = {
            0x00, 0x36, /* size */
            0x00, 0x0A, 0x00, 0x14, 0x00, (byte) 0xAF, 0x00, 0x78, /* picFrame */
            0x11, 0x01, /* version 1 */
            0x01, 0x00, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xFA, 0x01, (byte) 0x90, /* clipRgn -- 10 byte region */
            0x61, 0x00, 0x0A, 0x00, 0x14, 0x00, (byte) 0xAF, 0x00, 0x78, 0x00, 0x03, 0x00, 0x2D, /* paintArc rectangle,startangle,endangle */
            0x08, 0x00, 0x0A, /* pnMode patXor -- note that the pnMode comes before the pnPat */
            0x09, (byte) 0xAA, 0x55, (byte) 0xAA, 0x55, (byte) 0xAA, 0x55, (byte) 0xAA, 0x55, /* pnPat gray */
            0x69, 0x00, 0x03, 0x00, 0x2D, /* paintSameArc startangle,endangle */
            (byte) 0xFF, /* fin */
    };

    private static final byte[] DATA_V1_COPY_BITS = {
            0x00, 0x48, /* size */
            0x00, 0x0A, 0x00, 0x14, 0x00, (byte) 0xAF, 0x00, 0x78, /* picFrame */
            0x11, 0x01, /* version 1 */
            0x01, 0x00, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xFA, 0x01, (byte) 0x90, /* clipRgn -- 10 byte region */
            0x31, 0x00, 0x0A, 0x00, 0x14, 0x00, (byte) 0xAF, 0x00, 0x78, /* paintRect rectangle */
            (byte) 0x90, 0x00, 0x02, 0x00, 0x0A, 0x00, 0x14, 0x00, 0x0F, 0x00, 0x1C, /* BitsRect rowbytes bounds (note that bounds is wider than smallr) */
            0x00, 0x0A, 0x00, 0x14, 0x00, 0x0F, 0x00, 0x19, /* srcRect */
            0x00, 0x00, 0x00, 0x00, 0x00, 0x14, 0x00, 0x1E, /* dstRect */
            0x00, 0x06, /* mode=notSrcXor */
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, /* 5 rows of empty bitmap (we copied from a
                                still-blank window) */
            (byte) 0xFF, /* fin */
    };
}
