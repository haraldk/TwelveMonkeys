## Latest

Master branch build status: [![Build Status](https://travis-ci.org/haraldk/TwelveMonkeys.svg?branch=master)](https://travis-ci.org/haraldk/TwelveMonkeys)

TwelveMonkeys ImageIO [3.2](http://search.maven.org/#search%7Cga%7C1%7Cg%3Acom.twelvemonkeys*%20AND%20v%3A%223.2%22) is released (Nov. 1st, 2015).

## About

TwelveMonkeys ImageIO is a collection of plugins and extensions for Java's ImageIO.

These plugins extends the number of image file formats supported in Java, using the javax.imageio.* package.
The main purpose of this project is to provide support for formats not covered by the JRE itself.

Support for formats is important, both to be able to read data found
"in the wild", as well as to maintain access to data in legacy formats.
Because there is lots of legacy data out there, we see the need for open implementations of readers for popular formats.
The goal is to create a set of efficient and robust ImageIO plug-ins, that can be distributed independently.

----

## Features

Mainstream format support

#### BMP - MS Windows/IBM OS/2 Device Independent Bitmap
 
* Read support for all known versions of the DIB/BMP format
  * Indexed color, 1, 4 and 8 bit, including 4 and 8 bit RLE
  * RGB, 16, 24 and 32 bit
  * Embedded PNG and JPEG data
  * Windows and OS/2 versions
* Native and standard metadata format

#### JPEG

* Read support for the following JPEG "flavors":
  * All JFIF compliant JPEGs
  * All Exif compliant JPEGs
  * YCbCr JPEGs without JFIF segment (converted to RGB, using embedded ICC profile)
  * CMYK JPEGs (converted to RGB by default or as CMYK, using embedded ICC profile)
  * Adobe YCCK JPEGs (converted to RGB by default or as CMYK, using embedded ICC profile)
  * JPEGs containing ICC profiles with interpretation other than 'Perceptual' or class other than 'Display'
  * JPEGs containing ICC profiles that are incompatible with stream data, corrupted ICC profiles or corrupted `ICC_PROFILE` segments
  * JPEGs using non-standard color spaces, unsupported by Java 2D
  * JPEGs with APP14/Adobe segments with length other than 14 bytes
  * 8 bit JPEGs with 16 bit DQT segments
  * Issues warnings instead of throwing exceptions in cases of corrupted or non-conformant data where ever the image 
  data can still be read in a reasonable way
* Thumbnail support:
  * JFIF thumbnails (even if stream contains "inconsistent metadata")
  * JFXX thumbnails (JPEG, Indexed and RGB)
  * EXIF thumbnails (JPEG, RGB and YCbCr)
* Metadata support:
  * JPEG metadata in both standard and native formats (even if stream contains "inconsistent metadata")
  * `javax_imageio_jpeg_image_1.0` format (currently as native format, may change in the future)
  * Non-conforming combinations of JFIF, Exif and Adobe markers, using "unknown" segments in the
   "MarkerSequence" tag for the unsupported segments (for `javax_imageio_jpeg_image_1.0` format)
* Extended write support in progress:
  * CMYK JPEGs
  * YCCK JPEGs

#### JPEG-2000

* Possibly coming in the future, pending some license issues.

If you are one of the authors, or know one of the authors and/or the current license holders of either the original
jj2000 package or the JAI ImageIO project, please contact me (I've tried to get in touch in various ways, 
without success so far).

Alternatively, if you have or know of a JPEG-2000 implementation in Java with a suitable license, get in touch. :-)

#### PNM - NetPBM Portable Any Map

* Read support for the following file types:
  * PBM in 'P1' (ASCII) and 'P4' (binary) formats, 1 bit per pixel
  * PGM in 'P2' (ASCII) and 'P5' (binary) formats, up to 16/32 bits per pixel
  * PPM in 'P3' (ASCII) and 'P6' (binary) formats, up to 16/32 bits per pixel component
  * PAM in 'P7' (binary) format up to 32 bits per pixel component
  * Limited support for PFM in 'Pf' (gray) and 'PF' (RGB) formats, 32 bits floating point
* Write support for the following formats:
  * PPM in 'P6' (binary) format
  * PAM in 'P7' (binary) format
* Standard metadata support

#### PSD - Adobe Photoshop Document

* Read support for the following file types:
  * Monochrome, 1 channel, 1 bit
  * Indexed, 1 channel, 8 bit
  * Gray, 1 channel, 8, 16 and 32 bit
  * Duotone, 1 channel, 8, 16 and 32 bit
  * RGB, 3-4 channels, 8, 16 and 32 bit
  * CMYK, 4-5 channels, 8, 16 and 32 bit
* Read support for the following compression types:
  * Uncompressed
  * RLE (PackBits)<
* Layer support
  * Image layers only, in all of the above types
* Thumbnail support
  * JPEG
  * RAW (RGB)
* Support for "Large Document Format" (PSB)
* Native and Standard metadata support

#### TIFF - Aldus/Adobe Tagged Image File Format

* Read support for the following "Baseline" TIFF file types:
  * Class B (Bi-level), all relevant compression types, 1 bit per sample
  * Class G (Gray), all relevant compression types, 2, 4, 8, 16 or 32 bits per sample, unsigned integer
  * Class P (Palette/indexed color), all relevant compression types, 1, 2, 4, 8 or 16 bits per sample, unsigned integer
  * Class R (RGB), all relevant compression types, 8 or 16 bits per sample, unsigned integer
* Read support for the following TIFF extensions:
  * Tiling
  * Class F (Facsimile), CCITT Modified Huffman RLE, T4 and T6 (type 2, 3 and 4) compressions.
  * LZW Compression (type 5)
  * "Old-style" JPEG Compression (type 6), as a best effort, as the spec is not well-defined
  * JPEG Compression (type 7)
  * ZLib (aka Adobe-style Deflate) Compression (type 8)
  * Deflate Compression (type 32946)
  * Horizontal differencing Predictor (type 2) for LZW, ZLib, Deflate and PackBits compression
  * Alpha channel (ExtraSamples type 1/Associated Alpha and type 2/Unassociated Alpha)
  * CMYK data (PhotometricInterpretation type 5/Separated)
  * YCbCr data (PhotometricInterpretation type 6/YCbCr) for JPEG
  * CIELab data in TIFF, ITU and ICC variants (PhotometricInterpretation type 9, 10 and 11)
  * Planar data (PlanarConfiguration type 2/Planar)
  * ICC profiles (ICCProfile)
  * BitsPerSample values up to 16 for most PhotometricInterpretations
  * Multiple images (pages) in one file
* Write support for most "Baseline" TIFF options
  * Uncompressed, PackBits, ZLib and Deflate 
  * Additional support for CCITT T4 and and T6 compressions.
  * Additional support for LZW and JPEG (type 7) compressions
  * Horizontal differencing Predictor (type 2) for LZW, ZLib, Deflate
* Native and Standard metadata support

Legacy formats

#### HDR - Radiance High Dynamic Range RGBE Format

* Read support for the most common RGBE (.hdr) format
* Samples are converted to 32 bit floating point (`float`) and normalized using a global tone mapper by default.
  * Support for custom global tone mappers
  * Alternatively, use a "null-tone mapper", for unnormalized data (allows local tone mapping)
* Unconverted RGBE samples accessible using `readRaster`
* Standard metadata support

#### IFF - Commodore Amiga/Electronic Arts Interchange File Format

* Legacy format, allows reading popular image format from the Commodore Amiga computer.
* Read support for the following file types:
  * ILBM Indexed color, 1-8 interleaved bit planes, including 6 bit EHB
  * ILBM  Gray, 8 bit interleaved bit planes
  * ILBM RGB, 24 and 32 bit interleaved bit planes
  * ILBM HAM6 and HAM8
  * PBM Indexed color, 1-8 bit,
  * PBM Gray, 8 bit
  * PBM RGB, 24 and 32 bit
  * PBM HAM6 and HAM8
* Write support
  * ILBM Indexed color, 1-8 bits per sample, 8 bit gray, 24 and 32 bit true color.
* Support for the following compression types (read/write):
  * Uncompressed
  * RLE (PackBits)

#### PCX - ZSoft Paintbrush Format

* Read support for the following file types:
  * Indexed color, 1, 2, 4 or 8 bits per pixel, bit planes or interleaved
  * Grayscale, 8 bits per pixel
  * Color (RGB), 8 bits per pixel component
* Read support for DCX (multi-page) fax format, containing any of the above types
* Support for the following compression types:
  * Uncompressed (experimental)
  * RLE compressed
* Standard metadata support

#### PICT - Apple Mac Paint Picture Format

* Legacy format, especially useful for reading OS X clipboard data.
* Read support for the following file types:
  * QuickDraw (format support is not complete, but supports most OS X clipboard data as well as RGB pixel data)
  * QuickDraw bitmap
  * QuickDraw pixmap
  * QuickTime stills
* Write support for RGB pixel data:
  * QuickDraw pixmap

#### SGI - Silicon Graphics Image Format

* Read support for the following file types:
  * 1, 2, 3 or 4 channel image data
  * 8 or 16 bits per pixel component
* Support for the following compression types:
  * Uncompressed
  * RLE compressed
* Standard metadata support

#### TGA - Truevision TGA Image Format

* Read support for the following file types:
  * ColorMapped
  * Monochrome
  * TrueColor
* Support for the following compression types:
  * Uncompressed
  * RLE compressed
* Standard metadata support

Icon/other formats

#### ICNS - Apple Icon Image

* Read support for the following icon types:
  * All known "native" icon types
  * Large PNG encoded icons
  * Large JPEG 2000 encoded icons (requires JPEG 2000 ImageIO plugin or fallback to `sips` command line tool)

#### ICO & CUR - MS Windows Icon and Cursor Formats

* Read support for the following file types:
  * ICO Indexed color, 1, 4 and 8 bit
  * ICO RGB, 16, 24 and 32 bit
  * CUR Indexed color, 1, 4 and 8 bit
  * CUR RGB, 16, 24 and 32 bit
* *3.1* Note: These formats are now part of the BMP plugin

#### Thumbs.db - MS Windows Thumbs DB

* Read support

Other formats, using 3rd party libraries

#### SVG - Scalable Vector Graphics

* Read-only support using Batik

#### WMF - MS Windows MetaFile

* Limited read-only support using Batik

**Important note on using Batik:** *Please read [The Apache™ XML Graphics Project - Security](http://xmlgraphics.apache.org/security.html), and make sure you use
either version 1.6.1, 1.7.1 or 1.8+.*


## Basic usage

Most of the time, all you need to do is simply include the plugins in your project and write:

    BufferedImage image = ImageIO.read(file);

This will load the first image of the file, entirely into memory.

The basic and simplest form of writing is:

    if (!ImageIO.write(image, format, file)) {
       // Handle image not written case
    }

This will write the entire image into a single file, using the default settings for the given format.

The plugins are discovered automatically at run time. See the [FAQ](#faq) for more info on how this mechanism works.

## Advanced usage

If you need more control of read parameters and the reading process, the common idiom for reading is something like:

    // Create input stream
    ImageInputStream input = ImageIO.createImageInputStream(file);

    try {
        // Get the reader
        Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

        if (!readers.hasNext()) {
            throw new IllegalArgumentException("No reader for: " + file);
        }

        ImageReader reader = readers.next();

        try {
            reader.setInput(input);

            // Optionally, listen for read warnings, progress, etc.
            reader.addIIOReadWarningListener(...);
            reader.addIIOReadProgressListener(...);

            ImageReadParam param = reader.getDefaultReadParam();

            // Optionally, control read settings like sub sampling, source region or destination etc.
            param.setSourceSubsampling(...);
            param.setSourceRegion(...);
            param.setDestination(...);
            // ...

            // Finally read the image, using settings from param
            BufferedImage image = reader.read(0, param);

            // Optionally, read thumbnails, meta data, etc...
            int numThumbs = reader.getNumThumbnails(0);
            // ...
        }
        finally {
            // Dispose reader in finally block to avoid memory leaks
            reader.dispose();
        }
    }
    finally {
        // Close stream in finally block to avoid resource leaks
        input.close();
    }

Query the reader for source image dimensions using `reader.getWidth(n)` and `reader.getHeight(n)` without reading the
entire image into memory first.

It's also possible to read multiple images from the same file in a loop, using `reader.getNumImages()`.


If you need more control of write parameters and the writing process, the common idiom for writing is something like:

    // Get the writer
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);

    if (!writers.hasNext()) {
        throw new IllegalArgumentException("No writer for: " + format);
    }

    ImageWriter writer = writers.next();

    try {
        // Create output stream
        ImageOutputStream output = ImageIO.createImageOutputStream(file);

        try {
            writer.setOutput(output);

            // Optionally, listen to progress, warnings, etc.

            ImageWriteParam param = writer.getDefaultWriteParam();

            // Optionally, control format specific settings of param (requires casting), or
            // control generic write settings like sub sampling, source region, output type etc.

            // Optionally, provide thumbnails and image/stream metadata
            writer.write(..., new IIOImage(..., image, ...), param);
        }
        finally {
            // Close stream in finally block to avoid resource leaks
            output.close();
        }
    }
    finally {
        // Dispose writer in finally block to avoid memory leaks
        writer.dispose();
    }

For more advanced usage, and information on how to use the ImageIO API, I suggest you read the
[Java Image I/O API Guide](http://docs.oracle.com/javase/7/docs/technotes/guides/imageio/spec/imageio_guideTOC.fm.html)
from Oracle.


#### Deploying the plugins in a web app

Because the `ImageIO` plugin registry (the `IIORegistry`) is "VM global", it doesn't by default work well with
servlet contexts. This is especially evident if you load plugins from the `WEB-INF/lib` or `classes` folder.
Unless you add `ImageIO.scanForPlugins()` somewhere in your code, the plugins might never be available at all.

I addition, servlet contexts dynamically loads and unloads classes (using a new class loader per context).
If you restart your application, old classes will by default remain in memory forever (because the next time
`scanForPlugins` is called, it's another `ClassLoader` that scans/loads classes, and thus they will be new instances
in the registry). If a read is attempted using one of the remaining "old" readers, weird exceptions
(like `NullPointerException`s when accessing `static final` initialized fields or `NoClassDefFoundError`s 
for uninitialized inner classes) may occur.

To work around both the discovery problem and the resource leak,
it is *strongly recommended* to use the `IIOProviderContextListener` that implements
dynamic loading and unloading of ImageIO plugins for web applications.

    <web-app ...>

    ...

        <listener>
            <display-name>ImageIO service provider loader/unloader</display-name>
            <listener-class>com.twelvemonkeys.servlet.image.IIOProviderContextListener</listener-class>
        </listener>

    ...

    </web-app>

Loading plugins from `WEB-INF/lib` without the context listener installed is unsupported and will not work correctly.

The context listener has no dependencies to the TwelveMonkeys ImageIO plugins, and may be used with JAI ImageIO 
or other ImageIO plugins as well.

Another safe option, is to place the JAR files in the application server's shared or common lib folder. 

#### Using the ResampleOp

The library comes with a resampling (image resizing) operation, that contains many different algorithms
to provide excellent results at reasonable speed.

    import com.twelvemonkeys.image.ResampleOp;

    ...

    BufferedImage input = ...; // Image to resample
    int width, height = ...; // new width/height

    BufferedImageOp resampler = new ResampleOp(width, height, ResampleOp.FILTER_LANCZOS); // A good default filter, see class documentation for more info
    BufferedImage output = resampler.filter(input, null);


#### Using the DiffusionDither

The library comes with a dithering operation, that can be used to convert `BufferedImage`s to `IndexColorModel` using
Floyd-Steinberg error-diffusion dither.

    import com.twelvemonkeys.image.DiffusionDither;

    ...

    BufferedImage input = ...; // Image to dither

    BufferedImageOp ditherer = new DiffusionDither();
    BufferedImage output = ditherer.filter(input, null);


## Building

Download the project (using [Git](http://git-scm.com/downloads)):

    $ git clone git@github.com:haraldk/TwelveMonkeys.git

This should create a folder named `TwelveMonkeys` in your current directory. Change directory to the `TwelveMonkeys`
folder, and issue the command below to build.

Build the project (using [Maven](http://maven.apache.org/download.cgi)):

    $ mvn package

Currently, the recommended JDK for making a build is Oracle JDK 7.x or 8.x. 

It's possible to build using OpenJDK, but some tests might fail due to some minor differences between the color management systems used. You will need to either disable the tests in question, or build without tests altogether.

Because the unit tests needs quite a bit of memory to run, you might have to set the environment variable `MAVEN_OPTS`
to give the Java process that runs Maven more memory. I suggest something like `-Xmx512m -XX:MaxPermSize=256m`.

Optionally, you can install the project in your local Maven repository using:

    $ mvn install

## Installing

To install the plug-ins,
either use Maven and add the necessary dependencies to your project,
or manually add the needed JARs along with required dependencies in class-path.

The ImageIO registry and service lookup mechanism will make sure the plugins are available for use.

To verify that the JPEG plugin is installed and used at run-time, you could use the following code:

    Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("JPEG");
    while (readers.hasNext()) {
        System.out.println("reader: " + readers.next());
    }

The first line should print:

    reader: com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageReader@somehash

#### Maven dependency example

To depend on the JPEG and TIFF plugin using Maven, add the following to your POM:

    ...
    <dependencies>
        ...
        <dependency>
            <groupId>com.twelvemonkeys.imageio</groupId>
            <artifactId>imageio-jpeg</artifactId>
            <version>3.2</version> <!-- Alternatively, build your own version -->
        </dependency>
        <dependency>
            <groupId>com.twelvemonkeys.imageio</groupId>
            <artifactId>imageio-tiff</artifactId>
            <version>3.2</version> <!-- Alternatively, build your own version -->
        </dependency>
    </dependencies>

#### Manual dependency example

To depend on the JPEG and TIFF plugin in your IDE or program, add all of the following JARs to your class path:

    twelvemonkeys-common-lang-3.2.jar
    twelvemonkeys-common-io-3.2.jar
    twelvemonkeys-common-image-3.2.jar
    twelvemonkeys-imageio-core-3.2.jar
    twelvemonkeys-imageio-metadata-3.2.jar
    twelvemonkeys-imageio-jpeg-3.2.jar
    twelvemonkeys-imageio-tiff-3.2.jar

### Links to prebuilt binaries

##### Latest version (3.2.x)

Requires Java 7 or later.
 
Common dependencies
* [common-lang-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/common/common-lang/3.2/common-lang-3.2.jar)
* [common-io-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/common/common-io/3.2/common-io-3.2.jar)
* [common-image-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/common/common-image/3.2/common-image-3.2.jar)

ImageIO dependencies
* [imageio-core-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-core/3.2/imageio-core-3.2.jar)
* [imageio-metadata-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-metadata/3.2/imageio-metadata-3.2.jar)

ImageIO plugins
* [imageio-bmp-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-bmp/3.2/imageio-bmp-3.2.jar)
* [imageio-jpeg-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-jpeg/3.2/imageio-jpeg-3.2.jar)
* [imageio-tiff-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-tiff/3.2/imageio-tiff-3.2.jar)
* [imageio-pnm-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-pnm/3.2/imageio-pnm-3.2.jar)
* [imageio-psd-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-psd/3.2/imageio-psd-3.2.jar)
* [imageio-hdr-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-hdr/3.2/imageio-hdr-3.2.jar)
* [imageio-iff-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-iff/3.2/imageio-iff-3.2.jar)
* [imageio-pcx-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-pcx/3.2/imageio-pcx-3.2.jar)
* [imageio-pict-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-pict/3.2/imageio-pict-3.2.jar)
* [imageio-sgi-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-sgi/3.2/imageio-sgi-3.2.jar)
* [imageio-tga-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-tga/3.2/imageio-tga-3.2.jar)
* [imageio-icns-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-icns/3.2/imageio-icns-3.2.jar)
* [imageio-thumbsdb-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-thumbsdb/3.2/imageio-thumbsdb-3.2.jar)

ImageIO plugins requiring 3rd party libs
* [imageio-batik-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-batik/3.2/imageio-batik-3.2.jar)

Photoshop Path support for ImageIO
* [imageio-clippath-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-clippath/3.2/imageio-clippath-3.2.jar)

Servlet support
* [servlet-3.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/servlet/servlet/3.2/servlet-3.2.jar)

##### Old version (3.0.x)

Use this version for projects that requires Java 6 or need the JMagick support. *Does not support Java 8*. 

Common dependencies
* [common-lang-3.0.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/common/common-lang/3.0.2/common-lang-3.0.2.jar)
* [common-io-3.0.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/common/common-io/3.0.2/common-io-3.0.2.jar)
* [common-image-3.0.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/common/common-image/3.0.2/common-image-3.0.2.jar)
 
ImageIO dependencies
* [imageio-core-3.0.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-core/3.0.2/imageio-core-3.0.2.jar)
* [imageio-metadata-3.0.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-metadata/3.0.2/imageio-metadata-3.0.2.jar)
 
ImageIO plugins
* [imageio-jpeg-3.0.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-jpeg/3.0.2/imageio-jpeg-3.0.2.jar)
* [imageio-tiff-3.0.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-tiff/3.0.2/imageio-tiff-3.0.2.jar)
* [imageio-psd-3.0.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-psd/3.0.2/imageio-psd-3.0.2.jar)
* [imageio-pict-3.0.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-pict/3.0.2/imageio-pict-3.0.2.jar)
* [imageio-iff-3.0.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-iff/3.0.2/imageio-iff-3.0.2.jar)
* [imageio-icns-3.0.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-icns/3.0.2/imageio-icns-3.0.2.jar)
* [imageio-ico-3.0.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-ico/3.0.2/imageio-ico-3.0.2.jar)
* [imageio-thumbsdb-3.0.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-thumbsdb/3.0.2/imageio-thumbsdb-3.0.2.jar)
 
ImageIO plugins requiring 3rd party libs
* [imageio-batik-3.0.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-batik/3.0.2/imageio-batik-3.0.2.jar)
* [imageio-jmagick-3.0.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-jmagick/3.0.2/imageio-jmagick-3.0.2.jar)
 
Servlet support
* [servlet-3.0.2.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/servlet/servlet/3.0.2/servlet-3.0.2.jar)


## License

The project is distributed under the OSI approved [BSD license](http://opensource.org/licenses/BSD-3-Clause):

    Copyright (c) 2008-2015, Harald Kuhr
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

    o Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    o Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.

    o Neither the name "TwelveMonkeys" nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
    "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
    LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
    A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
    CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


## FAQ

q: How do I use it?

a: The easiest way is to build your own project using Maven, and just add dependencies to the specific plug-ins you need.
 If you don't use Maven, make sure you have all the necessary JARs in classpath. See the Install section above.


q: What changes do I have to make to my code in order to use the plug-ins?

a: The short answer is: None. For basic usage, like `ImageIO.read(...)` or `ImageIO.getImageReaders(...)`, there is no need
to change your code. Most of the functionality is available through standard ImageIO APIs, and great care has been taken
 not to introduce extra API where none is necessary.

Should you want to use very specific/advanced features of some of the formats, you might have to use specific APIs, like
   setting base URL for an SVG image that consists of multiple files,
   or controlling the output compression of a TIFF file.


q: How does it work?

a: The TwelveMonkeys ImageIO project contains plug-ins for ImageIO. ImageIO uses a service lookup mechanism, to discover plug-ins at runtime. 

All you have have to do, is to make sure you have the TwelveMonkeys JARs in your classpath.

You can read more about the registry and the lookup mechanism in the [IIORegistry API doc](http://docs.oracle.com/javase/7/docs/api/javax/imageio/spi/IIORegistry.html).

The fine print: The TwelveMonkeys service providers for JPEG, BMP and TIFF, overrides the onRegistration method, and
utilizes the pairwise partial ordering mechanism of the `IIOServiceRegistry` to make sure it is installed before
the Sun/Oracle provided `JPEGImageReader` and `BMPImageReader`, and the Apple provided `TIFFImageReader` on OS X, 
respectively. Using the pairwise ordering will not remove any functionality form these implementations, but in most 
cases you'll end up using the TwelveMonkeys plug-ins instead.


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
