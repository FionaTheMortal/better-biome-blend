package fionathemortal.betterbiomeblend;

public class GenCache 
{
	int   blendRadius;
	int[] colors;
	
	int[] R;
	int[] G;
	int[] B;
	
	public GenCache(int blendRadius)
	{
		int genCacheDim = 16 + 2 * blendRadius;
		
		this.blendRadius = blendRadius;
		this.colors      = new int[genCacheDim * genCacheDim];
		
		this.R = new int[genCacheDim];
		this.G = new int[genCacheDim];
		this.B = new int[genCacheDim];
	}
}
