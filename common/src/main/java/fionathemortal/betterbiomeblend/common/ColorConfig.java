package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import fionathemortal.betterbiomeblend.common.cache.CacheConfig;

public final class ColorConfig
{
    public static final ColorConfig[]
    blendConfigs =
    {
        new ColorConfig( 0, 4, 0, 0),
        new ColorConfig( 1, 4, 0, 0),
        new ColorConfig( 2, 3, 1, 0),
        new ColorConfig( 3, 3, 1, 1),
        new ColorConfig( 4, 3, 1, 0),
        new ColorConfig( 5, 3, 1, 1),
        new ColorConfig( 6, 2, 2, 2),
        new ColorConfig( 7, 2, 2, 2),
        new ColorConfig( 8, 2, 2, 0),
        new ColorConfig( 9, 2, 2, 2),
        new ColorConfig(10, 2, 2, 2),
        new ColorConfig(11, 2, 2, 2),
        new ColorConfig(12, 2, 2, 0),
        new ColorConfig(13, 2, 2, 2),
        new ColorConfig(14, 2, 2, 2)
    };

    public final int blendRadius;
    public final int blendDim;

    // NOTE: The sample size in blocks

    public final int sampleSizeLog2;
    public final int sampleSize;

    // NOTE: The slice size in samples

    public final int sliceSizeLog2;
    public final int sliceSize;

    // NOTE: The sample and slice origin offset in blocks

    public final int baseOffset;

    public
    ColorConfig(int blendRadius, int sliceSizeLog2, int sampleSizeLog2, int baseOffset)
    {
        this.blendRadius    = blendRadius;
        this.blendDim       = 2 * blendRadius + 1;

        this.sampleSizeLog2 = sampleSizeLog2;
        this.sampleSize     = 1 << sampleSizeLog2;

        this.sliceSizeLog2  = sliceSizeLog2;
        this.sliceSize      = 1 << sliceSizeLog2;

        this.baseOffset     = baseOffset;
    }

    public static ColorConfig
    getCurrentConfig()
    {
        ColorConfig result;

        int blendRadius = BetterBiomeBlendClient.getBlendRadiusSetting();

        if (blendRadius >= 0 && blendRadius < blendConfigs.length)
        {
            result = blendConfigs[blendRadius];
        }
        else
        {
            result = blendConfigs[blendConfigs.length - 1];
        }

        return result;
    }

    public CacheConfig
    getSourceConfig()
    {
        CacheConfig result = new CacheConfig(sliceSizeLog2, sampleSizeLog2, baseOffset, 2);

        return result;
    }

    public CacheConfig
    getResultConfig()
    {
        CacheConfig result = new CacheConfig(4, 0, 0, blendRadius + 2);

        return result;
    }

    public int
    floorBlockToSample(int block)
    {
        int result = (block - baseOffset) >> sampleSizeLog2;

        return result;
    }

    public int
    ceilBlockToSample(int block)
    {
        return floorBlockToSample(block + sampleSize - 1);
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
