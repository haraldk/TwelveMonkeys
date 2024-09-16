package com.twelvemonkeys.imageio.plugins.dds;

interface DDS {
	byte[] MAGIC = new byte[]{'D', 'D', 'S', ' '};
	int HEADER_SIZE = 124;

	int FLAG_CAPS = 0x1;              // Required in every .dds file.
	int FLAG_HEIGHT = 0x2;            // Required in every .dds file.
	int FLAG_WIDTH = 0x4;             // Required in every .dds file.
	int FLAG_PITCH = 0x8;             // Required when pitch is provided for an uncompressed texture.
	int FLAG_PIXELFORMAT = 0x1000;    // Required in every .dds file.
	int FLAG_MIPMAPCOUNT = 0x20000;   // Required in a mipmapped texture.
	int FLAG_LINEARSIZE = 0x80000;    // Required when pitch is provided for a compressed texture.
	int FLAG_DEPTH = 0x800000;        // Required in a depth texture.
}
