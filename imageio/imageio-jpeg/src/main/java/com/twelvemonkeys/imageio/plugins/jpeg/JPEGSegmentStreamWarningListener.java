package com.twelvemonkeys.imageio.plugins.jpeg;

/**
 * JPEGSegmentStreamWarningListener
 */
interface JPEGSegmentStreamWarningListener {
    void warningOccurred(String warning);

    JPEGSegmentStreamWarningListener NULL_LISTENER = new JPEGSegmentStreamWarningListener() {
        @Override
        public void warningOccurred(final String warning) {}
    };
}
