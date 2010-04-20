/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.servlet.image;

import com.twelvemonkeys.servlet.GenericServlet;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.zip.CRC32;

/**
 * Creates a minimal 1 x 1 pixel PNG image, in a color specified by the
 * {@code "color"} parameter. The color is HTML-style #RRGGBB, with two
 * digits hex number for red, green and blue (the hash, '#', is optional).
 * <p/>
 * The class does only byte manipulation, there is no server-side image
 * processing involving AWT ({@code Toolkit} class) of any kind.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/image/ColorServlet.java#2 $
 */
public class ColorServlet extends GenericServlet {
    private final static String RGB_PARAME = "color";

    // A minimal, one color indexed PNG
    private final static byte[] PNG_IMG = new byte[]{
        (byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G', // PNG signature (8 bytes)
        0x0d, 0x0a, 0x1a, 0x0a,

        0x00, 0x00, 0x00, 0x0d, // IHDR length (13)
        (byte) 'I', (byte) 'H', (byte) 'D', (byte) 'R', // Image header
        0x00, 0x00, 0x00, 0x01, // width
        0x00, 0x00, 0x00, 0x01, // height
        0x01, 0x03, 0x00, 0x00, 0x00, // bits, color type, compression, filter, interlace
        0x25, (byte) 0xdb, 0x56, (byte) 0xca, // IHDR CRC

        0x00, 0x00, 0x00, 0x03, // PLTE length (3)
        (byte) 'P', (byte) 'L', (byte) 'T', (byte) 'E', // Palette
        0x00, 0x00, (byte) 0xff, // red, green, blue (updated by this servlet)
        (byte) 0x8a, (byte) 0x78, (byte) 0xd2, 0x57, // PLTE CRC

        0x00, 0x00, 0x00, 0x0a, // IDAT length (10)
        (byte) 'I', (byte) 'D', (byte) 'A', (byte) 'T', // Image data
        0x78, (byte) 0xda, 0x63, 0x60, 0x00, 0x00, 0x00, 0x02, 0x00, 0x01,
        (byte) 0xe5, 0x27, (byte) 0xde, (byte) 0xfc, // IDAT CRC


        0x00, 0x00, 0x00, 0x00, // IEND length (0)
        (byte) 'I', (byte) 'E', (byte) 'N', (byte) 'D', // Image end
        (byte) 0xae, (byte) 0x42, (byte) 0x60, (byte) 0x82 // IEND CRC
    };

    private final static int PLTE_CHUNK_START = 37; // after chunk length
    private final static int PLTE_CHUNK_LENGTH = 7; // chunk name & data

    private final static int RED_IDX = 4;
    private final static int GREEN_IDX = RED_IDX + 1;
    private final static int BLUE_IDX = GREEN_IDX + 1;

    private final CRC32 mCRC = new CRC32();

    /**
     * Creates a ColorDroplet.
     */
    public ColorServlet() {
        super();
    }

    /**
     * Renders the 1 x 1 single color PNG to the response.
     *
     * @see ColorServlet class description
     *
     * @param pRequest the request
     * @param pResponse the response
     *
     * @throws IOException
     * @throws ServletException
     */
    public void service(ServletRequest pRequest, ServletResponse pResponse) throws IOException, ServletException {

        int red = 0;
        int green = 0;
        int blue = 0;

        // Get color parameter and parse color
        String rgb = pRequest.getParameter(RGB_PARAME);
        if (rgb != null && rgb.length() >= 6 && rgb.length() <= 7) {
            int index = 0;

            // If the hash ('#') character is included, skip it.
            if (rgb.length() == 7) {
                index++;
            }

            try {
                // Two digit hex for each color
                String r = rgb.substring(index, index += 2);
                red = Integer.parseInt(r, 0x10);

                String g = rgb.substring(index, index += 2);
                green = Integer.parseInt(g, 0x10);

                String b = rgb.substring(index, index += 2);
                blue = Integer.parseInt(b, 0x10);
            }
            catch (NumberFormatException nfe) {
                log("Wrong color format for ColorDroplet: " + rgb + ". Must be RRGGBB.");
            }
        }

        // Set MIME type for PNG
        pResponse.setContentType("image/png");
        ServletOutputStream out = pResponse.getOutputStream();

        try {
            // Write header (and palette chunk length)
            out.write(PNG_IMG, 0, PLTE_CHUNK_START);

            // Create palette chunk, excl lenght, and write
            byte[] palette = makePalette(red, green, blue);
            out.write(palette);

            // Write image data until end
            int pos = PLTE_CHUNK_START + PLTE_CHUNK_LENGTH + 4;
            out.write(PNG_IMG, pos, PNG_IMG.length - pos);
        }
        finally {
            out.flush();
        }
    }

    /**
     * Updates the CRC for a byte array. Note that the byte array must be at
     * least {@code pOff + pLen + 4} bytes long, as the CRC is stored in the
     * 4 last bytes.
     *
     * @param pBytes the bytes to create CRC for
     * @param pOff the offset into the byte array to create CRC for
     * @param pLen the length of the byte array to create CRC for
     */
    private void updateCRC(byte[] pBytes, int pOff, int pLen) {
        int value;

        synchronized (mCRC) {
            mCRC.reset();
            mCRC.update(pBytes, pOff, pLen);
            value = (int) mCRC.getValue();
        }

        pBytes[pOff + pLen    ] = (byte) ((value >> 24) & 0xff);
        pBytes[pOff + pLen + 1] = (byte) ((value >> 16) & 0xff);
        pBytes[pOff + pLen + 2] = (byte) ((value >>  8) & 0xff);
        pBytes[pOff + pLen + 3] = (byte) ( value        & 0xff);
    }

    /**
     * Creates a PNG palette (PLTE) chunk with one color.
     * The palette chunk data is always 3 bytes in length (one byte per color
     * component).
     * The returned byte array is then {@code 4 + 3 + 4 = 11} bytes,
     * including chunk header, data and CRC.
     *
     * @param pRed the red component
     * @param pGreen the reen component
     * @param pBlue the blue component
     *
     * @return the bytes for the PLTE chunk, including CRC (but not length)
     */
    private byte[] makePalette(int pRed, int pGreen, int pBlue) {
        byte[] palette = new byte[PLTE_CHUNK_LENGTH + 4];
        System.arraycopy(PNG_IMG, PLTE_CHUNK_START, palette, 0, PLTE_CHUNK_LENGTH);

        palette[RED_IDX]   = (byte) pRed;
        palette[GREEN_IDX] = (byte) pGreen;
        palette[BLUE_IDX]  = (byte) pBlue;

        updateCRC(palette, 0, PLTE_CHUNK_LENGTH);

        return palette;
    }
}
