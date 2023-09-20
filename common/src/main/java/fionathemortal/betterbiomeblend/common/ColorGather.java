package fionathemortal.betterbiomeblend.common;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

public final class ColorGather
{
    public static boolean
    regionIsLoaded(
        Level world,
        int   minX,
        int   minZ,
        int   maxX,
        int   maxZ)
    {
        boolean result = true;

        int chunkMinX = minX >> 4;
        int chunkMinZ = minZ >> 4;
        int chunkMaxX = maxX >> 4;
        int chunkMaxZ = maxZ >> 4;

        for (int chunkZ = chunkMinZ;
            chunkZ < chunkMaxZ;
            ++chunkZ)
        {
            for (int chunkX = chunkMinX;
                 chunkX < chunkMaxX;
                 ++chunkX)
            {
                ChunkAccess chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.BIOMES, false);

                if (chunk == null)
                {
                    result = false;
                    break;
                }
            }
        }

        return result;
    }

    static void
    gatherColorsForBlendBuffer()
    {

    }

    static void
    gatherColorsForBlending(
        BlendBuffer blendBuffer,
        int         minX,
        int         minY,
        int         minZ,
        int         maxX,
        int         maxY,
        int         maxZ)
    {
        final int blendRadius    = 0;
        final int blockSizeLog2  = 1;
        final int sliceSizeLog2  = 1;
        final int blockSize      = 1 << blockSizeLog2;
        final int blocksPerSlice = 1 << (sliceSizeLog2 - blockSizeLog2);
        final int blockOffset    = 0;

        int blendRegionMinX = minX - blendRadius;
        int blendRegionMinY = minY - blendRadius;
        int blendRegionMinZ = minZ - blendRadius;

        int blendRegionMaxX = maxX + blendRadius;
        int blendRegionMaxY = maxY + blendRadius;
        int blendRegionMaxZ = maxZ + blendRadius;

        boolean regionIsLoaded = regionIsLoaded(world, minX, minZ, maxX, maxZ);

        if (regionIsLoaded)
        {
            int blockMinX = (blendRegionMinX - blockOffset) >> blockSizeLog2;
            int blockMinY = (blendRegionMinY - blockOffset) >> blockSizeLog2;
            int blockMinZ = (blendRegionMinZ - blockOffset) >> blockSizeLog2;

            int blockMaxX =  (blendRegionMaxX - blockOffset) >> blockSizeLog2;
            int blockMaxY =  (blendRegionMaxY - blockOffset) >> blockSizeLog2;
            int blockMaxZ =  (blendRegionMaxZ - blockOffset) >> blockSizeLog2;

            int sliceMinX = blockMinX >> (sliceSizeLog2 - blockSizeLog2);
            int sliceMinY = blockMinY >> (sliceSizeLog2 - blockSizeLog2);
            int sliceMinZ = blockMinZ >> (sliceSizeLog2 - blockSizeLog2);

            int sliceMaxX = blockMaxX >> (sliceSizeLog2 - blockSizeLog2);
            int sliceMaxY = blockMaxY >> (sliceSizeLog2 - blockSizeLog2);
            int sliceMaxZ = blockMaxZ >> (sliceSizeLog2 - blockSizeLog2);

            for (int sliceX = sliceMax;
                 sliceX <= sliceMax;
                 ++sliceX)
            {
                // get slice

                int sliceBlockMin = sliceX << (sliceSizeLog2 = blockSizeLog2);
                int sliceBlockMax = sliceBlockMin + sliceSizeInBlocks;

                int gatherMinX = Math.max(sliceBlockMin, blockMinX);
                int gatherMaxX = Math.min(sliceBlockMax, blockMaxX);
            }
        }



        // we now have a buffer with all the colors required
    }
}
