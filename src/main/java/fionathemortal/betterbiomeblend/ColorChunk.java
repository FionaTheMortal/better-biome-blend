package fionathemortal.betterbiomeblend;

import java.util.concurrent.atomic.AtomicInteger;

public final class ColorChunk 
{
	public byte[] data;
	
	public long key;
	public int  invalidationCounter;
	
	public AtomicInteger refCount   = new AtomicInteger();
	public AtomicInteger regionMask = new AtomicInteger();

	public 
	ColorChunk()
	{
		this.markAsInvalid();
		
		this.data = new byte[16 * 16 * 3];
	}
	
	public int
	getValidRegions()
	{
		int result = regionMask.get();
		
		return result;
	}
	
	public void
	addToValidRegions(int value)
	{
		int mask = regionMask.get();
		
		regionMask.set(mask | value);
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
		key = Long.MIN_VALUE;
	}
	
	public int
	getColor(int x, int z)
	{
		int blockX = x & 15;
		int blockZ = z & 15;
		
		int index = 3 * ((blockZ << 4) | blockX);
		
		byte colorR = this.data[index + 0];
		byte colorG = this.data[index + 1];
		byte colorB = this.data[index + 2];
		
		int result = Color.makeRGBAWithFullAlpha(colorR, colorG, colorB);
		
		return result;
	}
}
