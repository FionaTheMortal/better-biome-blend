package fionathemortal.betterbiomeblend;

import java.lang.reflect.Field;
import java.util.List;

public final class BetterBiomeBlendClient
{
	public static final int BIOME_BLEND_RADIUS_MAX = 14;
	public static final int BIOME_BLEND_RADIUS_MIN = 0;
	
	/*
	public static final SliderPercentageOption BIOME_BLEND_RADIUS = new SliderPercentageOption(
		"options.biomeBlendRadius", 
		BIOME_BLEND_RADIUS_MIN, 
		BIOME_BLEND_RADIUS_MAX, 
		1.0F,
		BetterBiomeBlendClient::biomeBlendRadiusOptionGetValue, 
		BetterBiomeBlendClient::biomeBlendRadiusOptionSetValue,
		BetterBiomeBlendClient::biomeBlendRadiusOptionGetDisplayText);

	public static final GameSettings gameSettings = Minecraft.getInstance().gameSettings;

	public static final RegistryObject<Biome> PLAINS = RegistryObject.of(
		new ResourceLocation("minecraft:plains"), ForgeRegistries.BIOMES);
	
	@SubscribeEvent
	public static void
	postInitGUIEvent(InitGuiEvent.Post event)
	{
		Screen screen = event.getGui();
		
		if (screen instanceof VideoSettingsScreen)
		{
			VideoSettingsScreen videoSettingsScreen = (VideoSettingsScreen)screen;
			
			replaceBiomeBlendRadiusOption(videoSettingsScreen);
		}
	}

	public static int
	getBlendRadiusSetting()
	{
		int result = gameSettings.biomeBlendRadius;
		
		return result;
	}
	
	@SuppressWarnings("resource")
	public static void
	replaceBiomeBlendRadiusOption(VideoSettingsScreen screen)
	{
		List<? extends IGuiEventListener> children = screen.getEventListeners();
		
		for (IGuiEventListener child : children)
		{
			if (child instanceof OptionsRowList)
			{
				OptionsRowList rowList = (OptionsRowList)child;
				
				List<OptionsRowList.Row> rowListEntries = rowList.getEventListeners();
				
				boolean replacedOption = false;
				
				for (int index = 0;
					index < rowListEntries.size();
					++index)
				{
					OptionsRowList.Row row = rowListEntries.get(index);
					
					List<? extends IGuiEventListener> rowChildren = row.getEventListeners();
					
					for (IGuiEventListener rowChild : rowChildren)
					{
						if (rowChild instanceof AccessorOptionSlider)
						{
							AccessorOptionSlider accessor = (AccessorOptionSlider)rowChild;
							
							if (accessor.getOption() == AbstractOption.BIOME_BLEND_RADIUS)
							{
								OptionsRowList.Row newRow = OptionsRowList.Row.create(
									screen.getMinecraft().gameSettings, 
									screen.width, 
									BIOME_BLEND_RADIUS);
								
								rowListEntries.set(index, newRow);
								
								replacedOption = true;
							}
						}
					}
					
					if (replacedOption)
					{
						break;
					}
				}
			}
		}
	}
	
	public static Double
	biomeBlendRadiusOptionGetValue(GameSettings settings)
	{
		double result = (double)settings.biomeBlendRadius;
		
		return result;
	}
	
	@SuppressWarnings("resource")
	public static void
	biomeBlendRadiusOptionSetValue(GameSettings settings, Double optionValues)
	{
		// NOTE: Concurrent modification exception with structure generation
		// But this code is a 1 to 1 copy of vanilla code so it might just be an unlikely bug on their end
		
		int currentValue = (int)optionValues.doubleValue();
		int newSetting   = MathHelper.clamp(currentValue, BIOME_BLEND_RADIUS_MIN, BIOME_BLEND_RADIUS_MAX);
		
		if (settings.biomeBlendRadius != newSetting)
		{
			settings.biomeBlendRadius = newSetting;
			
			BiomeColor.clearBlendCaches();
			
			Minecraft.getInstance().worldRenderer.loadRenderers();
		}
	}
	
	public static ITextComponent
	biomeBlendRadiusOptionGetDisplayText(GameSettings settings, SliderPercentageOption optionValues)
	{
		int currentValue  = (int)optionValues.get(settings);
		int blendDiameter = 2 * currentValue + 1;
		
		ITextComponent result = new TranslationTextComponent(
			"options.generic_value",
			new TranslationTextComponent("options.biomeBlendRadius"), 
			new TranslationTextComponent("options.biomeBlendRadius." + blendDiameter));
		
		return result;
	}
	
	public static void
	overwriteOptifineGUIBlendRadiusOption()
	{
		boolean success = false;

		try
		{
			Class<?> guiDetailSettingsOFClass = Class.forName("net.optifine.gui.GuiDetailSettingsOF");
			
			try 
			{
				Field enumOptionsField = guiDetailSettingsOFClass.getDeclaredField("enumOptions");
				
				enumOptionsField.setAccessible(true);
			
				AbstractOption[] enumOptions = (AbstractOption[])enumOptionsField.get(null);
				
				boolean found = false;
				
				for (int index = 0;
					index < enumOptions.length;
					++index)
				{
					AbstractOption option = enumOptions[index];
					
					if (option == AbstractOption.BIOME_BLEND_RADIUS)
					{
						enumOptions[index] = BIOME_BLEND_RADIUS;
						
						found = true;
						
						break;
					}
				}
				
				if (found)
				{
					success = true;
				}
				else
				{
					BetterBiomeBlend.LOGGER.warn("Optifine GUI option was not found.");
				}
			}
			catch (Exception e) 
			{
				BetterBiomeBlend.LOGGER.warn(e);
			}
		} 
		catch (ClassNotFoundException e) 
		{
		}
		
		if (success)
		{
			BetterBiomeBlend.LOGGER.info("Optifine GUI option was successfully replaced.");
		}
	}
	*/
}