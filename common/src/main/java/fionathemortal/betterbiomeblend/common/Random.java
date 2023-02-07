package fionathemortal.betterbiomeblend.common;

public class Random
{
    /* NOTE:
     * This is a very simple and dumb noise function. Sort of based on some LCG numbers from wikipedia.
     * Should be good enough for looking random on a small enough scale.
     */
    public static int
    noise(int input, int seed)
    {
        int seededValue = (input ^ seed) + seed;
        int lcgResult = 214013 * seededValue + 2531011;
        int result = lcgResult >> 8;

        return result;
    }
}
