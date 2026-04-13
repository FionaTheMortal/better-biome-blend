package fionathemortal.betterbiomeblend.common.cache;

public final class CacheConfig
{
    public final int sampleSizeLog2;
    public final int sampleSize;
    public final int sliceSizeLog2;
    public final int sliceSize;
    public final int offset;
    public final int perimeter;

    public
    CacheConfig(int sliceSizeLog2, int sampleSizeLog2, int baseOffset, int perimeter)
    {
        this.sampleSizeLog2 = sampleSizeLog2;
        this.sampleSize     = 1 << sampleSizeLog2;
        this.sliceSizeLog2  = sliceSizeLog2;
        this.sliceSize      = 1 << sliceSizeLog2;
        this.offset         = baseOffset;
        this.perimeter      = perimeter;
    }

    public int
    floorBlockToSample(int block)
    {
        return (block - offset) >> sampleSizeLog2;
    }

    public int
    ceilBlockToSample(int block)
    {
        return floorBlockToSample(block + sampleSize - 1);
    }

    public int
    floorSampleToSlice(int sample)
    {
        int result = sample >> sliceSizeLog2;

        return result;
    }

    public int
    ceilSampleToSlice(int sample)
    {
        return floorSampleToSlice(sample + sliceSize - 1);
    }

    public int
    floorBlockToSlice(int block)
    {
        int sample = floorBlockToSample(block);
        int result = floorSampleToSlice(sample);

        return result;
    }

    public int
    ceilBlockToSlice(int block)
    {
        int sample = ceilBlockToSample(block);
        int result = ceilSampleToSlice(sample);

        return result;
    }
    
    public int
    getSampleFromSlice(int slice)
    {
        int result = slice << sliceSizeLog2;

        return result;
    }
}
