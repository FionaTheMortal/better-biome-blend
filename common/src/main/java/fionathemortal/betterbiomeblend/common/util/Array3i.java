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

    public static void
    copy(
        int[] src,
        int   srcArrayDimX,
        int   srcArrayDimY,
        int   srcMinX,
        int   srcMinY,
        int   srcMinZ,
        int[] dst,
        int   dstArrayDimX,
        int   dstArrayDimY,
        int   dstMinX,
        int   dstMinY,
        int   dstMinZ,
        int   dimX,
        int   dimY,
        int   dimZ)
    {
        int dstStrideX = getStrideX();
        int dstStrideY = getStrideY(dstArrayDimX);
        int dstStrideZ = getStrideZ(dstArrayDimX, dstArrayDimY);

        int srcStrideX = getStrideX();
        int srcStrideY = getStrideY(srcArrayDimX);
        int srcStrideZ = getStrideZ(srcArrayDimX, srcArrayDimY);

        int srcIndexZ = getArrayIndex(srcArrayDimX, srcArrayDimY, srcMinX, srcMinY, srcMinZ);
        int dstIndexZ = getArrayIndex(dstArrayDimX, dstArrayDimY, dstMinX, dstMinY, dstMinZ);

        for (int z = 0;
             z < dimZ;
             ++z)
        {
            int srcIndexY = srcIndexZ;
            int dstIndexY = dstIndexZ;

            for (int y = 0;
                 y < dimY;
                 ++y)
            {
                int srcIndex = srcIndexY;
                int dstIndex = dstIndexY;

                for (int x = 0;
                     x < dimX;
                     ++x)
                {
                    dst[dstIndex] = src[srcIndex];

                    srcIndex += srcStrideX;
                    dstIndex += dstStrideX;
                }

                dstIndexY += dstStrideY;
                srcIndexY += srcStrideY;
            }

            dstIndexZ += dstStrideZ;
            srcIndexZ += srcStrideZ;
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

    public static int[]
    ensureCapacity(int[] value, int dimX, int dimY, int dimZ)
    {
        int[] result = value;

        int requiredSize = getSize(dimX, dimY, dimZ);

        if (value == null || value.length < requiredSize)
        {
            result = new int[requiredSize];
        }

        return result;
    }
}
