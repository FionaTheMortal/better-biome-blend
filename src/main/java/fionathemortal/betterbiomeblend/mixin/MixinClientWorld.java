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
	private Object2ObjectArrayMap<ColorResolver, ColorCache> colorCaches = 
		new Object2ObjectArrayMap<ColorResolver, ColorCache>();

	private final ColorChunkCache colorCache    = new ColorChunkCache(2048);
	private final ColorChunkCache rawColorCache = new ColorChunkCache(512);
	
	private final ThreadLocal<ColorChunk> threadLocalWaterChunk   = 
		ThreadLocal.withInitial(
			() -> 
			{ 
				ColorChunk chunk = new ColorChunk(); 
				chunk.acquire(); 
				return chunk; 
			});
	
	private final ThreadLocal<ColorChunk> threadLocalGrassChunk   = 
		ThreadLocal.withInitial(
			() -> 
			{
				ColorChunk chunk = new ColorChunk(); 
				chunk.acquire(); 
				return chunk; 
			});
	
	private final ThreadLocal<ColorChunk> threadLocalFoliageChunk = 
		ThreadLocal.withInitial(
			() -> 
			{ 
				ColorChunk chunk = new ColorChunk(); 
				chunk.acquire(); 
				return chunk;
			});
	
	@Inject(method = "clearColorCaches", at = @At("HEAD"))
   	public void
   	onClearColorCaches(CallbackInfo ci)
   	{
		colorCache.invalidateAll();
		rawColorCache.invalidateAll();
   	}
	
	@Inject(method = "onChunkLoaded", at = @At("HEAD"))
	public void 
	onOnChunkLoaded(int chunkX, int chunkZ, CallbackInfo ci)
	{
		colorCache.invalidateNeighbourhood(chunkX, chunkZ);
		
		rawColorCache.invalidateNeighbourhood2(chunkX, chunkZ);
	}
	
	@Overwrite
	public int 
	getBlockColor(BlockPos blockPosIn, ColorResolver colorResolverIn)
	{
		int                     colorType;
		ThreadLocal<ColorChunk> threadLocalChunk;
		
		if (colorResolverIn == BiomeColors.GRASS_COLOR)
		{
			colorType        = BiomeColorType.GRASS;
			threadLocalChunk = threadLocalGrassChunk;
		}
		else if (colorResolverIn == BiomeColors.WATER_COLOR)
		{
			colorType        = BiomeColorType.WATER;
			threadLocalChunk = threadLocalWaterChunk;
		}
		else
		{
			colorType        = BiomeColorType.FOLIAGE;
			threadLocalChunk = threadLocalFoliageChunk;
		}

		int x = blockPosIn.getX();
		int z = blockPosIn.getZ();
		
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		
		ColorChunk chunk = BetterBiomeBlendClient.getThreadLocalChunk(threadLocalChunk, chunkX, chunkZ, colorType);

		// TODO: Jump table all of this?
		
		if (chunk == null)
		{
			chunk = BetterBiomeBlendClient.getBlendedColorChunk(this, colorType, chunkX, chunkZ, colorCache, rawColorCache);
			
			BetterBiomeBlendClient.setThreadLocalChunk(threadLocalChunk, chunk, colorCache);
		}
		
		int result = 0;
		
		if (true) // chunk is generated
		{
			result = chunk.getColor(x, z);
		}
		else
		{
			// gen color if necessary
		}
		
		return result;
	}
}
