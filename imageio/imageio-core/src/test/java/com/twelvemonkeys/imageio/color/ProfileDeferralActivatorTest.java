package com.twelvemonkeys.imageio.color;

import org.junit.jupiter.api.Test;

import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.spi.ServiceRegistry;

import static org.mockito.Mockito.*;

public class ProfileDeferralActivatorTest {

    @Test
    public void testActivateProfiles() {
        // Should just run with no exceptions...
        ProfileDeferralActivator.activateProfiles();
    }

    @Test
    public void testSpiRegistration() {
        ProfileDeferralActivator.Spi spi = new ProfileDeferralActivator.Spi();
        ServiceRegistry registry = mock(ServiceRegistry.class);
        Class<ImageInputStreamSpi> category = ImageInputStreamSpi.class;

        spi.onRegistration(registry, category);

        verify(registry, only()).deregisterServiceProvider(spi, category);
    }
}