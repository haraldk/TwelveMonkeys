package com.twelvemonkeys.imageio.plugins.psd;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * PSDGridAndGuideInfo
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDGridAndGuideInfo.java,v 1.0 Nov 7, 2009 8:46:13 PM haraldk Exp$
 */
final class PSDGridAndGuideInfo extends PSDImageResource {
/* Grid & guide header */
//typedef struct {
//  guint32       fVersion;               /* Version - always 1 for PS */
//  guint32       fGridCycleV;            /* Vertical grid size */
//  guint32       fGridCycleH;            /* Horizontal grid size */
//  guint32       fGuideCount;            /* Number of guides */
//} GuideHeader;

/* Guide resource block */
//typedef struct {
//  guint32       fLocation;              /* Guide position in Pixels * 100 */
//  gchar         fDirection;             /* Guide orientation */
//} GuideResource;

    int version;
    int gridCycleVertical;
    int gridCycleHorizontal;
    int guideCount;

    GuideResource[] guides;

    PSDGridAndGuideInfo(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        version = pInput.readInt();
        gridCycleVertical = pInput.readInt();
        gridCycleHorizontal = pInput.readInt();
        guideCount = pInput.readInt();

        guides = new GuideResource[guideCount];

        for (int i = 0; i < guides.length; i++) {
            guides[i] = new GuideResource();
            guides[i].location = pInput.readInt();
            guides[i].direction = pInput.readByte();
        }
    }

    static class GuideResource {
        int location;
        byte direction; // 0: vertical, 1: horizontal
    }
}
