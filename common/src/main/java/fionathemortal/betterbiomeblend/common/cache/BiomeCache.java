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
    newSlice(int size, int salt)
    {
        BiomeSlice result = new BiomeSlice(size, salt);

        return result;
    }
}
