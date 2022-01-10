package fionathemortal.betterbiomeblend.common;

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

        for (int z = -1;
            z <= 1;
            ++z)
        {
            for (int x = -1;
                x <= 1;
                ++x)
            {
                // TODO: I mean this is real silly but it works. Find a better way to access chunks

                for (int y = 0;
                    y <= 20;
                    ++y)
                {
                    for (int colorType = BiomeColorType.FIRST;
                         colorType < CustomColorResolverCompatibility.nextColorResolverID;
                         ++colorType)
                    {
                        long key = ColorCaching.getChunkKey(chunkX + x, y, chunkZ + z, colorType);

                        if (x == 0 && z == 0)
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
                                int minX = ColorBlending.getNeighborRectMin(x);
                                int minY = 0;
                                int minZ = ColorBlending.getNeighborRectMin(z);

                                int maxX = ColorBlending.getNeighborRectMax(x);
                                int maxY = 4;
                                int maxZ = ColorBlending.getNeighborRectMax(z);

                                int cacheDim = 4;

                                for (int y1 = minY;
                                    y1 < maxY;
                                    ++y1)
                                {
                                    for (int z1 = minZ;
                                         z1 < maxZ;
                                         ++z1)
                                    {
                                        for (int x1 = minX;
                                             x1 < maxX;
                                             ++x1)
                                        {
                                            int cacheIndex = x1 + z1 * cacheDim + y1 * cacheDim * cacheDim;

                                            chunk.data[3 * cacheIndex + 0] = (byte)-1;
                                            chunk.data[3 * cacheIndex + 1] = (byte)-1;
                                            chunk.data[3 * cacheIndex + 2] = (byte)-1;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        lock.unlock();
    }

    public ColorChunk
    getOrDefaultInitializeChunk(int chunkX, int chunkY, int chunkZ, int colorType)
    {
        long key = ColorCaching.getChunkKey(chunkX, chunkY, chunkZ, colorType);

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
