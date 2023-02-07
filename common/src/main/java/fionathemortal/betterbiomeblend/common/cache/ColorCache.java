package fionathemortal.betterbiomeblend.common.cache;

public final class ColorCache extends SliceCache<ColorSlice>
{
    public
    ColorCache(int count)
    {
        super(count, ColorSlice::new);
    }
}
