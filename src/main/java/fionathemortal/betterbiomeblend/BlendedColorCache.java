package fionathemortal.betterbiomeblend;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public class BlendedColorCache 
{
	public Long2ObjectLinkedOpenHashMap<BlendedColorChunk> hash;
	public List<BlendedColorChunk> freeList;
	
	public int chunkCount; 
	public int invalidationCounter;
	
	public
	BlendedColorCache(int count)
	{
		hash = new Long2ObjectLinkedOpenHashMap<BlendedColorChunk>(count);
		freeList = new ArrayList<BlendedColorChunk>(count);
		
		for (int index = 0;
			index < count;
			++index)
		{
			freeList.add(new BlendedColorChunk());
		}
		
		chunkCount = count;
	}
	
	public static long
	getChunkKey(int chunkX, int chunkZ)
	{
		long result = ((long)chunkZ << 32) | ((long)chunkX & 0xFFFFFFFFL);
		
		return result;
	}

	public void
	releaseChunkWithoutLock(BlendedColorChunk chunk)
	{
		int refCount = chunk.release();
		
		if (refCount == 0)
		{
			freeList.add(chunk);				
		}
	}
	
	public void
	releaseChunk(BlendedColorChunk chunk)
	{
		int refCount = chunk.release();
		
		if (refCount == 0)
		{
			synchronized(this)
			{
				freeList.add(chunk);				
			}
		}
	}
	
	public void
	invalidateNeighbourhood(int chunkX, int chunkZ)
	{
		synchronized(this)
		{
			++invalidationCounter;
			
			for (int x = -1;
				x <= 1;
				++x)
			{
				for (int z = -1;
					z <= 1;
					++z)
				{
					long key = getChunkKey(chunkX + x, chunkZ + z);
					
					BlendedColorChunk chunk = hash.remove(key);
					
					if (chunk != null)
					{
						releaseChunkWithoutLock(chunk);
					}
				}
			}	
		}
	}

	public BlendedColorChunk
	getChunkFromHash(int chunkX, int chunkZ)
	{
		long key = getChunkKey(chunkX, chunkZ);
	
		BlendedColorChunk result;
		
		synchronized(this)
		{
			result = hash.getAndMoveToFirst(key);
			
			if (result != null)
			{
				result.acquire();
			}
		}
		
		return result;
	}
	
	// BUG:  If 2 threads start generating the same chunk with the first one being an invalid chunk. If the first one gets done after the second it will take its place
	//       in the hash as the legitimate chunk. This will cause incorrect colors to be rendered.
	
	// NOTE: Adding an invalidationCounter does not fix the issue
	//       We also do not invalidate the thread local chunk. To if the same thread generates the same chunk first incomplete and then complete the second will be incorrect.
	//       The thread local cache seems to be causing it. So that ^ is probably the issue
	
	public void
	addChunkToHash(BlendedColorChunk chunk)
	{
		chunk.acquire();
		
		synchronized(this)
		{
			BlendedColorChunk prev = hash.getAndMoveToFirst(chunk.key);
			
			if (prev != null)
			{
				BlendedColorChunk chunkToRelease;
				
				if (chunk.invalidationCounter >= prev.invalidationCounter)
				{
					hash.putAndMoveToFirst(chunk.key, chunk);
					
					chunkToRelease = prev;
				}
				else
				{
					chunkToRelease = chunk;
				}
				
				releaseChunkWithoutLock(chunkToRelease);
			}
			else
			{
				hash.putAndMoveToFirst(chunk.key, chunk);
			}
		}
	}
	
	public BlendedColorChunk
	allocateChunk(int chunkX, int chunkZ)
	{
		BlendedColorChunk result = null;
		
		long key = getChunkKey(chunkX, chunkZ);
		
		synchronized(this)
		{
			if (freeList.size() > 0)
			{
				result = freeList.remove(freeList.size() - 1);
			}
			else
			{
				for (;;)
				{
					result  = hash.removeLast();
					
					if (result.refCount.get() == 1)
					{
						result.release();
						break;
					}
					
					hash.putAndMoveToFirst(result.key, result);
				}
			}
		}
		
		result.key = key;
		result.invalidationCounter = invalidationCounter;
		
		result.acquire();
		
		return result;
	}
}
