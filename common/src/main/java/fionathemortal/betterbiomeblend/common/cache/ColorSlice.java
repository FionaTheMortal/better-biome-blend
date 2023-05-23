package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.ColorCaching;

import java.util.Arrays;

public final class ColorSlice extends Slice
{
    public byte[] data;

    public
    ColorSlice(int sliceSize)
    {
        super();

        this.data = new byte[sliceSize * sliceSize * sliceSize * 3];
    }

    @Override
    public void
    invalidateRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
    {
        for (int y = minY;
             y < maxY;
             ++y)
        {
            for (int z = minZ;
                 z < maxZ;
                 ++z)
            {
                for (int x = minX;
                     x < maxX;
                     ++x)
                {
                    int cacheIndex = ColorCaching.getArrayIndex(16, x, y, z);

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
