package fionathemortal.betterbiomeblend.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fionathemortal.betterbiomeblend.BiomeColor;
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
    @Shadow
    private final Object2ObjectArrayMap<ColorResolver, ColorCache> colorCaches =
        new Object2ObjectArrayMap<>();

    @Unique
    private final ColorChunkCache colorCache    = new ColorChunkCache(2048);

    @Unique
    private final ColorChunkCache rawColorCache = new ColorChunkCache(512);

    @Unique
    private final ThreadLocal<ColorChunk> threadLocalWaterChunk   =
        ThreadLocal.withInitial(
            () ->
            {
                ColorChunk chunk = new ColorChunk();
                chunk.acquire();
                return chunk;
            });

    @Unique
    private final ThreadLocal<ColorChunk> threadLocalGrassChunk   =
        ThreadLocal.withInitial(
            () ->
            {
                ColorChunk chunk = new ColorChunk();
                chunk.acquire();
                return chunk;
            });

    @Unique
    private final ThreadLocal<ColorChunk> threadLocalFoliageChunk =
        ThreadLocal.withInitial(
            () ->
            {
                ColorChunk chunk = new ColorChunk();
                chunk.acquire();
                return chunk;
            });

    protected
    MixinClientWorld(
        ISpawnWorldInfo     worldInfo,
        RegistryKey<World>  dimension,
        DimensionType       dimensionType,
        Supplier<IProfiler> profiler,
        boolean             isRemote,
        boolean             isDebug,
        long                seed)
    {
        super(worldInfo, dimension, dimensionType, profiler, isRemote, isDebug, seed);
    }

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

        rawColorCache.invalidateSmallNeighbourhood(chunkX, chunkZ);
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

        ColorChunk chunk = BiomeColor.getThreadLocalChunk(threadLocalChunk, chunkX, chunkZ, colorType);

        if (chunk == null)
        {
            chunk = BiomeColor.getBlendedColorChunk(this, colorType, chunkX, chunkZ, colorCache, rawColorCache, colorResolverIn);

            BiomeColor.setThreadLocalChunk(threadLocalChunk, chunk, colorCache);
        }

        int result = chunk.getColor(x, z);

        return result;
    }
}
