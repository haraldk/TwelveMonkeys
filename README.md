We did it

-----

## Background

TwelveMonkeys ImageIO is a collection of plug-ins for Java's ImageIO.


These plugins extends the number of file formats supported in Java, using the javax.imageio.* package.
The main purpose of this project is to provide support for formats not covered by the JDK itself.

Support for formats is important, both to be able to read data found
"in the wild", as well as to maintain access to data in legacy formats.
Because there is lots of legacy data out there, we see the need for open implementations of readers for popular formats.
The goal is to create a set of efficient and robust ImageIO plug-ins, that can be distributed independently.



## Features

Mainstream format support

# JPEG

    full EXIF support
    support for CMYK/YCCK


# JPEG-LS

    possibly coming in the future

# JPEG-2000

    possibly coming in the future, pending some license issues

# PSD

    read-only support

# TIFF

    read-only support (for now)

# PICT

    Legacy format, especially useful for reading OS X clipboard data.
    read and limited write support

# IFF

    Legacy format, allows reading popular image from the Commodore Amiga computer.
    read and write support


Icon/other formats

# ICNS

# ICO

# Thumbs.db


Other formats, using 3rd party libraries

# SVG

    read-only support using Batik

# WMF

    limited read-only support using Batik


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
 If you don't use Maven, make sure you have all the necessary JARs in classpath. See the Install section below.


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
