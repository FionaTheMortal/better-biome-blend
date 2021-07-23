package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeColors;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.storage.ISpawnWorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(value = ClientWorld.class)
public abstract class MixinClientWorld extends World
{
    @Shadow
    private final Object2ObjectArrayMap<ColorResolver, net.minecraft.client.renderer.color.ColorCache> colorCaches =
        new Object2ObjectArrayMap<>();

    @Unique
    private final BlendCache betterBiomeBlend$blendColorCache = new BlendCache(2048);

    @Unique
    private final ColorCache betterBiomeBlend$chunkColorCache = new ColorCache(512);

    @Unique
    private final BiomeCache betterBiomeBlend$chunkBiomeCache = new BiomeCache(32);

    @Unique
    private final ThreadLocal<ColorChunk> betterBiomeBlend$threadLocalWaterChunk =
        ThreadLocal.withInitial(
            () ->
            {
                ColorChunk chunk = new ColorChunk();
                chunk.acquire();
                return chunk;
            });

    @Unique
    private final ThreadLocal<ColorChunk> betterBiomeBlend$threadLocalGrassChunk =
        ThreadLocal.withInitial(
            () ->
            {
                ColorChunk chunk = new ColorChunk();
                chunk.acquire();
                return chunk;
            });

    @Unique
    private final ThreadLocal<ColorChunk> betterBiomeBlend$threadLocalFoliageChunk =
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
        betterBiomeBlend$blendColorCache.invalidateAll();
    }

    @Inject(method = "onChunkLoaded", at = @At("HEAD"))
    public void
    onOnChunkLoaded(int chunkX, int chunkZ, CallbackInfo ci)
    {
        betterBiomeBlend$blendColorCache.invalidateChunk(chunkX, chunkZ);
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
            threadLocalChunk = betterBiomeBlend$threadLocalGrassChunk;
        }
        else if (colorResolverIn == BiomeColors.WATER_COLOR)
        {
            colorType        = BiomeColorType.WATER;
            threadLocalChunk = betterBiomeBlend$threadLocalWaterChunk;
        }
        else
        {
            colorType        = BiomeColorType.FOLIAGE;
            threadLocalChunk = betterBiomeBlend$threadLocalFoliageChunk;
        }

        int x = blockPosIn.getX();
        int z = blockPosIn.getZ();

        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        ColorChunk chunk = ColorCaching.getThreadLocalChunk(threadLocalChunk, chunkX, chunkZ, colorType);

        if (chunk == null)
        {
            chunk = ColorCaching.getBlendedColorChunk(
                this,
                colorResolverIn,
                colorType,
                chunkX,
                chunkZ,
                betterBiomeBlend$blendColorCache,
                betterBiomeBlend$chunkColorCache,
                betterBiomeBlend$chunkBiomeCache);

            ColorCaching.setThreadLocalChunk(threadLocalChunk, chunk, betterBiomeBlend$blendColorCache);
        }

        int result = chunk.getColor(x, z);

        return result;
    }
}
