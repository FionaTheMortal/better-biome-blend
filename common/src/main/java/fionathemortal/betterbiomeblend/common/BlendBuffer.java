package fionathemortal.betterbiomeblend.common;

public final class BlendBuffer
{
    public final int blendRadius;

    public final int sliceSizeLog2;
    public final int blockSizeLog2;

    public final int sliceSize;
    public final int blockSize;

    public final int blendSize;
    public final int blendBufferSize;

    public final int scaledBlendDiameter;

    public final float[] color;
    public final float[] blend;
    public final float[] sum;

    public int colorBitsExclusive;
    public int colorBitsInclusive;

    public BlendBuffer(int blendRadius)
    {
        this.blendRadius = blendRadius;

        this.sliceSizeLog2 = BlendConfig.getSliceSizeLog2(blendRadius);
        this.blockSizeLog2 = BlendConfig.getBlockSizeLog2(blendRadius);

        this.sliceSize = 1 << sliceSizeLog2;
        this.blockSize = 1 << blockSizeLog2;

        this.blendSize       = BlendConfig.getBlendSize(blendRadius);
        this.blendBufferSize = BlendConfig.getBlendBufferSize(blendRadius);

        this.scaledBlendDiameter = (2 * blendRadius) >> blockSizeLog2;

        this.color = new float[3 * blendBufferSize * blendBufferSize * blendBufferSize];
        this.blend = new float[3 * blendBufferSize * blendBufferSize]; // blendSize
        this.sum   = new float[3 * blendBufferSize * blendBufferSize];

        colorBitsExclusive = 0xFFFFFFFF;
        colorBitsInclusive = 0;
    }
}
