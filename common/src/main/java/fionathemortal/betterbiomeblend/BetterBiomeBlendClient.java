package fionathemortal.betterbiomeblend;

import net.minecraft.client.Minecraft;

public final class BetterBiomeBlendClient
{
    public static int
    getBlendRadiusSetting()
    {
        return ((BetterBiomeBlendOptions) Minecraft.getInstance().options).betterBiomeBlendRadius().get();
    }

    public static void
    setBlendRadiusSetting(int blendRadius)
    {
        ((BetterBiomeBlendOptions) Minecraft.getInstance().options).betterBiomeBlendRadius().set(blendRadius);
    }
}
