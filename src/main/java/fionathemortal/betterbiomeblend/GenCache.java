package fionathemortal.betterbiomeblend;

public class GenCache 
{
	int    blendRadius;
	byte[] color;
	
	int[] R;
	int[] G;
	int[] B;
	
	public GenCache(int blendRadius)
	{
		int genCacheDim = 16 + 2 * blendRadius;
		
		this.blendRadius = blendRadius;
		this.color = new byte[3 * genCacheDim * genCacheDim];
		
		this.R = new int[genCacheDim];
		this.G = new int[genCacheDim];
		this.B = new int[genCacheDim];
	}
}
