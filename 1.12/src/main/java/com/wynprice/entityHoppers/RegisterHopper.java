package com.wynprice.entityHoppers;

import com.wynprice.entityHoppers.hopper.BlockEntityHopper;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class RegisterHopper 
{
	public static Block Entity_Hopper;
	
	
	public static void Init()
	{
		Entity_Hopper = new BlockEntityHopper();
	}
	
	public static void reg()
	{
		regBlock(Entity_Hopper,64);

	}
	
	private static void regBlock(Block block, int StackSize)
	{
		GameRegistry.register(block);
		ItemBlock item = new ItemBlock(block);
		item.setRegistryName(block.getRegistryName());
		item.setMaxStackSize(StackSize);
		GameRegistry.register(item);
	}
	
	public static void regRenders()
	{
		regRender(Entity_Hopper);
	}
	
	private static void regRender(Block block)
	{
		block.setCreativeTab(MainRegistry.Creative_Tab);
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(Item.getItemFromBlock(block), 0, new ModelResourceLocation(block.getRegistryName(), "inventory"));
	}
}
