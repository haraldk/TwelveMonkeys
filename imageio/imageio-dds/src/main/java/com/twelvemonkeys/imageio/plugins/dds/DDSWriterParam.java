package com.twelvemonkeys.imageio.plugins.dds;

import com.twelvemonkeys.util.LinkedSet;

import javax.imageio.ImageWriteParam;
import java.util.Objects;
import java.util.Set;

public class DDSWriterParam extends ImageWriteParam {
    static final DDSWriterParam DEFAULT_PARAM = DDSWriterParam.builder().formatBC1().build();
    private final int optionalBitFlags;
    private final DDSType ddsType;
    private final boolean enableDxt10;
    private final int dxgiFormat;

    DDSWriterParam(int optionalBitFlags, DDSType ddsType, boolean isUsingDxt10, int dxgiFormat) {
        super();
        this.optionalBitFlags = optionalBitFlags;
        this.ddsType = ddsType;
        this.enableDxt10 = isUsingDxt10;
        this.dxgiFormat = dxgiFormat;
    }

    public static Builder builder() {
        return new Builder();
    }

    int getOptionalBitFlags() {
        return this.optionalBitFlags;
    }

    DDSType getType() {
        return this.ddsType;
    }

    public boolean isUsingDxt10() {
        return enableDxt10;
    }

    int getDxgiFormat() {
        return dxgiFormat;
    }

    public static final class Builder {
        //we use Set collection to prevent duplications of bitflag setter calls
        private final Set<Integer> optionalBitFlags;
        private DDSType ddsType;
        private boolean isUsingDxt10;
        private int dxgiFormat;

        public Builder() {
            this.optionalBitFlags = new LinkedSet<>();
            ddsType = null;
            isUsingDxt10 = false;
        }

        /**
         * Enable saving file as Direct3D 10+ format.
         */
        public Builder formatDXT10() {
            isUsingDxt10 = true;
            return this;
        }

        /**
         * Set the compression type to be BC1 (DXT1).
         * If DXT10 is enabled, this will set DXGI Format to DXGI_FORMAT_BC1_UNORM_SRGB
         */
        public Builder formatBC1() {
            ddsType = DDSType.DXT1;
            dxgiFormat = DDS.DXGI_FORMAT_BC1_UNORM_SRGB;
            return this;
        }

        /**
         * Set the compression type to be BC2 (DXT2/DXT3).
         * If DXT10 is enabled, this will set DXGI Format to DXGI_FORMAT_BC2_UNORM_SRGB.
         */
        public Builder formatBC2() {
            ddsType = DDSType.DXT2;
            dxgiFormat = DDS.DXGI_FORMAT_BC2_UNORM_SRGB;
            return this;
        }

        /**
         * Set the compression type to be BC3 (DXT4/DXT5).
         * If DXT10 is enabled, this will set DXGI Format to DXGI_FORMAT_BC3_UNORM_SRGB.
         */
        public Builder formatBC3() {
            ddsType = DDSType.DXT5;
            dxgiFormat = DDS.DXGI_FORMAT_BC3_UNORM_SRGB;
            return this;
        }

        /**
         * Set the encoding type to be B8G8R8A8.
         * If DXT10 is enabled, this will set DXGI Format to DXGI_FORMAT_B8G8R8A8_UNORM_SRGB
         */
        public Builder formatB8G8R8A8() {
            ddsType = DDSType.A8B8G8R8;
            dxgiFormat = DDS.DXGI_FORMAT_B8G8R8A8_UNORM_SRGB;
            return this;
        }

        /**
         * Set the encoding type to be B8G8R8X8.
         * If DXT10 is enabled, this will set DXGI Format to DXGI_FORMAT_B8G8R8X8_UNORM_SRGB
         */
        public Builder formatB8G8R8X8() {
            ddsType = DDSType.X8B8G8R8;
            dxgiFormat = DDS.DXGI_FORMAT_B8G8R8X8_UNORM_SRGB;
            return this;
        }

        /**
         * Set the encoding type to be R8G8B8A8.
         * If DXT10 is enabled, this will set DXGI Format to DXGI_FORMAT_R8G8B8A8_UNORM_SRGB
         */
        public Builder formatR8G8B8A8() {
            ddsType = DDSType.A8R8G8B8;
            dxgiFormat = DDS.DXGI_FORMAT_R8G8B8A8_UNORM_SRGB;
            return this;
        }

        /**
         * Set bitflag DDSD_PITCH. Required when pitch is provided for an <b><i>uncompressed</i></b> texture.
         */
        public Builder flagPitch() {
            optionalBitFlags.add(DDS.FLAG_PITCH);
            return this;
        }

        /**
         * Set bitflag DDSD_MIPMAPCOUNT. Required in a mipmapped texture.
         */
        public Builder flagMipmapCount() {
            optionalBitFlags.add(DDS.FLAG_MIPMAPCOUNT);
            return this;
        }

        /**
         *
         * Set bitflag DDSD_LINEARSIZE. Required when pitch is provided for a <b><i>compressed</i></b> texture.
         */
        public Builder flagLinearSize() {
            optionalBitFlags.add(DDS.FLAG_LINEARSIZE);
            return this;
        }

        /**
         * Set bitflag DDSD_DEPTH. Required in a depth texture.
         */
        public Builder flagDepth() {
            optionalBitFlags.add(DDS.FLAG_DEPTH);
            return this;
        }

        public DDSWriterParam build() {
            Objects.requireNonNull(ddsType, "no DDS format specified.");
            return new DDSWriterParam(optionalBitFlags.stream().reduce((i1, i2) -> i1 | i2).orElse(0), ddsType, isUsingDxt10, dxgiFormat);
        }
    }
}
