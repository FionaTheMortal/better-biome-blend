package fionathemortal.betterbiomeblend.common;

public final class BlendBuffer
{
    public final int blendRadius;

    public final int sliceSizeLog2;
    public final int blockSizeLog2;

    public final int sliceSize;
    public final int blockSize;

    public final int scaledBlendDiameter;

    public final float[] R;
    public final float[] G;
    public final float[] B;

    public final float[] blendR;
    public final float[] blendG;
    public final float[] blendB;

    public final float[] sumR;
    public final float[] sumG;
    public final float[] sumB;

    public BlendBuffer(int blendRadius)
    {
        this.blendRadius = blendRadius;

        this.sliceSizeLog2 = BlendConfig.getSliceSizeLog2(blendRadius);
        this.blockSizeLog2 = BlendConfig.getBlockSizeLog2(blendRadius);

        this.sliceSize = 1 << sliceSizeLog2;
        this.blockSize = 1 << blockSizeLog2;

        this.scaledBlendDiameter = (2 * blendRadius) >> blockSizeLog2;

        final int blendSize = BlendConfig.getBlendSize(blendRadius);
        final int size      = BlendConfig.getBlendBufferSize(blendRadius);

        this.R      = new float[size * size * size];
        this.G      = new float[size * size * size];
        this.B      = new float[size * size * size];

        this.blendR = new float[size * blendSize];
        this.blendG = new float[size * blendSize];
        this.blendB = new float[size * blendSize];

        this.sumR   = new float[size * size];
        this.sumG   = new float[size * size];
        this.sumB   = new float[size * size];
    }
}
