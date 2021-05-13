/*
 * Copyright (c) 2011, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.icns;

import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.io.FileUtil;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

/**
 * QuickFix for OS X (where ICNS are most useful) and JPEG 2000.
 * Dumps the stream to disk and converts using sips command line tool:
 * {@code sips -s format png <temp>}.
 * Reads image back using ImageIO and known format (png).
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEG2000Reader.java,v 1.0 25.11.11 14:17 haraldk Exp$
 */
final class SipsJP2Reader {

    private static final File SIPS_COMMAND = new File("/usr/bin/sips");
    private static final boolean SIPS_EXISTS_AND_EXECUTES = existsAndExecutes(SIPS_COMMAND);
    private static final boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.icns.debug"));

    private static boolean existsAndExecutes(final File cmd) {
        try {
            return cmd.exists() && cmd.canExecute();
        }
        catch (SecurityException ignore) {
            if (DEBUG) {
                ignore.printStackTrace();
            }
        }

        return false;
    }

    private ImageInputStream input;

    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
        // Test if we have sips before dumping to be fail-fast
        if (SIPS_EXISTS_AND_EXECUTES) {
            File tempFile = dumpToFile(input);

            if (convertToPNG(tempFile)) {
                ImageInputStream stream = ImageIO.createImageInputStream(tempFile);
                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

                while (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    reader.setInput(stream);

                    try {
                        return reader.read(imageIndex, param);
                    }
                    catch (IOException ignore) {
                        if (stream.getFlushedPosition() <= 0) {
                            stream.seek(0);
                        }
                        else {
                            stream.close();
                            stream = ImageIO.createImageInputStream(tempFile);
                        }
                    }
                    finally {
                        reader.dispose();
                    }
                }
            }
        }

        return null;
    }

    public void setInput(final ImageInputStream input) {
        this.input = input;
    }

    private static boolean convertToPNG(final File tempFile) throws IIOException {
        try {
            Process process = Runtime.getRuntime().exec(buildCommand(SIPS_COMMAND, tempFile));

            // NOTE: sips return status is 0, even if error, need to check error message
            int status = process.waitFor();
            String message = checkErrorMessage(process);
            
            if (status == 0 && message == null) {
                return true;
            }
            else {
                throw new IOException(message);
            }
        }
        catch (InterruptedException e) {
            throw new IIOException("Interrupted converting JPEG 2000 format", e);
        }
        catch (SecurityException e) {
            // Exec might need permissions in sandboxed environment
            throw new IIOException("Cannot convert JPEG 2000 format without file permissions", e);
        }
        catch (IOException e) {
            throw new IIOException("Error converting JPEG 2000 format: " + e.getMessage(), e);
        }
    }

    private static String checkErrorMessage(final Process process) throws IOException {
        InputStream stream = process.getErrorStream();
        
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String message = reader.readLine();

            return message != null && message.startsWith("Error: ") ? message.substring(7) : null;
        }
        finally {
            stream.close();
        }
    }

    private static String[] buildCommand(final File sipsCommand, final File tempFile) {
        return new String[]{
                sipsCommand.getAbsolutePath(), "-s", "format", "png", tempFile.getAbsolutePath()
        };
    }


    private static File dumpToFile(final ImageInputStream stream) throws IOException {
        File tempFile = File.createTempFile("imageio-icns-", ".png");
        tempFile.deleteOnExit();

        FileOutputStream out = new FileOutputStream(tempFile);

        try {
            FileUtil.copy(IIOUtil.createStreamAdapter(stream), out);
        }
        finally {
            out.close();
        }

        return tempFile;
    }

    static boolean isAvailable() {
        return SIPS_EXISTS_AND_EXECUTES;
    }
}
