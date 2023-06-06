package fionathemortal.betterbiomeblend.common.cache;

import net.minecraft.world.level.biome.Biome;

import java.util.Arrays;

public final class BiomeSlice extends Slice
{
    public Biome[] data;

    public
    BiomeSlice(int size, int salt)
    {
        super(size, salt);

        this.data = new Biome[size * size * size];
    }

    public void
    invalidateData()
    {
        Arrays.fill(this.data, null);
    }
}
