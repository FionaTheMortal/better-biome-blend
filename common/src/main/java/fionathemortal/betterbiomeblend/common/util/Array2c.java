package fionathemortal.betterbiomeblend.common.util;

public final class Array2c
{
    public static final int ELEMENT_SIZE = 3;

    public static int
    getArrayIndex(int dimX, int x, int y)
    {
        int result = ELEMENT_SIZE * (x + y * dimX);

        return result;
    }
}
