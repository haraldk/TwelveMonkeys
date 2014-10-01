package com.twelvemonkeys.imageio.plugins.pnm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import com.twelvemonkeys.util.StringTokenIterator;

final class Plain16BitDecoder extends InputStream {
    private final BufferedReader reader;

    private StringTokenIterator currentLine;
    private int leftOver = -1;

    public Plain16BitDecoder(final InputStream in) {
        reader = new BufferedReader(new InputStreamReader(in, Charset.forName("ASCII")));
    }

    @Override public int read() throws IOException {
        if (leftOver != -1) {
            int next = leftOver;
            leftOver = -1;

            return next;
        }

        // Each number is one byte. Skip whitespace.
        if (currentLine == null || !currentLine.hasNext()) {
            String line = reader.readLine();
            if (line == null) {
                return -1;
            }

            currentLine = new StringTokenIterator(line);

            if (!currentLine.hasNext()) {
                return -1;
            }
        }

        int next = Integer.parseInt(currentLine.next()) & 0xffff;
        leftOver = next & 0xff;

        return (next >> 8) & 0xff;
    }

    @Override public void close() throws IOException {
        reader.close();
    }
}
