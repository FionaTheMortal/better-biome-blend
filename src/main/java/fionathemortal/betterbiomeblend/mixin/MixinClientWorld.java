package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.world.BiomeColorCache;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld extends World
{
    @Shadow
    private Object2ObjectArrayMap<ColorResolver, BiomeColorCache> colorCache =
        new Object2ObjectArrayMap<>();

    @Unique
    private final ColorCache betterBiomeBlend$blendColorCache = new ColorCache(2048);

    @Unique
    private final ColorCache betterBiomeBlend$rawColorCache   = new ColorCache(512);

    @Unique
    private final BiomeCache betterBiomeBlend$biomeCache      = new BiomeCache(32);

    @Unique
    private final ThreadLocal<ColorChunk> betterBiomeBlend$threadLocalWaterChunk   =
        ThreadLocal.withInitial(
            () ->
            {
                ColorChunk chunk = new ColorChunk();
                chunk.acquire();
                return chunk;
            });

    @Unique
    private final ThreadLocal<ColorChunk> betterBiomeBlend$threadLocalGrassChunk   =
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
        MutableWorldProperties worldInfo,
        RegistryKey<World>     dimension,
        DimensionType          dimensionType,
        Supplier<Profiler>     profiler,
        boolean                isRemote,
        boolean                isDebug,
        long                   seed)
    {
        super(worldInfo, dimension, dimensionType, profiler, isRemote, isDebug, seed);
    }

    @Inject(method = "reloadColor", at = @At("HEAD"))
    public void
    onReloadColor(CallbackInfo ci)
    {
        betterBiomeBlend$blendColorCache.invalidateAll();
        betterBiomeBlend$rawColorCache.invalidateAll();
        betterBiomeBlend$biomeCache.invalidateAll();
    }

    @Inject(method = "resetChunkColor", at = @At("HEAD"))
    public void
    onResetChunkColor(ChunkPos position, CallbackInfo ci)
    {
        betterBiomeBlend$blendColorCache.invalidateNeighbourhood(position.x, position.z);
        betterBiomeBlend$rawColorCache.invalidateChunk(position.x, position.z);
        betterBiomeBlend$biomeCache.invalidateChunk(position.x, position.z);
    }

    @Overwrite
    public int
    getColor(BlockPos blockPosIn, ColorResolver colorResolverIn)
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
                betterBiomeBlend$rawColorCache,
                betterBiomeBlend$biomeCache);

            ColorCaching.setThreadLocalChunk(threadLocalChunk, chunk, betterBiomeBlend$blendColorCache);
        }

        int result = chunk.getColor(x, z);

        return result;
    }
}
