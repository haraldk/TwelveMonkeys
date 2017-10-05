package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class IOCAImageReaderTest extends ImageReaderAbstractTest<IOCAImageReader> {

	@Override
	protected List<TestData> getTestData() {
		return Arrays.asList(
				new TestData(getClassLoaderResource("/ioca/G4MMR.ica"), new Dimension(2480, 3300)),
				new TestData(getClassLoaderResource("/ioca/JPEG_YCbCr.ica"), new Dimension(1656, 2338)),
				new TestData(getClassLoaderResource("/ioca/JPEG.ica"), new Dimension(778, 497))
		);
	}

	@Override
	protected ImageReaderSpi createProvider() {
		return new IOCAImageReaderSpi();
	}

	@Override
	protected Class<IOCAImageReader> getReaderClass() {
		return IOCAImageReader.class;
	}

	@Override
	protected IOCAImageReader createReader() {
		return new IOCAImageReader(createProvider());
	}

	@Override
	protected List<String> getFormatNames() {
		return Arrays.asList("ioca", "IOCA");
	}

	@Override
	protected List<String> getSuffixes() {
		return Arrays.asList("ica", "ioca");
	}

	@Override
	protected List<String> getMIMETypes() {
		return Arrays.asList(

				// IBM's AFP IOCA subset for bilevel raster image.
				"image/x-afp+fs10",

				// IBM's AFP IOCA subset for grayscale and color raster image.
				"image/x-afp+fs11",

				// IBM's AFP IOCA subset for grayscale and color tiled raster image.
				"image/x-afp+fs45"
		);
	}

	@Ignore("Known issue: Subsampled reading is currently broken")
	@Test
	@Override
	public void testReadWithSubsampleParamPixels() throws IOException {
		super.testReadWithSubsampleParamPixels();
	}
}
