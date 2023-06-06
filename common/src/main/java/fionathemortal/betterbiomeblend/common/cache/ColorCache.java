package fionathemortal.betterbiomeblend.common.cache;

public final class ColorCache extends SliceCache<ColorSlice>
{
    public
    ColorCache(int count)
    {
        super(count);
    }

    @Override
    public ColorSlice
    newSlice(int size, int salt)
    {
        ColorSlice result = new ColorSlice(size, salt);

        return result;
    }
}
