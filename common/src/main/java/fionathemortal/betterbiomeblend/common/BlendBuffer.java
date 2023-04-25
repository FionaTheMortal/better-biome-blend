package fionathemortal.betterbiomeblend.common;

public final class BlendBuffer
{
    public float[] colors;
    public float[] scanline;

    public int blendRadius;

    public BlendBuffer(int blendRadius)
    {
        final int size      = BlendConfig.getBlendBufferSize(blendRadius);
        final int blendSize = BlendConfig.getBlendSize(blendRadius);

        this.blendRadius = blendRadius;
        this.colors      = new float[3 * size * size * size];
        this.scanline    = new float[3 * blendSize];
    }
}
