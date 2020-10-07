package com.twelvemonkeys.imageio.plugins.xwd;

interface X11 {
//    int X10_HEADER_SIZE = 40;
//    int X10_HEADER_VERSION = 0x06;

    int X11_HEADER_SIZE = 100;
    int X11_HEADER_VERSION = 0x07;

    int PIXMAP_FORMAT_XYBITMAP = 0; // 1 bit format
    int PIXMAP_FORMAT_XYPIXMAP = 1; // single plane bitmap
    int PIXMAP_FORMAT_ZPIXMAP  = 2; // multiple bitplanes

    int VISUAL_CLASS_STATIC_GRAY  = 0;
    int VISUAL_CLASS_GRAY_SCALE   = 1;
    int VISUAL_CLASS_STATIC_COLOR = 2;
    int VISUAL_CLASS_PSEUDO_COLOR = 3;
    int VISUAL_CLASS_TRUE_COLOR   = 4;
    int VISUAL_CLASS_DIRECT_COLOR = 5;
}
