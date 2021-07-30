package fionathemortal.betterbiomeblend;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

public final class BlendCache
{
    public final ReentrantLock                            lock;
    public final Long2ObjectLinkedOpenHashMap<ColorChunk> hash;
    public final Stack<ColorChunk>                        freeStack;
    public final ArrayList<ColorChunk>                    generating;

    public int invalidationCounter = 0;

    public
    BlendCache(int count)
    {
        lock       = new ReentrantLock();
        hash       = new Long2ObjectLinkedOpenHashMap<>(count);
        freeStack  = new Stack<>();
        generating = new ArrayList<>();

        for (int index = 0;
            index < count;
            ++index)
        {
            freeStack.add(new ColorChunk());
        }
    }

    public void
    releaseChunkWithoutLock(ColorChunk chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            freeStack.push(chunk);
        }
    }

    public void
    releaseChunk(ColorChunk chunk)
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

        for (int x = -1;
            x <= 1;
            ++x)
        {
            for (int z = -1;
                z <= 1;
                ++z)
            {
                for (int colorType = BiomeColorType.FIRST;
                    colorType <= BiomeColorType.LAST;
                    ++colorType)
                {
                    long key = ColorCaching.getChunkKey(chunkX + x, chunkZ + z, colorType);

                    ColorChunk chunk = hash.remove(key);

                    if (chunk != null)
                    {
                        releaseChunkWithoutLock(chunk);

                        chunk.markAsInvalid();
                    }
                    else
                    {
                        ListIterator<ColorChunk> iterator = generating.listIterator();

                        while (iterator.hasNext())
                        {
                            ColorChunk generatingChunk = iterator.next();

                            if (generatingChunk.key == key)
                            {
                                generatingChunk.markAsInvalid();

                                iterator.remove();
                            }
                        }
                    }
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

        for (ColorChunk chunk : hash.values())
        {
            releaseChunkWithoutLock(chunk);

            chunk.markAsInvalid();
        }

        hash.clear();

        lock.unlock();
    }

    public ColorChunk
    getChunk(int chunkX, int chunkZ, int colorType)
    {
        long key = ColorCaching.getChunkKey(chunkX, chunkZ, colorType);

        lock.lock();

        ColorChunk result = hash.getAndMoveToFirst(key);

        if (result != null)
        {
            result.acquire();
        }

        lock.unlock();

        return result;
    }

    public ColorChunk
    newChunk(int chunkX, int chunkZ, int colorType)
    {
        long key = ColorCaching.getChunkKey(chunkX, chunkZ, colorType);

        lock.lock();

        ColorChunk result = null;

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
        result.acquire();

        generating.add(result);

        lock.unlock();

        return result;
    }

    public ColorChunk
    putChunk(ColorChunk chunk)
    {
        ColorChunk result = chunk;

        lock.lock();

        if (generating.remove(chunk))
        {
            chunk.acquire();

            ColorChunk prev = hash.getAndMoveToFirst(chunk.key);

            if (prev == null)
            {
                hash.putAndMoveToFirst(chunk.key, chunk);
            }
            else
            {
                ColorChunk olderChunk;

                if (chunk.invalidationCounter >= prev.invalidationCounter)
                {
                    olderChunk = prev;

                    hash.put(chunk.key, chunk);
                }
                else
                {
                    olderChunk = chunk;

                    result = prev;
                }

                releaseChunkWithoutLock(olderChunk);

                olderChunk.markAsInvalid();
            }
        }

        lock.unlock();

        return result;
    }
}
