package com.wynprice.entityHoppers;

import com.wynprice.entityHoppers.proxys.Client;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class CreativeTab extends CreativeTabs
{
	public CreativeTab()
	{
		super("tabEntityHopper");
	}

	@Override
	public ItemStack getTabIconItem()
	{
		return new ItemStack(RegisterHopper.Entity_Hopper);
	}

}
