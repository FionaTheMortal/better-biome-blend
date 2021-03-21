package fionathemortal.betterbiomeblend;

import java.util.concurrent.atomic.AtomicInteger;

public class ColorChunk 
{
	public int[] data;
	
	public long key;
	public int  invalidationCounter;
	
	public AtomicInteger refCount   = new AtomicInteger();
	public AtomicInteger regionMask = new AtomicInteger();
	
	//
	
	public byte[] data2;

	//
	
	public 
	ColorChunk()
	{
		this.data = new int[16 * 16];
		
		this.markAsInvalid();
		
		this.data2 = new byte[16 * 16 * 3];
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
		
		int blockIndex = (blockZ << 4) | blockX;
		
		int colorR = 0xFF & this.data2[3 * blockIndex + 0];
		int colorG = 0xFF & this.data2[3 * blockIndex + 1];
		int colorB = 0xFF & this.data2[3 * blockIndex + 2];
		
		int color = 
			(colorR <<  0) |
			(colorG <<  8) |
			(colorB << 16) |
			0xFF000000;

		return color;
	}
}
