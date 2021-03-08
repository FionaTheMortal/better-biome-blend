package fionathemortal.betterbiomeblend.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import fionathemortal.betterbiomeblend.BiomeColorType;
import fionathemortal.betterbiomeblend.ColorChunk;
import fionathemortal.betterbiomeblend.ColorChunkCache;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.renderer.color.ColorCache;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeColors;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.storage.ISpawnWorldInfo;

@Mixin(value = ClientWorld.class)
public abstract class MixinClientWorld extends World
{
	protected MixinClientWorld(
		ISpawnWorldInfo worldInfo, 
		RegistryKey<World> dimension, 
		DimensionType dimensionType,
		Supplier<IProfiler> profiler, 
		boolean isRemote, 
		boolean isDebug, 
		long seed) 
	{
		super(worldInfo, dimension, dimensionType, profiler, isRemote, isDebug, seed);
	}

	@Shadow
	private Object2ObjectArrayMap<ColorResolver, ColorCache> colorCaches = new Object2ObjectArrayMap<ColorResolver, ColorCache>();
	
	private final ColorChunkCache waterColorCache   = new ColorChunkCache(1024);
	private final ColorChunkCache grassColorCache   = new ColorChunkCache(1024);
	private final ColorChunkCache foliageColorCache = new ColorChunkCache(1024);
	
	private final ColorChunkCache rawWaterColorCache   = new ColorChunkCache(256);
	private final ColorChunkCache rawGrassColorCache   = new ColorChunkCache(256);
	private final ColorChunkCache rawFoliageColorCache = new ColorChunkCache(256);
	
	private final ThreadLocal<ColorChunk> threadLocalWaterChunk   = ThreadLocal.withInitial(() -> { ColorChunk chunk = new ColorChunk(); chunk.acquire(); return chunk; });
	private final ThreadLocal<ColorChunk> threadLocalGrassChunk   = ThreadLocal.withInitial(() -> { ColorChunk chunk = new ColorChunk(); chunk.acquire(); return chunk; });
	private final ThreadLocal<ColorChunk> threadLocalFoliageChunk = ThreadLocal.withInitial(() -> { ColorChunk chunk = new ColorChunk(); chunk.acquire(); return chunk; });
	
	@Inject(method = "clearColorCaches", at = @At("HEAD"))
   	public void
   	onClearColorCaches(CallbackInfo ci)
   	{
		waterColorCache.invalidateAll();
		grassColorCache.invalidateAll();
		foliageColorCache.invalidateAll();
		
		rawWaterColorCache.invalidateAll();
		rawGrassColorCache.invalidateAll();
		rawFoliageColorCache.invalidateAll();
   	}
	
	@Inject(method = "onChunkLoaded", at = @At("HEAD"))
	public void 
	onOnChunkLoaded(int chunkX, int chunkZ, CallbackInfo ci)
	{
		if (BetterBiomeBlendClient.gameSettings.biomeBlendRadius > 0)
		{
			waterColorCache.invalidateNeighbourhood(chunkX, chunkZ);
			grassColorCache.invalidateNeighbourhood(chunkX, chunkZ);
			foliageColorCache.invalidateNeighbourhood(chunkX, chunkZ);
		}
		else
		{
			waterColorCache.invalidateChunk(chunkX, chunkZ);
			grassColorCache.invalidateChunk(chunkX, chunkZ);
			foliageColorCache.invalidateChunk(chunkX, chunkZ);
		}
		
		rawWaterColorCache.invalidateChunk(chunkX, chunkZ);
		rawGrassColorCache.invalidateChunk(chunkX, chunkZ);
		rawFoliageColorCache.invalidateChunk(chunkX, chunkZ);
	}
	
	@Overwrite
	public int 
	getBlockColor(BlockPos blockPosIn, ColorResolver colorResolverIn)
	{
		int result;
		
		ThreadLocal<ColorChunk> threadLocalChunk;
		
		if (colorResolverIn == BiomeColors.GRASS_COLOR)
		{
			threadLocalChunk = threadLocalGrassChunk;
		}
		else if (colorResolverIn == BiomeColors.WATER_COLOR)
		{
			threadLocalChunk = threadLocalWaterChunk;
		}
		else
		{
			threadLocalChunk = threadLocalFoliageChunk;
		}
	
		int x = blockPosIn.getX();
		int z = blockPosIn.getZ();
		
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		
		ColorChunk chunk = BetterBiomeBlendClient.getThreadLocalChunk(threadLocalChunk, chunkX, chunkZ);

		if (chunk != null)
		{	
			result = chunk.getColor(x, z);
		}
		else
		{
			BiomeColorType  colorType;
			
			ColorChunkCache colorCache;
			ColorChunkCache rawColorCache;
			
			if (colorResolverIn == BiomeColors.GRASS_COLOR)
			{
				colorType     = BiomeColorType.GRASS;
				
				colorCache    = grassColorCache;
				rawColorCache = rawGrassColorCache;
			}
			else if (colorResolverIn == BiomeColors.WATER_COLOR)
			{
				colorType     = BiomeColorType.WATER;
				
				colorCache    = waterColorCache;
				rawColorCache = rawWaterColorCache;
			}
			else
			{
				colorType     = BiomeColorType.FOLIAGE;
				
				colorCache    = foliageColorCache;
				rawColorCache = rawFoliageColorCache;
			}
			
			chunk = BetterBiomeBlendClient.getBlendedColorChunk(this, chunkX, chunkZ, colorCache, colorType, rawColorCache);
			
			BetterBiomeBlendClient.setThreadLocalChunk(threadLocalChunk, chunk, colorCache);
			
			result = chunk.getColor(x, z);
		}

		return result;
	}
}
