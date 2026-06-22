package fionathemortal.betterbiomeblend;

import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class SereneSeasonsCompat
{
    private static final String MOD_ID = "sereneseasons";

    private static boolean checked;
    private static boolean available;
    private static Method  getClientSeasonTime;
    private static Method  getSubSeason;
    private static Object  lastSubSeason;

    private
    SereneSeasonsCompat()
    {
    }

    public static void
    reset()
    {
        lastSubSeason = null;
    }

    public static boolean
    hasSubSeasonChanged()
    {
        boolean result = false;
        Object subSeason = getSubSeason();

        if (subSeason != null)
        {
            result = lastSubSeason != null && lastSubSeason != subSeason;

            lastSubSeason = subSeason;
        }

        return result;
    }

    private static Object
    getSubSeason()
    {
        Object result = null;

        if (!checked)
        {
            findHooks();
        }

        if (available)
        {
            try
            {
                Object seasonTime = getClientSeasonTime.invoke(null);

                if (getSubSeason == null)
                {
                    getSubSeason = seasonTime.getClass().getMethod("getSubSeason");
                }

                result = getSubSeason.invoke(seasonTime);
            }
            catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
            {
                BetterBiomeBlend.LOGGER.warn("Failed to read Serene Seasons state. Disabling Serene Seasons color cache compatibility.", e);
                available = false;
            }
        }

        return result;
    }

    private static void
    findHooks()
    {
        checked = true;

        if (Loader.isModLoaded(MOD_ID))
        {
            try
            {
                Class<?> seasonHandlerClass = Class.forName("sereneseasons.handler.season.SeasonHandler");
                getClientSeasonTime = seasonHandlerClass.getMethod("getClientSeasonTime");
                available = true;
            }
            catch (ClassNotFoundException | NoSuchMethodException e)
            {
                BetterBiomeBlend.LOGGER.warn("Serene Seasons is loaded, but Better Biome Blend could not find its season state hooks.", e);
                available = false;
            }
        }
        else
        {
            available = false;
        }
    }
}
