package fionathemortal.betterbiomeblend.common.cache;

public class BiomeCache extends SliceCache<BiomeSlice>
{
    public
    BiomeCache(int count)
    {
        super(count, BiomeSlice::new);
    }
}
