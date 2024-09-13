package com.twelvemonkeys.imageio.plugins.dds;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

public final class DDSImageReader extends ImageReaderBase {

	private DDSHeader header;

	public DDSImageReader(final ImageReaderSpi provider) {
		super(provider);
	}

	@Override
	protected void resetMembers() {
		header = null;
	}

	@Override
	public int getWidth(final int imageIndex) throws IOException {
		checkBounds(imageIndex);
		readHeader();

		return header.getWidth();
	}

	@Override
	public int getHeight(int imageIndex) throws IOException {
		checkBounds(imageIndex);
		readHeader();

		return header.getHeight();
	}

	@Override
	public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
		checkBounds(imageIndex);
		readHeader();

		// TODO changes based on format
		ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
		return Collections.singletonList(ImageTypeSpecifiers.createInterleaved(sRGB, new int[]{0, 1, 2}, DataBuffer.TYPE_FLOAT, false, false)).iterator();
	}

	@Override
	public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
		checkBounds(imageIndex);
		readHeader();

		int width = getWidth(imageIndex);
		int height = getHeight(imageIndex);

		BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);

		// TODO
		byte[] buffer = new byte[width * height * 4];
		imageInput.read(buffer);

		int[] pixels = DDSReader.read(buffer, DDSReader.ARGB, 0);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, width, height, pixels, 0, width);

		processImageComplete();

		return image;
	}

	private void readHeader() throws IOException {
		if (header == null) {
			header = DDSHeader.read(imageInput);

			imageInput.flushBefore(imageInput.getStreamPosition());
		}

		imageInput.seek(imageInput.getFlushedPosition());
	}

	public static void main(final String[] args) throws IOException {
		File file = new File("imageio/imageio-dds/src/test/resources/dds/dxt5.dds");

		BufferedImage image = ImageIO.read(file);

		showIt(image, file.getName());
	}
}
