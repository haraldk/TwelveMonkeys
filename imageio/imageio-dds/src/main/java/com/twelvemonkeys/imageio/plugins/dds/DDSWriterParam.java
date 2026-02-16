package com.twelvemonkeys.imageio.plugins.dds;

import javax.imageio.ImageWriteParam;
import java.util.Objects;

public class DDSWriterParam extends ImageWriteParam {
    public static final DDSWriterParam DEFAULT_PARAM = DDSWriterParam.builder().formatBC5().build();
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
        private int optionalBitFlag;
        private DDSEncoderType encoderType;
        private boolean isUsingDxt10;

        public Builder() {
            this.optionalBitFlag = 0;
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
            return setFlag(DDSFlags.DDSD_LINEARSIZE);
        }

        /**
         * Set the compression type to be BC2 (DXT3).
         * If DXT10 is enabled, this will set DXGI Format to DXGI_FORMAT_BC2_UNORM.
         */
        public Builder formatBC2() {
            encoderType = DDSEncoderType.BC2;
            return setFlag(DDSFlags.DDSD_LINEARSIZE);
        }

        /**
         * Set the compression type to be BC3 (DXT5).
         * If DXT10 is enabled, this will set DXGI Format to DXGI_FORMAT_BC3_UNORM.
         */
        public Builder formatBC3() {
            encoderType = DDSEncoderType.BC3;
            return setFlag(DDSFlags.DDSD_LINEARSIZE);
        }

        /**
         * Set the compression type to be BC4 (ATI1).
         * If DXT10 is enabled, This will set DXGI Format to DXGI_FORMAT_BC4_UNORM.
         */
        public Builder formatBC4() {
            encoderType = DDSEncoderType.BC4;
            return setFlag(DDSFlags.DDSD_LINEARSIZE);
        }

        /**
         * Set the compression type to be BC5 (ATI2).
         * This will set DXGI Format to DXGI_FORMAT_BC5_UNORM.
         */
        public Builder formatBC5() {
            encoderType = DDSEncoderType.BC5;
            return setFlag(DDSFlags.DDSD_LINEARSIZE);
        }

        public Builder setFlag(DDSFlags flag) {
            optionalBitFlag |= flag.getValue();
            return this;
        }

        /**
         * Set other optional flags for the DDS Header.
         */
        public Builder setFlags(DDSFlags... flags) {
            for (DDSFlags flag : flags)
                setFlag(flag);
            return this;
        }

        public DDSWriterParam build() {
            Objects.requireNonNull(encoderType, "no DDS format specified.");
            return new DDSWriterParam(optionalBitFlag, encoderType, isUsingDxt10);
        }

        public enum DDSFlags {
            DDSD_PITCH(DDS.FLAG_PITCH),// Required when pitch is provided for an uncompressed texture.
            DDSD_MIPMAPCOUNT(DDS.FLAG_MIPMAPCOUNT),// Required in a mipmapped texture.
            DDSD_LINEARSIZE(DDS.FLAG_LINEARSIZE),// Required when pitch is provided for a compressed texture.
            DDSD_DEPTH(DDS.FLAG_DEPTH);// Required in a depth texture.

            private final int flag;
            DDSFlags(int flag) {
                this.flag = flag;
            }

            public int getValue() {
                return flag;
            }
        }
    }
}
