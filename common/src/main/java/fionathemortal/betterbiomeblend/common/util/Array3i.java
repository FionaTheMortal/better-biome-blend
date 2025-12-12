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

        int indexZ = getArrayIndex(arrayDimX, arrayDimY, minX, minY, minZ);

        for (int z = 0;
             z < dimZ;
             ++z)
        {
            int indexY = indexZ;

            for (int y = 0;
                 y < dimY;
                 ++y)
            {
                int index = indexY;

                for (int x = 0;
                     x < dimX;
                     ++x)
                {
                    output[index] = value;

                    index += strideX;
                }

                indexY += strideY;
            }

            indexZ += strideZ;
        }
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
    getArrayIndex(int dimX, int dimY, int x, int y, int z)
    {
        int result = x + (y + z * dimY) * dimX;

        return result;
    }

    public static int
    getSize(int dimX, int dimY, int dimZ)
    {
        int result = ELEMENT_SIZE * dimX * dimY * dimZ;

        return result;
    }
}
