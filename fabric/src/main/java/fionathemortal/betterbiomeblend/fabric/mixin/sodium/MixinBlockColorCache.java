package fionathemortal.betterbiomeblend.fabric.mixin.sodium;

import fionathemortal.betterbiomeblend.common.ColorType;
import fionathemortal.betterbiomeblend.common.ColorGen;
import fionathemortal.betterbiomeblend.common.cache.SliceCache;
import fionathemortal.betterbiomeblend.common.util.Array3i;
import fionathemortal.betterbiomeblend.fabric.compat.sodium.SourceCacheProvider;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.biome.BlockColorCache;

import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockColorCache.class)
public class MixinBlockColorCache
{
    @Unique
    private int bbb$baseX;

    @Unique
    private int bbb$baseY;

    @Unique
    private int bbb$baseZ;

    @Unique
    private Level bbb$world;

    @Unique
    private Reference2ReferenceOpenHashMap<ColorResolver, int[]> bbb$colors;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    public void
    constructorTail(WorldSlice slice, int radius, CallbackInfo ci)
    {
        bbb$world = ((WorldSliceAccessor)slice).getWorld();

        SectionPos pos = slice.getOrigin();

        this.bbb$baseX = pos.minBlockX();
        this.bbb$baseY = pos.minBlockY();
        this.bbb$baseZ = pos.minBlockZ();

        this.bbb$colors = new Reference2ReferenceOpenHashMap<>();
    }

    @Unique
    private SliceCache
    bbb$getSourceCache()
    {
        return ((SourceCacheProvider)this.bbb$world).bbb$sodium$getSourceCache();
    }

    @Overwrite(remap = false)
    public int
    getColor(ColorResolver resolver, int posX, int posY, int posZ)
    {
        int[] colors = this.bbb$colors.get(resolver);

        int minWBorderX = bbb$baseX - 1;
        int minWBorderY = bbb$baseY - 1;
        int minWBorderZ = bbb$baseZ - 1;

        if (colors == null)
        {
            colors = new int[18 * 18 * 18];

            SliceCache cache = bbb$getSourceCache();

            int colorType = ColorType.getColorTypeFromResolver(resolver);

            ColorGen.genColors(
                bbb$world,
                resolver,
                colorType,
                cache.colorConfig,
                cache,
                minWBorderX,
                minWBorderY,
                minWBorderZ,
                18,
                18,
                18,
                colors,
                0,
                0,
                0,
                18,
                18);

            bbb$colors.put(resolver, colors);
        }

        int x = Mth.clamp(posX - minWBorderX, 0, 17);
        int y = Mth.clamp(posY - minWBorderY, 0, 17);
        int z = Mth.clamp(posZ - minWBorderZ, 0, 17);

        int index = Array3i.getArrayIndex(18, 18, x, y, z);

        int result = colors[index];

        return result;
    }
}
