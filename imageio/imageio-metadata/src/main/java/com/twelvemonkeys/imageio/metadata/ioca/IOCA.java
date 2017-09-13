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

	short BITORDR_LTR       = 0x00;
	short BITORDR_RTL       = 0x01;

	short IDESZ_BILEVEL     = 0x01;

	short FORMAT_RGB        = 0x01;
	short FORMAT_YCRCB      = 0x02;
	short FORMAT_CMYK       = 0x04;
	short FORMAT_YCBCR      = 0x12;

	short CODE_POINT_BEGIN_SEGMENT                              = 0x70;
	short CODE_POINT_END_SEGMENT                                = 0x71;
	short CODE_POINT_BEGIN_TILE                                 = 0x8C;
	short CODE_POINT_END_TILE                                   = 0x8D;
	short CODE_POINT_BEGIN_TRANSPARENCY_MASK                    = 0x8E;
	short CODE_POINT_END_TRANSPARENCY_MASK                      = 0x8F;
	short CODE_POINT_BEGIN_IMAGE_CONTENT                        = 0x91;
	short CODE_POINT_END_IMAGE_CONTENT                          = 0x93;
	short CODE_POINT_IMAGE_SIZE_PARAMETER                       = 0x94;
	short CODE_POINT_IMAGE_ENCODING_PARAMETER                   = 0x95;
	short CODE_POINT_IDE_SIZE_PARAMETER                         = 0x96;
	short CODE_POINT_IMAGE_LUT_ID_PARAMETER                     = 0x97;
	short CODE_POINT_BAND_IMAGE_PARAMETER                       = 0x98;
	short CODE_POINT_IDE_STRUCTURE_PARAMETER                    = 0x9B;
	short CODE_POINT_EXTERNAL_ALGORITHM_SPECIFICATION_PARAMETER = 0x9F;
	short CODE_POINT_TILE_POSITION                              = 0xB5;
	short CODE_POINT_TILE_SIZE                                  = 0xB6;
	short CODE_POINT_TILE_SET_COLOR                             = 0xB7;

	short EXTENDED_CODE_POINT                                   = 0xFE;

	short CODE_POINT_IMAGE_DATA                                 = 0x92;
	short CODE_POINT_BAND_IMAGE_DATA                            = 0x9C;
	short CODE_POINT_INCLUDE_TILE                               = 0xB8;
	short CODE_POINT_TILE_TOC                                   = 0xBB;
	short CODE_POINT_IMAGE_SUBSAMPLING_PARAMETER                = 0xCE;
}
