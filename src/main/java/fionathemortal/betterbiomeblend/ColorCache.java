package fionathemortal.betterbiomeblend;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

public final class ColorCache
{
    public final ReentrantLock                            lock;
    public final Long2ObjectLinkedOpenHashMap<ColorChunk> hash;
    public final Stack<ColorChunk>                        free;

    public
    ColorCache(int count)
    {
        lock = new ReentrantLock();
        hash = new Long2ObjectLinkedOpenHashMap<>(count);
        free = new Stack<>();

        for (int index = 0;
            index < count;
            ++index)
        {
            free.add(new ColorChunk());
        }
    }

    public void
    releaseChunkWithoutLock(ColorChunk chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            free.push(chunk);
        }
    }

    public void
    releaseChunk(ColorChunk chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            lock.lock();

            free.push(chunk);

            lock.unlock();
        }
    }

    public void
    invalidateAll()
    {
        lock.lock();

        for (ColorChunk chunk : hash.values())
        {
            releaseChunkWithoutLock(chunk);

            chunk.markAsInvalid();
        }

        hash.clear();

        lock.unlock();
    }

    public void
    invalidateSmallNeighborhood(int chunkX, int chunkZ)
    {
        lock.lock();

        for (int chunkIndex = 0;
            chunkIndex < 9;
            ++chunkIndex)
        {
            int offsetX = ColorBlending.getNeighborOffsetX(chunkIndex);
            int offsetZ = ColorBlending.getNeighborOffsetZ(chunkIndex);

            for (int colorType = BiomeColorType.FIRST;
                colorType < CustomColorResolverCompatibility.nextColorResolverID;
                ++colorType)
            {
                long key = ColorCaching.getChunkKey(chunkX + offsetX, chunkZ + offsetZ, colorType);

                if (chunkIndex == 0)
                {
                    ColorChunk chunk = hash.remove(key);

                    if (chunk != null)
                    {
                        releaseChunkWithoutLock(chunk);

                        chunk.markAsInvalid();
                    }
                }
                else
                {
                    ColorChunk chunk = hash.get(key);

                    if (chunk != null)
                    {
                        int minX = ColorBlending.getNeighborRectMinX(chunkIndex, 2);
                        int minZ = ColorBlending.getNeighborRectMinZ(chunkIndex, 2);
                        int maxX = ColorBlending.getNeighborRectMaxX(chunkIndex, 2);
                        int maxZ = ColorBlending.getNeighborRectMaxZ(chunkIndex, 2);

                        for (int z1 = minZ;
                            z1 < maxZ;
                            ++z1)
                        {
                            for (int x1 = minX;
                                x1 < maxX;
                                ++x1)
                            {
                                chunk.data[3 * (16 * z1 + x1) + 0] = (byte)-1;
                                chunk.data[3 * (16 * z1 + x1) + 1] = (byte)-1;
                                chunk.data[3 * (16 * z1 + x1) + 2] = (byte)-1;
                            }
                        }
                    }
                }
            }
        }

        lock.unlock();
    }

    public ColorChunk
    getOrDefaultInitializeChunk(int chunkX, int chunkZ, int colorType)
    {
        long key = ColorCaching.getChunkKey(chunkX, chunkZ, colorType);

        lock.lock();

        ColorChunk result = hash.getAndMoveToFirst(key);

        if (result == null)
        {
            if (!free.empty())
            {
                result = free.pop();
            }
            else
            {
                for (;;)
                {
                    long lastKey = hash.lastLongKey();

                    result = hash.removeLast();

                    if (result.getReferenceCount() == 1)
                    {
                        result.release();
                        break;
                    }
                    else
                    {
                        hash.putAndMoveToFirst(lastKey, result);
                    }
                }
            }

            result.key = key;

            Arrays.fill(result.data, (byte)-1);

            result.acquire();

            hash.putAndMoveToFirst(result.key, result);
        }

        result.acquire();

        lock.unlock();

        return result;
    }
}
