package fionathemortal.betterbiomeblend.fabric;

import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class BetterBiomeBlendFabric implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                BetterBiomeBlendClient.registerCommands(dispatcher);
        });
    }
}
