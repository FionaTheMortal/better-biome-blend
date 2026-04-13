package fionathemortal.betterbiomeblend.fabric.compat.sodium.mixin;

import fionathemortal.betterbiomeblend.common.ColorConfig;
import fionathemortal.betterbiomeblend.common.cache.Slice;
import fionathemortal.betterbiomeblend.common.util.Array3i;
import fionathemortal.betterbiomeblend.common.util.Util;
import fionathemortal.betterbiomeblend.fabric.compat.sodium.SodiumColorGen;
import fionathemortal.betterbiomeblend.fabric.compat.sodium.SodiumConfig;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.world.biome.BiomeColorCache;
import me.jellysquid.mods.sodium.client.world.biome.BiomeSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BiomeColorCache.class)
public class MixinBiomeColorCache
{
    private static final int MARGIN     = 2;
    private static final int SLICE_SIZE = Util.CHUNK_SIZE + 2 * MARGIN;

    @Shadow(remap = false)
    private BiomeSlice biomeData;

    @Unique
    private int bbb$baseX;

    @Unique
    private int bbb$baseY;

    @Unique
    private int bbb$baseZ;

    @Unique
    private Reference2ReferenceOpenHashMap<ColorResolver, int[]> bbb$colors;

    @Inject(
        method = "<init>",
        at = @At("TAIL"),
        remap = false)
    private void
    bbb$onInit(CallbackInfo ci)
    {
        bbb$colors = new Reference2ReferenceOpenHashMap<>(3);
    }

    @Inject(
        method = "update",
        at = @At("TAIL"),
        remap = false)
    private void
    bbb$onUpdate(ChunkRenderContext context, CallbackInfo ci)
    {
        this.bbb$baseX = context.getOrigin().minBlockX();
        this.bbb$baseY = context.getOrigin().minBlockY();
        this.bbb$baseZ = context.getOrigin().minBlockZ();

        for (int[] colors : bbb$colors.values())
        {
            bbb$invalidate(colors);
        }
    }

    @Overwrite(remap = false)
    public int
    getColor(ColorResolver resolver, int blockX, int blockY, int blockZ)
    {
        int minX = bbb$baseX - MARGIN;
        int minY = bbb$baseY - MARGIN;
        int minZ = bbb$baseZ - MARGIN;

        int maxX = bbb$baseX + Util.CHUNK_SIZE + MARGIN;
        int maxY = bbb$baseY + Util.CHUNK_SIZE + MARGIN;
        int maxZ = bbb$baseZ + Util.CHUNK_SIZE + MARGIN;

        int clampedX = Mth.clamp(blockX, minX, maxX - 1);
        int clampedY = Mth.clamp(blockY, minY, maxY - 1);
        int clampedZ = Mth.clamp(blockZ, minZ, maxZ - 1);

        int relX = clampedX - minX;
        int relY = clampedY - minY;
        int relZ = clampedZ - minZ;

        int[] colors = bbb$colors.get(resolver);

        if (colors == null)
        {
            colors = new int[SLICE_SIZE * SLICE_SIZE * SLICE_SIZE];

            bbb$colors.put(resolver, colors);
        }

        if (!bbb$isValid(colors))
        {
            ColorConfig config = SodiumConfig.getCurrentConfig();

            SodiumColorGen.genColors(
                biomeData,
                resolver,
                config,
                minX,
                minY,
                minZ,
                SLICE_SIZE,
                SLICE_SIZE,
                SLICE_SIZE,
                colors,
                0,
                0,
                0,
                SLICE_SIZE,
                SLICE_SIZE);
        }

        int index = Array3i.getArrayIndex(SLICE_SIZE, SLICE_SIZE, relX, relY, relZ);

        int result = colors[index];

        return result;
    }

    @Unique
    private static boolean
    bbb$isValid(int[] colors)
    {
        boolean result = (colors[0] != Slice.INVALID_COLOR);

        return result;
    }

    @Unique
    private static void
    bbb$invalidate(int[] colors)
    {
        colors[0] = Slice.INVALID_COLOR;
    }
}
