package fionathemortal.betterbiomeblend;

import net.minecraft.client.Minecraft;

public final class BetterBiomeBlendClient
{
    public static int
    getBiomeBlendRadius()
    {
        int result = Minecraft.getInstance().options.biomeBlendRadius().get();

        return result;
    }
}
