package com.twelvemonkeys.servlet.image.aoi;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:erlend@escenic.com">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
public class AreaOfInterestFactory {
    private final static AtomicReference<AreaOfInterestFactory> DEFAULT =
            new AtomicReference<AreaOfInterestFactory>(new AreaOfInterestFactory());

    public static void setDefault(AreaOfInterestFactory factory) {
        DEFAULT.set(factory);
    }

    public static AreaOfInterestFactory getDefault() {
        return DEFAULT.get();
    }

    public AreaOfInterest createAreaOfInterest(int pDefaultWidth, int pDefaultHeight, boolean aoiPercent, boolean aoiUniform) {
        if (aoiPercent && aoiUniform) {
            throw new IllegalArgumentException("Cannot be both uniform and percent Area of Interest");
        }
        if (aoiPercent) {
            return new PercentAreaOfInterest(pDefaultWidth, pDefaultHeight);
        }
        else if (aoiUniform) {
            return new UniformAreaOfInterest(pDefaultWidth, pDefaultHeight);
        }
        return new DefaultAreaOfInterest(pDefaultWidth, pDefaultHeight);
    }
}
