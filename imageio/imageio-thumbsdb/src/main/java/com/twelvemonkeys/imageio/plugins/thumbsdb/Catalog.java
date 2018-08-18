/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.thumbsdb;

import com.twelvemonkeys.io.LittleEndianDataInputStream;
import com.twelvemonkeys.io.ole2.CompoundDocument;
import com.twelvemonkeys.lang.StringUtil;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;

/**
 * Represents a {@code Catalog} structure, typically found in a {@link com.twelvemonkeys.io.ole2.CompoundDocument}. 
 *
 * @see <a href="http://www.petedavis.net/MySite/DynPageView.aspx?pageid=31">PeteDavis.NET</a>
 *
 * @author <a href="mailto:harald.kuhr@gmail.no">Harald Kuhr</a>
 * @author last modified by $Author: haku$
 * @version $Id: Catalog.java,v 1.0 01.feb.2007 17:19:59 haku Exp$
 */
// TODO: Consider moving this one to io.ole2
public final class Catalog implements Iterable<Catalog.CatalogItem> {

    private final CatalogHeader header;
    private final CatalogItem[] items;

    Catalog(final CatalogHeader pHeader, final CatalogItem[] pItems) {
        header = pHeader;
        items = pItems;
    }

    /**
     * Reads the {@code Catalog} entry from the given input stream.
     *
     * @param pInput the input stream
     * @return a new {@code Catalog}
     *
     * @throws java.io.IOException if an I/O exception occurs during read
     */
    public static Catalog read(final InputStream pInput) throws IOException {
        DataInput dataInput = new LittleEndianDataInputStream(pInput);
        return read(dataInput);
    }

    /**
     * Reads the {@code Catalog} entry from the given input stream.
     * <p/>
     * The data is assumed to be in little endian byte order.
     *
     * @param pDataInput the input stream
     * @return a new {@code Catalog}
     *
     * @throws java.io.IOException if an I/O exception occurs during read
     */
    public static Catalog read(final DataInput pDataInput) throws IOException {
        CatalogHeader header = CatalogHeader.read(pDataInput);

        CatalogItem[] items = new CatalogItem[header.getThumbnailCount()];
        for (int i = 0; i < header.getThumbnailCount(); i++) {
            CatalogItem item = CatalogItem.read(pDataInput);
            //System.out.println("item: " + item);
            items[item.getItemId() - 1] = item;
        }

        return new Catalog(header, items);
    }

    public final int getThumbnailCount() {
        return header.mThumbCount;
    }

    public final int getMaxThumbnailWidth() {
        return header.mThumbWidth;
    }

    public final int getMaxThumbnailHeight() {
        return header.mThumbHeight;
    }

    final CatalogItem getItem(final int pIndex) {
        return items[pIndex];
    }

    final CatalogItem getItem(final String pName) {
        return items[getIndex(pName)];
    }

    final int getItemId(final int pIndex) {
        return items[pIndex].getItemId();
    }

    public final int getIndex(final String pName) {
        for (int i = 0; i < items.length; i++) {
            CatalogItem item = items[i];

            if (item.getName().equals(pName)) {
                return i;
            }
        }

        return -1;
    }

    public final String getStreamName(final int pIndex) {
        return StringUtil.reverse(String.valueOf(getItemId(pIndex)));
    }

    public final String getName(String pStreamName) {
        return getName(Integer.parseInt(StringUtil.reverse(pStreamName)));
    }

    final String getName(int pItemId) {
        return items[pItemId - 1].getName();
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), header);
    }

    public Iterator<CatalogItem> iterator() {
        return new Iterator<CatalogItem>() {
            int mCurrentIdx;

            public boolean hasNext() {
                return mCurrentIdx < items.length;
            }

            public CatalogItem next() {
                return items[mCurrentIdx++];
            }

            public void remove() {
                throw new UnsupportedOperationException("Remove not supported");
            }
        };
    }

    private static class CatalogHeader {
        short mReserved1;
        short mReserved2;
        int mThumbCount;
        int mThumbWidth;
        int mThumbHeight;

        CatalogHeader() {
        }

        public static CatalogHeader read(final DataInput pDataInput) throws IOException {
            CatalogHeader header = new CatalogHeader();

            header.mReserved1 = pDataInput.readShort();
            header.mReserved2 = pDataInput.readShort();
            header.mThumbCount = pDataInput.readInt();
            header.mThumbWidth = pDataInput.readInt();
            header.mThumbHeight = pDataInput.readInt();

            return header;
        }

        public int getThumbnailCount() {
            return mThumbCount;
        }

        public int getThumbHeight() {
            return mThumbHeight;
        }

        public int getThumbWidth() {
            return mThumbWidth;
        }

        @Override
        public String toString() {
            return String.format(
                    "%s: %s %s thumbs: %d maxWidth: %d maxHeight: %d",
                    getClass().getSimpleName(),  mReserved1, mReserved2, mThumbCount, mThumbWidth, mThumbHeight
            );
        }
    }

    public static final class CatalogItem {
        int mReserved1;
        int mItemId; // Reversed stream name
        String mFilename;
        short mReserved2;
        private long mLastModified;

        private static CatalogItem read(final DataInput pDataInput) throws IOException {
            CatalogItem item = new CatalogItem();
            item.mReserved1 = pDataInput.readInt();
            item.mItemId = pDataInput.readInt();

            item.mLastModified = CompoundDocument.toJavaTimeInMillis(pDataInput.readLong());

            char[] chars = new char[256];
            char ch;
            int len = 0;
            while ((ch = pDataInput.readChar()) != 0) {
                chars[len++] = ch;
            }

            String name = new String(chars, 0, len);
            item.mFilename = StringUtil.getLastElement(name, "\\");

            item.mReserved2 = pDataInput.readShort();
            return item;
        }

        public String getName() {
            return mFilename;
        }

        public int getItemId() {
            return mItemId;
        }

        public long lastModified() {
            return mLastModified;
        }

        @Override
        public String toString() {
            return String.format(
                    "%s: %d itemId: %d lastModified: %s fileName: %s %s",
                    getClass().getSimpleName(), mReserved1, mItemId, new Date(mLastModified), mFilename, mReserved2
            );
        }
    }
}
