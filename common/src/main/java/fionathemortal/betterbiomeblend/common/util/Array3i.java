package fionathemortal.betterbiomeblend.common.util;

public final class Array3i
{
    public final int strideX = 1;

    public int[] data;

    public int minX;
    public int minY;
    public int minZ;

    public int dimX;
    public int dimY;
    public int dimZ;

    public int strideY;
    public int strideZ;

    public int first;

    public void
    init(int[] data, int minX, int minY, int minZ, int dimX, int dimY, int dimZ, int dataDimX, int dataDimY)
    {
        this.data = data;

        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;

        this.dimX = dimX;
        this.dimY = dimY;
        this.dimZ = dimZ;

        this.strideY = dataDimX;
        this.strideZ = dataDimX * dataDimY;

        this.first = minX + minY * this.strideY + minZ * this.strideZ;
    }

    public void
    free()
    {
        this.data = null;

        this.minX = 0;
        this.minY = 0;
        this.minZ = 0;

        this.dimX = 0;
        this.dimY = 0;
        this.dimZ = 0;

        this.strideY = 0;
        this.strideZ = 0;

        this.first = 0;
    }

    public void
    fill(int value)
    {
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
                    data[index] = value;

                    ++index;
                }

                index += strideX;
            }

            index += strideX * strideY;
        }
    }

    public static int
    getArrayIndex(int dimX, int dimY, int x, int y, int z)
    {
        int result = x + (y + z * dimY) * dimX;

        return result;
    }
}
