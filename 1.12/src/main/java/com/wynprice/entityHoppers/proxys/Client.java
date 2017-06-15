package com.wynprice.entityHoppers.proxys;

import com.wynprice.entityHoppers.MainRegistry;
import com.wynprice.entityHoppers.RegisterHopper;
import com.wynprice.entityHoppers.hopper.BlockEntityHopper;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class Client extends Common
{
	public void PreInit(FMLPreInitializationEvent e)
	{
		super.PreInit(e);
	}
	
	public void Init(FMLInitializationEvent e)
	{
		super.Init(e);
		RegisterHopper.regRenders();
	}
	
	
	public void PostInit(FMLPostInitializationEvent e)
	{
		super.PostInit(e);
	}

}
