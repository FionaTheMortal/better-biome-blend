package fionathemortal.betterbiomeblend.common.cache;

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

    public void
    invalidateData()
    {
        Arrays.fill(this.data, null);
    }
}
