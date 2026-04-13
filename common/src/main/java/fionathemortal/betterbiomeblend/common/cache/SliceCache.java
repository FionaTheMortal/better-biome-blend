package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.ColorConfig;
import fionathemortal.betterbiomeblend.common.util.Util;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

public final class SliceCache
{
    public static final long INVALID_KEY = -1;

    private static final int KEY_X_POS_BITS      = 23;
    private static final int KEY_Z_POS_BITS      = 23;
    private static final int KEY_Y_POS_BITS      = 7;
    private static final int KEY_H_POS_BITS      = KEY_X_POS_BITS + KEY_Z_POS_BITS;
    private static final int KEY_POS_BITS        = KEY_H_POS_BITS + KEY_Y_POS_BITS;
    private static final int KEY_COLOR_TYPE_BITS = Long.SIZE - KEY_POS_BITS;

    public final ColorConfig colorConfig;
    public final CacheConfig cacheConfig;

    private final ReentrantLock                       lock             = new ReentrantLock();
    private final Long2ObjectLinkedOpenHashMap<Slice> blockSliceHash;
    private final Long2ObjectLinkedOpenHashMap<Slice> solidSliceHash;
    private final Stack<Slice>                        freeBlockSlices  = new Stack<>();
    private final Stack<Slice>                        freeSolidSlices  = new Stack<>();
    private final Long2ObjectOpenHashMap<Slice>       invalidationHash = new Long2ObjectOpenHashMap<>();

    private long genTick = 0;

    public
    SliceCache(ColorConfig colorConfig, CacheConfig cacheConfig, int blockSliceCount, int solidSliceCount)
    {
        this.colorConfig = colorConfig;
        this.cacheConfig = cacheConfig;

        this.blockSliceHash = new Long2ObjectLinkedOpenHashMap<>(blockSliceCount);
        this.solidSliceHash = new Long2ObjectLinkedOpenHashMap<>(solidSliceCount);

        allocSlices(freeBlockSlices, blockSliceCount, false);
        allocSlices(freeSolidSlices, solidSliceCount, true);
    }

    public boolean
    ownsSlice(Slice slice)
    {
        return (slice.config == this.cacheConfig);
    }

    public static long
    getKey(int sliceX, int sliceY, int sliceZ, int colorType)
    {
        long xKey = Util.lowerBits(sliceX, KEY_X_POS_BITS);
        long zKey = Util.lowerBits(sliceZ, KEY_Z_POS_BITS);
        long yKey = Util.lowerBits(sliceY, KEY_Y_POS_BITS);

        long colorTypeKey = Util.lowerBits(colorType, KEY_COLOR_TYPE_BITS);

        long keyPos = xKey | (zKey << KEY_X_POS_BITS) | (yKey << KEY_H_POS_BITS);
        long result = keyPos | (colorTypeKey << KEY_POS_BITS);

        return result;
    }

    public static long
    getInvalidationKey(int sliceX, int sliceZ)
    {
        return getKey(sliceX, 0, sliceZ, 0);
    }

    public static long
    getInvalidationKey(long key)
    {
        long result = Util.lowerBits(key, KEY_H_POS_BITS);

        return result;
    }

    public Slice
    getSlice(int sliceX, int sliceY, int sliceZ, int colorType, boolean make, boolean solid)
    {
        lock.lock();

        Slice result = lockedGetSlice(sliceX, sliceY, sliceZ, colorType);

        if (result == null && make)
        {
            long ticket = getGenTick();

            result = lockedNewSlice(sliceX, sliceY, sliceZ, colorType, solid);
            result = lockedPutSlice(result, ticket);
        }

        lock.unlock();

        return result;
    }

    public Slice
    newSlice(int sliceX, int sliceY, int sliceZ, int colorType, boolean solid)
    {
        lock.lock();

        Slice result = lockedNewSlice(sliceX, sliceY, sliceZ, colorType, solid);

        lock.unlock();

        return result;
    }

    public Slice
    putSlice(Slice slice, long ticket)
    {
        lock.lock();

        Slice result = lockedPutSlice(slice, ticket);

        lock.unlock();

        return result;
    }

    public void
    releaseSlice(Slice slice)
    {
        lock.lock();

        lockedReleaseSlice(slice);

        lock.unlock();
    }

    public void
    invalidateBlocks(int blockMinX, int blockMinZ, int blockMaxX, int blockMaxZ, boolean includePerimeter, boolean invalidateSlices)
    {
        int affectedBlockMinX = blockMinX;
        int affectedBlockMinZ = blockMinZ;

        int affectedBlockMaxX = blockMaxX;
        int affectedBlockMaxZ = blockMaxZ;

        if (includePerimeter)
        {
            affectedBlockMinX -= cacheConfig.perimeter;
            affectedBlockMinZ -= cacheConfig.perimeter;

            affectedBlockMaxX += cacheConfig.perimeter;
            affectedBlockMaxZ += cacheConfig.perimeter;
        }

        int sampleMinX = cacheConfig.floorBlockToSample(affectedBlockMinX);
        int sampleMinZ = cacheConfig.floorBlockToSample(affectedBlockMinZ);

        int sampleMaxX = cacheConfig.ceilBlockToSample(affectedBlockMaxX);
        int sampleMaxZ = cacheConfig.ceilBlockToSample(affectedBlockMaxZ);

        invalidateSlices(sampleMinX, sampleMinZ, sampleMaxX, sampleMaxZ, invalidateSlices);
    }

    public void
    invalidateChunk(int chunkX, int chunkZ, boolean includePerimeter, boolean invalidateSlices)
    {
        int blockMinX = Util.chunkToBlock(chunkX);
        int blockMinZ = Util.chunkToBlock(chunkZ);

        int blockMaxX = Util.chunkToBlock(chunkX + 1);
        int blockMaxZ = Util.chunkToBlock(chunkZ + 1);

        invalidateBlocks(blockMinX, blockMinZ, blockMaxX, blockMaxZ, includePerimeter, invalidateSlices);
    }

    public void
    destroy()
    {
        lock.lock();

        // NOTE: Invalidate all slices to prevent use of stale data

        for (Slice slice : blockSliceHash.values())
        {
            slice.markAsInvalid();

            lockedReleaseSlice(slice);
        }

        for (Slice slice : solidSliceHash.values())
        {
            slice.markAsInvalid();

            lockedReleaseSlice(slice);
        }

        lock.unlock();
    }

    public long
    getGenTick()
    {
        return this.genTick;
    }

    private void
    lockedInvalidateSlice(Slice slice)
    {
        lockedRemoveFromHash(slice);

        slice.markAsInvalid();
    }

    private void
    lockedInvalidateSlices(int sliceX, int sliceZ)
    {
        long invalidationKey = getInvalidationKey(sliceX, sliceZ);

        Slice first = invalidationHash.get(invalidationKey);

        for (Slice current = first;
             current != null;
            )
        {
            Slice next = current.next;

            lockedInvalidateSlice(current);

            current = next;
        }
    }

    private void
    lockedInvalidateSamplesInSlice(Slice slice, int minX, int minZ, int maxX, int maxZ)
    {
        int minY = 0;
        int maxY = cacheConfig.sliceSize;

        if (!slice.isSolid())
        {
            for (int z = minZ;
                 z < maxZ;
                 ++z)
            {
                for (int y = minY;
                     y < maxY;
                     ++y)
                {
                    for (int x = minX;
                         x < maxX;
                         ++x)
                    {
                        slice.setColor(x, y, z, Slice.INVALID_COLOR);
                    }
                }
            }
        }
        else
        {
            lockedInvalidateSlice(slice);
        }
    }

    private void
    lockedInvalidateSamplesInSlices(int sliceX, int sliceZ, int sampleMinX, int sampleMinZ, int sampleMaxX, int sampleMaxZ)
    {
        int sliceSampleMinX = cacheConfig.getSampleFromSlice(sliceX);
        int sliceSampleMinZ = cacheConfig.getSampleFromSlice(sliceZ);

        int sliceSampleMaxX = cacheConfig.getSampleFromSlice(sliceX + 1);
        int sliceSampleMaxZ = cacheConfig.getSampleFromSlice(sliceZ + 1);

        int minX = Math.max(sampleMinX, sliceSampleMinX);
        int minZ = Math.max(sampleMinZ, sliceSampleMinZ);

        int maxX = Math.min(sampleMaxX, sliceSampleMaxX);
        int maxZ = Math.min(sampleMaxZ, sliceSampleMaxZ);

        int inSliceMinX = minX - sliceSampleMinX;
        int inSliceMinZ = minZ - sliceSampleMinZ;

        int inSliceMaxX = maxX - sliceSampleMinX;
        int inSliceMaxZ = maxZ - sliceSampleMinZ;

        long invalidationKey = getInvalidationKey(sliceX, sliceZ);

        Slice first = invalidationHash.get(invalidationKey);

        for (Slice current = first;
             current != null;
            )
        {
            Slice next = current.next;

            lockedInvalidateSamplesInSlice(current, inSliceMinX, inSliceMinZ, inSliceMaxX, inSliceMaxZ);

            current = next;
        }
    }

    private void
    invalidateSlices(int sampleMinX, int sampleMinZ, int sampleMaxX, int sampleMaxZ, boolean invalidateSlices)
    {
        int sliceMinX = cacheConfig.floorSampleToSlice(sampleMinX);
        int sliceMinZ = cacheConfig.floorSampleToSlice(sampleMinZ);

        int sliceMaxX = cacheConfig.ceilSampleToSlice(sampleMaxX);
        int sliceMaxZ = cacheConfig.ceilSampleToSlice(sampleMaxZ);

        lock.lock();

        ++this.genTick;

        for (int sliceZ = sliceMinZ;
             sliceZ < sliceMaxZ;
             ++sliceZ)
        {
            for (int sliceX = sliceMinX;
                 sliceX < sliceMaxX;
                 ++sliceX)
            {
                if (invalidateSlices)
                {
                    lockedInvalidateSlices(sliceX, sliceZ);
                }
                else
                {
                    lockedInvalidateSamplesInSlices(sliceX, sliceZ, sampleMinX, sampleMinZ, sampleMaxX, sampleMaxZ);
                }
            }
        }

        lock.unlock();
    }

    private Stack<Slice>
    getFreeList(boolean solid)
    {
        Stack<Slice> result;

        if (solid)
        {
            result = freeSolidSlices;
        }
        else
        {
            result = freeBlockSlices;
        }

        return result;
    }

    private Stack<Slice>
    getFreeList(Slice slice)
    {
        return getFreeList(slice.isSolid());
    }

    private Long2ObjectLinkedOpenHashMap<Slice>
    getHash(boolean solid)
    {
        Long2ObjectLinkedOpenHashMap<Slice> result;

        if (solid)
        {
            result = solidSliceHash;
        }
        else
        {
            result = blockSliceHash;
        }

        return result;
    }

    private Long2ObjectLinkedOpenHashMap<Slice>
    getHash(Slice slice)
    {
        return getHash(slice.isSolid());
    }

    private void
    allocSlices(Stack<Slice> freeList, int count, boolean solid)
    {
        freeList.setSize(count);

        for (int index = 0;
            index < count;
            ++index)
        {
            Slice slice = new Slice(cacheConfig, cacheConfig.sliceSize, cacheConfig.sliceSize, solid);

            freeList.set(index, slice);
        }
    }

    private void
    lockedReleaseSlice(Slice slice)
    {
        int refCount = slice.release();

        if (refCount == 0)
        {
            lockedAddToFreeList(slice);
        }
    }

    private void
    lockedAddToFreeList(Slice slice)
    {
        Stack<Slice> freeList = getFreeList(slice);

        freeList.add(slice);
    }

    private void
    lockedAddToInvalidationHash(Slice slice)
    {
        long invalidationKey = slice.getInvalidationKey();

        Slice parent = invalidationHash.get(invalidationKey);

        if (parent != null)
        {
            parent.linkInsert(slice);
        }
        else
        {
            invalidationHash.put(invalidationKey, slice);
        }
    }

    private void
    lockedAddToHash(Slice slice)
    {
        Long2ObjectLinkedOpenHashMap<Slice> hash = getHash(slice);

        hash.putAndMoveToFirst(slice.key, slice);

        slice.acquire();

        lockedAddToInvalidationHash(slice);
    }

    private void
    lockedRemoveFromInvalidationHash(Slice slice)
    {
        if (slice.prev == null)
        {
            invalidationHash.remove(slice.getInvalidationKey());

            if (slice.next != null)
            {
                Slice nextSlice = slice.next;

                invalidationHash.put(nextSlice.getInvalidationKey(), slice.next);
            }
        }

        slice.linkRemove();
    }

    private void
    lockedRemoveFromHash(Slice slice)
    {
        Long2ObjectLinkedOpenHashMap<Slice> hash = getHash(slice);

        hash.remove(slice.key);

        lockedRemoveFromInvalidationHash(slice);

        lockedReleaseSlice(slice);
    }

    private Slice
    lockedGetSlice(long key)
    {
        Slice result = blockSliceHash.getAndMoveToFirst(key);

        if (result == null)
        {
            result = solidSliceHash.getAndMoveToFirst(key);
        }

        if (result != null)
        {
            result.acquire();
        }

        return result;
    }

    private Slice
    lockedGetSlice(int sliceX, int sliceY, int sliceZ, int colorType)
    {
        long key = getKey(sliceX, sliceY, sliceZ, colorType);

        Slice result = lockedGetSlice(key);

        return result;
    }

    private Slice
    lockedPutSlice(Slice slice, long ticket)
    {
        Slice result;

        slice.ticket = ticket;

        Slice storedSlice = lockedGetSlice(slice.key);

        if (storedSlice == null || storedSlice.ticket < slice.ticket)
        {
            if (storedSlice != null)
            {
                lockedInvalidateSlice(storedSlice);

                lockedReleaseSlice(storedSlice);
            }

            lockedAddToHash(slice);

            result = slice;
        }
        else
        {
            slice.markAsInvalid();

            lockedReleaseSlice(slice);

            result = storedSlice;
        }

        return result;
    }

    private Slice
    lockedEvictLRUSlice(Long2ObjectLinkedOpenHashMap<Slice> hash)
    {
        Slice result = null;

        int hashSize = hash.size();

        for (int index = 0;
            index < hashSize;
            ++index)
        {
            Slice slice = hash.removeLast();

            if (slice.getReferenceCount() == 1)
            {
                result = slice;
                break;
            }
            else
            {
                hash.putAndMoveToFirst(slice.key, slice);
            }
        }

        if (result != null)
        {
            result.release();

            lockedRemoveFromInvalidationHash(result);
        }

        return result;
    }

    private Slice
    lockedNewSlice(int sliceX, int sliceY, int sliceZ, int colorType, boolean solid)
    {
        Slice result;

        long key = getKey(sliceX, sliceY, sliceZ, colorType);

        Stack<Slice>                        freeList = getFreeList(solid);
        Long2ObjectLinkedOpenHashMap<Slice> hash     = getHash(solid);

        if (!freeList.empty())
        {
            result = freeList.pop();
        }
        else
        {
            result = lockedEvictLRUSlice(hash);
        }

        result.markAsInvalid();
        result.init(key);
        result.acquire();

        return result;
    }
}
