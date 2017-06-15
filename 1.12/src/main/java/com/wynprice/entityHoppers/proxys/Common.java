package com.wynprice.entityHoppers.proxys;

import com.wynprice.entityHoppers.UpdateChecker;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class Common {

	public void PreInit(FMLPreInitializationEvent e) 
	{
		// TODO Auto-generated method stub
		
	}

	public void Init(FMLInitializationEvent e) 
	{
		UpdateChecker handler = new UpdateChecker();
		MinecraftForge.EVENT_BUS.register(handler);
		FMLCommonHandler.instance().bus().register(handler);
	}

	public void PostInit(FMLPostInitializationEvent e) 
	{
		// TODO Auto-generated method stub
		
	}

}
