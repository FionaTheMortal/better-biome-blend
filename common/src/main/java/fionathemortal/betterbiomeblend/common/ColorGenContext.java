package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.common.util.*;

public final class ColorGenContext
{
    public static final int INITIAL_COLOR_BITS_EXCLUSIVE = 0xFFFFFFFF;
    public static final int INITIAL_COLOR_BITS_INCLUSIVE = 0;

    public final ColorConfig blendConfig;

    public int[] samples;

    // NOTE: Blocks

    public int blockMinX;
    public int blockMinY;
    public int blockMinZ;

    public int blockMaxX;
    public int blockMaxY;
    public int blockMaxZ;

    // NOTE: Samples

    public int sampleMinX;
    public int sampleMinY;
    public int sampleMinZ;

    public int sampleMaxX;
    public int sampleMaxY;
    public int sampleMaxZ;

    // NOTE: Sample count

    public int sampleCountX;
    public int sampleCountY;
    public int sampleCountZ;

    // NOTE: Sample box coordinates in world space

    public int sampleBlockMinX;
    public int sampleBlockMinZ;

    public int sampleBlockMaxX;
    public int sampleBlockMaxZ;

    // NOTE: Slices

    public int sliceMinX;
    public int sliceMinY;
    public int sliceMinZ;

    public int sliceMaxX;
    public int sliceMaxY;
    public int sliceMaxZ;

    // NOTE: Uniform color tracking

    public int colorBitsExclusive;
    public int colorBitsInclusive;

    // NOTE: Filter config

    public float filterMultiplier;
    public float filterSupport;

    // NOTE: Blending

    public float[] lineSamples;
    public float[] lineBuffer;
    public float[] lineSum;

    public float[] planeBuffer;
    public float[] planeSum;

    public int lineCount;
    public int planeCount;

    // NOTE: Output

    int[] output;

    int outputMinX;
    int outputMinY;
    int outputMinZ;

    int outputDimX;
    int outputDimY;
    int outputDimZ;

    int outputArrayDimX;
    int outputArrayDimY;

    public
    ColorGenContext(ColorConfig blendConfig)
    {
        this.blendConfig = blendConfig;
    }

    public void
    initSource(
        int   blockMinX,
        int   blockMinY,
        int   blockMinZ,
        int   blockDimX,
        int   blockDimY,
        int   blockDimZ)
    {
        resetColorBits();

        this.blockMinX = blockMinX;
        this.blockMinY = blockMinY;
        this.blockMinZ = blockMinZ;

        this.blockMaxX = blockMinX + blockDimX;
        this.blockMaxY = blockMinY + blockDimY;
        this.blockMaxZ = blockMinZ + blockDimZ;

        this.outputDimX = blockDimX;
        this.outputDimY = blockDimY;
        this.outputDimZ = blockDimZ;

        int blendMinX = this.blockMinX - blendConfig.blendRadius;
        int blendMinY = this.blockMinY - blendConfig.blendRadius;
        int blendMinZ = this.blockMinZ - blendConfig.blendRadius;

        int blendMaxX = this.blockMaxX + blendConfig.blendRadius;
        int blendMaxY = this.blockMaxY + blendConfig.blendRadius;
        int blendMaxZ = this.blockMaxZ + blendConfig.blendRadius;

        this.sampleMinX = blendConfig.floorBlockToSample(blendMinX);
        this.sampleMinY = blendConfig.floorBlockToSample(blendMinY);
        this.sampleMinZ = blendConfig.floorBlockToSample(blendMinZ);

        this.sampleMaxX = blendConfig.ceilBlockToSample(blendMaxX);
        this.sampleMaxY = blendConfig.ceilBlockToSample(blendMaxY);
        this.sampleMaxZ = blendConfig.ceilBlockToSample(blendMaxZ);

        this.sampleCountX = this.sampleMaxX - this.sampleMinX;
        this.sampleCountY = this.sampleMaxY - this.sampleMinY;
        this.sampleCountZ = this.sampleMaxZ - this.sampleMinZ;

        this.sampleBlockMinX = blendConfig.getBlockFromSample(this.sampleMinX);
        this.sampleBlockMinZ = blendConfig.getBlockFromSample(this.sampleMinZ);

        this.sampleBlockMaxX = blendConfig.getBlockFromSample(this.sampleMaxX);
        this.sampleBlockMaxZ = blendConfig.getBlockFromSample(this.sampleMaxZ);

        this.sliceMinX = blendConfig.getSliceFromSample(this.sampleMinX);
        this.sliceMinY = blendConfig.getSliceFromSample(this.sampleMinY);
        this.sliceMinZ = blendConfig.getSliceFromSample(this.sampleMinZ);

        this.sliceMaxX = blendConfig.getSliceFromSample(this.sampleMaxX + blendConfig.sliceSize - 1);
        this.sliceMaxY = blendConfig.getSliceFromSample(this.sampleMaxY + blendConfig.sliceSize - 1);
        this.sliceMaxZ = blendConfig.getSliceFromSample(this.sampleMaxZ + blendConfig.sliceSize - 1);

        int maxSamplesInFilter = (blendConfig.blendDim + 2 * (blendConfig.sampleSize - 1)) >> blendConfig.sampleSizeLog2;

        this.lineCount  = maxSamplesInFilter;
        this.planeCount = maxSamplesInFilter;

        filterSupport = blendConfig.blendDim * blendConfig.blendDim * blendConfig.blendDim;
        filterMultiplier = 1.0f / filterSupport;

        ensureBufferCapacities();
    }

    public void
    initOutput(
        int[] output,
        int   outputMinX,
        int   outputMinY,
        int   outputMinZ,
        int   outputDataDimX,
        int   outputDataDimY)
    {
        this.output = output;

        this.outputMinX = outputMinX;
        this.outputMinY = outputMinY;
        this.outputMinZ = outputMinZ;

        this.outputArrayDimX = outputDataDimX;
        this.outputArrayDimY = outputDataDimY;
    }

    public void
    releaseOutput()
    {
        this.output = null;
    }

    private void
    resetColorBits()
    {
        this.colorBitsExclusive = INITIAL_COLOR_BITS_EXCLUSIVE;
        this.colorBitsInclusive = INITIAL_COLOR_BITS_INCLUSIVE;
    }

    public boolean
    isSolid()
    {
        boolean result = false;

        if (this.colorBitsExclusive == this.colorBitsInclusive)
        {
            result = true;
        }
        else
        {
            int i = 0;
        }

        return result;
    }

    public int
    getSingleColor()
    {
        int result = 0;

        if (isSolid())
        {
            result = this.colorBitsExclusive;
        }

        return result;
    }

    public void
    setColorSample(int color, int index)
    {
        this.samples[index] = color;

        this.colorBitsExclusive &= color;
        this.colorBitsInclusive |= color;
    }

    private void
    ensureBufferCapacities()
    {
        this.samples     = Array3i.ensureCapacity(this.samples, sampleCountX, sampleCountY, sampleCountZ);
        this.lineSamples = Array1c.ensureCapacity(this.lineSamples, sampleCountX);
        this.lineSum     = Array1c.ensureCapacity(this.lineSum, outputDimX);
        this.lineBuffer  = Array2c.ensureCapacity(this.lineBuffer, outputDimX, lineCount);
        this.planeSum    = Array2c.ensureCapacity(this.planeSum, outputDimX, outputDimY);
        this.planeBuffer = Array3c.ensureCapacity(this.planeBuffer, outputDimX, outputDimY, planeCount);
    }
}
