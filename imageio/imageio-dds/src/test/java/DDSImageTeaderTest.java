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
        Dimension dim256 = new Dimension(256, 256);
        Dimension dim128 = new Dimension(128, 128);
        Dimension dim64 = new Dimension(64, 64);
        Dimension dim32 = new Dimension(32, 32);
        Dimension dim16 = new Dimension(16, 16);
        Dimension dim8 = new Dimension(8, 8);
        Dimension dim4 = new Dimension(4, 4);
        Dimension dim2 = new Dimension(2, 2);
        Dimension dim1 = new Dimension(1, 1);

        return Arrays.asList(
                new TestData(getClassLoaderResource("/dds/dds_A1R5G5B5.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_A1R5G5B5_mipmap.dds"), dim256, dim128, dim64, dim32, dim16, dim8, dim4, dim2, dim1),
                new TestData(getClassLoaderResource("/dds/dds_A4R4G4B4.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_A4R4G4B4_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_A8B8G8R8.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_A8B8G8R8_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_A8R8G8B8.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_A8R8G8B8_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_DXT1.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_DXT1_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_DXT2.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_DXT2_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_DXT3.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_DXT3_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_DXT4.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_DXT4_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_DXT5.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_DXT5_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_R5G6B5.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_R5G6B5_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_R8G8B8.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_R8G8B8_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_X1R5G5B5.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_X1R5G5B5_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_X4R4G4B4.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_X4R4G4B4_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_X8B8G8R8.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_X8B8G8R8_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_X8R8G8B8.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_X8R8G8B8_mipmap.dds"), dim256, dim128, dim64)
        );
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList("DDS", "dds");
    }

    @Override
    protected List<String> getSuffixes() {
        return Collections.singletonList("dds");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Collections.singletonList("image/vnd-ms.dds");
    }
}
