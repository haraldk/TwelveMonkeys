Sample JPEG XL files from the JXLatte project, https://github.com/Traneptora/jxlatte
Copyright 2022-2024 Leo Izen (Traneptora), MIT License:
  art.jxl, bench.jxl, blendmodes_5.jxl, lenna.jxl, patches-lossless.jxl, white.jxl

*-jxlatte.png are reference images, decoded to 8 bit sRGB PNG by the JXLatte command line tool:
  java -jar jxlatte.jar --png-depth=8 --png-compression=9 <file>.jxl <file>-jxlatte.png

Synthetic 64x48 test images, created for this project:
  gray8.jxl, gray16.jxl, gray16-linear.jxl, graya8.jxl, grayf32-lossless.jxl, grayf32-lossy.jxl,
  rgb16.jxl, rgba16.jxl, rgb8-icc.jxl, rgb16-pq.jxl

The source PNGs are written by make-test-images.py (the pixel formulas are mirrored in JXLImageReaderTest),
and encoded with ffmpeg using libjxl 0.11:
  python3 make-test-images.py <out> /usr/share/color/icc/colord/SwappedRedAndGreen.icc
  ffmpeg -threads 1 -i <in>.png -frames:v 1 -update 1 -c:v libjxl -threads 1 -distance 0 -pix_fmt <fmt> [-vf <filter>] <out>.jxl

  gray8.jxl             gray8.png,  -pix_fmt gray
  gray16.jxl            gray16.png, -pix_fmt gray16le
  gray16-linear.jxl     gray16.png, -pix_fmt gray16le, -vf setparams=color_trc=linear
  graya8.jxl            graya8.png, -pix_fmt ya8
  grayf32-lossless.jxl  gray16.png, -pix_fmt grayf32le (tagged linear by ffmpeg)
  grayf32-lossy.jxl     gray16.png, -pix_fmt grayf32le, -distance 1 (instead of 0)
  rgb16.jxl             rgb16.png,  -pix_fmt rgb48le
  rgba16.jxl            rgba16.png, -pix_fmt rgba64le
  rgb8-icc.jxl          rgb8-icc.png (embedded ICC profile), -pix_fmt rgb24
  rgb16-pq.jxl          rgb16-pq.png, -pix_fmt rgb48le,
                        -vf setparams=color_primaries=bt2020:color_trc=smpte2084:colorspace=bt2020nc

gray8-meta.jxl is gray8.jxl wrapped in a container with Exif, brob, split codestream (jxlp) and XMP boxes:
  python3 make-test-container.py gray8.jxl gray8-meta.jxl
gray8-brob.jxl is the same, but with Brotli compressed (brob) Exif and XMP boxes (needs the brotli CLI, 1.2.0 used):
  python3 make-test-container.py gray8.jxl gray8-brob.jxl --brob

SwappedRedAndGreen.icc (embedded in rgb8-icc.jxl) is from colord, Copyright 2012 Richard Hughes, CC0-1.0.
