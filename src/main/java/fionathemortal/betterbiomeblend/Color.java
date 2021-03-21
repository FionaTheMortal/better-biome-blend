package fionathemortal.betterbiomeblend;

public final class Color 
{
	public static int
	makeRGBAWithFullAlpha(byte R, byte G, byte B)
	{
		int result = 
			((0xFF & (int)R) << 0)  |
			((0xFF & (int)G) << 8)  |
			((0xFF & (int)B) << 16) |
			0xFF000000;
		
		return result;
	}
	
	public static int
	RGBAGetR(int color)
	{
		int result = color & 0xFF;
		
		return result;
	}
	
	public static int
	RGBAGetG(int color)
	{
		int result = (color >> 8) & 0xFF;
		
		return result;
	}
	
	public static int
	RGBAGetB(int color)
	{
		int result = (color >> 16) & 0xFF;
		
		return result;
	}
}
