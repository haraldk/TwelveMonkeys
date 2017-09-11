package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.metadata.ioca.*;
import com.twelvemonkeys.imageio.plugins.tiff.CCITTFaxDecoderStream;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFBaseline;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFExtension;

import javax.imageio.*;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.*;
import java.util.*;

public final class IOCAImageReader extends ImageReaderBase {

	private List<List<SFD>> imageContentSFDs;

	IOCAImageReader(final ImageReaderSpi provider) {
		super(provider);
	}

	@Override
	public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
		buffer();

		SFD imageSize = null;
		SFD imageEncoding = null;

		final ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
		final List<SFD> imageContentSFDs = this.imageContentSFDs.get(imageIndex);

		for (SFD sfd : imageContentSFDs) {
			if (sfd.getCode() == IOCA.CODE_END_IMAGE_CONTENT) {
				break;
			}

			switch (sfd.getCode()) {
				case IOCA.CODE_IMAGE_SIZE_PARAMETER:
					imageSize = sfd;
					break;

				case IOCA.CODE_IMAGE_ENCODING_PARAMETER:
					imageEncoding = sfd;
					break;

				case IOCA.CODE_IMAGE_DATA:
					bos.write((byte[]) sfd.getEntryById(IOCA.FIELD_DATA).getValue());
					break;
			}
		}

		if (null == imageSize || null == imageEncoding) {
			throw new IOException("No image size or encoding field defined.");
		}

		return parseUntiledImage(bos.toByteArray(), imageEncoding, imageSize);
	}

	private BufferedImage parseUntiledImage(final byte[] buffer, final SFD imageEncoding, final SFD imageSize)
			throws IOException {
		final int compressionId = (int) imageEncoding.getEntryById(IOCA.FIELD_COMPRID).getValue();
		final InputStream bis = new ByteArrayInputStream(buffer);
		final ImageReader reader;

		switch (compressionId) {
			case IOCA.COMPRID_G3_MH:
				return decodeCCITTFax(bis, TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE, imageEncoding, imageSize);

			case IOCA.COMPRID_G3_MR:
				return decodeCCITTFax(bis, TIFFExtension.COMPRESSION_CCITT_T4, imageEncoding, imageSize);

			case IOCA.COMPRID_IBM_MMR:
			case IOCA.COMPRID_G4_MMR:
				return decodeCCITTFax(bis, TIFFExtension.COMPRESSION_CCITT_T6, imageEncoding, imageSize);
		}

		reader = createReader(compressionId, bis);

		try {
			return reader.read(0);
		} finally {
			reader.dispose();
		}
	}

	private ImageReader createReader(final int compressionId, final InputStream in) throws IOException {
		final Iterator<ImageReader> readers;

		switch (compressionId) {
			case IOCA.COMPRID_TIFF_2:
			case IOCA.COMPRID_TIFF_LZW:
			case IOCA.COMPRID_TIFF_PB:
				readers = ImageIO.getImageReadersByMIMEType("image/tiff");
				break;

			case IOCA.COMPRID_JPEG:
				readers = ImageIO.getImageReadersByMIMEType("image/jpeg");
				break;

			case IOCA.COMPRID_JBIG2:
				readers = ImageIO.getImageReadersByMIMEType("image/jbig2");
				break;

			default:
				throw new IOException(String.format("Unknown compression ID: %02x", compressionId));
		}

		if (!readers.hasNext()) {
			throw new IOException(String.format("Unable to read image of type: %02x", compressionId));
		}

		final ImageReader reader = readers.next();

		reader.setInput(in);
		return reader;
	}

	private BufferedImage decodeCCITTFax(final InputStream in, final int type, final SFD imageEncoding,
	                                     final SFD imageSize)
			throws IOException {
		int fillOrder;
		byte bitOrder = (byte) imageEncoding.getEntryById(IOCA.FIELD_BITORDR).getValue();

		int width = (int) imageSize.getEntryById(IOCA.FIELD_HSIZE).getValue();
		int height = (int) imageSize.getEntryById(IOCA.FIELD_VSIZE).getValue();

		if (bitOrder == 0x00) {
			fillOrder = TIFFBaseline.FILL_LEFT_TO_RIGHT;
		} else if (bitOrder == 0x01) {
			fillOrder = TIFFExtension.FILL_RIGHT_TO_LEFT;
		} else {
			throw new IOException("Invalid bit order: " + bitOrder + ".");
		}

		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
		final byte[] raster = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

		final CCITTFaxDecoderStream decoder = new CCITTFaxDecoderStream(in, width, type, fillOrder, 0L);

		for (int b, i = 0, l = raster.length; i < l; i++) {
			b = decoder.read();
			if (b < 0) {
				throw new EOFException();
			}

			// Invert the colour. For some reason the decoder uses an inverted mapping.
			raster[i] = (byte) ((~b) & 0x00FF);
		}

		return image;
	}

	@Override
	public boolean canReadRaster() {
		return true;
	}

	@Override
	public Raster readRaster(int imageIndex, ImageReadParam param) throws IOException {
		return read(imageIndex, param).getData();
	}

	@Override
	public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
		final int compressionId = (int) fetchSFD(imageIndex, IOCA.CODE_IMAGE_ENCODING_PARAMETER)
				.getEntryById(IOCA.FIELD_COMPRID).getValue();
		final List<ImageTypeSpecifier> types = new ArrayList<>();

		switch (compressionId) {
			case IOCA.COMPRID_G3_MH:
			case IOCA.COMPRID_G3_MR:
			case IOCA.COMPRID_G4_MMR:
			case IOCA.COMPRID_IBM_MMR:
			case IOCA.COMPRID_JBIG2:
				types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_BINARY));
				break;

			case IOCA.COMPRID_TIFF_2:
			case IOCA.COMPRID_TIFF_PB:
				types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_BINARY));
				types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY));
				types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB));
				break;

			case IOCA.COMPRID_JPEG:
				types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY));
				types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB));
				break;
		}

		return types.iterator();
	}

	@Override
	public int getNumImages(final boolean allowSearch) throws IOException {
		if (!allowSearch && null == imageContentSFDs) {
			return -1;
		}

		buffer();
		return imageContentSFDs.size();
	}

	@Override
	public int getHeight(int imageIndex) throws IOException {
		return (int) fetchSFD(imageIndex, IOCA.CODE_IMAGE_SIZE_PARAMETER).getEntryById(IOCA.FIELD_VSIZE).getValue();
	}

	@Override
	public int getWidth(int imageIndex) throws IOException {
		return (int) fetchSFD(imageIndex, IOCA.CODE_IMAGE_SIZE_PARAMETER).getEntryById(IOCA.FIELD_HSIZE).getValue();
	}

	@Override
	protected void resetMembers() {
		imageContentSFDs = null;
	}

	private void buffer() throws IOException {
		if (imageInput == null) {
			throw new IllegalStateException("Input not set.");
		}

		if (imageContentSFDs != null) {
			return;
		}

		final IOCADirectory SFDs = (IOCADirectory) new IOCAReader().read(imageInput);

		this.imageContentSFDs = new LinkedList<>();

		for (int i = 0, l = SFDs.directoryCount(); i < l; i++) {
			SFD sfd = (SFD) SFDs.getDirectory(i);

			if (sfd.getCode() != IOCA.CODE_BEGIN_IMAGE_CONTENT) {
				continue;
			}

			final LinkedList<SFD> imageContentSFDs = new LinkedList<>();

			while ((sfd = (SFD) SFDs.getDirectory(i++)).getCode() != IOCA.CODE_END_IMAGE_CONTENT) {
				imageContentSFDs.add(sfd);
			}

			this.imageContentSFDs.add(imageContentSFDs);
		}
	}

	private SFD fetchSFD(int imageIndex, final short code) throws IOException {
		buffer();

		for (final SFD sfd : this.imageContentSFDs.get(imageIndex)) {
			if (sfd.getCode() == code) {
				return sfd;
			}
		}

		throw new IndexOutOfBoundsException();
	}
}
