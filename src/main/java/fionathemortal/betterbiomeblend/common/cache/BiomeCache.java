package fionathemortal.betterbiomeblend.common.cache;

public final class BiomeCache extends SliceCache<BiomeSlice>
{
    public
    BiomeCache(int count)
    {
        super(count, BiomeSlice::new);
    }
}
