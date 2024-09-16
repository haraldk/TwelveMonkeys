package com.twelvemonkeys.imageio.plugins.dds;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class DDSHeader {

	// https://learn.microsoft.com/en-us/windows/win32/direct3ddds/dx-graphics-dds-pguide
	private int flags;
	private int width;
	private int height;
	private int mipmap;

	private int pixelFormatFlags;
	private int fourCC;
	private int bitCount;
	private int redMask;
	private int greenMask;
	private int blueMask;
	private int alphaMask;

	public static DDSHeader read(final ImageInputStream imageInput) throws IOException {
		DDSHeader header = new DDSHeader();

		imageInput.mark();

		try {
			imageInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);

			// Read MAGIC bytes [0,3]
			byte[] magic = new byte[DDS.MAGIC.length];
			imageInput.readFully(magic);
			if (!Arrays.equals(DDS.MAGIC, magic)) {
				throw new IIOException("Unsupported MAGIC bytes.");
			}

			// DDS_HEADER structure
			// https://learn.microsoft.com/en-us/windows/win32/direct3ddds/dds-header
			int dwSize = imageInput.readInt(); // [4,7]
			if (dwSize != DDS.HEADER_SIZE) {
				throw new IIOException("Invalid header size: " + dwSize);
			}

			// Verify flags
			header.flags = imageInput.readInt(); // [8,11]
			if (header.getFlag(DDS.FLAG_CAPS
					& DDS.FLAG_HEIGHT
					& DDS.FLAG_WIDTH
					& DDS.FLAG_PIXELFORMAT)) {
				throw new IIOException("Required DDS Flag missing in header: " + Integer.toHexString(header.flags));
			}

			// Read Height & Width
			header.height = imageInput.readInt(); // [12,15]
			header.width = imageInput.readInt();  // [16,19]


			int dwPitchOrLinearSize = imageInput.readInt(); // [20,23]
			int dwDepth = imageInput.readInt(); // [24,27]
			header.mipmap = imageInput.readInt(); // [28,31]

			byte[] dwReserved1 = new byte[11 * 4];  // [32,75]
			imageInput.readFully(dwReserved1);

			// DDS_PIXELFORMAT structure
			int px_dwSize = imageInput.readInt(); // [76,79]

			header.pixelFormatFlags = imageInput.readInt(); // [80,83]
			header.fourCC = imageInput.readInt(); // [84,87]
			header.bitCount = imageInput.readInt(); // [88,91]
			header.redMask = imageInput.readInt(); // [92,95]
			header.greenMask = imageInput.readInt(); // [96,99]
			header.blueMask = imageInput.readInt(); // [100,103]
			header.alphaMask = imageInput.readInt(); // [104,107]

			int dwCaps = imageInput.readInt();
			int dwCaps2 = imageInput.readInt();
			int dwCaps3 = imageInput.readInt();
			int dwCaps4 = imageInput.readInt();

			int dwReserved2 = imageInput.readInt();
		} finally {
			imageInput.reset();
		}
		return header;
	}

	private boolean getFlag(int mask) {
		return (flags & mask) != 0;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getMipmap() {
		return mipmap;
	}

	public int getAlphaMask() {
		return alphaMask;
	}

	public int getBitCount() {
		return bitCount;
	}

	public int getBlueMask() {
		return blueMask;
	}

	public int getFlags() {
		return flags;
	}

	public int getFourCC() {
		return fourCC;
	}

	public int getGreenMask() {
		return greenMask;
	}

	public int getPixelFormatFlags() {
		return pixelFormatFlags;
	}

	public int getRedMask() {
		return redMask;
	}
}
