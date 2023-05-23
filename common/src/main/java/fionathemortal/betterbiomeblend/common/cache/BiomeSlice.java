package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.ColorCaching;
import net.minecraft.world.level.biome.Biome;

import java.util.Arrays;

public final class BiomeSlice extends Slice
{
    public Biome[] data;

    public
    BiomeSlice(int sliceSize)
    {
        super();

        this.data = new Biome[sliceSize * sliceSize * sliceSize];
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
