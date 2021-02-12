package fionathemortal.betterbiomeblend;

public class GenCache 
{
	int   blendRadius;
	int[] colors;
	
	public GenCache(int blendRadius)
	{
		int genCacheDim = 16 + 2 * blendRadius;
		
		this.blendRadius = blendRadius;
		this.colors      = new int[genCacheDim * genCacheDim];
	}
}
