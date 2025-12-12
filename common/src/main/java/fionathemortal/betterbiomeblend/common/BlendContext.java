package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.common.util.Array1c;
import fionathemortal.betterbiomeblend.common.util.Array2c;
import fionathemortal.betterbiomeblend.common.util.Array3c;
import fionathemortal.betterbiomeblend.common.util.Color;

public final class BlendContext
{
    public static final int INITIAL_COLOR_BITS_EXCLUSIVE = 0xFFFFFFFF;
    public static final int INITIAL_COLOR_BITS_INCLUSIVE = 0;

    public final BlendConfig blendConfig;

    public float[] samples;

    public float[] lineBuffer;
    public float[] lineSum;

    public float[] planeBuffer;
    public float[] planeSum;

    public int lineCount;
    public int planeCount;

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

    // NOTE: Output

    int[] output;

    int outputDimX;
    int outputDimY;
    int outputDimZ;

    int outputMinX;
    int outputMinY;
    int outputMinZ;

    int outputArrayDimX;
    int outputArrayDimY;

    public BlendContext(BlendConfig blendConfig)
    {
        this.blendConfig = blendConfig;
    }

    public void
    init(
        int   blockMinX,
        int   blockMinY,
        int   blockMinZ,
        int[] output,
        int   outputMinX,
        int   outputMinY,
        int   outputMinZ,
        int   outputDimX,
        int   outputDimY,
        int   outputDimZ,
        int   outputDataDimX,
        int   outputDataDimY)
    {
        resetColorBits();

        this.output = output;

        this.outputMinX = outputMinX;
        this.outputMinY = outputMinY;
        this.outputMinZ = outputMinZ;

        this.outputDimX = outputDimX;
        this.outputDimY = outputDimY;
        this.outputDimZ = outputDimZ;

        this.outputArrayDimX = outputDataDimX;
        this.outputArrayDimY = outputDataDimY;

        this.blockMinX = blockMinX;
        this.blockMinY = blockMinY;
        this.blockMinZ = blockMinZ;

        this.blockMaxX = blockMinX + outputDimX;
        this.blockMaxY = blockMinY + outputDimY;
        this.blockMaxZ = blockMinZ + outputDimZ;

        int blendMinX = this.blockMinX - blendConfig.blendRadius;
        int blendMinY = this.blockMinY - blendConfig.blendRadius;
        int blendMinZ = this.blockMinZ - blendConfig.blendRadius;

        int blendMaxX = this.blockMaxX + blendConfig.blendRadius;
        int blendMaxY = this.blockMaxY + blendConfig.blendRadius;
        int blendMaxZ = this.blockMaxZ + blendConfig.blendRadius;

        this.sampleMinX = blendConfig.getSampleFromBlock(blendMinX);
        this.sampleMinY = blendConfig.getSampleFromBlock(blendMinY);
        this.sampleMinZ = blendConfig.getSampleFromBlock(blendMinZ);

        this.sampleMaxX = blendConfig.getSampleFromBlock(blendMaxX + blendConfig.sampleSize - 1);
        this.sampleMaxY = blendConfig.getSampleFromBlock(blendMaxY + blendConfig.sampleSize - 1);
        this.sampleMaxZ = blendConfig.getSampleFromBlock(blendMaxZ + blendConfig.sampleSize - 1);

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

        ensureBufferCapacities();
    }

    private void
    resetColorBits()
    {
        this.colorBitsExclusive = INITIAL_COLOR_BITS_EXCLUSIVE;
        this.colorBitsInclusive = INITIAL_COLOR_BITS_INCLUSIVE;
    }

    public void
    free()
    {
        this.output = null;

        resetColorBits();
    }

    public boolean
    isSingleColor()
    {
        boolean result = false;

        if (this.colorBitsExclusive == this.colorBitsInclusive)
        {
            result = true;
        }

        return result;
    }

    public void
    setColorSample(int color, int index)
    {
        Color.sRGBByteToOKLabs(color, this.samples, index);

        this.colorBitsExclusive &= color;
        this.colorBitsInclusive |= color;
    }

    private void
    ensureBufferCapacities()
    {
        this.samples     = Array3c.ensureCapacity(this.samples, sampleCountX, sampleCountY, sampleCountZ);
        this.lineSum     = Array1c.ensureCapacity(this.lineSum, outputDimX);
        this.lineBuffer  = Array2c.ensureCapacity(this.lineBuffer, outputDimX, lineCount);
        this.planeSum    = Array2c.ensureCapacity(this.planeSum, outputDimX, outputDimY);
        this.planeBuffer = Array3c.ensureCapacity(this.planeBuffer, outputDimX, outputDimY, planeCount);
    }
}
