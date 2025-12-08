package fionathemortal.betterbiomeblend.common.util;

public class Array3c
{
    public static int
    getArrayIndex(int dimX, int dimY, int x, int y, int z)
    {
        int result = 3 * (x + (y + z * dimY) * dimX);

        return result;
    }
}
