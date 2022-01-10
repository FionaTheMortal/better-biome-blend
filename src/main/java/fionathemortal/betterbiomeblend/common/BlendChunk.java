package fionathemortal.betterbiomeblend.common;

import java.util.concurrent.atomic.AtomicInteger;

public final class BlendChunk
{
    public byte[] data;
    public long   key;
    public int    invalidationCounter;

    public AtomicInteger refCount = new AtomicInteger();

    public
    BlendChunk()
    {
        this.data = new byte[5 * 5 * 5 * 3];

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
        key = ColorCaching.INVALID_CHUNK_KEY;
    }

    private static byte[]
    weights =
    {

    };

    public int
    getColor(int x, int y, int z)
    {
        int blockX = (x & 15) + 2;
        int blockY = (y & 15) + 2;
        int blockZ = (z & 15) + 2;

        int sectionX = blockX >> 2;
        int sectionY = blockY >> 2;
        int sectionZ = blockZ >> 2;

        int offsetX = blockX & 3;
        int offsetY = blockY & 3;
        int offsetZ = blockZ & 3;

        // TODO:

        int r0 = (4 - offsetX) * (4 - offsetZ) * (4 - offsetY) * (0xFF & data[3 * (sectionX +     4 * (sectionZ)     + 4 * 4 * (sectionY    )) + 0]);
        int r1 = (    offsetX) * (4 - offsetZ) * (4 - offsetY) * (0xFF & data[3 * (sectionX + 1 + 4 * (sectionZ)     + 4 * 4 * (sectionY    )) + 0]);
        int r2 = (4 - offsetX) * (    offsetZ) * (4 - offsetY) * (0xFF & data[3 * (sectionX +     4 * (sectionZ + 1) + 4 * 4 * (sectionY    )) + 0]);
        int r3 = (    offsetX) * (    offsetZ) * (4 - offsetY) * (0xFF & data[3 * (sectionX + 1 + 4 * (sectionZ + 1) + 4 * 4 * (sectionY    )) + 0]);
        int r4 = (4 - offsetX) * (4 - offsetZ) * (    offsetY) * (0xFF & data[3 * (sectionX +     4 * (sectionZ)     + 4 * 4 * (sectionY + 1)) + 0]);
        int r5 = (    offsetX) * (4 - offsetZ) * (    offsetY) * (0xFF & data[3 * (sectionX + 1 + 4 * (sectionZ)     + 4 * 4 * (sectionY + 1)) + 0]);
        int r6 = (4 - offsetX) * (    offsetZ) * (    offsetY) * (0xFF & data[3 * (sectionX +     4 * (sectionZ + 1) + 4 * 4 * (sectionY + 1)) + 0]);
        int r7 = (    offsetX) * (    offsetZ) * (    offsetY) * (0xFF & data[3 * (sectionX + 1 + 4 * (sectionZ + 1) + 4 * 4 * (sectionY + 1)) + 0]);

        int g0 = (4 - offsetX) * (4 - offsetZ) * (4 - offsetY) * (0xFF & data[3 * (sectionX +     4 * (sectionZ)     + 4 * 4 * (sectionY    )) + 1]);
        int g1 = (    offsetX) * (4 - offsetZ) * (4 - offsetY) * (0xFF & data[3 * (sectionX + 1 + 4 * (sectionZ)     + 4 * 4 * (sectionY    )) + 1]);
        int g2 = (4 - offsetX) * (    offsetZ) * (4 - offsetY) * (0xFF & data[3 * (sectionX +     4 * (sectionZ + 1) + 4 * 4 * (sectionY    )) + 1]);
        int g3 = (    offsetX) * (    offsetZ) * (4 - offsetY) * (0xFF & data[3 * (sectionX + 1 + 4 * (sectionZ + 1) + 4 * 4 * (sectionY    )) + 1]);
        int g4 = (4 - offsetX) * (4 - offsetZ) * (    offsetY) * (0xFF & data[3 * (sectionX +     4 * (sectionZ)     + 4 * 4 * (sectionY + 1)) + 1]);
        int g5 = (    offsetX) * (4 - offsetZ) * (    offsetY) * (0xFF & data[3 * (sectionX + 1 + 4 * (sectionZ)     + 4 * 4 * (sectionY + 1)) + 1]);
        int g6 = (4 - offsetX) * (    offsetZ) * (    offsetY) * (0xFF & data[3 * (sectionX +     4 * (sectionZ + 1) + 4 * 4 * (sectionY + 1)) + 1]);
        int g7 = (    offsetX) * (    offsetZ) * (    offsetY) * (0xFF & data[3 * (sectionX + 1 + 4 * (sectionZ + 1) + 4 * 4 * (sectionY + 1)) + 1]);

        int b0 = (4 - offsetX) * (4 - offsetZ) * (4 - offsetY) * (0xFF & data[3 * (sectionX +     4 * (sectionZ)     + 4 * 4 * (sectionY    )) + 2]);
        int b1 = (    offsetX) * (4 - offsetZ) * (4 - offsetY) * (0xFF & data[3 * (sectionX + 1 + 4 * (sectionZ)     + 4 * 4 * (sectionY    )) + 2]);
        int b2 = (4 - offsetX) * (    offsetZ) * (4 - offsetY) * (0xFF & data[3 * (sectionX +     4 * (sectionZ + 1) + 4 * 4 * (sectionY    )) + 2]);
        int b3 = (    offsetX) * (    offsetZ) * (4 - offsetY) * (0xFF & data[3 * (sectionX + 1 + 4 * (sectionZ + 1) + 4 * 4 * (sectionY    )) + 2]);
        int b4 = (4 - offsetX) * (4 - offsetZ) * (    offsetY) * (0xFF & data[3 * (sectionX +     4 * (sectionZ)     + 4 * 4 * (sectionY + 1)) + 2]);
        int b5 = (    offsetX) * (4 - offsetZ) * (    offsetY) * (0xFF & data[3 * (sectionX + 1 + 4 * (sectionZ)     + 4 * 4 * (sectionY + 1)) + 2]);
        int b6 = (4 - offsetX) * (    offsetZ) * (    offsetY) * (0xFF & data[3 * (sectionX +     4 * (sectionZ + 1) + 4 * 4 * (sectionY + 1)) + 2]);
        int b7 = (    offsetX) * (    offsetZ) * (    offsetY) * (0xFF & data[3 * (sectionX + 1 + 4 * (sectionZ + 1) + 4 * 4 * (sectionY + 1)) + 2]);

        int r = (r0 + r1 + r2 + r3 + r4 + r5 + r6 + r7) / 64;
        int g = (g0 + g1 + g2 + g3 + g4 + g5 + g6 + g7) / 64;
        int b = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + b7) / 64;

        int result = Color.makeRGBAWithFullAlpha(r, g, b);

        return result;
    }
}
