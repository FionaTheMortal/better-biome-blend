package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.ColorCaching;

import java.util.Arrays;

public final class ColorSlice extends Slice
{
    public int[] data;

    public
    ColorSlice(int sliceSize)
    {
        super();

        this.data = new int[sliceSize * sliceSize * sliceSize];
    }

    public void
    invalidateData()
    {
        Arrays.fill(this.data, 0);
    }
}
