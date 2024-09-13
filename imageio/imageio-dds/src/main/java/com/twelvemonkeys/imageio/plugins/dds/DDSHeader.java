package com.twelvemonkeys.imageio.plugins.dds;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;

final class DDSHeader {

	// https://learn.microsoft.com/en-us/windows/win32/direct3ddds/dx-graphics-dds-pguide
	private int width;
	private int height;

	public static DDSHeader read(final ImageInputStream imageInput) throws IOException {
		DDSHeader header = new DDSHeader();

		imageInput.mark();
		imageInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);

		byte[] magic = new byte[4];
		imageInput.readFully(magic);

		// DDS_HEADER structure
		int dwSize = imageInput.readInt();
		int dwFlags = imageInput.readInt();
		header.height = imageInput.readInt();
		header.width = imageInput.readInt();
		int dwPitchOrLinearSize = imageInput.readInt();
		int dwDepth = imageInput.readInt();
		int dwMipMapCount = imageInput.readInt();

		byte[] dwReserved1 = new byte[11];
		imageInput.readFully(dwReserved1);

		// DDS_PIXELFORMAT structure
		int px_dwSize = imageInput.readInt();
		int px_dwFlags = imageInput.readInt();
		int px_dwFourCC = imageInput.readInt();
		int px_dwRGBBitCount = imageInput.readInt();
		int px_dwRBitMask = imageInput.readInt();
		int px_dwGBitMask = imageInput.readInt();
		int px_dwBBitMask = imageInput.readInt();
		int px_dwABitMask = imageInput.readInt();

		int dwCaps = imageInput.readInt();
		int dwCaps2 = imageInput.readInt();
		int dwCaps3 = imageInput.readInt();
		int dwCaps4 = imageInput.readInt();
		int dwReserved2 = imageInput.readInt();

		imageInput.reset();
		return header;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}
