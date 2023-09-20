package fionathemortal.betterbiomeblend.fabric.mixin;

import fionathemortal.betterbiomeblend.common.*;
import fionathemortal.betterbiomeblend.common.cache.ColorCache;
import fionathemortal.betterbiomeblend.common.compat.CustomColorResolverCompatibility;
import fionathemortal.betterbiomeblend.fabric.SodiumColorBlending;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.biome.BlockColorCache;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockColorCache.class)
public class MixinBlockColorCache
{
    @Unique
    private int betterbiomeblend$baseX;

    @Unique
    private int betterbiomeblend$baseY;

    @Unique
    private int betterbiomeblend$baseZ;

    @Unique
    private int betterbiomeblend$chunkX;

    @Unique
    private int betterbiomeblend$chunkY;

    @Unique
    private int betterbiomeblend$chunkZ;

    @Unique
    private int betterbiomeblend$colorBufferCount;

    @Unique
    private int[][] betterbiomeblend$colorBuffers;

    @Unique
    private ColorResolver betterbiomeblend$lastColorResolver;

    @Unique
    private int betterbiomeblend$lastColorType;

    @Unique
    private ColorCache betterBiomeBlend$colorCache;


    @Shadow
    private WorldSlice slice;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    public void
    constructorTail(WorldSlice slice, int radius, CallbackInfo ci)
    {
        SectionPos pos = slice.getOrigin();

        int x = pos.minBlockX();
        int y = pos.minBlockY();
        int z = pos.minBlockZ();

        this.betterbiomeblend$baseX = x - 1;
        this.betterbiomeblend$baseY = y - 1;
        this.betterbiomeblend$baseZ = z - 1;

        this.betterbiomeblend$chunkX = x >> 4;
        this.betterbiomeblend$chunkY = y >> 4;
        this.betterbiomeblend$chunkZ = z >> 4;

        this.betterbiomeblend$colorBufferCount = 3;
        this.betterbiomeblend$colorBuffers     = new int[3][];

        this.betterBiomeBlend$colorCache = ((LevelCacheAccess)((WorldSliceAccessor)slice).getWorld()).getColorCache();
    }

    private void
    growColorBufferCount(int colorType)
    {
        int oldCount = this.betterbiomeblend$colorBufferCount;
        int newCount = colorType + 1;

        if (newCount > oldCount)
        {
            int[][] oldArray = this.betterbiomeblend$colorBuffers;
            int[][] newArray = new int[newCount][];

            for (int index = 0;
                 index < oldCount;
                 ++index)
            {
                newArray[index] = oldArray[index];
            }

            this.betterbiomeblend$colorBufferCount = newCount;
            this.betterbiomeblend$colorBuffers     = newArray;
        }
    }

    private int[]
    allocateNewColorBuffer(int colorType)
    {
        int[] result = new int[18 * 18 * 18];

        this.betterbiomeblend$colorBuffers[colorType] = result;

        return result;
    }

    @Overwrite(remap = false)
    public int
    getColor(ColorResolver resolver, int posX, int posY, int posZ)
    {
        int[] colors;
        int   colorType;

        if (resolver == betterbiomeblend$lastColorResolver)
        {
            colorType = betterbiomeblend$lastColorType;
            colors    = betterbiomeblend$colorBuffers[colorType];
        }
        else
        {
            if (resolver == BiomeColors.GRASS_COLOR_RESOLVER)
            {
                colorType = BiomeColorType.GRASS;
            }
            else if (resolver == BiomeColors.WATER_COLOR_RESOLVER)
            {
                colorType = BiomeColorType.WATER;
            }
            else if (resolver == BiomeColors.FOLIAGE_COLOR_RESOLVER)
            {
                colorType = BiomeColorType.FOLIAGE;
            }
            else
            {
                colorType = CustomColorResolverCompatibility.getColorType(resolver);

                if (colorType >= this.betterbiomeblend$colorBufferCount)
                {
                    growColorBufferCount(colorType);
                }
            }

            colors = this.betterbiomeblend$colorBuffers[colorType];

            if (colors == null)
            {
                colors = allocateNewColorBuffer(colorType);
            }

            this.betterbiomeblend$lastColorResolver = resolver;
            this.betterbiomeblend$lastColorType     = colorType;
        }

        int inBufferX = Mth.clamp(posX - this.betterbiomeblend$baseX, 0, 17);
        int inBufferY = Mth.clamp(posY - this.betterbiomeblend$baseY, 0, 17);
        int inBufferZ = Mth.clamp(posZ - this.betterbiomeblend$baseZ, 0, 17);

        int index = ColorCaching.getArrayIndex(18, inBufferX, inBufferY, inBufferZ);

        int color = colors[index];

        if (color == 0)
        {
            BiomeManager biomeManager = slice.getBiomeAccess();

            int blockX = inBufferX - 1;
            int blockY = inBufferX - 1;
            int blockZ = inBufferX - 1;

            SodiumColorBlending.generateColors(
                biomeManager,
                resolver,
                colorType,
                this.betterBiomeBlend$colorCache,
                blockX,
                blockY,
                blockZ,
                this.betterbiomeblend$chunkX,
                this.betterbiomeblend$chunkY,
                this.betterbiomeblend$chunkZ,
                colors);

            color = colors[index];
        }

        return color;
    }
}
