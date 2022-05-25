package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.common.*;
import fionathemortal.betterbiomeblend.sodium.SodiumColorBlending;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.biome.BlockColorCache;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    private Reference2ReferenceOpenHashMap<ColorResolver, byte[]> betterbiomeblend$colors;

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

        this.betterbiomeblend$baseX = pos.minBlockX();
        this.betterbiomeblend$baseY = pos.minBlockY();
        this.betterbiomeblend$baseZ = pos.minBlockZ();

        this.betterbiomeblend$colors = new Reference2ReferenceOpenHashMap<>();
    }

    @Overwrite(remap = false)
    public int
    getColor(ColorResolver resolver, int posX, int posY, int posZ)
    {
        byte[] colors = this.betterbiomeblend$colors.get(resolver);

        if (colors == null)
        {
            colors = new byte[3 * 5 * 5 * 5];

            BiomeManager biomeManager = slice.getBiomeAccess();

            SodiumColorBlending.generateBlendedColorChunk(
                biomeManager,
                resolver,
                this.betterbiomeblend$baseX >> 4,
                this.betterbiomeblend$baseY >> 4,
                this.betterbiomeblend$baseZ >> 4,
                colors);

            this.betterbiomeblend$colors.put(resolver, colors);
        }

        return betterbiomeblend$getColor(colors, posX, posY, posZ);
    }

    private int
    betterbiomeblend$getColor(byte[] colors, int x, int y, int z)
    {
        int blockX = Mth.clamp(x - this.betterbiomeblend$baseX, 0, 15);
        int blockY = Mth.clamp(y - this.betterbiomeblend$baseY, 0, 15);
        int blockZ = Mth.clamp(z - this.betterbiomeblend$baseZ, 0, 15);

        int sectionX = blockX >> 2;
        int sectionY = blockY >> 2;
        int sectionZ = blockZ >> 2;

        int offsetX = blockX & 3;
        int offsetY = blockY & 3;
        int offsetZ = blockZ & 3;

        int weight0 = (4 - offsetX) * (4 - offsetZ) * (4 - offsetY);
        int weight1 = (    offsetX) * (4 - offsetZ) * (4 - offsetY);
        int weight2 = (4 - offsetX) * (    offsetZ) * (4 - offsetY);
        int weight3 = (    offsetX) * (    offsetZ) * (4 - offsetY);
        int weight4 = (4 - offsetX) * (4 - offsetZ) * (    offsetY);
        int weight5 = (    offsetX) * (4 - offsetZ) * (    offsetY);
        int weight6 = (4 - offsetX) * (    offsetZ) * (    offsetY);
        int weight7 = (    offsetX) * (    offsetZ) * (    offsetY);

        int index0 = 3 * ColorBlending.getCacheArrayIndex(5, sectionX, sectionY, sectionZ);
        int index1 = index0 +  3;
        int index2 = index0 + 15;
        int index3 = index0 + 18;
        int index4 = index0 + 75;
        int index5 = index0 + 78;
        int index6 = index0 + 90;
        int index7 = index0 + 93;

        long packed0 = ((long)(0xFF & colors[index0 + 0])) | ((long)(0xFF & colors[index0 + 1]) << 24) | ((long)(0xFF & colors[index0 + 2]) << 48);
        long packed1 = ((long)(0xFF & colors[index1 + 0])) | ((long)(0xFF & colors[index1 + 1]) << 24) | ((long)(0xFF & colors[index1 + 2]) << 48);
        long packed2 = ((long)(0xFF & colors[index2 + 0])) | ((long)(0xFF & colors[index2 + 1]) << 24) | ((long)(0xFF & colors[index2 + 2]) << 48);
        long packed3 = ((long)(0xFF & colors[index3 + 0])) | ((long)(0xFF & colors[index3 + 1]) << 24) | ((long)(0xFF & colors[index3 + 2]) << 48);
        long packed4 = ((long)(0xFF & colors[index4 + 0])) | ((long)(0xFF & colors[index4 + 1]) << 24) | ((long)(0xFF & colors[index4 + 2]) << 48);
        long packed5 = ((long)(0xFF & colors[index5 + 0])) | ((long)(0xFF & colors[index5 + 1]) << 24) | ((long)(0xFF & colors[index5 + 2]) << 48);
        long packed6 = ((long)(0xFF & colors[index6 + 0])) | ((long)(0xFF & colors[index6 + 1]) << 24) | ((long)(0xFF & colors[index6 + 2]) << 48);
        long packed7 = ((long)(0xFF & colors[index7 + 0])) | ((long)(0xFF & colors[index7 + 1]) << 24) | ((long)(0xFF & colors[index7 + 2]) << 48);

        long v0 = packed0 * weight0;
        long v1 = packed1 * weight1;
        long v2 = packed2 * weight2;
        long v3 = packed3 * weight3;
        long v4 = packed4 * weight4;
        long v5 = packed5 * weight5;
        long v6 = packed6 * weight6;
        long v7 = packed7 * weight7;

        long packedResult = (v0 + v1 + v2 + v3 + v4 + v5 + v6 + v7) / 64;

        int r = (int)(packedResult      ) & 0xFF;
        int g = (int)(packedResult >> 24) & 0xFF;
        int b = (int)(packedResult >> 48) & 0xFF;

        int result = Color.makeRGBAWithFullAlpha(r, g, b);

        return result;
    }
}
