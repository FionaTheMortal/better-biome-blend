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
    newSlice(int sliceSize)
    {
        ColorSlice result = new ColorSlice(sliceSize);

        return result;
    }
}
