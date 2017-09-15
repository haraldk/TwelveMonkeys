package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.plugins.tiff.CCITTFaxDecoderStream;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;

import java.io.EOFException;
import java.io.IOException;

import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;

class CCITTFaxImageReader extends ImageReaderBase {

	// TODO: when this reader matures so that it can read the G3 and G4 file format instead of an unwrapped byte
	// stream, it should have a constructor which takes just the provider, like most ImageReaders.

	private final int width, height, bitOrder, type;

	CCITTFaxImageReader(final int width, final int height, final int bitOrder, final int type) {
		super(new CCITTFaxImageReaderSpi());
		this.width = width;
		this.height = height;
		this.type = type;
		this.bitOrder = bitOrder;
	}

	@Override
	public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
		checkBounds(imageIndex);
		assertInput();

		final BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);
		final DataBuffer raster = destination.getRaster().getDataBuffer();

		processImageStarted(imageIndex);

		// Cheat: the TIFF fill order constant values are really just IOCA bit order + 1.
		final InputStream decoder = new CCITTFaxDecoderStream((InputStream) getInput(),
				width, type, bitOrder + 1, 0L);

		for (int b, i = 0, l = raster.getSize(); i < l; i++) {
			b = decoder.read();
			if (b < 0) {
				throw new EOFException();
			}

			// Invert the colour.
			// For some reason the decoder uses an inverted mapping.
			raster.setElem(i, (~b) & 0x00FF);
			if (abortRequested()) {
				break;
			}
		}

		if (abortRequested()) {
			processReadAborted();
		} else {
			processImageProgress(100F);
		}

		processImageComplete();
		return destination;
	}

	@Override
	public boolean canReadRaster() {
		return false;
	}

	@Override
	public Raster readRaster(int imageIndex, ImageReadParam param) throws IOException {
		checkBounds(imageIndex);
		return read(imageIndex, param).getData();
	}

	@Override
	public ImageTypeSpecifier getRawImageType(final int imageIndex) throws IOException {
		checkBounds(imageIndex);

		// TODO: G4 MMR supports 4 bit CMYK and and 1-bit YCrCb.
		// See "Table 19. Valid Compression Algorithms for Each Data Type".
		return ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_BINARY);
	}

	@Override
	public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
		checkBounds(imageIndex);
		return Collections.singletonList(getRawImageType(imageIndex)).iterator();
	}

	@Override
	public int getNumImages(final boolean allowSearch) throws IOException {
		return 1;
	}

	@Override
	public int getHeight(final int imageIndex) throws IOException {
		checkBounds(imageIndex);
		return height;
	}

	@Override
	public int getWidth(final int imageIndex) throws IOException {
		checkBounds(imageIndex);
		return width;
	}

	@Override
	protected void resetMembers() {

		// No members.
	}
}
