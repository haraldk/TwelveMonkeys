[![Build Status](https://travis-ci.org/haraldk/TwelveMonkeys.svg?branch=master)](https://travis-ci.org/haraldk/TwelveMonkeys) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.twelvemonkeys.imageio/imageio/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.twelvemonkeys.imageio/imageio)
[![StackOverflow](https://img.shields.io/badge/tag-twelvemonkeys-orange.svg)](https://stackoverflow.com/questions/tagged/twelvemonkeys)
[![Donate](https://img.shields.io/badge/donate-PayPal-blue.svg)](https://paypal.me/haraldk76/100)

## About

TwelveMonkeys ImageIO is a collection of plugins and extensions for Java's ImageIO.

These plugins extend the number of image file formats supported in Java, using the `javax.imageio.*` package.
The main purpose of this project is to provide support for formats not covered by the JRE itself.

Support for formats is important, both to be able to read data found
"in the wild", as well as to maintain access to data in legacy formats.
Because there is lots of legacy data out there, we see the need for open implementations of readers for popular formats.
The goal is to create a set of efficient and robust ImageIO plug-ins, that can be distributed independently.

----

## File formats supported

| Plugin | Format   | Description           | Read | Write | Metadata | Notes |
| ------ | -------- | -----------           |:----:|:-----:| -------- | ----- |
| Batik  | **SVG**  | Scalable Vector Graphics | ✔ |  -  | - | Requires [Batik](https://xmlgraphics.apache.org/batik/)
|        | WMF      | MS Windows Metafile      | ✔ |  -  | - | Requires [Batik](https://xmlgraphics.apache.org/batik/)
| [BMP](https://github.com/haraldk/TwelveMonkeys/wiki/BMP-Plugin)    | **BMP**  | MS Windows and IBM OS/2 Device Independent Bitmap | ✔  | ✔   | Native & Standard |
|        | CUR      | MS Windows Cursor Format  | ✔  | -   | - |
|        | ICO      | MS Windows Icon Format    | ✔  | ✔   | - | 
| [HDR](https://github.com/haraldk/TwelveMonkeys/wiki/HDR-Plugin)    | HDR      | Radiance High Dynamic Range RGBE Format | ✔  | - | Standard | 
| [ICNS](https://github.com/haraldk/TwelveMonkeys/wiki/ICNS-Plugin)   | ICNS     | Apple Icon Image | ✔  | ✔   | -  | 
| [IFF](https://github.com/haraldk/TwelveMonkeys/wiki/IFF-Plugin)    | IFF      | Commodore Amiga/Electronic Arts Interchange File Format | ✔  | ✔   | - | 
| [JPEG](https://github.com/haraldk/TwelveMonkeys/wiki/JPEG-Plugin)   | **JPEG** | Joint Photographers Expert Group | ✔  | ✔   | Native & Standard |  
| [PCX](https://github.com/haraldk/TwelveMonkeys/wiki/PCX-Plugin)    | PCX      | ZSoft Paintbrush Format | ✔  | -  | Standard | 
|        | DCX      | Multi-page PCX fax document | ✔  | -   | Standard | 
| [PICT](https://github.com/haraldk/TwelveMonkeys/wiki/PICT-Plugin)   | PICT     | Apple Mac Paint Picture Format | ✔  | -   | - | 
| [PNM](https://github.com/haraldk/TwelveMonkeys/wiki/PNM-Plugin)    | PAM      | NetPBM Portable Any Map | ✔  | ✔   | Standard | 
|        | PBM      | NetPBM Portable Bit Map | ✔  | -     | Standard | 
|        | PGM      | NetPBM Portable Grey Map | ✔  | -   | Standard | 
|        | PPM      | NetPBM Portable Pix Map | ✔  | ✔   | Standard | 
|        | PFM      | Portable Float Map | ✔  | -   | Standard | 
| [PSD](https://github.com/haraldk/TwelveMonkeys/wiki/PSD-Plugin)    | **PSD**  | Adobe Photoshop Document | ✔  | - | Native & Standard |  
|        |  PSB     | Adobe Photoshop Large Document | ✔  | - | Native & Standard | 
| [SGI](https://github.com/haraldk/TwelveMonkeys/wiki/SGI-Plugin)    | SGI      | Silicon Graphics Image Format  | ✔  | -   | Standard | 
| [TGA](https://github.com/haraldk/TwelveMonkeys/wiki/TGA-Plugin)    | TGA      | Truevision TGA Image Format  | ✔  | ✔   | Standard | 
|ThumbsDB| Thumbs.db| MS Windows Thumbs DB | ✔  | -   | - | OLE2 Compound Document based format only
| [TIFF](https://github.com/haraldk/TwelveMonkeys/wiki/TIFF-Plugin)   | **TIFF** | Aldus/Adobe Tagged Image File Format | ✔  | ✔ | Native & Standard | 
|        | BigTIFF  |  | ✔  | - | Native & Standard |  
| [WebP](https://github.com/haraldk/TwelveMonkeys/wiki/WebP-Plugin)   | **WebP** | Google WebP Format | ✔  | - | Standard |  In progress
| XWD    | XWD      | X11 Window Dump Format | ✔  | -   | Standard | 


**Important note on using Batik:** *Please read [The Apache™ XML Graphics Project - Security](http://xmlgraphics.apache.org/security.html), 
and make sure you use either version 1.6.1, 1.7.1, 1.8+ or later.*

## Basic usage

Most of the time, all you need to do is simply include the plugins in your project and write:

```java
BufferedImage image = ImageIO.read(file);
```

This will load the first image of the file, entirely into memory.

The basic and simplest form of writing is:

```java
if (!ImageIO.write(image, format, file)) {
   // Handle image not written case
}
```

This will write the entire image into a single file, using the default settings for the given format.

The plugins are discovered automatically at run time. See the [FAQ](#faq) for more info on how this mechanism works.

## Advanced usage

If you need more control of read parameters and the reading process, the common idiom for reading is something like:

```java
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
```

Query the reader for source image dimensions using `reader.getWidth(n)` and `reader.getHeight(n)` without reading the
entire image into memory first.

It's also possible to read multiple images from the same file in a loop, using `reader.getNumImages()`.


If you need more control of write parameters and the writing process, the common idiom for writing is something like:

```java
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
```

For more advanced usage, and information on how to use the ImageIO API, I suggest you read the
[Java Image I/O API Guide](http://docs.oracle.com/javase/7/docs/technotes/guides/imageio/spec/imageio_guideTOC.fm.html)
from Oracle.

#### Adobe Clipping Path support

```java
import com.twelvemonkeys.imageio.path.Paths;

...

try (ImageInputStream stream = ImageIO.createImageInputStream(new File("image_with_path.jpg")) {
    BufferedImage image = Paths.readClipped(stream);

    // Do something with the clipped image...
}
```
See [Adobe Clipping Path support on the Wiki](https://github.com/haraldk/TwelveMonkeys/wiki/Photoshop-Clipping-Path-support) for more details and example code.


#### Using the ResampleOp

The library comes with a resampling (image resizing) operation, that contains many different algorithms
to provide excellent results at reasonable speed.

```java
import com.twelvemonkeys.image.ResampleOp;

...

BufferedImage input = ...; // Image to resample
int width, height = ...; // new width/height

BufferedImageOp resampler = new ResampleOp(width, height, ResampleOp.FILTER_LANCZOS); // A good default filter, see class documentation for more info
BufferedImage output = resampler.filter(input, null);
```

#### Using the DiffusionDither

The library comes with a dithering operation, that can be used to convert `BufferedImage`s to `IndexColorModel` using
Floyd-Steinberg error-diffusion dither.

```java
import com.twelvemonkeys.image.DiffusionDither;

...

BufferedImage input = ...; // Image to dither

BufferedImageOp ditherer = new DiffusionDither();
BufferedImage output = ditherer.filter(input, null);
```

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

```java
Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("JPEG");
while (readers.hasNext()) {
    System.out.println("reader: " + readers.next());
}
```

The first line should print:

    reader: com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageReader@somehash

#### Maven dependency example

To depend on the JPEG and TIFF plugin using Maven, add the following to your POM:

```xml
...
<dependencies>
    ...
    <dependency>
        <groupId>com.twelvemonkeys.imageio</groupId>
        <artifactId>imageio-jpeg</artifactId>
        <version>3.6.1</version>
    </dependency>
    <dependency>
        <groupId>com.twelvemonkeys.imageio</groupId>
        <artifactId>imageio-tiff</artifactId>
        <version>3.6.1</version>
    </dependency>

    <!--
    Optional dependency. Needed only if you deploy `ImageIO` plugins as part of a web app.
    Make sure you add the `IIOProviderContextListener` to your `web.xml`, see above.
    -->
    <dependency>
        <groupId>com.twelvemonkeys.servlet</groupId>
        <artifactId>servlet</artifactId>
        <version>3.6.1</version>
    </dependency>
</dependencies>
```

#### Manual dependency example

To depend on the JPEG and TIFF plugin in your IDE or program, add all of the following JARs to your class path:

    twelvemonkeys-common-lang-3.6.1.jar
    twelvemonkeys-common-io-3.6.1.jar
    twelvemonkeys-common-image-3.6.1.jar
    twelvemonkeys-imageio-core-3.6.1.jar
    twelvemonkeys-imageio-metadata-3.6.1.jar
    twelvemonkeys-imageio-jpeg-3.6.1.jar
    twelvemonkeys-imageio-tiff-3.6.1.jar

#### Deploying the plugins in a web app

Because the `ImageIO` plugin registry (the `IIORegistry`) is "VM global", it doesn't by default work well with
servlet contexts. This is especially evident if you load plugins from the `WEB-INF/lib` or `classes` folder.
Unless you add `ImageIO.scanForPlugins()` somewhere in your code, the plugins might never be available at all.

In addition, servlet contexts dynamically loads and unloads classes (using a new class loader per context).
If you restart your application, old classes will by default remain in memory forever (because the next time
`scanForPlugins` is called, it's another `ClassLoader` that scans/loads classes, and thus they will be new instances
in the registry). If a read is attempted using one of the remaining "old" readers, weird exceptions
(like `NullPointerException`s when accessing `static final` initialized fields or `NoClassDefFoundError`s 
for uninitialized inner classes) may occur.

To work around both the discovery problem and the resource leak,
it is *strongly recommended* to use the `IIOProviderContextListener` that implements
dynamic loading and unloading of ImageIO plugins for web applications.

```xml
<web-app ...>

...

    <listener>
        <display-name>ImageIO service provider loader/unloader</display-name>
        <listener-class>com.twelvemonkeys.servlet.image.IIOProviderContextListener</listener-class>
    </listener>

...

</web-app>
```

Loading plugins from `WEB-INF/lib` without the context listener installed is unsupported and will not work correctly.

The context listener has no dependencies to the TwelveMonkeys ImageIO plugins, and may be used with JAI ImageIO 
or other ImageIO plugins as well.

Another safe option, is to place the JAR files in the application server's shared or common lib folder. 

#### Including the plugins in a "fat" JAR

The recommended way to use the plugins, is just to include the JARs as-is in your project, through a Maven dependency or similar. 
Re-packaging is not necessary to use the library, and not recommended. 
 
However, if you like to create a "fat" 
JAR, or otherwise like to re-package the JARs for some reason, it's important to remember that automatic discovery of 
the plugins by ImageIO depends on the 
[Service Provider Interface (SPI)](https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html) mechanism. 
In short, each JAR contains a special folder, named `META-INF/services` containing one or more files, 
typically `javax.imageio.spi.ImageReaderSpi` and `javax.imageio.spi.ImageWriterSpi`. 
These files exist *with the same name in every JAR*, 
so if you simply unpack everything to a single folder or create a JAR, files will be overwritten and behavior be 
unspecified (most likely you will end up with a single plugin being installed). 

The solution is to make sure all files with the same name, are merged to a single file, 
containing all the SPI information of each type. If using the Maven Shade plugin, you should use the 
[ServicesResourceTransformer](https://maven.apache.org/plugins/maven-shade-plugin/examples/resource-transformers.html#ServicesResourceTransformer)
to properly merge these files. You may also want to use the 
[ManifestResourceTransforme](https://maven.apache.org/plugins/maven-shade-plugin/examples/resource-transformers.html#ManifestResourceTransformer) 
to get the correct vendor name, version info etc. 
Other "fat" JAR bundlers will probably have similar mechanisms to merge entries with the same name.

### Links to prebuilt binaries

##### Latest version (3.6.1)

Requires Java 7 or later.
 
Common dependencies
* [common-lang-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/common/common-lang/3.6.1/common-lang-3.6.1.jar)
* [common-io-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/common/common-io/3.6.1/common-io-3.6.1.jar)
* [common-image-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/common/common-image/3.6.1/common-image-3.6.1.jar)

ImageIO dependencies
* [imageio-core-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-core/3.6.1/imageio-core-3.6.1.jar)
* [imageio-metadata-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-metadata/3.6.1/imageio-metadata-3.6.1.jar)

ImageIO plugins
* [imageio-bmp-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-bmp/3.6.1/imageio-bmp-3.6.1.jar)
* [imageio-jpeg-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-jpeg/3.6.1/imageio-jpeg-3.6.1.jar)
* [imageio-tiff-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-tiff/3.6.1/imageio-tiff-3.6.1.jar)
* [imageio-pnm-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-pnm/3.6.1/imageio-pnm-3.6.1.jar)
* [imageio-psd-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-psd/3.6.1/imageio-psd-3.6.1.jar)
* [imageio-hdr-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-hdr/3.6.1/imageio-hdr-3.6.1.jar)
* [imageio-iff-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-iff/3.6.1/imageio-iff-3.6.1.jar)
* [imageio-pcx-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-pcx/3.6.1/imageio-pcx-3.6.1.jar)
* [imageio-pict-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-pict/3.6.1/imageio-pict-3.6.1.jar)
* [imageio-sgi-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-sgi/3.6.1/imageio-sgi-3.6.1.jar)
* [imageio-tga-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-tga/3.6.1/imageio-tga-3.6.1.jar)
* [imageio-icns-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-icns/3.6.1/imageio-icns-3.6.1.jar)
* [imageio-thumbsdb-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-thumbsdb/3.6.1/imageio-thumbsdb-3.6.1.jar)

ImageIO plugins requiring 3rd party libs
* [imageio-batik-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-batik/3.6.1/imageio-batik-3.6.1.jar)

Photoshop Path support for ImageIO
* [imageio-clippath-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/imageio/imageio-clippath/3.6.1/imageio-clippath-3.6.1.jar)

Servlet support
* [servlet-3.6.1.jar](http://search.maven.org/remotecontent?filepath=com/twelvemonkeys/servlet/servlet/3.6.1/servlet-3.6.1.jar)

##### Old version (3.0.x)

Use this version for projects that requires Java 6 or need the JMagick support. *Does not support Java 8 or later*.

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

    Copyright (c) 2008-2020, Harald Kuhr
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

    o Redistributions of source code must retain the above copyright notice, this
      list of conditions and the following disclaimer.

    o Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.

    o Neither the name of the copyright holder nor the names of its
      contributors may be used to endorse or promote products derived from
      this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
    DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
    FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
    DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
    SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
    CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
    OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
    OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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


q: What about JMagick or IM4Java? Can't you just use what's already available?

a: While great libraries with a wide range of formats support, the ImageMagick-based libraries has some disadvantages
compared to ImageIO.
- No real stream support, these libraries only work with files.
- No easy access to pixel data through standard Java2D/BufferedImage API.
- Not a pure Java solution, requires system specific native libs.


-----

We did it
