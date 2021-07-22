package fionathemortal.betterbiomeblend;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

public class BiomeCache
{
    public final ReentrantLock                            lock;
    public final Long2ObjectLinkedOpenHashMap<BiomeChunk> hash;
    public final Stack<BiomeChunk>                        freeStack;

    public volatile int invalidationCounter;

    public
    BiomeCache(int count)
    {
        lock      = new ReentrantLock();
        hash      = new Long2ObjectLinkedOpenHashMap<>(count);
        freeStack = new Stack<>();

        for (int index = 0;
            index < count;
            ++index)
        {
            freeStack.add(new BiomeChunk());
        }
    }

    public void
    releaseChunkWithoutLock(BiomeChunk chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            freeStack.push(chunk);
        }
    }

    public void
    releaseChunk(BiomeChunk chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            lock.lock();

            freeStack.push(chunk);

            lock.unlock();
        }
    }

    public void
    invalidateChunk(int chunkX, int chunkZ)
    {
        lock.lock();

        ++invalidationCounter;

        long key = ColorCaching.getChunkKey(chunkX, chunkZ, 0);

        BiomeChunk chunk = hash.remove(key);

        if (chunk != null)
        {
            releaseChunkWithoutLock(chunk);

            chunk.markAsInvalid();
        }

        lock.unlock();
    }

    public void
    invalidateNeighbourhood(int chunkX, int chunkZ)
    {
        lock.lock();

        ++invalidationCounter;

        for (int x = -1;
            x <= 1;
            ++x)
        {
            for (int z = -1;
                z <= 1;
                ++z)
            {
                long key = ColorCaching.getChunkKey(chunkX + x, chunkZ + z, 0);

                BiomeChunk chunk = hash.remove(key);

                if (chunk != null)
                {
                    releaseChunkWithoutLock(chunk);

                    chunk.markAsInvalid();
                }
            }
        }

        lock.unlock();
    }

    public void
    invalidateAll()
    {
        lock.lock();

        ++invalidationCounter;

        for (BiomeChunk chunk : hash.values())
        {
            releaseChunkWithoutLock(chunk);

            chunk.markAsInvalid();
        }

        hash.clear();

        lock.unlock();
    }

    public BiomeChunk
    getChunk(int chunkX, int chunkZ, int colorType)
    {
        long key = ColorCaching.getChunkKey(chunkX, chunkZ, colorType);

        lock.lock();

        BiomeChunk result = hash.getAndMoveToFirst(key);

        if (result != null)
        {
            result.acquire();
        }

        lock.unlock();

        return result;
    }

    public void
    putChunk(BiomeChunk chunk)
    {
        chunk.acquire();

        lock.lock();

        BiomeChunk prev = hash.getAndMoveToFirst(chunk.key);

        if (prev == null)
        {
            hash.putAndMoveToFirst(chunk.key, chunk);
        }
        else
        {
            BiomeChunk olderChunk;

            if (chunk.invalidationCounter >= prev.invalidationCounter)
            {
                olderChunk = prev;

                hash.put(chunk.key, chunk);
            }
            else
            {
                olderChunk = chunk;
            }

            releaseChunkWithoutLock(olderChunk);

            olderChunk.markAsInvalid();
        }

        lock.unlock();
    }

    public BiomeChunk
    newChunk(int chunkX, int chunkZ, int colorType)
    {
        long key = ColorCaching.getChunkKey(chunkX, chunkZ, colorType);

        lock.lock();

        BiomeChunk result = null;

        if (!freeStack.empty())
        {
            result = freeStack.pop();
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

        result.invalidationCounter = invalidationCounter;

        lock.unlock();

        result.key = key;
        result.acquire();

        return result;
    }

    public BiomeChunk
    getOrDefaultInitializeChunk(int chunkX, int chunkZ, int colorType)
    {
        BiomeChunk result;

        long key = ColorCaching.getChunkKey(chunkX, chunkZ, colorType);

        lock.lock();

        result = hash.getAndMoveToFirst(key);

        if (result == null)
        {
            if (!freeStack.empty())
            {
                result = freeStack.pop();
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
            result.invalidationCounter = invalidationCounter;

            Arrays.fill(result.data, null);

            result.acquire();

            BiomeChunk prev = hash.putAndMoveToFirst(result.key, result);

            if (prev != null)
            {
                releaseChunkWithoutLock(prev);

                prev.markAsInvalid();
            }
        }

        result.acquire();

        lock.unlock();

        return result;
    }
}
