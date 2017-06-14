package com.wynprice.entityHoppers;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.wynprice.entityHoppers.hopper.TileEntityEntityHopper;
import com.wynprice.entityHoppers.proxys.Common;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = References.MODID , name = References.NAME , version =References.VERSION)
public class MainRegistry
{
	
	@SidedProxy(clientSide = References.CLIENT_PROXY, serverSide = References.SERVER_PROXY)
	public static Common proxy;
		
	@Mod.Instance(References.MODID)
	public static MainRegistry instance;
	
	public static final CreativeTabs Creative_Tab= new CreativeTab();

	
	@EventHandler
	public static void PreInit(FMLPreInitializationEvent e)
	{
		getlogger().info("Summoning the dark lord to do my bidding");
		proxy.PreInit(e);
		RegisterHopper.Init();
		RegisterHopper.reg();
		
	}
	
	@EventHandler
	public static void Init(FMLInitializationEvent e)
	{
		proxy.Init(e);
		GameRegistry.addShapedRecipe(new ItemStack(RegisterHopper.Entity_Hopper),
				"oeo",
				"ehe",
				" o ",
				'o', Blocks.OBSIDIAN,
				'e', Items.ENDER_EYE,
				'h', Blocks.HOPPER);
	}
	
	@EventHandler
	public static void PostInit(FMLPostInitializationEvent e)
	{
		proxy.PostInit(e);
		GameRegistry.registerTileEntity(TileEntityEntityHopper.class, References.MODID + "TileEntityEntityHopper");
	}
	
	
	private static Logger logger; 
	public static Logger getlogger()
	{
		if(logger == null)
		{
			logger = LogManager.getFormatterLogger(References.MODID);
		}
		return logger;
	}
}
 