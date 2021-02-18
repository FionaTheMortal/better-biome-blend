package fionathemortal.betterbiomeblend;

import java.util.ArrayList;
import java.util.List;

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
	
	public void
	invalidateAll()
	{
		synchronized(this)
		{
			++invalidationCounter;
			
			for (BlendedColorChunk chunk : hash.values())
			{
				releaseChunkWithoutLock(chunk);
			}
			
			hash.clear();	
		}
	}

	public BlendedColorChunk
	getChunk(int chunkX, int chunkZ)
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
	
	public void
	putChunk(BlendedColorChunk chunk)
	{
		chunk.acquire();
		
		synchronized(this)
		{
			BlendedColorChunk prev = hash.getAndMoveToFirst(chunk.key);
			
			if (prev != null)
			{
				BlendedColorChunk olderChunk;
				
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
			}
			else
			{
				hash.putAndMoveToFirst(chunk.key, chunk);
			}
		}
	}
	
	public BlendedColorChunk
	newChunk(int chunkX, int chunkZ)
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
					result = hash.removeLast();
					
					if (result.refCount.get() == 1)
					{
						result.release();
						break;
					}
				}
			}
		}
		
		result.key = key;
		result.invalidationCounter = invalidationCounter;
		
		result.acquire();
		
		return result;
	}
}
