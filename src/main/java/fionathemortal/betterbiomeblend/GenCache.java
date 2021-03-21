package fionathemortal.betterbiomeblend;

public class GenCache 
{
	int   blendRadius;
	int[] colors;
	
	int[] R;
	int[] G;
	int[] B;
	
	byte[] color2;
	
	public GenCache(int blendRadius)
	{
		int genCacheDim = 16 + 2 * blendRadius;
		
		this.blendRadius = blendRadius;
		this.colors      = new int[genCacheDim * genCacheDim];
		
		this.R = new int[genCacheDim];
		this.G = new int[genCacheDim];
		this.B = new int[genCacheDim];
		
		this.color2 = new byte[3 * genCacheDim * genCacheDim];
	}
}
