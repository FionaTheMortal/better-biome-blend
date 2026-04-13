package fionathemortal.betterbiomeblend;

public class BetterBiomeBlend
{
    public static final String MOD_ID = "betterbiomeblend";

    public static boolean
    isRunningSodium()
    {
        try
        {
            Class.forName(
                "me.jellysquid.mods.sodium.client.world.biome.BlockColorCache",
                false,
                Thread.currentThread().getContextClassLoader());

            return true;
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }
}
