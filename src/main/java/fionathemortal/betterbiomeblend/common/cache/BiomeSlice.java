package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.ColorBlending;
import net.minecraft.world.level.biome.Biome;

import java.util.Arrays;

public final class BiomeSlice extends Slice
{
    public Biome[] data;

    public
    BiomeSlice()
    {
        super();

        this.data = new Biome[4 * 4 * 4];
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

                    this.data[cacheIndex] = null;
                }
            }
        }
    }

    public void
    invalidateData()
    {
        Arrays.fill(this.data, null);
    }
}
