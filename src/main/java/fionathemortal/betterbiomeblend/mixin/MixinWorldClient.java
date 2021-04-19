package fionathemortal.betterbiomeblend.mixin;

import org.spongepowered.asm.mixin.Mixin;

import fionathemortal.betterbiomeblend.ColorChunkCacheProvider;
import fionathemortal.betterbiomeblend.ColorChunkCache;
import net.minecraft.client.multiplayer.WorldClient;

@Mixin(WorldClient.class)
public abstract class MixinWorldClient implements ColorChunkCacheProvider
{
	public final ColorChunkCache colorChunkCache = new ColorChunkCache(2048);
	
    @Override
    public ColorChunkCache 
    getColorChunkCache() 
    {
    	return colorChunkCache;
    }
}
