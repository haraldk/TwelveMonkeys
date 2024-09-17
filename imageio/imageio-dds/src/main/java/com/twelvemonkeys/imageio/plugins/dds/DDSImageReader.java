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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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

		DDSReader dds = new DDSReader(header);
		int[] pixels = dds.read(imageInput, 0);
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

		String parentDir = "imageio/imageio-dds/src/test/resources/dds";

		List<File> testFiles = new ArrayList<>();
		testFiles.add(new File(parentDir, "dds_A1R5G5B5.dds"));
		testFiles.add(new File(parentDir, "dds_A1R5G5B5_mipmap.dds"));
		testFiles.add(new File(parentDir, "dds_A4R4G4B4.dds"));
		testFiles.add(new File(parentDir, "dds_A4R4G4B4_mipmap.dds"));
		testFiles.add(new File(parentDir, "dds_A8B8G8R8.dds"));
		testFiles.add(new File(parentDir, "dds_A8B8G8R8_mipmap.dds"));
		testFiles.add(new File(parentDir, "dds_A8R8G8B8.dds"));
		testFiles.add(new File(parentDir, "dds_A8R8G8B8_mipmap.dds"));
		testFiles.add(new File(parentDir, "dds_DXT1.dds"));
		testFiles.add(new File(parentDir, "dds_DXT1_mipmap.dds"));
		testFiles.add(new File(parentDir, "dds_DXT2.dds"));
		testFiles.add(new File(parentDir, "dds_DXT2_mipmap.dds"));
		testFiles.add(new File(parentDir, "dds_DXT3.dds"));
		testFiles.add(new File(parentDir, "dds_DXT3_mipmap.dds"));
		testFiles.add(new File(parentDir, "dds_DXT4.dds"));
		testFiles.add(new File(parentDir, "dds_DXT4_mipmap.dds"));
		testFiles.add(new File(parentDir, "dds_DXT5.dds"));
		testFiles.add(new File(parentDir, "dds_DXT5_mipmap.dds"));
		testFiles.add(new File(parentDir, "dds_R5G6B5.dds"));
		testFiles.add(new File(parentDir, "dds_R5G6B5_mipmap.dds"));
		testFiles.add(new File(parentDir, "dds_R8G8B8.dds"));
		testFiles.add(new File(parentDir, "dds_R8G8B8_mipmap.dds"));
		testFiles.add(new File(parentDir, "dds_X1R5G5B5.dds"));
		testFiles.add(new File(parentDir, "dds_X1R5G5B5_mipmap.dds"));
		testFiles.add(new File(parentDir, "dds_X4R4G4B4.dds"));
		testFiles.add(new File(parentDir, "dds_X4R4G4B4_mipmap.dds"));
		testFiles.add(new File(parentDir, "dds_X8B8G8R8.dds"));
		testFiles.add(new File(parentDir, "dds_X8B8G8R8_mipmap.dds"));
		testFiles.add(new File(parentDir, "dds_X8R8G8B8.dds"));
		testFiles.add(new File(parentDir, "dds_X8R8G8B8_mipmap.dds"));

		for (File file : testFiles) {
			BufferedImage image = ImageIO.read(file);
			showIt(image, file.getName());
		}

	}
}
