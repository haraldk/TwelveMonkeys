## Background

TwelveMonkeys ImageIO is a collection of plug-ins for Java's ImageIO.


These plugins extends the number of file formats supported in Java, using the javax.imageio.* package.
The main purpose of this project is to provide support for formats not covered by the JDK itself.

Support for formats is important, both to be able to read data found
"in the wild", as well as to maintain access to data in legacy formats.
Because there is lots of legacy data out there, we see the need for open implementations of readers for popular formats.
The goal is to create a set of efficient and robust ImageIO plug-ins, that can be distributed independently.

----

## Features

Mainstream format support

#### JPEG

* Exif support
* Support for CMYK/YCCK data
* Support for non-JFIF YCbCr data
* Thumbnail support (JFIF, JFXX and Exif)
* Extended metadata support
* Extended write support in progress

#### JPEG-2000

* Possibly coming in the future, pending some license issues

#### Adobe Photoshop Document (PSD)

* Read-only support for the following file types:
  * Monochrome, 1 channel, 1 bit
  * Indexed, 1 channel, 8 bit
  * Gray, 1 channel, 8 and 16 bit
  * Duotone, 1 channel, 8 and 16 bit
  * RGB, 3-4 channels, 8 and 16 bit
  * CMYK, 4-5 channels, 8 and 16 bit
* Read support for the following compression types:
  * Uncompressed
  * RLE (PackBits)

#### Aldus/Adobe Tagged Image File Format (TIFF)

* Read-only support (for now)
* Write support in progress
* Read support for the following "Baseline" TIFF file types:
 * Class B (Bi-level), all relevant compression types, 1 bit per sample
  * Class G (Gray), all relevant compression types, 2, 4, 8, 16 or 32 bits per sample, unsigned integer
  * Class P (Palette/indexed color), all relevant compression types, 1, 2, 4, 8 or 16 bits per sample, unsigned integer
  * Class R (RGB), all relevant compression types, 8 or 16 bits per sample, unsigned integer
* Read support for the following TIFF extensions:
  * Tiling
  * LZW Compression (type 5)
  * "Old-style" JPEG Compression (type 6), as a best effort, as the spec is not well-defined
  * JPEG Compression (type 7)
  * ZLib (aka Adobe-style Deflate) Compression (type 8)
  * Deflate Compression (type 32946)
  * Horizontal differencing Predictor (type 2) for LZW, ZLib, Deflate and PackBits compression
  * Alpha channel (ExtraSamples type 1/Associated Alpha)
  * CMYK data (PhotometricInterpretation type 5/Separated)
  * YCbCr data (PhotometricInterpretation type 6/YCbCr) for JPEG
  * Planar data (PlanarConfiguration type 2/Planar)
  * ICC profiles (ICCProfile)
  * BitsPerSample values up to 16 for most PhotometricInterpretations
  * Multiple images (pages) in one file

#### Apple Mac Paint Picture Format (PICT)

* Legacy format, especially useful for reading OS X clipboard data.
* Read and limited write support
* Read support for the following file types:
  * QuickDraw (format support is not complete, but supports most OS X clipboard data as well as RGB pixel data)
  * QuickDraw bitmap
  * QuickDraw pixmap
  * QuickTime stills
* Writing is limited to RGB pixel data

#### Commodore Amiga/Electronic Arts Interchange File Format (IFF)

* Legacy format, allows reading popular image from the Commodore Amiga computer.
* Read and write support
* Read support for the following file types:
  * ILBM Indexed color, 1-8 interleaved bit planes, including 6 bit EHB
  * ILBM  Gray, 8 bit interleaved bit planes
  * ILBM RGB, 24 and 32 bit interleaved bit planes
  * ILBM HAM6 and HAM8
  * PBM Indexed color, 1-8 bit,
  * PBM Gray, 8 bit
  * PBM RGB, 24 and 32 bit
  * PBM HAM6 and HAM8
* Support for the following compression types:
  * Uncompressed
  * RLE (PackBits)

Icon/other formats

#### Apple Icon Image (ICNS)

* Read support for most icon types, including PNG and JPEG 2000 (requires JPEG 2000 ImageIO plugin)

#### MS Windows Icon and Cursor Formats (ICO & CUR)

* Read support for the following file types:
  * ICO Indexed color, 1, 4 and 8 bit
  * ICO RGB, 16, 24 and 32 bit
  * CUR Indexed color, 1, 4 and 8 bit
  * CUR RGB, 16, 24 and 32 bit

#### MS Windows Thumbs DB (Thumbs.db)

* Read support

Other formats, using 3rd party libraries

#### SVG

* Read-only support using Batik

#### WMF

* Limited read-only support using Batik


TODO: Docuemnt other useful stuff in the core package?


## Usage

Most of the time, all you need to do is simply:

    BufferedImage image = ImageIO.read(file);

For more advanced usage, and information on how to use the ImageIO API, I suggest you read the
[Java Image I/O API Guide](http://docs.oracle.com/javase/6/docs/technotes/guides/imageio/spec/imageio_guideTOC.fm.html)
from Oracle.


TODO: Docuemnt ResampleOp as well?

    ResampleOp



## Building

    $ mvn clean install


## Installing

To install the plug-ins,
Either use Maven and add the necessary dependencies to your project,
or manually add the needed JARs along with required dependencies in class-path.

The ImageIO registry and service lookup mechanism will make sure the plugins are available for use.

To verify that the plugin is installed and used at run-time, you could use the following code:

    Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("JPEG");
    while (readers.hasNext()) {
        System.out.println("reader: " + readers.next());
    }

The first line should print:

    reader: com.twelvemonkeys.imageio.jpeg.JPEGImageReader@somehash

TODO: Maven dependency example

TODO: Manual dependency with hierarchy

TODO: Links to prebuilt binaries


## FAQ

q: How do I use it?

a: The easiest way is to build your own project using Maven, and just add dependencies to the specific plug-ins you need.
 If you don't use Maven, make sure you have all the necessary JARs in classpath. See the Install section above.


q: What changes do I have to make to my code in order to use the plug-ins?

a: The short answer is: None. For basic usage, like ImageIO.read(...) or ImageIO.getImageReaders(...), there is no need
to change your code. Most of the functionality is available through standard ImageIO APIs, and great care has been taken
 not to introduce extra API where none is necessary.

Should you want to use very specific/advanced features of some of the formats, you might have to use specific APIs, like
   setting base URL for an SVG image that consists of multiple files,
   or controlling the output compression of a TIFF file.


q: How does it work?

a: The TwelveMonkeys ImageIO project contains plug-ins for ImageIO.

ImageIO uses a service lookup mechanism, to discover plug-ins at runtime.

TODO: Describe SPI mechanism.

All you have have to do, is to make sure you have the TwelveMonkeys JARs in your classpath.

The fine print: The TwelveMonkeys service providers for TIFF and JPEG overrides the onRegistration method, and
utilizes the pairwise partial ordering mechanism of the IIOServiceRegistry to make sure it is installed before
the Sun/Oracle provided JPEGImageReader and the Apple provided TIFFImageReader on OS X, respectively.
Using the pairwise ordering will not remove any functionality form these implementations, but in most cases you'll end
up using the TwelveMonkeys plug-ins instead.


q: What about JAI? Several of the formats are already supported by JAI.

a: While JAI (and jai-imageio in particular) have support for some of the formats, JAI has some major issues.
The most obvious being:
- It's not actively developed. No issues has been fixed for years.
- To get full format support, you need native libs.
Native libs does not exist for several popular platforms/architectures, and further the native libs are not open source.
Some environments may also prevent deployment of native libs, which brings us back to square one.


q: What about JMagick or IM4Java? Can't you just use what´s already available?

a: While great libraries with a wide range of formats support, the ImageMagick-based libraries has some disadvantages
compared to ImageIO.
- No real stream support, these libraries only work with files.
- No easy access to pixel data through standard Java2D/BufferedImage API.
- Not a pure Java solution, requires system specific native libs.


-----

We did it
