package com.twelvemonkeys.imageio.plugins.dds;

import com.twelvemonkeys.util.LinkedSet;

import javax.imageio.ImageWriteParam;
import java.util.Objects;
import java.util.Set;

public class DDSWriterParam extends ImageWriteParam {
    public static final DDSWriterParam DEFAULT_PARAM = DDSWriterParam.builder().formatBC3().build();
    private final int optionalBitFlags;
    private final DDSEncoderType encoderType;
    private final boolean enableDxt10;

    DDSWriterParam(int optionalBitFlags, DDSEncoderType encoderType, boolean isUsingDxt10) {
        super();
        this.optionalBitFlags = optionalBitFlags;
        this.encoderType = encoderType;
        this.enableDxt10 = isUsingDxt10;
    }

    public static Builder builder() {
        return new Builder();
    }

    int getOptionalBitFlags() {
        return this.optionalBitFlags;
    }

    DDSEncoderType getEncoderType() {
        return this.encoderType;
    }

    public boolean isUsingDxt10() {
        return enableDxt10;
    }

    int getDxgiFormat() {
        return getEncoderType().getDx10Format();
    }

    public static final class Builder {
        //we use Set collection to prevent duplications of bitflag setter calls
        private final Set<Integer> optionalBitFlags;
        private DDSEncoderType encoderType;
        private boolean isUsingDxt10;

        public Builder() {
            this.optionalBitFlags = new LinkedSet<>();
            encoderType = null;
            isUsingDxt10 = false;
        }

        /**
         * Enable saving file as Direct3D 10+ format.
         */
        public Builder enableDX10() {
            isUsingDxt10 = true;
            return this;
        }

        /**
         * Set the compression type to be BC1 (DXT1).
         * If DXT10 is enabled, this will set DXGI Format to DXGI_FORMAT_BC1_UNORM.
         */
        public Builder formatBC1() {
            encoderType = DDSEncoderType.BC1;
            return flagLinearSize();
        }

        /**
         * Set the compression type to be BC2 (DXT2/DXT3).
         * If DXT10 is enabled, this will set DXGI Format to DXGI_FORMAT_BC2_UNORM.
         */
        public Builder formatBC2() {
            encoderType = DDSEncoderType.BC2;
            return flagLinearSize();
        }

        /**
         * Set the compression type to be BC3 (DXT4/DXT5).
         * If DXT10 is enabled, this will set DXGI Format to DXGI_FORMAT_BC3_UNORM.
         */
        public Builder formatBC3() {
            encoderType = DDSEncoderType.BC3;
            return flagLinearSize();
        }

        /**
         * Set the compression type to be BC4 (BC4U).
         * This will set DXGI Format to DXGI_FORMAT_BC4_UNORM.
         */
        public Builder formatBC4() {
            encoderType = DDSEncoderType.BC4;
            return flagLinearSize();
        }

        /**
         * Set the compression type to be BC5 (BC5U).
         * This will set DXGI Format to DXGI_FORMAT_BC5_UNORM.
         */
        public Builder formatBC5() {
            encoderType = DDSEncoderType.BC5;
            return flagLinearSize();
        }

        /**
         * Set the encoding type to be B8G8R8A8.
         * If DXT10 is enabled, this will set DXGI Format to DXGI_FORMAT_B8G8R8A8_UNORM_SRGB
         */
        public Builder formatB8G8R8A8() {
            encoderType = DDSEncoderType.BRGA32;
            return this;
        }

        /**
         * Set the encoding type to be B8G8R8X8.
         * If DXT10 is enabled, this will set DXGI Format to DXGI_FORMAT_B8G8R8X8_UNORM_SRGB
         */
        public Builder formatB8G8R8X8() {
            encoderType = DDSEncoderType.BGRX32;
            return this;
        }

        /**
         * Set the encoding type to be R8G8B8A8.
         * If DXT10 is enabled, this will set DXGI Format to DXGI_FORMAT_R8G8B8A8_UNORM_SRGB
         */
        public Builder formatR8G8B8A8() {
            encoderType = DDSEncoderType.RGBA32;
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
            Objects.requireNonNull(encoderType, "no DDS format specified.");
            return new DDSWriterParam(optionalBitFlags.stream().reduce((i1, i2) -> i1 | i2).orElse(0), encoderType, isUsingDxt10);
        }
    }
}
