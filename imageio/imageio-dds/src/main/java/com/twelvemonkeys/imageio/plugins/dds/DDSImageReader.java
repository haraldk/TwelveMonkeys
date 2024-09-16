package com.twelvemonkeys.imageio.plugins.dds;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.image.BufferedImage;
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
	public ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
		checkBounds(imageIndex);
		readHeader();

		// TODO change based on format DXT1 4bpp / DXT1-nonalpha


		return ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);
	}

	@Override
	public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
		return Collections.singletonList(getRawImageType(imageIndex)).iterator();
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

		DDSReader dds = new DDSReader(header);
		int[] pixels = dds.read(buffer, 0);
		destination.setRGB(0, 0, width, height, pixels, 0, width);

		processImageComplete();

		return destination;
	}

	private void readHeader() throws IOException {
		if (header == null) {
			header = DDSHeader.read(imageInput);

			imageInput.flushBefore(imageInput.getStreamPosition());
		}

		imageInput.seek(imageInput.getFlushedPosition());
	}

	public static void main(final String[] args) throws IOException {
		File file = new File("imageio/imageio-dds/src/test/resources/dds/stones.dxt5.dds");
		//File file = new File("imageio/imageio-dds/src/test/resources/dds/dxt1-noalpha.dds");

		BufferedImage image = ImageIO.read(file);

		showIt(image, file.getName());
	}
}
