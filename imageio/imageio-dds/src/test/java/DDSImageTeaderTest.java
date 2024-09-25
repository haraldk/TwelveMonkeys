import com.twelvemonkeys.imageio.plugins.dds.DDSImageReader;
import com.twelvemonkeys.imageio.plugins.dds.DDSImageReaderSpi;
import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
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
        Dimension dim = new Dimension(256, 256);

        List<TestData> testData = new ArrayList<>();
        testData.add(new TestData(getClassLoaderResource("/dds/dds_A1R5G5B5.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_A1R5G5B5_mipmap.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_A4R4G4B4.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_A4R4G4B4_mipmap.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_A8B8G8R8.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_A8B8G8R8_mipmap.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_A8R8G8B8.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_A8R8G8B8_mipmap.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_DXT1.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_DXT1_mipmap.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_DXT2.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_DXT2_mipmap.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_DXT3.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_DXT3_mipmap.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_DXT4.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_DXT4_mipmap.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_DXT5.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_DXT5_mipmap.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_R5G6B5.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_R5G6B5_mipmap.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_R8G8B8.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_R8G8B8_mipmap.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_X1R5G5B5.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_X1R5G5B5_mipmap.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_X4R4G4B4.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_X4R4G4B4_mipmap.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_X8B8G8R8.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_X8B8G8R8_mipmap.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_X8R8G8B8.dds"), dim));
        testData.add(new TestData(getClassLoaderResource("/dds/dds_X8R8G8B8_mipmap.dds"), dim));

        return testData;
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

    /* ************************************************************************************************************* *
     *                                            IGNORE Broken Tests...
     * ************************************************************************************************************* */

    @Ignore("Known issue: currently not supported in DDS")
    @Test
    @Override
    public void testGetNumImagesNoInput() throws IOException {
        super.testGetNumImagesNoInput();
    }

    @Ignore("Known issue: currently not supported in DDS")
    @Test
    @Override
    public void testAffineTransformOpCompatibility() throws IOException {
        super.testAffineTransformOpCompatibility();
    }

    @Ignore("Known issue: currently not supported in DDS")
    @Test
    @Override
    public void testReadWithSourceRegionParam() throws IOException {
        super.testReadWithSourceRegionParam();
    }

    @Ignore("Known issue: currently not supported in DDS")
    @Test
    @Override
    public void testReadWithSourceRegionParamEqualImage() throws IOException {
        super.testReadWithSourceRegionParamEqualImage();
    }

    @Ignore("Known issue: currently not supported in DDS")
    @Test
    @Override
    public void testReadWithSubsampleAndSourceRegionParam() throws IOException {
        super.testReadWithSubsampleAndSourceRegionParam();
    }

    @Ignore("Known issue: currently not supported in DDS")
    @Test
    @Override
    public void testReadWithSubsampleParamDimensions() throws IOException {
        super.testReadWithSubsampleParamDimensions();
    }

    @Ignore("Known issue: currently not supported in DDS")
    @Test
    @Override
    public void testReadWithSubsampleParamPixels() throws IOException {
        super.testReadWithSubsampleParamPixels();
    }

    @Ignore("Known issue: currently not supported in DDS")
    @Test
    @Override
    public void testSetDestination() throws IOException {
        super.testSetDestination();
    }

    @Ignore("Known issue: currently not supported in DDS")
    @Test
    @Override
    public void testSetDestinationIllegal() throws IOException {
        super.testSetDestinationIllegal();
    }
}
