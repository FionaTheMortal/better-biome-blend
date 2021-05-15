package fionathemortal.betterbiomeblend;

import java.lang.reflect.Field;
import java.util.List;

import fionathemortal.betterbiomeblend.mixin.AccessorDoubleOptionSliderWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonListWidget;
import net.minecraft.client.options.DoubleOption;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.Option;
import net.minecraft.text.TranslatableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;

public final class BetterBiomeBlendClient
{
	public static final int BIOME_BLEND_RADIUS_MAX = 14;
	public static final int BIOME_BLEND_RADIUS_MIN = 0;

	public static final DoubleOption BIOME_BLEND_RADIUS = new DoubleOption(
		"options.biomeBlendRadius", 
		BIOME_BLEND_RADIUS_MIN,
		BIOME_BLEND_RADIUS_MAX,
		1.0F,
		BetterBiomeBlendClient::biomeBlendRadiusOptionGetValue, 
		BetterBiomeBlendClient::biomeBlendRadiusOptionSetValue,
		BetterBiomeBlendClient::biomeBlendRadiusOptionGetDisplayText);

	public static GameOptions gameOptions;
	
	public static final RegistryKey<Biome> PLAINS = 
		RegistryKey.of(Registry.BIOME_KEY, new Identifier("minecraft", "plains"));
	
	public static void
	postInitGUIEvent(MinecraftClient client, Screen screen, int scaledWidth, int scaledHeight)
	{
		if (screen instanceof VideoOptionsScreen)
		{
			VideoOptionsScreen optionsScreen = (VideoOptionsScreen)screen;
			
			replaceBiomeBlendRadiusOption(optionsScreen);
		}
	}

	@SuppressWarnings("resource")
	public static void
	replaceBiomeBlendRadiusOption(Screen screen)
	{
		List<? extends Element> children = screen.children();
		
		for (Element child : children)
		{
			if (child instanceof ButtonListWidget)
			{
				ButtonListWidget rowList = (ButtonListWidget)child;
				
				List<ButtonListWidget.ButtonEntry> rowListEntries = rowList.children();
				
				boolean replacedOption = false;
				
				for (int index = 0;
					index < rowListEntries.size();
					++index)
				{
					ButtonListWidget.ButtonEntry row = rowListEntries.get(index);
					
					List<? extends Element> rowChildren = row.children();
					
					for (Element rowChild : rowChildren)
					{
						if (rowChild instanceof AccessorDoubleOptionSliderWidget)
						{
							AccessorDoubleOptionSliderWidget accessor = (AccessorDoubleOptionSliderWidget)rowChild;
							
							if (accessor.getOption() == Option.BIOME_BLEND_RADIUS)
							{
								ButtonListWidget.ButtonEntry newRow = ButtonListWidget.ButtonEntry.create(
									MinecraftClient.getInstance().options, 
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
	biomeBlendRadiusOptionGetValue(GameOptions settings)
	{
		double result = (double)settings.biomeBlendRadius;
		
		return result;
	}
	
	@SuppressWarnings("resource")
	public static void
	biomeBlendRadiusOptionSetValue(GameOptions settings, Double optionValues)
	{
		int currentValue = (int)optionValues.doubleValue();
		int newSetting   = MathHelper.clamp(currentValue, BIOME_BLEND_RADIUS_MIN, BIOME_BLEND_RADIUS_MAX);
		
		if (settings.biomeBlendRadius != newSetting)
		{
			settings.biomeBlendRadius = newSetting;
			
			BiomeColor.clearBlendCaches();
			
			MinecraftClient.getInstance().worldRenderer.reload();
		}
	}
	
	public static Text
	biomeBlendRadiusOptionGetDisplayText(GameOptions settings, DoubleOption optionValues)
	{
		int currentValue  = (int)optionValues.get(settings);
		int blendDiameter = 2 * currentValue + 1;
		
		Text result = new TranslatableText(
			"options.generic_value",
			new TranslatableText("options.biomeBlendRadius"), 
			new TranslatableText("options.biomeBlendRadius." + blendDiameter));
		
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

				Option[] enumOptions = (Option[])enumOptionsField.get(null);
				
				boolean found = false;
				
				for (int index = 0;
					index < enumOptions.length;
					++index)
				{
					Option option = enumOptions[index];
					
					if (option == Option.BIOME_BLEND_RADIUS)
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
					// BetterBiomeBlend.LOGGER.warn("Optifine GUI option was not found.");
				}
			}
			catch (Exception e) 
			{
				// BetterBiomeBlend.LOGGER.warn(e);
			}
		} 
		catch (ClassNotFoundException e) 
		{
		}
		
		if (success)
		{
			// BetterBiomeBlend.LOGGER.info("Optifine GUI option was successfully replaced.");
		}
	}

	@SuppressWarnings("resource")
	public static int
	getBlendRadiusSetting()
	{
		int result = 0;
		
		if (gameOptions == null)
		{
			gameOptions = MinecraftClient.getInstance().options;
		}
		
		if (gameOptions != null)
		{
			result = gameOptions.biomeBlendRadius;
		}
		
		return result;
	}
}