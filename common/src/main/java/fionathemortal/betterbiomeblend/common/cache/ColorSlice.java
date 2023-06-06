package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.ColorCaching;

import java.util.Arrays;

public final class ColorSlice extends Slice
{
    public int[] data;

    public
    ColorSlice(int size, int salt)
    {
        super(size, salt);

        this.data = new int[size * size * size];
    }

    public void
    invalidateData()
    {
        Arrays.fill(this.data, 0);
    }
}
