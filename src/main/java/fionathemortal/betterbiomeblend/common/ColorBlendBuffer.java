package fionathemortal.betterbiomeblend.common;

public final class ColorBlendBuffer
{
    public byte[] color;

    public ColorBlendBuffer()
    {
        this.color = new byte[3 * ColorBlending.BLEND_BUFFER_DIM * ColorBlending.BLEND_BUFFER_DIM * ColorBlending.BLEND_BUFFER_DIM];
    }
}
