package fionathemortal.betterbiomeblend.common.cache;

public class ColorCache extends SliceCache<ColorSlice>
{
    public
    ColorCache(int count)
    {
        super(count, ColorSlice::new);
    }
}
