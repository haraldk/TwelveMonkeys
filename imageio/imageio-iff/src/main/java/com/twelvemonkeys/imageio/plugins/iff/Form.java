package com.twelvemonkeys.imageio.plugins.iff;

import javax.imageio.IIOException;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.List;

import static com.twelvemonkeys.imageio.plugins.iff.IFFUtil.toChunkStr;

/**
 * Form.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: Form.java,v 1.0 31/01/2022 haraldk Exp$
 */
abstract class Form {

    final int formType;
    final List<GenericChunk> meta = new ArrayList<>();

    Form(int formType) {
        this.formType = formType;
    }

    abstract int width();
    abstract int height();
    abstract int xAspect();
    abstract int yAspect();
    abstract int bitplanes();
    abstract int compressionType();

    boolean isMultiPalette() {
        return false;
    }

    boolean isHAM() {
        return false;
    }

    public boolean premultiplied() {
        return false;
    }

    public int sampleSize() {
        return 1;
    }

    public int transparentIndex() {
        return -1;
    }

    public IndexColorModel colorMap() throws IIOException {
        return null;
    }

    public ColorModel colorMapForRow(IndexColorModel colorModel, int row) {
        throw new UnsupportedOperationException();
    }

    abstract long bodyOffset();
    abstract long bodyLength();

    @Override
    public String toString() {
        return toChunkStr(formType);
    }

    Form with(final IFFChunk chunk) throws IIOException {
        if (chunk instanceof GenericChunk) {
            // TODO: This feels kind of hackish, as it breaks the immutable design, perhaps we should just reconsider...
            meta.add((GenericChunk) chunk);

            return this;
        }

        throw new IllegalArgumentException(chunk + " not supported in FORM type " + toChunkStr(formType));
    }

    static Form ofType(int formType) {
        switch (formType) {
            case IFF.TYPE_ACBM:
            case IFF.TYPE_ILBM:
            case IFF.TYPE_PBM:
            case IFF.TYPE_RGB8:
                return new ILBMForm(formType);
            case IFF.TYPE_DEEP:
            case IFF.TYPE_TVPP:
                return new DEEPForm(formType);
            default:
                throw new IllegalArgumentException("FORM type " + toChunkStr(formType) + " not supported");
        }
    }

    /**
     * The set of chunks used in the "original" ILBM,
     * and also ACBM, PBM and RGB8 FORMs.
     */
    static final class ILBMForm extends Form {
        private final BMHDChunk bitmapHeader;
        private final CAMGChunk viewMode;
        private final CMAPChunk colorMap;
        private final AbstractMultiPaletteChunk multiPalette;
        private final BODYChunk body;

        ILBMForm(int formType) {
            this(formType, null, null, null, null, null);
        }

        private ILBMForm(final int formType, final BMHDChunk bitmapHeader, final CAMGChunk viewMode, final CMAPChunk colorMap, final AbstractMultiPaletteChunk multiPalette, final BODYChunk body) {
            super(formType);
            this.bitmapHeader = bitmapHeader;
            this.viewMode = viewMode;
            this.colorMap = colorMap;
            this.multiPalette = multiPalette;
            this.body = body;
        }

        @Override
        int width() {
            return bitmapHeader.width;
        }

        @Override
        int height() {
            return bitmapHeader.height;
        }

        @Override
        int bitplanes() {
            return bitmapHeader.bitplanes;
        }

        @Override
        int compressionType() {
            return bitmapHeader.compressionType;
        }

        @Override
        int xAspect() {
            return bitmapHeader.xAspect;
        }

        @Override
        int yAspect() {
            return bitmapHeader.yAspect;
        }

        @Override
        boolean isMultiPalette() {
            return multiPalette != null;
        }

        boolean isEHB() {
            return viewMode != null && viewMode.isEHB();
        }

        @Override
        boolean isHAM() {
            return viewMode != null && viewMode.isHAM();
        }

        boolean isLaced() {
            return viewMode != null && viewMode.isLaced();
        }

        @Override
        public int transparentIndex() {
            return bitmapHeader.maskType == BMHDChunk.MASK_TRANSPARENT_COLOR ? bitmapHeader.transparentIndex : -1;
        }

        @Override
        public IndexColorModel colorMap() throws IIOException {
            return colorMap != null ? colorMap.getIndexColorModel(this) : null;
        }

        @Override
        public ColorModel colorMapForRow(final IndexColorModel colorModel, final int row) {
            return multiPalette != null ? multiPalette.getColorModel(colorModel, row, isLaced()) : null;
        }

        @Override
        long bodyOffset() {
            return body.chunkOffset;
        }

        @Override
        long bodyLength() {
            return body.chunkLength;
        }

        @Override
        ILBMForm with(final IFFChunk chunk) throws IIOException {
            if (chunk instanceof BMHDChunk) {
                if (bitmapHeader != null) {
                    throw new IIOException("Multiple BMHD chunks not allowed");
                }

                return new ILBMForm(formType, (BMHDChunk) chunk, null, colorMap, multiPalette, body);
            }
            else if (chunk instanceof CAMGChunk) {
                if (viewMode != null) {
                    throw new IIOException("Multiple CAMG chunks not allowed");
                }

                return new ILBMForm(formType, bitmapHeader, (CAMGChunk) chunk, colorMap, multiPalette, body);
            }
            else if (chunk instanceof CMAPChunk) {
                if (colorMap != null) {
                    throw new IIOException("Multiple CMAP chunks not allowed");
                }

                return new ILBMForm(formType, bitmapHeader, viewMode, (CMAPChunk) chunk, multiPalette, body);
            }
            else if (chunk instanceof AbstractMultiPaletteChunk) {
                // NOTE: We prefer PHCG over SHAM/CTBL style palette changes, if both are present
                if (multiPalette instanceof PCHGChunk) {
                    if (chunk instanceof PCHGChunk) {
                        throw new IIOException("Multiple PCHG/SHAM/CTBL chunks not allowed");
                    }

                    return this;
                }

                return new ILBMForm(formType, bitmapHeader, viewMode, colorMap, (AbstractMultiPaletteChunk) chunk, body);
            }
            else if (chunk instanceof BODYChunk) {
                if (body != null) {
                    throw new IIOException("Multiple " + toChunkStr(chunk.chunkId) + " chunks not allowed");
                }

                return new ILBMForm(formType, bitmapHeader, viewMode, colorMap, multiPalette, (BODYChunk) chunk);
            }
            else if (chunk instanceof GRABChunk) {
                // Ignored for now
                return this;
            }

            return (ILBMForm) super.with(chunk);
        }

        @Override
        public String toString() {
            return super.toString() + '{' + bitmapHeader +
                    (viewMode != null ? ", " + viewMode : "" ) +
                    (colorMap != null ? ", " + colorMap : "" ) +
                    (multiPalette != null ? ", " + multiPalette : "" ) +
                    '}';
        }
    }

    /**
     * The set of chunks used in DEEP and TVPP FORMs.
     */
    private static final class DEEPForm extends Form {
        private final DGBLChunk deepGlobal;
        private final DLOCChunk deepLocation;
        private final DPELChunk deepPixel;
        private final BODYChunk body;

        DEEPForm(int formType) {
            this(formType, null, null, null, null);
        }

        private DEEPForm(final int formType, final DGBLChunk deepGlobal, final DLOCChunk deepLocation, final DPELChunk deepPixel, final BODYChunk body) {
            super(formType);
            this.deepGlobal = deepGlobal;
            this.deepLocation = deepLocation;
            this.deepPixel = deepPixel;
            this.body = body;
        }


        @Override
        int width() {
            return deepLocation.width;
        }

        @Override
        int height() {
            return deepLocation.height;
        }

        @Override
        int bitplanes() {
            return deepPixel.bitsPerPixel();
        }

        @Override
        public int sampleSize() {
            return bitplanes() / 8;
        }

        @Override
        public boolean premultiplied() {
            return true;
        }

        @Override
        int compressionType() {
            return deepGlobal.compressionType;
        }

        @Override
        int xAspect() {
            return deepGlobal.xAspect;
        }

        @Override
        int yAspect() {
            return deepGlobal.yAspect;
        }

        @Override
        long bodyOffset() {
            return body.chunkOffset;
        }

        @Override
        long bodyLength() {
            return body.chunkLength;
        }

        @Override
        DEEPForm with(final IFFChunk chunk) throws IIOException {
            if (chunk instanceof DGBLChunk) {
                if (deepGlobal != null) {
                    throw new IIOException("Multiple DGBL chunks not allowed");
                }

                return new DEEPForm(formType, (DGBLChunk) chunk, null, null, body);
            }
            else if (chunk instanceof DLOCChunk) {
                if (deepLocation != null) {
                    throw new IIOException("Multiple DLOC chunks not allowed");
                }

                return new DEEPForm(formType, deepGlobal, (DLOCChunk) chunk, deepPixel, body);
            }
            else if (chunk instanceof DPELChunk) {
                if (deepPixel != null) {
                    throw new IIOException("Multiple DPEL chunks not allowed");
                }

                return new DEEPForm(formType, deepGlobal, deepLocation, (DPELChunk) chunk, body);
            }
            else if (chunk instanceof BODYChunk) {
                if (body != null) {
                    throw new IIOException("Multiple " + toChunkStr(chunk.chunkId) + " chunks not allowed");
                }

                return new DEEPForm(formType, deepGlobal, deepLocation, deepPixel, (BODYChunk) chunk);
            }

            return (DEEPForm) super.with(chunk);
        }

        @Override
        public String toString() {
            return super.toString() + '{' + deepGlobal + ", " + deepLocation + ", " + deepPixel + '}';
        }
    }
}
