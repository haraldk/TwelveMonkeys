package com.twelvemonkeys.imageio.metadata.ioca;

public interface IOCA {

	// IBM MMR - Modified Modified READ
	short COMPRID_IBM_MMR   = 0x01;

	// No compression
	short COMPRID_NO_COMP   = 0x03;

	// RL4 (Run Length 4)
	short COMPRID_RL4       = 0x06;

	// ABIC (Bilevel Q-Coder)
	short COMPRID_ABIC_Q    = 0x08;

	// TIFF algorithm 2
	short COMPRID_TIFF_2    = 0x09;

	// Concatenated ABIC
	short COMPRID_ABIC_C    = 0x0A;

	// Color compression used by OS/2 Image Support, part number 49F4608
	short COMPRID_OS2       = 0x0B;

	// TIFF PackBits
	short COMPRID_TIFF_PB   = 0x0C;

	// TIFF LZW
	short COMPRID_TIFF_LZW  = 0x0D;

	// Solid Fill Rectangle
	short COMPRID_SOLID     = 0x20;

	// G3 MH-Modified
	// Huffman (ITU-TSS T.4 Group 3 one-dimensional coding standard for facsimile)
	short COMPRID_G3_MH     = 0x80;

	// G3 MH-Modified READ (ITU-TSS T.4
	// Group 3 two-dimensional coding option for facsimile)
	short COMPRID_G3_MR     = 0x81;

	// G4 MMR - Modified Modified READ
	// (ITU-TSS T.6 Group 4 two-dimensional coding standard for facsimile).
	short COMPRID_G4_MMR    = 0x82;

	// JPEG algorithms (see the External Algorithm Specification parameter for detail).
	short COMPRID_JPEG      = 0x83;

	// JBIG2
	short COMPRID_JBIG2     = 0x84;

	// User-defined algorithms (see the External Algorithm Specification parameter for details).
	short COMPRID_USERDEF   = 0xFE;

	short TYPE_BITS = 0; // Bit string
	short TYPE_CHAR = 1; // Character string
	short TYPE_CODE = 2; // Architected constant
	short TYPE_UBIN = 3; // Unsigned binary
	short TYPE_BLOB = 4;

	String[] TYPE_NAMES = {
			"BITS", "CHAR", "CODE", "UBIN", "BLOB"
	};

	short FIELD_NAME        = 0;
	short FIELD_UNITBASE    = 1;
	short FIELD_HRESOL      = 2;
	short FIELD_VRESOL      = 3;
	short FIELD_HSIZE       = 4;
	short FIELD_VSIZE       = 5;
	short FIELD_COMPRID     = 6;
	short FIELD_RECID       = 7;
	short FIELD_BITORDR     = 8;
	short FIELD_IDESZ       = 9;
	short FIELD_FLAGS       = 10;
	short FIELD_FORMAT      = 11;
	short FIELD_SIZE1       = 12;
	short FIELD_SIZE2       = 13;
	short FIELD_SIZE3       = 14;
	short FIELD_SIZE4       = 15;
	short FIELD_DATA        = 16;

	String[] FIELD_NAMES = {
			"ID", "LENGTH", "NAME", "UNITBASE", "HRESOL", "VRESOL", "HSIZE", "VSIZE", "COMPRID", "RECID", "BITORDR",
			"IDESZ", "FLAGS", "FORMAT", "SIZE1", "SIZE2", "SIZE3", "SIZE4", "DATA"
	};

	short CODE_BEGIN_SEGMENT                                = 0;
	short CODE_END_SEGMENT                                  = 1;
	short CODE_BEGIN_TILE                                   = 2;
	short CODE_END_TILE                                     = 3;
	short CODE_BEGIN_TRANSPARENCY_MASK                      = 4;
	short CODE_END_TRANSPARENCY_MASK                        = 5;
	short CODE_BEGIN_IMAGE_CONTENT                          = 6;
	short CODE_END_IMAGE_CONTENT                            = 7;
	short CODE_IMAGE_SIZE_PARAMETER                         = 8;
	short CODE_IMAGE_ENCODING_PARAMETER                     = 9;
	short CODE_IDE_SIZE_PARAMETER                           = 10;
	short CODE_IMAGE_LUT_ID_PARAMETER                       = 11;
	short CODE_BAND_IMAGE_PARAMETER                         = 12;
	short CODE_IDE_STRUCTURE_PARAMETER                      = 13;
	short CODE_EXTERNAL_ALGORITHM_SPECIFICATION_PARAMETER   = 14;
	short CODE_TILE_POSITION                                = 15;
	short CODE_TILE_SIZE                                    = 16;
	short CODE_TILE_SET_COLOR                               = 17;
	short CODE_IMAGE_DATA                                   = 18;
	short CODE_BAND_IMAGE_DATA                              = 19;
	short CODE_INCLUDE_TILE                                 = 20;
	short CODE_TILE_TOC                                     = 21;
	short CODE_IMAGE_SUBSAMPLING_PARAMETER                  = 22;

	byte CODE_POINT_BEGIN_SEGMENT                           = 0x70;
	byte CODE_POINT_END_SEGMENT                             = 0x71;
	byte CODE_POINT_BEGIN_TILE                              = (byte) 0x8C;
	byte CODE_POINT_END_TILE                                = (byte) 0x8D;
	byte CODE_POINT_BEGIN_TRANSPARENCY_MASK                 = (byte) 0x8E;
	byte CODE_POINT_END_TRANSPARENCY_MASK                   = (byte) 0x8F;
	byte CODE_POINT_BEGIN_IMAGE_CONTENT                     = (byte) 0x91;
	byte CODE_POINT_END_IMAGE_CONTENT                       = (byte) 0x93;
	byte CODE_POINT_IMAGE_SIZE_PARAMETER                    = (byte) 0x94;
	byte CODE_POINT_IMAGE_ENCODING_PARAMETER                = (byte) 0x95;
	byte CODE_POINT_IDE_SIZE_PARAMETER                      = (byte) 0x96;
	byte CODE_POINT_IMAGE_LUT_ID_PARAMETER                  = (byte) 0x97;
	byte CODE_POINT_BAND_IMAGE_PARAMETER                    = (byte) 0x98;
	byte CODE_POINT_IDE_STRUCTURE_PARAMETER                 = (byte) 0x9B;
	byte CODE_POINT_EXTERNAL_ALGORITHM_SPECIFICATION_PARAMETER = (byte) 0x9F;
	byte CODE_POINT_TILE_POSITION                           = (byte) 0xB5;
	byte CODE_POINT_TILE_SIZE                               = (byte) 0xB6;
	byte CODE_POINT_TILE_SET_COLOR                          = (byte) 0xB7;

	int CODE_POINT_IMAGE_DATA                               = 0xFE92;
	int CODE_POINT_BAND_IMAGE_DATA                          = 0xFE9C;
	int CODE_POINT_INCLUDE_TILE                             = 0xFEB8;
	int CODE_POINT_TILE_TOC                                 = 0xFEBB;
	int CODE_POINT_IMAGE_SUBSAMPLING_PARAMETER              = 0xFECE;

	byte EXTENDED_CODE_POINT                                = (byte) 0xFE;

	byte[] CODE_POINTS = {
			CODE_POINT_BEGIN_SEGMENT,
			CODE_POINT_END_SEGMENT,
			CODE_POINT_BEGIN_TILE,
			CODE_POINT_END_TILE,
			CODE_POINT_BEGIN_TRANSPARENCY_MASK,
			CODE_POINT_END_TRANSPARENCY_MASK,
			CODE_POINT_BEGIN_IMAGE_CONTENT,
			CODE_POINT_END_IMAGE_CONTENT,
			CODE_POINT_IMAGE_SIZE_PARAMETER,
			CODE_POINT_IMAGE_ENCODING_PARAMETER,
			CODE_POINT_IDE_SIZE_PARAMETER,
			CODE_POINT_IMAGE_LUT_ID_PARAMETER,
			CODE_POINT_BAND_IMAGE_PARAMETER,
			CODE_POINT_IDE_STRUCTURE_PARAMETER,
			CODE_POINT_EXTERNAL_ALGORITHM_SPECIFICATION_PARAMETER,
			CODE_POINT_TILE_POSITION,
			CODE_POINT_TILE_SIZE,
			CODE_POINT_TILE_SET_COLOR
	};

	int[] EXTENDED_CODE_POINTS = {
			CODE_POINT_IMAGE_DATA,
			CODE_POINT_BAND_IMAGE_DATA,
			CODE_POINT_INCLUDE_TILE,
			CODE_POINT_TILE_TOC,
			CODE_POINT_IMAGE_SUBSAMPLING_PARAMETER
	};
}
