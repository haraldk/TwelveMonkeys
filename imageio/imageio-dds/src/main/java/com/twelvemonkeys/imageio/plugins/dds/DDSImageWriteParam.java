package com.twelvemonkeys.imageio.plugins.dds;

import javax.imageio.ImageWriteParam;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class DDSImageWriteParam extends ImageWriteParam {

    static final DDSType DEFAULT_TYPE = DDSType.DXT5;
    private static final String[] COMPRESSION_TYPES = compressionTypes();

    private static String[] compressionTypes() {
        // TODO: Maybe hardcode subset of values that we actually support writing?
        List<String> compressionTypes = Arrays.stream(DDSType.values())
            .filter(DDSType::isBlockCompression)
            .map(Enum::name)
            .collect(Collectors.toList());
        compressionTypes.add(0, "None");

        return compressionTypes.toArray(new String[0]);
    }

    private int optionalBitFlags;
    private boolean writeDXT10;

    DDSImageWriteParam() {
        canWriteCompressed = true;
        compressionTypes = COMPRESSION_TYPES;
        compressionType = DEFAULT_TYPE.name();
        setLinearSize();
    }

    // TODO: Set this always for compressed images?
    public void setLinearSize() {
        optionalBitFlags |= DDS.FLAG_LINEARSIZE;
    }

    public void clearLinearSize() {
        optionalBitFlags &= ~DDS.FLAG_LINEARSIZE;
    }

    // TODO: Set this always for uncompressed images?
    public void setPitch() {
        optionalBitFlags |= DDS.FLAG_PITCH;
    }

    public void clearPitch() {
        optionalBitFlags &= ~DDS.FLAG_PITCH;
    }

    // TODO: Other flags?

    public int optionalBitFlags() {
        return optionalBitFlags;
    }

    public void setWriteDX10() {
        writeDXT10 = true;
    }

    public void clearWriteDX10() {
        writeDXT10 = false;
    }

    public boolean isWriteDXT10() {
        return writeDXT10;
    }

    BlockCompression compression() {
        DDSType type = type();

        if (type != null) {
            return type.compression;
        }

        return null;
    }

    DDSType type() {
        if (compressionType == null || compressionType.equals("None")) {
            return null;
        }

        return DDSType.valueOf(compressionType);
    }

    int getDxgiFormat() {
        DDSType type = type();

        if (type != null) {
            return type.dxgiFormat();
        }

        return DXGI.DXGI_FORMAT_UNKNOWN;

    }
}
