package fionathemortal.betterbiomeblend.common.util;

public final class Array1c
{
    public static final int ELEMENT_SIZE = 3;

    public static int
    getSize(int dimX)
    {
        int result = ELEMENT_SIZE * dimX;

        return result;
    }

    public static float[]
    ensureCapacity(float[] value, int dimX)
    {
        float[] result = value;

        int requiredSize = getSize(dimX);

        if (value == null || value.length < requiredSize)
        {
            result = new float[requiredSize];
        }

        return result;
    }
}
