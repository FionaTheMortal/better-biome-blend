package fionathemortal.betterbiomeblend.common;

public final class BlendConfig
{
    public static final int BIOME_BLEND_RADIUS_MIN  = 0;
    public static final int BIOME_BLEND_RADIUS_MAX  = 14;

    public static final int SODIUM_BLEND_RADIUS_MAX = 13;

    public static final byte[]
    blendRadiusConfig =
    {
        2, 0,
        3, 0,
        3, 1,
        3, 1,
        3, 1,
        3, 1,
        4, 2,
        4, 2,
        4, 2,
        4, 2,
        4, 2,
        4, 2,
        4, 2,
        4, 2,
        4, 2,
    };

    public static int
    getSliceSizeLog2(int blendRadius)
    {
        int result = blendRadiusConfig[(blendRadius << 1)];

        return result;
    }

    public static int
    getBlockSizeLog2(int blendRadius)
    {
        int result = blendRadiusConfig[(blendRadius << 1) + 1];

        return result;
    }

    public static int
    getSliceSize(int blendRadius)
    {
        int result = 1 << getSliceSizeLog2(blendRadius);

        return result;
    }

    public static int
    getBlendSize(int blendRadius)
    {
        final int blockSizeLog2 = getBlockSizeLog2(blendRadius);
        final int sliceSizeLog2 = getSliceSizeLog2(blendRadius);

        final int blockSize = 1 << blockSizeLog2;
        final int sliceSize = 1 << sliceSizeLog2;

        final int blendSize       = sliceSize + 2 * blendRadius;
        final int scaledBlendSize = blendSize >> blockSizeLog2;

        return scaledBlendSize;
    }

    public static int
    getBlendBufferSize(int blendRadius)
    {
        int blendSize = getBlendSize(blendRadius);
        int sliceSize = getSliceSize(blendRadius);

        int result = Math.max(blendSize, sliceSize);

        return result;
    }

    public static int
    getFilterSupport(int blendRadius)
    {
        final int sliceSizeLog2 = BlendConfig.getSliceSizeLog2(blendRadius);
        final int blockSizeLog2 = BlendConfig.getBlockSizeLog2(blendRadius);

        final int sliceSize = 1 << sliceSizeLog2;

        final int blendDim = BlendConfig.getBlendSize(blendRadius);

        final int scaledSliceSize = sliceSize >> blockSizeLog2;
        final int filterSupport   = blendDim - scaledSliceSize + 1;

        return filterSupport;
    }
}
