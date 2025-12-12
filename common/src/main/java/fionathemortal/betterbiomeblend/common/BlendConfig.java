package fionathemortal.betterbiomeblend.common;

public final class BlendConfig
{
    public static final int BIOME_BLEND_RADIUS_MIN = 0;
    public static final int BIOME_BLEND_RADIUS_MAX = 14;

    public static final BlendConfig[]
    blendConfigs =
    {
        new BlendConfig( 0, 4, 0, 0),
        new BlendConfig( 1, 4, 0, 0),
        new BlendConfig( 2, 4, 0, 0),
        new BlendConfig( 3, 4, 0, 0),
        new BlendConfig( 4, 4, 0, 0),
        new BlendConfig( 5, 4, 0, 0),
        new BlendConfig( 6, 4, 0, 0),
        new BlendConfig( 7, 4, 0, 0),
        new BlendConfig( 8, 4, 0, 0),
        new BlendConfig( 9, 4, 0, 0),
        new BlendConfig(10, 4, 0, 0),
        new BlendConfig(11, 4, 0, 0),
        new BlendConfig(12, 4, 0, 0),
        new BlendConfig(13, 4, 0, 0),
        new BlendConfig(14, 3, 1, 2)
    };

    public final int blendRadius;
    public final int blendDim;

    // NOTE: The sample size in blocks

    public final int sampleSizeLog2;
    public final int sampleSize;

    // NOTE: The slice size in samples

    public final int sliceSizeLog2;
    public final int sliceSize;

    // NOTE: The sample and slice coordinate offset in blocks

    public final int baseOffset;

    public
    BlendConfig(int blendRadius, int sliceSizeLog2, int sampleSizeLog2, int baseOffset)
    {
        this.blendRadius    = blendRadius;
        this.blendDim       = 2 * blendRadius + 1;

        this.sampleSizeLog2 = sampleSizeLog2;
        this.sampleSize     = 1 << sampleSizeLog2;

        this.sliceSizeLog2  = sliceSizeLog2;
        this.sliceSize      = 1 << sliceSizeLog2;

        this.baseOffset     = baseOffset;
    }

    public static BlendConfig
    getBlendConfigForBlendRadius(int blendRadius)
    {
        BlendConfig result;

        if (blendRadius >= 0 && blendRadius < blendConfigs.length)
        {
            result = blendConfigs[blendRadius];
        }
        else
        {
            result = blendConfigs[0];
        }

        return result;
    }

    public int
    ceilBlockToSample(int block)
    {
        int result = (block - baseOffset + sampleSize - 1) >> sampleSizeLog2;

        return result;
    }

    public int
    floorBlockToSample(int block)
    {
        int result = (block - baseOffset) >> sampleSizeLog2;

        return result;
    }

    public int
    getSampleFromBlock(int block)
    {
        int result = floorBlockToSample(block);

        return result;
    }

    public int
    getBlockFromSample(int sample)
    {
        int result = (sample << sampleSizeLog2) + baseOffset;

        return result;
    }

    public int
    getSampleFromSlice(int slice)
    {
        int result = slice << sliceSizeLog2;

        return result;
    }

    public int
    getSliceFromSample(int sample)
    {
        int result = sample >> sliceSizeLog2;

        return result;
    }
}
