package com.twelvemonkeys.imageio.plugins.dds;

import javax.imageio.IIOException;

public enum DDSType {

    DXT1(0x31545844),
    DXT2(0x32545844),
    DXT3(0x33545844),
    DXT4(0x34545844),
    DXT5(0x35545844),
    A1R5G5B5((1 << 16) | 2),
    X1R5G5B5((2 << 16) | 2),
    A4R4G4B4((3 << 16) | 2),
    X4R4G4B4((4 << 16) | 2),
    R5G6B5((5 << 16) | 2),
    R8G8B8((1 << 16) | 3),
    A8B8G8R8((1 << 16) | 4),
    X8B8G8R8((2 << 16) | 4),
    A8R8G8B8((3 << 16) | 4),
    X8R8G8B8((4 << 16) | 4);

    private final int value;

    DDSType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static DDSType parse(int type) throws IIOException {
        for (DDSType t : DDSType.values()) {
            if (type == t.value()) {
                return t;
            }
        }
        throw new IIOException("Unknown type: " + Integer.toHexString(type));
    }
}
