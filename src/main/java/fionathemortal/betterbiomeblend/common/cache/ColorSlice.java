package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.ColorBlending;

import java.util.Arrays;

public final class ColorSlice extends Slice
{
    public byte[] data;

    public
    ColorSlice()
    {
        super();

        this.data = new byte[4 * 4 * 4 * 3];
    }

    @Override
    public void
    invalidateRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
    {
        int cacheDim = 4;

        for (int y1 = minY;
             y1 < maxY;
             ++y1)
        {
            for (int z1 = minZ;
                 z1 < maxZ;
                 ++z1)
            {
                for (int x1 = minX;
                     x1 < maxX;
                     ++x1)
                {
                    int cacheIndex = ColorBlending.getCacheArrayIndex(cacheDim, x1, y1, z1);

                    this.data[3 * cacheIndex + 0] = (byte)-1;
                    this.data[3 * cacheIndex + 1] = (byte)-1;
                    this.data[3 * cacheIndex + 2] = (byte)-1;
                }
            }
        }
    }

    public void
    invalidateData()
    {
        Arrays.fill(this.data, (byte)-1);
    }
}
