package fionathemortal.betterbiomeblend.fabric;

import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import fionathemortal.betterbiomeblend.common.BlendBuffer;
import fionathemortal.betterbiomeblend.common.BlendConfig;
import fionathemortal.betterbiomeblend.common.Color;
import fionathemortal.betterbiomeblend.common.ColorBlending;
import fionathemortal.betterbiomeblend.common.cache.ColorCache;
import fionathemortal.betterbiomeblend.common.cache.ColorSlice;
import fionathemortal.betterbiomeblend.common.debug.Debug;
import fionathemortal.betterbiomeblend.common.debug.DebugEvent;
import fionathemortal.betterbiomeblend.common.debug.DebugEventType;
import fionathemortal.betterbiomeblend.fabric.mixin.MixinBlockColorCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

public class SodiumColorBlending
{

    public static void
    gatherColors(
        BiomeManager  biomeManager,
        ColorResolver resolver,
        BlendBuffer   blendBuffer,
        int           pX,
        int           pY,
        int           pZ,
        int           blendRadius)
    {
        /*
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        final int sliceSizeLog2 = BlendConfig.getSliceSizeLog2(blendRadius);
        final int blockSizeLog2 = BlendConfig.getBlockSizeLog2(blendRadius);

        final int sliceX = pX >> sliceSizeLog2;
        final int sliceY = pY >> sliceSizeLog2;
        final int sliceZ = pZ >> sliceSizeLog2;

        final int blendSize = BlendConfig.getBlendSize(blendRadius);
        final int blendDim  = BlendConfig.getBlendBufferSize(blendRadius);

        final int scaledBlendDiameter = (2 * blendRadius) >> blockSizeLog2;

        int worldMinX = (sliceX << sliceSizeLog2) - ((scaledBlendDiameter << blockSizeLog2) >> 1);
        int worldMinY = (sliceY << sliceSizeLog2) - ((scaledBlendDiameter << blockSizeLog2) >> 1);
        int worldMinZ = (sliceZ << sliceSizeLog2) - ((scaledBlendDiameter << blockSizeLog2) >> 1);

        int indexY = 0;

        for (int y = 0;
            y < blendSize;
            ++y)
        {
            int indexZ = indexY;

            for (int z = 0;
                 z < blendSize;
                 ++z)
            {
                int indexX = indexZ;

                for (int x = 0;
                     x < blendSize;
                     ++x)
                {
                    final int sampleMinX = worldMinX + (x << blockSizeLog2);
                    final int sampleMinY = worldMinY + (y << blockSizeLog2);
                    final int sampleMinZ = worldMinZ + (z << blockSizeLog2);

                    final int sampleX = sampleMinX;
                    final int sampleY = sampleMinY;
                    final int sampleZ = sampleMinZ;

                    blockPos.set(sampleX, sampleY, sampleZ);

                    Holder<Biome> biomeHolder = biomeManager.getBiome(blockPos);

                    int color = 0xFFFFFFFF;

                    if (biomeHolder.isBound())
                    {
                        Biome biome = biomeHolder.value();

                        color = resolver.getColor(biome, sampleX, sampleZ);
                    }
                    else
                    {
                        System.out.printf("No Biome! %d %d %d, %d %d %d%n", pX, pY, pZ, sampleX, sampleY, sampleZ);
                    }

                    int cachedR = Color.RGBAGetR(color);
                    int cachedG = Color.RGBAGetG(color);
                    int cachedB = Color.RGBAGetB(color);

                    Color.sRGBByteToOKLabs(cachedR, cachedG, cachedB, blendBuffer.colors, indexX);

                    indexX += 3;
                }

                indexZ += 3 * blendDim;
            }

            indexY += 3 * blendDim * blendDim;
        }
        */
    }

    public static void
    generateColors(
        BiomeManager   biomeManager,
        ColorResolver  resolver,
        int            x,
        int            y,
        int            z,
        int[]          result)
    {
        /*
        DebugEvent debugEvent = Debug.pushColorGenEvent(x, y, z, 0);

        final int blendRadius = BetterBiomeBlendClient.getBiomeBlendRadius();

        if (blendRadius >  BlendConfig.BIOME_BLEND_RADIUS_MIN &&
            blendRadius <= BlendConfig.BIOME_BLEND_RADIUS_MAX)
        {
            BlendBuffer blendBuffer = ColorBlending.acquireBlendBuffer(blendRadius);

            gatherColors(
                biomeManager,
                resolver,
                blendBuffer,
                x,
                y,
                z,
                blendRadius);

            DebugEvent subEvent = Debug.pushSubevent(DebugEventType.SUBEVENT);

            ColorBlending.blendColorsForChunk(blendRadius, blendBuffer, result, x, y, z);

            Debug.endEvent(subEvent);

            ColorBlending.releaseBlendBuffer(blendBuffer);
        }
        else
        {
            /*
            gatherColorsDirectly(
                resolver,
                x,
                y,
                z,
                result);
            */
        /*
        }

        Debug.endEvent(debugEvent);
        */
    }
}
