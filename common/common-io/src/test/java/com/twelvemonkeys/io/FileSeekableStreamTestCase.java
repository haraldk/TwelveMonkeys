package com.twelvemonkeys.io;

import java.io.*;

/**
 * MemoryCacheSeekableStreamTestCase
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/FileSeekableStreamTestCase.java#3 $
 */
public class FileSeekableStreamTestCase extends SeekableInputStreamAbstractTestCase {
    public FileSeekableStreamTestCase(String name) {
        super(name);
    }

    protected SeekableInputStream makeInputStream(final InputStream pStream) {
        try {
            return new FileSeekableStream(createFileWithContent(pStream));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File createFileWithContent(final InputStream pStream) throws IOException {
        File temp = File.createTempFile("tm-io-junit", null);
        temp.deleteOnExit();
        OutputStream os = new FileOutputStream(temp);
        try {
            FileUtil.copy(pStream, os);
        }
        finally {
            os.close();
            pStream.close();
        }
        return temp;
    }

    @Override
    public void testCloseUnderlyingStream() throws IOException {
        // There is no underlying stream here...
    }

    public void testCloseUnderlyingFile() throws IOException {
        final boolean[] closed = new boolean[1];

        File file = createFileWithContent(new ByteArrayInputStream(makeRandomArray(256)));

        RandomAccessFile raf = new RandomAccessFile(file, "r") {
            @Override
            public void close() throws IOException {
                closed[0] = true;
                super.close();
            }
        };

        FileSeekableStream stream = new FileSeekableStream(raf);

        try {
            FileUtil.read(stream); // Read until EOF

            assertEquals("EOF not reached (test case broken)", -1, stream.read());
            assertFalse("Underlying stream closed before close", closed[0]);
        }
        finally {
            stream.close();
        }

        assertTrue("Underlying stream not closed", closed[0]);
    }
}
