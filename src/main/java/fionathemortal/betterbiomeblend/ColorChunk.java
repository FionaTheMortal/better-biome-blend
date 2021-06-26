package fionathemortal.betterbiomeblend;

import net.minecraft.world.biome.BiomeColorHelper;

import java.util.concurrent.atomic.AtomicInteger;

public final class ColorChunk
{
    public byte[] data;

    public long key;
    public int  invalidationCounter;

    public AtomicInteger refCount   = new AtomicInteger();

    public
    ColorChunk()
    {
        this.data = new byte[16 * 16 * 3];

        this.markAsInvalid();
    }

    public int
    getReferenceCount()
    {
        int result = refCount.get();

        return result;
    }

    public int
    release()
    {
        int result = refCount.decrementAndGet();

        return result;
    }

    public void
    acquire()
    {
        refCount.incrementAndGet();
    }

    public void
    markAsInvalid()
    {
        key =
            ((long)(0x02000000L) << 26) |
            ((long)(0x02000000L))       |
            ((long)-1 << 52);
    }

    public int
    getColor(int x, int z)
    {
        int blockX = x & 15;
        int blockZ = z & 15;

        int offset = 3 * ((blockZ << 4) | blockX);

        int colorR = this.data[offset + 0];
        int colorG = this.data[offset + 1];
        int colorB = this.data[offset + 2];

        int result = Color.makeRGBAWithFullAlpha(colorR, colorG, colorB);

        return result;
    }
}
