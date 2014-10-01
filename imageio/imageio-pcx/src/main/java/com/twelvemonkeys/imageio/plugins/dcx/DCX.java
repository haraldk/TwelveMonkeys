package com.twelvemonkeys.imageio.plugins.dcx;

/**
 * The DXC file format, is just a small header before a number of PCX streams.
 * Mostly used as a FAX format.
 *
 * @see <a href="http://www.fileformat.info/format/pcx/egff.htm#PCX-DMYID.3.8">[PCX] Related File Formats</a>
 */
interface DCX {
    int MAGIC = 0x3ADE68B1;
}
