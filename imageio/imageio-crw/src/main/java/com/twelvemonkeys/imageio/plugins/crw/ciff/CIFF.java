package com.twelvemonkeys.imageio.plugins.crw.ciff;

import java.nio.charset.StandardCharsets;

/**
 * CIFF
 *
 * @see <a href="http://xyrion.org/ciff/CIFFspecV1R04.pdf">CIFF: Specification on Image Data File</a>
 */
public interface CIFF {

    int HEADER_SIZE = 26;
    int VERSION_1_2 = 0x00010002;

    byte[] TYPE_HEAP = "HEAP".getBytes(StandardCharsets.US_ASCII);

    // According to CIFF spec, other subtypes are "JPGM", "TIFP" and "ARCH".
    byte[] SUBTYPE_ARCH = "ARCH".getBytes(StandardCharsets.US_ASCII);
    byte[] SUBTYPE_CCDR = "CCDR".getBytes(StandardCharsets.US_ASCII);
    byte[] SUBTYPE_JPGM = "JPGM".getBytes(StandardCharsets.US_ASCII);
    byte[] SUBTYPE_TIFP = "TIFP".getBytes(StandardCharsets.US_ASCII);

    int STORAGE_HEAP = 0x0000;      // kStg_InHeapSpace
    int STORAGE_RECORD = 0x4000;    // kStg_InRecordEntry

    int DATA_TYPE_BYTE = 0x0000;
    int DATA_TYPE_ASCII = 0x0800;
    int DATA_TYPE_WORD = 0x1000;
    int DATA_TYPE_DWORD = 0x1800;
    int DATA_TYPE_UNDEFINED = 0x2000; // kDT_BYTE2

    int DATA_TYPE_HEAP_1 = 0x2800; // kDT_HeapTypeProperty1
    int DATA_TYPE_HEAP_2 = 0x3000; // kDT_HeapTypeProperty2

    // From Spec
    int TAG_WILDCARD = 0xffff;
    int TAG_NULL = 0x0000;      // null record
    int TAG_FREE = 0x0001;      // free record
    int TAG_EX_USED = 0x0002;   // special type for implementation purpose

    int TAG_DESCRIPTION = DATA_TYPE_ASCII | 0x0005;
    int TAG_MODEL_NAME = DATA_TYPE_ASCII | 0x000a;
    int TAG_FIRMWARE_VERSION = DATA_TYPE_ASCII | 0x000b;
    int TAG_COMPONENT_VERSION = DATA_TYPE_ASCII | 0x000c;
    int TAG_ROM_OPERATION_MODE = DATA_TYPE_ASCII | 0x000d;
    int TAG_OWNER_NAME = DATA_TYPE_ASCII | 0x0010;
    int TAG_IMAGE_FILE_NAME = DATA_TYPE_ASCII | 0x0016;
    int TAG_THUMBNAIL_FILE_NAME = DATA_TYPE_ASCII | 0x0017;
    int TAG_TARGET_IMAGE_TYPE = DATA_TYPE_WORD | 0x000a;
    int TAG_SR_RELEASE_METHOD = DATA_TYPE_WORD | 0x0010;
    int TAG_SR_RELEASE_TIMING = DATA_TYPE_WORD | 0x0011;
    int TAG_RELEASE_SETTING = DATA_TYPE_WORD | 0x0016;
    int TAG_BODY_SENSITIVITY = DATA_TYPE_WORD | 0x001c;
    int TAG_IMAGE_FORMAT = DATA_TYPE_DWORD | 0x0003;
    int TAG_RECORD_ID = DATA_TYPE_DWORD | 0x0004;
    int TAG_SELF_TIMER_TIME = DATA_TYPE_DWORD | 0x0006;
    int TAG_SR_TARGET_DISTANCE_SETTING = DATA_TYPE_DWORD | 0x0007;
    int TAG_BODY_ID = DATA_TYPE_DWORD | 0x000b;
    int TAG_CAPTURED_TIME = DATA_TYPE_DWORD | 0x000e;
    int TAG_IMAGE_SPEC = DATA_TYPE_DWORD | 0x0010;
    int TAG_SR_EF = DATA_TYPE_DWORD | 0x0013;
    int TAG_MI_EV = DATA_TYPE_DWORD | 0x0014;
    int TAG_SERIAL_NUMBER = DATA_TYPE_DWORD | 0x0017;
    int TAG_SR_EXPOSURE = DATA_TYPE_DWORD | 0x0018;
    int TAG_CAMERA_OBJECT = 0x0007 | DATA_TYPE_HEAP_1;
    int TAG_SHOOTING_RECORD = 0x0002 | DATA_TYPE_HEAP_2;
    int TAG_MEASURED_INFO = 0x0003 | DATA_TYPE_HEAP_2;
    int TAG_CAMERA_SPECIFICATiON = 0x0004 | DATA_TYPE_HEAP_2; // kTC_CameraSpecificaiton

    // From Phil Harvey's ExifTool (https://sno.phy.queensu.ca/~phil/exiftool/canon_raw.html)

    // 0x0006	-	0x300b	-	8	-
    int TAG_CANON_COLOR_INFO1 = DATA_TYPE_BYTE | 0x0032; //  - Subdir: 0x300b, Exif: -
    // 0x0036	-	0x300b	?	varies	-
    // 0x003f	-	0x300b	?	5120	-
    // 0x0040	-	0x300b	?	256	-
    // 0x0041	-	0x300b	?	256	-

    // ASCII Strings
//    int TAG_CANON_FILE_DESCRIPTION = 0x0805; //  - Subdir: 0x2804, Exif: -
//    int TAG_USER_COMMENT = DATA_TYPE_ASCII | 0x0005; //  - Subdir: 0x300a, Exif: -
//    int TAG_CANON_RAW_MAKE_MODEL    = DATA_TYPE_ASCII | 0x080a; //  - Subdir: 0x2807, Exif: -
//    int TAG_CANON_FIRMWARE_VERSION  = DATA_TYPE_ASCII | 0x080b; // 32	Firmware version. eg) "Firmware Version 1.1.1" - Subdir: 0x3004, Exif: 0x07
//    int TAG_COMPONENT_VERSION = 0x080c;	// ?
//    int TAG_ROM_OPERATION_MODE = 0x080d; // 4	eg) The string "USA" for 300D's sold in North America - Subdir: 0x3004, Exif: -
//    int TAG_OWNER_NAME = 0x0810; // 32	Owner's name. eg) "Phil Harvey" - Subdir: 0x2807, Exif: 0x09
    int TAG_CANON_IMAGE_TYPE        = DATA_TYPE_ASCII | 0x0815; // 32	Type of file. eg) "CRW:EOS DIGITAL REBEL CMOS RAW" - Subdir: 0x2804, Exif: 0x06
//    int TAG_ORIGINAL_FILE_NAME = 0x0816; // 32	Original file name. eg) "CRW_1834.CRW" - Subdir: 0x300a, Exif: -
//    int TAG_THUMBNAIL_FILE_NAME = 0x0817; // 32	Thumbnail file name. eg) "CRW_1834.THM" - Subdir: 0x300a, Exif: -

    // SHORT (2-Byte Alignmnt)
//    int TAG_TARGET_IMAGE_TYPE = 0x100a; // 2	0=real-world subject, 1=written document - Subdir: 0x300a, Exif: -
//    int TAG_SHUTTER_RELEASE_METHOD = 0x1010; // 2	0=single shot, 1=continuous shooting - Subdir: 0x3002, Exif: -
//    int TAG_SHUTTER_RELEASE_TIMING = 0x1011; // 2	0=priority on shutter, 1=priority on focus - Subdir: 0x3002, Exif: -
    // 0x1014	-	0x3002	-	8	-
//    int TAG_RELEASE_SETTING = 0x1016; // 2	- - Subdir: 0x3002, Exif: -
    int TAG_BASE_ISO                = DATA_TYPE_WORD | 0x101c; // 2	The camera body's base ISO sensitivity - Subdir: 0x3004, Exif: -
    // 0x1026	-	0x300a	-	6	-
    int TAG_CANON_FLASH_INFO        = DATA_TYPE_WORD | 0x1028; // 8	Unknown information, flash related - Subdir: 0x300b, Exif: 0x03
    int TAG_FOCAL_LENGTH            = DATA_TYPE_WORD | 0x1029; // 8	Four 16 bit integers: 0) unknown, 1) focal length in mm, 2-3) sensor width and height in units of 1/1000 inch - Subdir: 0x300b, Exif: 0x02
    int TAG_CANON_SHOT_INFO         = DATA_TYPE_WORD | 0x102a; // varies	Data block giving shot information - Subdir: 0x300b, Exif: 0x04
    int TAG_CANON_COLOR_INFO2       = DATA_TYPE_WORD | 0x102c; // 256	Data block of color information (format unknown) - Subdir: 0x300b, Exif: -
    int TAG_CANON_CAMERA_SETTINGS   = DATA_TYPE_WORD | 0x102d; // varies	Data block giving camera settings - Subdir: 0x300b, Exif: 0x01
    int TAG_WHITE_SAMPLE            = DATA_TYPE_WORD | 0x1030; // 102 or 118	White sample information with encrypted 8x8 sample data - Subdir: 0x300b, Exif: -
    int TAG_SENSOR_INFO             = DATA_TYPE_WORD | 0x1031; // 34	Sensor size and resolution information - Subdir: 0x300b, Exif: -
    int TAG_CANON_CUSTOM_FUNCTIONS  = DATA_TYPE_WORD | 0x1033; // varies	Data block giving Canon custom settings - Subdir: 0x300b, Exif: 0x0f
    int TAG_CANON_AF_INFO           = DATA_TYPE_WORD | 0x1038; // varies	Data block giving AF-specific information - Subdir: 0x300b, Exif: 0x12
    // 0x1039	0x13	0x300b	?	8	-
    // 0x103c	-	0x300b	?	156	-
    // 0x107f	-	0x300b	-	varies	-
    int TAG_CANON_FILE_INFO         = DATA_TYPE_WORD | 0x1093; // 18	Data block giving file-specific information - Subdir: 0x300b, Exif: 0x93
    // 0x10a8	0xa8	0x300b	?	20	-
    int TAG_COLOR_BALANCE           = DATA_TYPE_WORD | 0x10a9; // 82	Table of 16-bit integers. The first integer (like many other data blocks) is the number of bytes in the record. This is followed by red, green1, green2 and blue levels for WhiteBalance settings: auto, daylight, shade, cloudy, tungsten, fluorescent, flash, custom and kelvin. The final 4 entries appear to be some sort of baseline red, green1, green2 and blue levels. - Subdir: 0x300b, Exif: 0xa9
    // 0x10aa	0xaa	0x300b	?	10	-
    // 0x10ad	-	0x300b	?	62	-
    int TAG_COLOR_TEMPERATURE       = DATA_TYPE_WORD | 0x10ae; // 2	16-bit integer giving the color temperature - Subdir: 0x300b, Exif: 0xae
    // 0x10af	-	0x300b	?	2	-
    int TAG_COLOR_SPACE             = DATA_TYPE_WORD | 0x10b4; // 2	16-bit integer specifying the color space (1=sRGB, 2=Adobe RGB, 0xffff=uncalibrated) - Subdir: 0x300b, Exif: 0xb4
    int TAG_RAW_JPEG_INFO           = DATA_TYPE_WORD | 0x10b5; // 10	Data block giving embedded JPG information - Subdir: 0x300b, Exif: 0xb5
    // 0x10c0	0xc0	0x300b	?	26	-
    // 0x10c1	0xc1	0x300b	?	26	-
    // 0x10c2	-	0x300b	?	884	-

    // LONG (4-Byte Alignment)
//    int TAG_IMAGE_FORMAT = 0x1803; // 8	32-bit integer specifying image format (0x20001 for CRW), followed by 32-bit float giving target compression ratio - Subdir: 0x300a, Exif: -
//    int TAG_RECORD_ID = 0x1804; // 4	The number of pictures taken since the camera was manufactured - Subdir: 0x300a, Exif: -
    // 0x1805	-	0x3002	-	8	-
//    int TAG_SELF_TIMER_TIME = 0x1806; // 4	32-bit integer giving self-timer time in milliseconds - Subdir: 0x3002, Exif: -
//    int TAG_TARGET_DISTANCE_SETTING = 0x1807; // 4	32-bit float giving target distance in mm - Subdir: 0x3002, Exif: -
//    int TAG_SERIAL_NUMBER = 0x180b; // 4	The camera body number for EOS models. eg) 00560012345 - Subdir: 0x3004, Exif: 0x0c
    int TAG_TIME_STAMP              = DATA_TYPE_DWORD | 0x180e; // 12	32-bit integer giving the time in seconds when the picture was taken, followed by a 32-bit timezone in seconds - Subdir: 0x300a, Exif: -
    int TAG_IMAGE_INFO              = DATA_TYPE_DWORD | 0x1810; // 28	Data block containing image information, including rotation - Subdir: 0x300a, Exif: -
    // 0x1812	-	0x3004	-	40	-
    int TAG_FLASH_INFO              = DATA_TYPE_DWORD | 0x1813; // 8	Two 32-bit floats: The flash guide number and the flash threshold - Subdir: 0x3002, Exif: -
    int TAG_MEASURED_EV             = DATA_TYPE_DWORD | 0x1814; // 4	32-bit float giving the measured EV - Subdir: 0x3003, Exif: -
    int TAG_FILE_NUMBER             = DATA_TYPE_DWORD | 0x1817; // 4	32-bit integer giving the number of this file. eg) 1181834 - Subdir: 0x300a, Exif: 0x08
//    int TAG_EXPOSURE_INFO           = DATA_TYPE_DWORD | 0x1818; // 12	Three 32-bit floats: Exposure compensation, Tv, Av - Subdir: 0x3002, Exif: -
    // 0x1819	-	0x300b	-	64	-
    int TAG_CANON_MODEL_ID          = DATA_TYPE_DWORD | 0x1834; // 4	Unsigned 32-bit integer giving unique model ID - Subdir: 0x300b, Exif: 0x10
    int TAG_DECODER_TABLE           = DATA_TYPE_DWORD | 0x1835; // 16	RAW decoder table information - Subdir: 0x300b, Exif: -
    int TAG_SERIAL_NUMBER_FORMAT    = DATA_TYPE_DWORD | 0x183b; // 4	32-bit integer (0x90000000=format 1, 0xa0000000=format 2) - Subdir: 0x300b, Exif: 0x15

    // UNDEFINED (Mixed Data Records)
    int TAG_RAW_DATA            = DATA_TYPE_UNDEFINED | 0x2005; // The raw data itself (the bulk of the CRW file) - Subdir: root
    int TAG_JPEG_PREVIEW        = DATA_TYPE_UNDEFINED | 0x2007; // The embedded JPEG image (2048x1360 pixels for the 300D with Canon firmware) - Subdir: root
    int TAG_THUMBNAIL           = DATA_TYPE_UNDEFINED | 0x2008; // Thumbnail image (JPEG, 160x120 pixels) - Subdir: root

    // SubDirectory Blocks
    int TAG_IMAGE_DESCRIPTION       = DATA_TYPE_HEAP_1 | 0x2804; // The image description subdirectory - Subdir: 0x300a
//    int TAG_CAMERA_OBJECT        = 0x2807; // The camera object subdirectory - Subdir: 0x300a
//    int TAG_SHOOTING_RECORD      = 0x3002; // The shooting record subdirectory - Subdir: 0x300a
//    int TAG_MEASURED_INFO        = 0x3003; // The measured information subdirectory - Subdir: 0x300a
    int TAG_CAMERA_SPECIFICATION    = DATA_TYPE_HEAP_2 | 0x3004; // The camera specification subdirectory - Subdir: 0x2807

    int TAG_IMAGE_PROPERTIES        = DATA_TYPE_HEAP_2 | 0x300a; // The main subdirectory containing all meta information - Subdir: root
    int TAG_EXIF_INFORMATION        = DATA_TYPE_HEAP_2 | 0x300b; // The subdirectory containing most of the JPEG/TIFF Exif information - Subdir: 0x300a

}
