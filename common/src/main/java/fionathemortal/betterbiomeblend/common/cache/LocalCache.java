package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.ColorType;
import fionathemortal.betterbiomeblend.common.ColorSource;
import fionathemortal.betterbiomeblend.common.compat.ColorResolverCompat;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.ColorResolver;

import java.util.Arrays;

public final class LocalCache
{
    public static final Slice SENTINEL = new Slice(null, 0, 0, true);

    public ColorResolver lastColorResolver = null;
    public int           lastColorType     = 0;
    public Slice[]       slices            = new Slice[3];

    public
    LocalCache()
    {
        Arrays.fill(slices, SENTINEL);
    }

    public int
    getColorType(ColorResolver colorResolver)
    {
        int result;

        if (colorResolver == lastColorResolver)
        {
            result = lastColorType;
        }
        else if (colorResolver == BiomeColors.GRASS_COLOR_RESOLVER)
        {
            result = ColorType.GRASS;
        }
        else if (colorResolver == BiomeColors.WATER_COLOR_RESOLVER)
        {
            result = ColorType.WATER;
        }
        else if (colorResolver == BiomeColors.FOLIAGE_COLOR_RESOLVER)
        {
            result = ColorType.FOLIAGE;
        }
        else
        {
            result = ColorResolverCompat.getColorType(colorResolver);

            ensureColorTypeSupport(result);
        }

        return result;
    }

    public Slice
    getSlice(int sliceX, int sliceY, int sliceZ, int colorType)
    {
        Slice result = null;

        long key = SliceCache.getKey(sliceX, sliceY, sliceZ, colorType);

        Slice localSlice = slices[colorType];

        if (localSlice.key == key)
        {
            result = localSlice;
        }

        return result;
    }

    public void
    putSlice(ColorSource source, Slice slice, int colorType, ColorResolver colorResolver)
    {
        Slice prevSlice = this.slices[colorType];

        source.releaseSlice(prevSlice);

        ensureColorTypeSupport(colorType);

        this.slices[colorType] = slice;
        this.lastColorResolver = colorResolver;
        this.lastColorType     = colorType;
    }

    private void
    ensureColorTypeSupport(int colorType)
    {
        if (colorType >= slices.length)
        {
            int oldLength = slices.length;

            slices = Arrays.copyOf(this.slices, colorType + 1);

            Arrays.fill(slices, oldLength, slices.length, SENTINEL);
        }
    }
}
