package fionathemortal.betterbiomeblend.common;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

public final class BiomeCache
{
    public final ReentrantLock                            lock;
    public final Long2ObjectLinkedOpenHashMap<BiomeChunk> hash;
    public final Stack<BiomeChunk>                        free;

    public
    BiomeCache(int count)
    {
        lock = new ReentrantLock();
        hash = new Long2ObjectLinkedOpenHashMap<>(count);
        free = new Stack<>();

        for (int index = 0;
            index < count;
            ++index)
        {
            free.add(new BiomeChunk());
        }
    }

    public void
    releaseChunkWithoutLock(BiomeChunk chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            free.push(chunk);
        }
    }

    public void
    releaseChunk(BiomeChunk chunk)
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

        for (BiomeChunk chunk : hash.values())
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
                            BiomeChunk chunk = hash.remove(key);

                            if (chunk != null)
                            {
                                releaseChunkWithoutLock(chunk);

                                chunk.markAsInvalid();
                            }
                        }
                        else
                        {
                            BiomeChunk chunk = hash.get(key);

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

                                            chunk.data[cacheIndex] = null;
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

    public BiomeChunk
    getOrDefaultInitializeChunk(int chunkX, int chunkY, int chunkZ)
    {
        long key = ColorCaching.getChunkKey(chunkX, chunkY, chunkZ, 0);

        lock.lock();

        BiomeChunk result = hash.getAndMoveToFirst(key);

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

            Arrays.fill(result.data, null);

            result.acquire();

            hash.putAndMoveToFirst(result.key, result);
        }

        result.acquire();

        lock.unlock();

        return result;
    }
}
