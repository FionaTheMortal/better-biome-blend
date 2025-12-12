package fionathemortal.betterbiomeblend.common.util;

public final class Array3c
{
    public static final int ELEMENT_SIZE = 3;

    public static int
    getArrayIndex(int dimX, int dimY, int x, int y, int z)
    {
        int result = ELEMENT_SIZE * (x + (y + z * dimY) * dimX);

        return result;
    }

    public static int
    getStrideX()
    {
        int result = ELEMENT_SIZE;

        return result;
    }

    public static int
    getStrideY(int dimX)
    {
        int result = getStrideX() * dimX;

        return result;
    }

    public static int
    getStrideZ(int dimX, int dimY)
    {
        int result = getStrideY(dimX) * dimY;

        return result;
    }

    public static int
    getSize(int dimX, int dimY, int dimZ)
    {
        int result = ELEMENT_SIZE * dimX * dimY * dimZ;

        return result;
    }

    public static float[]
    ensureCapacity(float[] value, int dimX, int dimY, int dimZ)
    {
        float[] result = value;

        int requiredSize = getSize(dimX, dimY, dimZ);

        if (value == null || value.length < requiredSize)
        {
            result = new float[requiredSize];
        }

        return result;
    }
}
