package fionathemortal.betterbiomeblend.common.util;

public final class Array3i
{
    public static final int ELEMENT_SIZE = 1;

    public static void
    fill(int value, int[] output, int arrayDimX, int arrayDimY, int minX, int minY, int minZ, int dimX, int dimY, int dimZ)
    {
        int strideX = getStrideX();
        int strideY = getStrideY(arrayDimX);
        int strideZ = getStrideZ(arrayDimX, arrayDimY);

        int index = minX + minY * strideX + minZ * strideX * strideY;

        for (int z = 0;
             z < dimZ;
             ++z)
        {
            for (int y = 0;
                 y < dimY;
                 ++y)
            {
                for (int x = 0;
                     x < dimX;
                     ++x)
                {
                    output[index] = value;

                    index += strideX;
                }

                index += strideY;
            }

            index += strideZ;
        }
    }

    public static int
    getStrideX()
    {
        return ELEMENT_SIZE;
    }

    public static int
    getStrideY(int dimX)
    {
        return dimX;
    }

    public static int
    getStrideZ(int dimX, int dimY)
    {
        return dimX * dimY;
    }

    public static int
    getArrayIndex(int dimX, int dimY, int x, int y, int z)
    {
        int result = x + (y + z * dimY) * dimX;

        return result;
    }
}
