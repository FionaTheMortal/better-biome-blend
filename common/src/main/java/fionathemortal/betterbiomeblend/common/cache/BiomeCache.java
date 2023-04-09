package fionathemortal.betterbiomeblend.common.cache;

public final class BiomeCache extends SliceCache<BiomeSlice>
{
    public
    BiomeCache(int count)
    {
        super(count);
    }

    @Override
    public BiomeSlice
    newSlice(int sliceSize)
    {
        BiomeSlice result = new BiomeSlice(sliceSize);

        return result;
    }
}
