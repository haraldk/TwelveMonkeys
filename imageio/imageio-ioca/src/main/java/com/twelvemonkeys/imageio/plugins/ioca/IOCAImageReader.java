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

	private final static boolean DEBUG = true; //"true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.ioca.debug"));

	private LinkedList<IOCASegment> segments;
	private List<IOCAImageContent> imageContents;

	private IOCASegment segment;
	private IOCAImageContent imageContent;

	IOCAImageReader(final ImageReaderSpi provider) {
		super(provider);
	}

	@Override
	public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
		buffer();
		checkBounds(imageIndex);

		final IOCAImageContent imageContent = imageContents.get(imageIndex);

		final short compressionId = imageContent.getImageEncoding().getCompressionId();
		final InputStream bis = new ByteArrayInputStream(imageContent.getData());
		final ImageReader reader;
		final Iterator<ImageReader> readers;

		switch (compressionId) {
			case IOCA.COMPRID_G3_MH:
				return decodeCCITTFax(bis, TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE, imageContent);

			case IOCA.COMPRID_G3_MR:
				return decodeCCITTFax(bis, TIFFExtension.COMPRESSION_CCITT_T4, imageContent);

			case IOCA.COMPRID_IBM_MMR:
			case IOCA.COMPRID_G4_MMR:
				return decodeCCITTFax(bis, TIFFExtension.COMPRESSION_CCITT_T6, imageContent);

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
				throw new IIOException(String.format("Unknown compression ID: %02x", compressionId));
		}

		if (!readers.hasNext()) {
			throw new IIOException(String.format("Unable to read image of type: %02x", compressionId));
		}

		reader = readers.next();
		reader.setInput(bis);

		try {
			return reader.read(0);
		} finally {
			reader.dispose();
		}
	}

	private BufferedImage decodeCCITTFax(final InputStream in, final int type, final IOCAImageContent imageContent)
			throws IOException {
		short bitOrder = imageContent.getImageEncoding().getBitOrder();
		int fillOrder;
		int width = imageContent.getImageSize().getHSize();
		int height = imageContent.getImageSize().getVSize();

		if (bitOrder == IOCA.BITORDR_LTR) {
			fillOrder = TIFFBaseline.FILL_LEFT_TO_RIGHT;
		} else {
			fillOrder = TIFFExtension.FILL_RIGHT_TO_LEFT;
		}

		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
		final byte[] raster = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

		final CCITTFaxDecoderStream decoder = new CCITTFaxDecoderStream(in, width, type, fillOrder, 0L);

		for (int b, i = 0, l = raster.length; i < l; i++) {
			b = decoder.read();
			if (b < 0) {
				throw new EOFException();
			}

			// Invert the colour.
			// For some reason the decoder uses an inverted mapping.
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
		buffer();
		checkBounds(imageIndex);

		final List<ImageTypeSpecifier> types = new ArrayList<>();
		final IOCAImageContent imageContent = imageContents.get(imageIndex);

		if (IOCA.COMPRID_JPEG == imageContent.getImageEncoding().getCompressionId()) {
			types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB));
			types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY));
		} else if (0xFF == imageContent.getIdeSize()) { // Not sure about this.
			types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY));
		} else {
			types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_BINARY));
		}

		return types.iterator();
	}

	@Override
	public int getNumImages(final boolean allowSearch) throws IOException {
		if (!allowSearch && null == imageContents) {
			return -1;
		}

		buffer();
		return imageContents.size();
	}

	@Override
	public int getHeight(int imageIndex) throws IOException {
		buffer();
		checkBounds(imageIndex);
		return imageContents.get(imageIndex).getImageSize().getVSize();
	}

	@Override
	public int getWidth(final int imageIndex) throws IOException {
		buffer();
		checkBounds(imageIndex);
		return imageContents.get(imageIndex).getImageSize().getHSize();
	}

	@Override
	protected void resetMembers() {
		segment = null;
		segments = null;
		imageContent = null;
		imageContents = null;
	}

	private void buffer() throws IOException {
		assertInput();

		if (imageContents != null) {
			return;
		}

		if (DEBUG) {
			System.err.println("IOCA: buffering");
		}

		segments = new LinkedList<>();
		imageContents = new ArrayList<>(); // Keep a sequential, flat list of image contents for easy lookup by index.

		int code;

		while (-1 != (code = imageInput.read())) {
			if (abortRequested()) {
				break;
			}

			if (IOCA.EXTENDED_CODE_POINT == code) {
				readExtended(imageInput.read());
			} else {
				readLong(code);
			}
		}

		processImageProgress(100f);

		if (abortRequested()) {
			processReadAborted();
		}
	}

	private void readLong(final int code) throws IOException {
		final int length = imageInput.read();

		switch (code) {
			case IOCA.CODE_POINT_BEGIN_SEGMENT:
				beginSegment(length);
				break;

			case IOCA.CODE_POINT_END_SEGMENT:
				endSegment();
				break;

			case IOCA.CODE_POINT_BEGIN_IMAGE_CONTENT:
				beginImageContent(length);
				break;

			case IOCA.CODE_POINT_END_IMAGE_CONTENT:
				endImageContent();
				break;

			case IOCA.CODE_POINT_IMAGE_SIZE_PARAMETER:
				imageSize(length);
				break;

			case IOCA.CODE_POINT_IMAGE_ENCODING_PARAMETER:
				imageEncoding(length);
				break;

			case IOCA.CODE_POINT_IDE_SIZE_PARAMETER:
				ideSize(length);
				break;

			case IOCA.CODE_POINT_IDE_STRUCTURE_PARAMETER:
				ideStructure(length);
				break;

			case IOCA.CODE_POINT_BEGIN_TILE:
			case IOCA.CODE_POINT_END_TILE:
			case IOCA.CODE_POINT_IMAGE_LUT_ID_PARAMETER:
			case IOCA.CODE_POINT_BAND_IMAGE_PARAMETER:
			case IOCA.CODE_POINT_EXTERNAL_ALGORITHM_SPECIFICATION_PARAMETER:
			case IOCA.CODE_POINT_TILE_POSITION:
			case IOCA.CODE_POINT_TILE_SIZE:
			case IOCA.CODE_POINT_TILE_SET_COLOR:
			case IOCA.CODE_POINT_BEGIN_TRANSPARENCY_MASK:
			case IOCA.CODE_POINT_END_TRANSPARENCY_MASK:
				throw new IIOException("Not supported.");

			default:
				throw new IIOException(String.format("Unknown code point: %02x", code));
		}
	}

	private void readExtended(final int code) throws IOException {
		final int length = imageInput.readUnsignedShort();

		switch (code) {
			case IOCA.CODE_POINT_IMAGE_DATA:
				imageData(length);
				break;

			case IOCA.CODE_POINT_BAND_IMAGE_DATA:
			case IOCA.CODE_POINT_INCLUDE_TILE:
			case IOCA.CODE_POINT_TILE_TOC:
			case IOCA.CODE_POINT_IMAGE_SUBSAMPLING_PARAMETER:
				throw new IIOException("Not supported.");

			default:
				throw new IIOException(String.format("Unknown code point: %02x", code));
		}
	}

	private void beginSegment(final int length) throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: begin segment");
		}

		if (length > 0x04) {
			throw new IIOException("EC-0003: invalid length.");
		}

		segment = new IOCASegment();

		if (0x01 == length) {
			segment.setName(imageInput.readUnsignedByte());
		} else if (0x02 == length) {
			segment.setName(imageInput.readUnsignedShort());
		} else if (0x04 == length) {
			segment.setName(imageInput.readUnsignedInt());
		}
	}

	private void endSegment() throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: end segment");
		}

		if (null == segment) {
			throw new IIOException("EC-710F: invalid sequence.");
		}

		segments.add(segment);
		segment = null;
	}

	private void beginImageContent(final int length) throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: begin image content");
		}

		if (0x01 != length) {
			throw new IIOException("EC-0003: invalid length.");
		}

		if (0xFF != imageInput.readUnsignedByte()) {
			throw new IIOException("EC-0004: invalid parameter value.");
		}

		if (null == segment) {
			throw new IIOException("EC-910F: invalid sequence.");
		}

		imageContent = new IOCAImageContent();
	}

	private void endImageContent() throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: end image content");
		}

		if (null == segment || null == imageContent) {
			throw new IIOException("EC-910F: invalid sequence.");
		}

		segment.addImageContent(imageContent);
		imageContents.add(imageContent);
		imageContent = null;
	}

	private void imageSize(final int length) throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: image size");
		}

		if (0x09 != length) {
			throw new IIOException("EC-0003: invalid length.");
		}

		if (null == imageContent) {
			throw new IIOException("EC-940F: invalid sequence.");
		}

		final IOCAImageSize imageSize = new IOCAImageSize();

		imageSize.setUnitBase((short) imageInput.readUnsignedByte());

		imageSize.setHResolution(imageInput.readUnsignedShort());
		imageSize.setVResolution(imageInput.readUnsignedShort());

		imageSize.setHSize(imageInput.readUnsignedShort());
		imageSize.setVSize(imageInput.readUnsignedShort());

		imageContent.setImageSize(imageSize);
	}

	private void imageEncoding(final int length) throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: image encoding");
		}

		if (length < 0x02 || length > 0x03) {
			throw new IIOException("EC-0003: invalid length.");
		}

		if (null == imageContent) {
			throw new IIOException("EC-950F: invalid sequence.");
		}

		final IOCAImageEncoding imageEncoding = new IOCAImageEncoding();

		imageEncoding.setCompressionId((short) imageInput.readUnsignedByte());
		imageEncoding.setRecordingId((short) imageInput.readUnsignedByte());

		if (0x03 == length) {
			imageEncoding.setBitOrder((short) imageInput.readUnsignedByte());
		}

		imageContent.setImageEncoding(imageEncoding);
	}

	private void ideSize(final int length) throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: IDE size");
		}

		if (length != 0x01) {
			throw new IIOException("EC-0003: invalid length.");
		}

		if (null == imageContent) {
			throw new IIOException("EC-960F: invalid sequence.");
		}

		imageContent.setIdeSize((short) imageInput.readUnsignedByte());
	}

	private void ideStructure(final int length) throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: IDE structure");
		}

		if (length < 0x06 || length > 0x09) {
			throw new IIOException("EC-0005: invalid length");
		}

		if (null == imageContent) {
			throw new IIOException("EC-9B0F: invalid sequence.");
		}

		final IOCAIdeStructure ideStructure = new IOCAIdeStructure();

		ideStructure.setFlags(imageInput.readByte());
		ideStructure.setFormat(imageInput.readByte());

		if (0x0000 != imageInput.readShort()) {
			throw new IIOException("EC-9B10: invalid or unsupported IDE Structure parameter value");
		}

		ideStructure.setSize1(imageInput.readByte());

		if (length > 0x06) {
			ideStructure.setSize2(imageInput.readByte());
		}

		if (length > 0x07) {
			ideStructure.setSize3(imageInput.readByte());
		}

		if (length > 0x08) {
			ideStructure.setSize4(imageInput.readByte());
		}

		imageContent.setIdeStructure(ideStructure);
	}

	private void imageData(final int length) throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: image data");
		}

		if (length < 0x0001 || length > 0xFFFF) {
			throw new IIOException("EC-0003: invalid length.");
		}

		if (null == imageContent) {
			throw new IIOException("EC-920F: invalid sequence.");
		}

		final byte[] buffer = new byte[length];

		imageInput.readFully(buffer, 0, length);
		imageContent.addData(buffer);
	}
}
