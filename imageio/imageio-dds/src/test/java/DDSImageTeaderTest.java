import com.twelvemonkeys.imageio.plugins.dds.DDSImageReader;
import com.twelvemonkeys.imageio.plugins.dds.DDSImageReaderSpi;
import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DDSImageTeaderTest extends ImageReaderAbstractTest<DDSImageReader> {
	@Override
	protected ImageReaderSpi createProvider() {
		return new DDSImageReaderSpi();
	}

	@Override
	protected List<TestData> getTestData() {
		return Collections.singletonList(
				new TestData(getClassLoaderResource("/dds/dxt5.dds"), new Dimension(512, 512))
		);
	}

	@Override
	protected List<String> getFormatNames() {
		return Arrays.asList("DDS", "dds");
	}

	@Override
	protected List<String> getSuffixes() {
		return Arrays.asList("dds");
	}

	@Override
	protected List<String> getMIMETypes() {
		return Collections.singletonList("image/vnd-ms.dds");
	}
}
