package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.common.util.Array3i;
import fionathemortal.betterbiomeblend.common.util.Color;

public final class BlendContext
{
    public final BlendConfig blendConfig;

    public final Array3i output;

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

    public BlendContext(BlendConfig blendConfig)
    {
        this.blendConfig = blendConfig;

        this.output = new Array3i();

        this.colorBitsExclusive = 0xFFFFFFFF;
        this.colorBitsInclusive = 0;
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
        this.blockMinX = blockMinX;
        this.blockMinY = blockMinY;
        this.blockMinZ = blockMinZ;

        this.blockMaxX = blockMinX + outputDimX;
        this.blockMaxY = blockMinY + outputDimY;
        this.blockMaxZ = blockMinZ + outputDimZ;

        this.sampleMinX = blendConfig.getSampleFromBlock(this.blockMinX);
        this.sampleMinY = blendConfig.getSampleFromBlock(this.blockMinY);
        this.sampleMinZ = blendConfig.getSampleFromBlock(this.blockMinZ);

        this.sampleMaxX = blendConfig.ceilBlockToSample(this.blockMaxX);
        this.sampleMaxY = blendConfig.ceilBlockToSample(this.blockMaxY);
        this.sampleMaxZ = blendConfig.ceilBlockToSample(this.blockMaxZ);

        this.sampleCountX = this.sampleMaxX - this.sampleMinX;
        this.sampleCountY = this.sampleMaxY - this.sampleMinY;
        this.sampleCountZ = this.sampleMaxZ - this.sampleMinZ;

        this.sampleBlockMinX = blendConfig.getBlockFromSample(this.sampleMinX);
        this.sampleBlockMinZ = blendConfig.getBlockFromSample(this.sampleMinZ);
        this.sampleBlockMaxX = blendConfig.getBlockFromSample(this.sampleMinX + 1);
        this.sampleBlockMaxZ = blendConfig.getBlockFromSample(this.sampleMinZ + 1);

        this.sliceMinX = blendConfig.getSliceFromSample(this.sampleMinX);
        this.sliceMinY = blendConfig.getSliceFromSample(this.sampleMinY);
        this.sliceMinZ = blendConfig.getSliceFromSample(this.sampleMinZ);

        this.sliceMaxX = blendConfig.getSliceFromSample(this.sampleMaxX + 1);
        this.sliceMaxY = blendConfig.getSliceFromSample(this.sampleMaxY + 1);
        this.sliceMaxZ = blendConfig.getSliceFromSample(this.sampleMaxZ + 1);

        int filterDim = blendConfig.blendDim;

        int filterDimInSamples = (filterDim + 2 * (blendConfig.sampleSize - 1)) >> blendConfig.sliceSizeLog2;

        this.lineCount = filterDimInSamples;
        this.planeCount = filterDimInSamples;

        this.output.init(
            output,
            outputMinX,
            outputMinY,
            outputMinZ,
            outputDimX,
            outputDimY,
            outputDimZ,
            outputDataDimX,
            outputDataDimY);

        checkBuffers();

        this.colorBitsExclusive = 0xFFFFFFFF;
        this.colorBitsInclusive = 0;
    }

    public void
    free()
    {
        this.output.free();
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
    checkBuffers()
    {
        int sampleCount = this.sampleCountX * this.sampleCountY * this.sampleCountZ;

        if (this.samples == null || this.samples.length < 3 * sampleCount)
        {
            this.samples = new float[3 * sampleCount];
        }

        int lineLength = 3 * output.dimX;

        if (this.lineSum == null || lineSum.length < lineLength)
        {
            lineSum = new float[lineLength];
        }

        int lineBufferSize = lineLength * this.lineCount;

        if (this.lineBuffer == null || lineBuffer.length < lineBufferSize)
        {
            lineBuffer = new float[lineBufferSize];
        }

        int planeSize = 3 * output.dimX * output.dimY;

        if (this.planeSum == null || planeSum.length < planeSize)
        {
            planeSum = new float[planeSize];
        }

        int planeBufferSize = planeSize * this.planeCount;

        if (this.planeBuffer == null || planeBuffer.length < planeBufferSize)
        {
            planeBuffer = new float[planeBufferSize];
        }
    }
}
