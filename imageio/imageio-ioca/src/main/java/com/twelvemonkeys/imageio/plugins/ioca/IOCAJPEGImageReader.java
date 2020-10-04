package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.ImageReaderDecorator;
import com.twelvemonkeys.imageio.color.ColorSpaces;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

class IOCAJPEGImageReader extends ImageReaderDecorator {

	private IOCAImageContent imageContent;

	IOCAJPEGImageReader(final ImageReader delegate, final IOCAImageContent imageContent) {
		super(delegate);
		this.imageContent = imageContent;
	}

	@Override
	public BufferedImage read(final int imageIndex, ImageReadParam param) throws IOException {
		final BufferedImage destination = getDestination(param, getImageTypes(imageIndex),
				getWidth(imageIndex), getHeight(imageIndex));

		if (null == param) {
			param = getDefaultReadParam();
		}

		param.setDestination(destination);
		return delegate.read(imageIndex, param);
	}

	@Override
	public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
		return Collections.singletonList(getRawImageType(imageIndex)).iterator();
	}

	@Override
	public ImageTypeSpecifier getRawImageType(final int imageIndex) throws IOException {
		final IOCAIdeStructure ideStructure = imageContent.getIdeStructure();

		// If the IDE Structure Parameter is absent,
		// the image is assumed to be bilevel if the IDE size is 1
		// and grayscale otherwise.
		// See: "Appendix B. Bilevel, Grayscale, and Color Images: Related Image Data Parameters"
		// and "Table 19. Valid Compression Algorithms for Each Data Type".
		if (null == ideStructure) {
			if (imageContent.getIdeSize() == 1) {
				throw new IIOException("JPEG compression doesn't support bi-level color.");
			}

			// Should be YCrCb with only the Y component set according to the spec, but this
			// should equivalent.
			if (imageContent.getIdeSize() == 8) {
				return ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY);
			}

			// 16-bit grayscale is also possible.
			if (imageContent.getIdeSize() == 16) {
				return ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_USHORT_GRAY);
			}

			throw new IIOException(String.format("Unsupported IDE size for JPEG with no explicit color space: %d.",
					imageContent.getIdeSize()));
		}

		// 8-bit grayscale (just the Y channel in one byte).
		if (ideStructure.is8BitGrayscale()) {
			return ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY);
		}

		// Standard 24-bit RGB (each channel occupying 8 bits).
		// Java packs all the bits into a 32-bit int, hence "INT_RGB" (it reserves the extra 8 bits in case
		// transparency is needed).
		if (ideStructure.is24BitRGB()) {
			return ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
		}

		// Standard 24-bit YCbCr (each component occupying 8 bits).
		// Java stores each component in a separate byte internally, hence TYPE_BYTE.
		if (ideStructure.is24BitYCbCr()) {
			return ImageTypeSpecifier.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_PYCC),
					new int[] {0, 1, 2}, DataBuffer.TYPE_BYTE, false, false);
		}

		// Standard 32-bit CMYK.
		if (ideStructure.is32BitCMYK()) {
			return ImageTypeSpecifier.createInterleaved(ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK),
					new int[] {3, 2, 1, 0}, DataBuffer.TYPE_BYTE, false, false);
		}

		throw new IIOException("Unsupported format colour model for JPEG compression.");
	}
}
