package com.wynprice.entityHoppers.hopper;

import java.rmi.server.Skeleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerHopper;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.IHopper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.walkers.ItemStackDataLists;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class TileEntityEntityHopper extends TileEntityLockableLoot implements IHopper, ITickable
{
    private NonNullList<ItemStack> inventory = NonNullList.<ItemStack>withSize(5, ItemStack.EMPTY);
    private int transferCooldown = -1;
    private long tickedGameTime;

    public static void registerFixesHopper(DataFixer fixer)
    {
        fixer.registerWalker(FixTypes.BLOCK_ENTITY, new ItemStackDataLists(TileEntityEntityHopper.class, new String[] {"Items"}));
    }

    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.inventory = NonNullList.<ItemStack>withSize(this.getSizeInventory(), ItemStack.EMPTY);

        if (!this.checkLootAndRead(compound))
        {
            ItemStackHelper.loadAllItems(compound, this.inventory);
        }

        if (compound.hasKey("CustomName", 8))
        {
            this.customName = compound.getString("CustomName");
        }

        this.transferCooldown = compound.getInteger("TransferCooldown");
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);

        if (!this.checkLootAndWrite(compound))
        {
            ItemStackHelper.saveAllItems(compound, this.inventory);
        }

        compound.setInteger("TransferCooldown", this.transferCooldown);

        if (this.hasCustomName())
        {
            compound.setString("CustomName", this.customName);
        }

        return compound;
    }

    /**
     * Returns the number of slots in the inventory.
     */
    public int getSizeInventory()
    {
        return this.inventory.size();
    }

    /**
     * Removes up to a specified number of items from an inventory slot and returns them in a new stack.
     */
    public ItemStack decrStackSize(int index, int count)
    {
        this.fillWithLoot((EntityPlayer)null);
        ItemStack itemstack = ItemStackHelper.getAndSplit(this.getItems(), index, count);
        return itemstack;
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    public void setInventorySlotContents(int index, ItemStack stack)
    {
        this.fillWithLoot((EntityPlayer)null);
        this.getItems().set(index, stack);

        if (stack.getCount() > this.getInventoryStackLimit())
        {
            stack.setCount(this.getInventoryStackLimit());
        }
    }

    /**
     * Get the name of this object. For players this returns their username
     */
    public String getName()
    {
        return this.hasCustomName() ? this.customName : "container.entityhopper";
    }

    /**
     * Returns the maximum stack size for a inventory slot. Seems to always be 64, possibly will be extended.
     */
    public int getInventoryStackLimit()
    {
        return 64;
    }

    /**
     * Like the old updateEntity(), except more generic.
     */
    public void update()
    {
        if (this.world != null && !this.world.isRemote)
        {
            --this.transferCooldown;
            this.tickedGameTime = this.world.getTotalWorldTime();

            if (!this.isOnTransferCooldown())
            {
                this.setTransferCooldown(0);
                this.updateHopper();
            }
        }
    }

    private boolean updateHopper()
    {
        if (this.world != null && !this.world.isRemote)
        {
            if (!this.isOnTransferCooldown() && !world.isBlockPowered(pos))
            {
                boolean flag = false;

                if (!this.isInventoryEmpty())
                {
                    flag = this.transferItemsOut();
                }

                if (!this.isFull())
                {
                    flag = captureDroppedItems(this) || flag;
                }

                if (flag)
                {
                    this.setTransferCooldown(8);
                    this.markDirty();
                    return true;
                }
            }

            return false;
        }
        else
        {
            return false;
        }
    }

    private boolean isInventoryEmpty()
    {
        for (ItemStack itemstack : this.inventory)
        {
            if (!itemstack.isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    public boolean isEmpty()
    {
        return this.isInventoryEmpty();
    }

    private boolean isFull()
    {
        for (ItemStack itemstack : this.inventory)
        {
            if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize())
            {
                return false;
            }
        }

        return true;
    }

    private boolean transferItemsOut()
    {

        if (com.wynprice.entityHoppers.hopper.InventoryCodeHooks.insertHook(this)) { return true; }
        IInventory iinventory = this.getInventoryForHopperTransfer();

        if (iinventory == null && getEntityInventory() == null)
        {
        	return false;
        }
        else
        {
        	if(getEntityInventory() != null)
        		iinventory = getEntityInventory();
            EnumFacing enumfacing = BlockEntityHopper.getFacing(this.getBlockMetadata()).getOpposite();

            if (this.isInventoryFull(iinventory, enumfacing))
            {
                return false;
            }
            else
            {
                for (int i = 0; i < this.getSizeInventory(); ++i)
                {
                    if (!this.getStackInSlot(i).isEmpty())
                    {
                        ItemStack itemstack = this.getStackInSlot(i).copy();
                        ItemStack itemstack1 = putStackInInventoryAllSlots(this, iinventory, this.decrStackSize(i, 1), enumfacing);

                        if (itemstack1.isEmpty())
                        {
                            iinventory.markDirty();
                            return true;
                        }
                        
                        this.setInventorySlotContents(i, itemstack);
                    }
                }

                return false;
            }
        }
    }

    /**
     * Returns false if the inventory has any room to place items in
     */
    private boolean isInventoryFull(IInventory inventoryIn, EnumFacing side)
    {
        if (inventoryIn instanceof ISidedInventory)
        {
            ISidedInventory isidedinventory = (ISidedInventory)inventoryIn;
            int[] aint = isidedinventory.getSlotsForFace(side);

            for (int k : aint)
            {
                ItemStack itemstack1 = isidedinventory.getStackInSlot(k);

                if (itemstack1.isEmpty() || itemstack1.getCount() != itemstack1.getMaxStackSize())
                {
                    return false;
                }
            }
        }
        else
        {
            int i = inventoryIn.getSizeInventory();

            for (int j = 0; j < i; ++j)
            {
                ItemStack itemstack = inventoryIn.getStackInSlot(j);

                if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize())
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns false if the specified IInventory contains any items
     */
    private static boolean isInventoryEmpty(IInventory inventoryIn, EnumFacing side)
    {
        if (inventoryIn instanceof ISidedInventory)
        {
            ISidedInventory isidedinventory = (ISidedInventory)inventoryIn;
            int[] aint = isidedinventory.getSlotsForFace(side);

            for (int i : aint)
            {
                if (!isidedinventory.getStackInSlot(i).isEmpty())
                {
                    return false;
                }
            }
        }
        else
        {
            int j = inventoryIn.getSizeInventory();

            for (int k = 0; k < j; ++k)
            {
                if (!inventoryIn.getStackInSlot(k).isEmpty())
                {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean captureDroppedItems(IHopper hopper)
    {
        Boolean ret = net.minecraftforge.items.VanillaInventoryCodeHooks.extractHook(hopper);
        if (ret != null) return ret;
        IInventory iinventory = getHopperInventory(hopper);

        if (iinventory != null)
        {
            EnumFacing enumfacing = EnumFacing.DOWN;

            if (isInventoryEmpty(iinventory, enumfacing))
            {
                return false;
            }            
            if (iinventory instanceof ISidedInventory)
            {
                ISidedInventory isidedinventory = (ISidedInventory)iinventory;
                int[] aint = isidedinventory.getSlotsForFace(enumfacing);

                for (int i : aint)
                {
                    if (pullItemFromSlot(hopper, iinventory, i, enumfacing))
                    {
                        return true;
                    }
                }
            }
            else
            {
                int j = iinventory.getSizeInventory();

                for (int k = 0; k < j; ++k)
                {
                    if (pullItemFromSlot(hopper, iinventory, k, enumfacing))
                    {
                        return true;
                    }
                }
            }
        }
        else
        {
        	for(Entity entity : hopper.getWorld().loadedEntityList)
        	{
        		if(entity.getDistance(hopper.getXPos(), hopper.getYPos(), hopper.getZPos()) < 0.7f)
        		{
        			if(entity instanceof EntityPlayer)
        			{
        				//wcode
        				//winput
        				EntityPlayer player = (EntityPlayer) entity;
            			for(Slot slot : player.inventoryContainer.inventorySlots)
            				if(takeItemsFromInventory(player.inventoryContainer.getInventory().get(slot.slotNumber), hopper))
            				{
            					 player.inventoryContainer.getSlot(slot.slotNumber).decrStackSize(1);
            					 break;
            				}
            					
        			}
        			else if(entity instanceof EntityVillager)
        			{
        				EntityVillager villager = (EntityVillager) entity;
            			for(int slot = 0; slot < villager.getVillagerInventory().getSizeInventory(); slot ++)
            				if(takeItemsFromInventory(villager.getVillagerInventory().getStackInSlot(slot), hopper))
            				{
            					villager.getVillagerInventory().decrStackSize(slot, 1);
            					break;
            				}
            					
        			}
        			else if(entity instanceof EntityZombie)
        			{
        				EntityZombie zombie = (EntityZombie) entity;
        				ArrayList<ItemStack> items = new ArrayList<ItemStack>(Arrays.asList(zombie.getHeldItemMainhand(), zombie.getHeldItemOffhand()));
            			for(int i = 0; i < 2; i ++)
            				if(takeItemsFromInventory(items.get(i), hopper))
            				{
            					ItemStack repaceItem = items.get(i);
            					repaceItem.setCount(repaceItem.getCount() - 1);
            					zombie.setHeldItem(i == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND, items.get(i));
            					break;
            				}
            					
        			}
        			else if(entity instanceof EntitySkeleton)
        			{
        				EntitySkeleton skeleton = (EntitySkeleton) entity;
        				ArrayList<ItemStack> items = new ArrayList<ItemStack>(Arrays.asList(skeleton.getHeldItemMainhand(), skeleton.getHeldItemOffhand()));
        				ItemStack itemBowDrop = null;
            			for(int i = 0; i < 2; i ++)
            			{
            				itemBowDrop = items.get(i);
            				if(items.get(i).getItem() == Items.BOW)
            				{
            					itemBowDrop.setCount(0);
            					if(new Random().nextInt(10) + 1 == 1)
            					{
            						itemBowDrop.setCount(1);
            						itemBowDrop.setItemDamage(Items.BOW.getMaxDamage() - new Random().nextInt(40));
            					}
            				}
            				if(takeItemsFromInventory(itemBowDrop, hopper))
            				{
            					ItemStack repaceItem = itemBowDrop;
            					repaceItem.setCount(repaceItem.getCount() - 1);
            					skeleton.setHeldItem(i == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND, itemBowDrop);
            					break;
            				}
            			}		
        			}
        			else if(entity instanceof EntityWitherSkeleton)
        			{
        				EntityWitherSkeleton witherSkeleton = (EntityWitherSkeleton) entity;
        				ArrayList<ItemStack> items = new ArrayList<ItemStack>(Arrays.asList(witherSkeleton.getHeldItemMainhand(), witherSkeleton.getHeldItemOffhand()));
        				ItemStack itemBowDrop = null;
            			for(int i = 0; i < 2; i ++)
            			{
            				itemBowDrop = items.get(i);
            				if(items.get(i).getItem() == Items.STONE_SWORD)
            				{
            					itemBowDrop.setCount(0);
            					if(new Random().nextInt(10) + 1 == 1)
            					{
            						itemBowDrop.setCount(1);
            						itemBowDrop.setItemDamage(Items.STONE_SWORD.getMaxDamage() - new Random().nextInt(40));
            					}
            				}
            				if(takeItemsFromInventory(itemBowDrop, hopper))
            				{
            					ItemStack repaceItem = itemBowDrop;
            					repaceItem.setCount(repaceItem.getCount() - 1);
            					witherSkeleton.setHeldItem(i == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND, itemBowDrop);
            					break;
            				}
            			}		
        			}		
        		}
        		
        	}
            for (EntityItem entityitem : getCaptureItems(hopper.getWorld(), hopper.getXPos(), hopper.getYPos(), hopper.getZPos()))
            {
                if (putDropInInventoryAllSlots((IInventory)null, hopper, entityitem))
                {
                    return true;
                }
            }
            
        }

        return false;
    }
        
    private static boolean takeItemsFromInventory(ItemStack itemIn, IHopper hopper)
    {
    	
    	ItemStack item = new ItemStack(itemIn.getItem());
		item.setItemDamage(itemIn.getItemDamage());
		ArrayList<Boolean> isAdd = new ArrayList<Boolean>();
		for(int i = 0; i < hopper.getSizeInventory(); i++)
			isAdd.add(getOwnInventory(hopper).getStackInSlot(i).getItem() == Item.getItemFromBlock(Blocks.AIR) ||(getOwnInventory(hopper).getStackInSlot(i).getItem() == itemIn.getItem() && getOwnInventory(hopper).getStackInSlot(i).getCount() < getOwnInventory(hopper).getStackInSlot(i).getItem().getItemStackLimit()));
		if(item.getItem() != Item.getItemFromBlock(Blocks.AIR) && isAdd.contains(true))
		{
			NBTTagCompound nbt =itemIn.getTagCompound();
			item.setTagCompound(nbt);
			putStackInInventoryAllSlots((IInventory)null, hopper, item,  (EnumFacing)null);
			return true;
		}
		return false;

    }

    /**
     * Pulls from the specified slot in the inventory and places in any available slot in the hopper. Returns true if
     * the entire stack was moved
     */
    private static boolean pullItemFromSlot(IHopper hopper, IInventory inventoryIn, int index, EnumFacing direction)
    {
        ItemStack itemstack = inventoryIn.getStackInSlot(index);

        if (!itemstack.isEmpty() && canExtractItemFromSlot(inventoryIn, itemstack, index, direction))
        {
            ItemStack itemstack1 = itemstack.copy();
            ItemStack itemstack2 = putStackInInventoryAllSlots(inventoryIn, hopper, inventoryIn.decrStackSize(index, 1), (EnumFacing)null);

            if (itemstack2.isEmpty())
            {
                inventoryIn.markDirty();
                return true;
            }

            inventoryIn.setInventorySlotContents(index, itemstack1);
        }

        return false;
    }

    /**
     * Attempts to place the passed EntityItem's stack into the inventory using as many slots as possible. Returns false
     * if the stackSize of the drop was not depleted.
     */
    public static boolean putDropInInventoryAllSlots(IInventory p_145898_0_, IInventory itemIn, EntityItem p_145898_2_)
    {
        boolean flag = false;

        if (p_145898_2_ == null)
        {
            return false;
        }
        else
        {
            ItemStack itemstack = p_145898_2_.getEntityItem().copy();
            ItemStack itemstack1 = putStackInInventoryAllSlots(p_145898_0_, itemIn, itemstack, (EnumFacing)null);

            if (itemstack1.isEmpty())
            {
                flag = true;
                p_145898_2_.setDead();
            }
            else
            {
                p_145898_2_.setEntityItemStack(itemstack1);
            }

            return flag;
        }
    }


    protected net.minecraftforge.items.IItemHandler createUnSidedHandler()
    {
        return new HopperItemHandler(this);
    }

    /**
     * Attempts to place the passed stack in the inventory, using as many slots as required. Returns leftover items
     * @param isFromEntity 
     */
    public static ItemStack putStackInInventoryAllSlots(IInventory inventoryIn, IInventory stack, ItemStack side, @Nullable EnumFacing p_174918_3_)
    {
        if (stack instanceof ISidedInventory && p_174918_3_ != null)
        {
            ISidedInventory isidedinventory = (ISidedInventory)stack;
            int[] aint = isidedinventory.getSlotsForFace(p_174918_3_);

            for (int k = 0; k < aint.length && !side.isEmpty(); ++k)
            {
                side = insertStack(inventoryIn, stack, side, aint[k], p_174918_3_);
            }
        }
        else
        {
            int i = stack.getSizeInventory();

            for (int j = 0; j < i && !side.isEmpty(); ++j)
            {
                side = insertStack(inventoryIn, stack, side, j, p_174918_3_);
            }
        }

        return side;
    }

    /**
     * Can this hopper insert the specified item from the specified slot on the specified side?
     */
    private static boolean canInsertItemInSlot(IInventory inventoryIn, ItemStack stack, int index, EnumFacing side)
    {
        return !inventoryIn.isItemValidForSlot(index, stack) ? false : !(inventoryIn instanceof ISidedInventory) || ((ISidedInventory)inventoryIn).canInsertItem(index, stack, side);
    }

    /**
     * Can this hopper extract the specified item from the specified slot on the specified side?
     */
    private static boolean canExtractItemFromSlot(IInventory inventoryIn, ItemStack stack, int index, EnumFacing side)
    {
        return !(inventoryIn instanceof ISidedInventory) || ((ISidedInventory)inventoryIn).canExtractItem(index, stack, side);
    }

    /**
     * Insert the specified stack to the specified inventory and return any leftover items
     */
    private static ItemStack insertStack(IInventory inventoryIn, IInventory stack, ItemStack index, int side, EnumFacing p_174916_4_)
    {
        ItemStack itemstack = stack.getStackInSlot(side);

        if (canInsertItemInSlot(stack, index, side, p_174916_4_))
        {
            boolean flag = false;
            boolean flag1 = stack.isEmpty();

            if (itemstack.isEmpty())
            {
                stack.setInventorySlotContents(side, index);
                index = ItemStack.EMPTY;
                flag = true;
            }
            else if (canCombine(itemstack, index))
            {
                int i = index.getMaxStackSize() - itemstack.getCount();
                int j = Math.min(index.getCount(), i);
                index.shrink(j);
                itemstack.grow(j);
                flag = j > 0;
            }

            if (flag)
            {
                if (stack instanceof TileEntityEntityHopper)
                {
                    TileEntityEntityHopper tileentityhopper1 = (TileEntityEntityHopper)stack;

                    if (!tileentityhopper1.mayTransfer())
                    {
                        int k = 0;

                        if (inventoryIn != null && inventoryIn instanceof TileEntityEntityHopper)
                        {
                            TileEntityEntityHopper tileentityhopper = (TileEntityEntityHopper)inventoryIn;

                            if (tileentityhopper1.tickedGameTime >= tileentityhopper.tickedGameTime)
                            {
                                k = 1;
                            }
                        }

                        tileentityhopper1.setTransferCooldown(8 - k);
                    }
                }

                stack.markDirty();
            }
        }

        return index;
    }

    /**
     * Returns the IInventory that this hopper is pointing into
     */
    private IInventory getInventoryForHopperTransfer()
    {
        EnumFacing enumfacing = BlockEntityHopper.getFacing(this.getBlockMetadata());
        return getInventoryAtPosition(this.getWorld(), this.getXPos() + (double)enumfacing.getFrontOffsetX(), this.getYPos() + (double)enumfacing.getFrontOffsetY(), this.getZPos() + (double)enumfacing.getFrontOffsetZ());
    }
    
    private IInventory getEntityInventory()
    {
    	//woutput
        EnumFacing enumfacing = BlockEntityHopper.getFacing(this.getBlockMetadata());
        BlockPos pos = new BlockPos(this.getXPos() + (double)enumfacing.getFrontOffsetX(), this.getYPos() + (double)enumfacing.getFrontOffsetY(), this.getZPos() + (double)enumfacing.getFrontOffsetZ());
        BlockPos pos2 = new BlockPos(this.getXPos() + (double)enumfacing.getFrontOffsetX(), this.getYPos() + (double)enumfacing.getFrontOffsetY() - 1 , this.getZPos() + (double)enumfacing.getFrontOffsetZ());
        BlockPos pos3 = new BlockPos(this.getXPos(), this.getYPos() - 2, this.getZPos());
        for(Entity entity : world.getLoadedEntityList())
        {
        	ArrayList<String> positions = new ArrayList<String>(Arrays.asList(pos.toString(), pos2.toString(), pos3.toString()));
        	if(entity instanceof EntityWitherSkeleton)
        	{
        		positions.remove(2);
        		positions.add((new BlockPos(this.getXPos() + (double)enumfacing.getFrontOffsetX(), this.getYPos() + (double)enumfacing.getFrontOffsetY() - 2 , this.getZPos() + (double)enumfacing.getFrontOffsetZ())).toString());
        		positions.add(new BlockPos(this.getXPos(), this.getYPos() - 2, this.getZPos()).toString());
        	}
        	if(positions.contains(entity.getPosition().toString()))
        	{
        		if(entity instanceof EntityPlayer)
        		{
        			EntityPlayer player = (EntityPlayer) entity;
        			return player.inventory;
        		}
        		if(entity instanceof EntityVillager)
        		{
        			EntityVillager villager = (EntityVillager) entity;
        			return villager.getVillagerInventory();
        		}
        		
        		if(entity instanceof EntitySkeleton)
        		{
        			EntitySkeleton skeleton = (EntitySkeleton) entity;
        			
        			for (int i = 0; i < this.getSizeInventory();)
                    {
                        if (!this.getStackInSlot(i).isEmpty())
                        {
                            ItemStack itemstack = this.getStackInSlot(i).copy();
                            Boolean flag = false;
                            if(skeleton.getHeldItemMainhand().isEmpty() || (skeleton.getHeldItemMainhand().getCount() < skeleton.getHeldItemMainhand().getMaxStackSize() && itemstack.getItem() == skeleton.getHeldItemMainhand().getItem()))
                            {
                            	ItemStack giveItem = itemstack.copy();
                            	giveItem.setCount(skeleton.getHeldItemMainhand().getCount() + 1);
                            	skeleton.setHeldItem(EnumHand.MAIN_HAND, giveItem);
                            	flag = true;
                            }
                            else if(!flag && skeleton.getHeldItemOffhand().isEmpty() || (skeleton.getHeldItemOffhand().getCount() < skeleton.getHeldItemOffhand().getMaxStackSize() && itemstack.getItem() == skeleton.getHeldItemOffhand().getItem()))
                            {
                            	ItemStack giveItem = itemstack.copy();
                            	giveItem.setCount(skeleton.getHeldItemOffhand().getCount() + 1);
                            	skeleton.setHeldItem(EnumHand.OFF_HAND, giveItem);
                            	flag = true;
                            }
                            if(flag)
                            {
                            	ItemStack returnItemStack = itemstack.copy();
                                returnItemStack.setCount(returnItemStack.getCount() - 1);
                                this.setInventorySlotContents(i, returnItemStack);
                                this.setTransferCooldown(8);
                            }
                        }
                        return null;
                    }
        		}
        		
        		if(entity instanceof EntityZombie)
        		{
        			EntityZombie zombie = (EntityZombie) entity;
        			for (int i = 0; i < this.getSizeInventory();)
                    {
                        if (!this.getStackInSlot(i).isEmpty())
                        {
                            ItemStack itemstack = this.getStackInSlot(i).copy();
                            Boolean flag = false;
                            if(zombie.getHeldItemMainhand().isEmpty() || (zombie.getHeldItemMainhand().getCount() < zombie.getHeldItemMainhand().getMaxStackSize() && itemstack.getItem() == zombie.getHeldItemMainhand().getItem()))
                            {
                            	ItemStack giveItem = itemstack.copy();
                            	giveItem.setCount(zombie.getHeldItemMainhand().getCount() + 1);
                            	zombie.setHeldItem(EnumHand.MAIN_HAND, giveItem);
                            	flag = true;
                            }
                            else if(!flag && zombie.getHeldItemOffhand().isEmpty() || (zombie.getHeldItemOffhand().getCount() < zombie.getHeldItemOffhand().getMaxStackSize() && itemstack.getItem() == zombie.getHeldItemOffhand().getItem()))
                            {
                            	ItemStack giveItem = itemstack.copy();
                            	giveItem.setCount(zombie.getHeldItemOffhand().getCount() + 1);
                            	zombie.setHeldItem(EnumHand.OFF_HAND, giveItem);
                            	flag = true;
                            }
                            if(flag)
                            {
                            	ItemStack returnItemStack = itemstack.copy();
                                returnItemStack.setCount(returnItemStack.getCount() - 1);
                                this.setInventorySlotContents(i, returnItemStack);
                                this.setTransferCooldown(8);
                            }
                        }
                        return null;
                    }	
        		}
        		
        		if(entity instanceof EntityWitherSkeleton)
        		{
        			EntityWitherSkeleton witherSkeleton = (EntityWitherSkeleton) entity;
        			
        			for (int i = 0; i < this.getSizeInventory();)
                    {
                        if (!this.getStackInSlot(i).isEmpty())
                        {
                            ItemStack itemstack = this.getStackInSlot(i).copy();
                            Boolean flag = false;
                            if(witherSkeleton.getHeldItemMainhand().isEmpty() || (witherSkeleton.getHeldItemMainhand().getCount() < witherSkeleton.getHeldItemMainhand().getMaxStackSize() && itemstack.getItem() == witherSkeleton.getHeldItemMainhand().getItem()))
                            {
                            	ItemStack giveItem = itemstack.copy();
                            	giveItem.setCount(witherSkeleton.getHeldItemMainhand().getCount() + 1);
                            	witherSkeleton.setHeldItem(EnumHand.MAIN_HAND, giveItem);
                            	flag = true;
                            }
                            else if(!flag && witherSkeleton.getHeldItemOffhand().isEmpty() || (witherSkeleton.getHeldItemOffhand().getCount() < witherSkeleton.getHeldItemOffhand().getMaxStackSize() && itemstack.getItem() == witherSkeleton.getHeldItemOffhand().getItem()))
                            {
                            	ItemStack giveItem = itemstack.copy();
                            	giveItem.setCount(witherSkeleton.getHeldItemOffhand().getCount() + 1);
                            	witherSkeleton.setHeldItem(EnumHand.OFF_HAND, giveItem);
                            	flag = true;
                            }
                            if(flag)
                            {
                            	ItemStack returnItemStack = itemstack.copy();
                                returnItemStack.setCount(returnItemStack.getCount() - 1);
                                this.setInventorySlotContents(i, returnItemStack);
                                this.setTransferCooldown(8);
                            }
                        }
                        return null;
                    }
        		}
        	}
        }
		return null;
    }

    /**
     * Returns the IInventory for the specified hopper
     */
    public static IInventory getHopperInventory(IHopper hopper)
    {
        return getInventoryAtPosition(hopper.getWorld(), hopper.getXPos(), hopper.getYPos() + 1.0D, hopper.getZPos());
    }
    
    public static IInventory getOwnInventory(IHopper hopper)
    {
    	return getInventoryAtPosition(hopper.getWorld(), hopper.getXPos(), hopper.getYPos(), hopper.getZPos());
    }

    public static List<EntityItem> getCaptureItems(World worldIn, double p_184292_1_, double p_184292_3_, double p_184292_5_)
    {
        return worldIn.<EntityItem>getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(p_184292_1_ - 0.5D, p_184292_3_, p_184292_5_ - 0.5D, p_184292_1_ + 0.5D, p_184292_3_ + 1.5D, p_184292_5_ + 0.5D), EntitySelectors.IS_ALIVE);
    }
    /**
     * Returns the IInventory (if applicable) of the TileEntity at the specified position
     */
    public static IInventory getInventoryAtPosition(World worldIn, double x, double y, double z)
    {
        IInventory iinventory = null;
        int i = MathHelper.floor(x);
        int j = MathHelper.floor(y);
        int k = MathHelper.floor(z);
        BlockPos blockpos = new BlockPos(i, j, k);
        net.minecraft.block.state.IBlockState state = worldIn.getBlockState(blockpos);
        Block block = state.getBlock();

        if (block.hasTileEntity(state))
        {
            TileEntity tileentity = worldIn.getTileEntity(blockpos);

            if (tileentity instanceof IInventory)
            {
                iinventory = (IInventory)tileentity;

                if (iinventory instanceof TileEntityChest && block instanceof BlockChest)
                {
                    iinventory = ((BlockChest)block).getContainer(worldIn, blockpos, true);
                }
            }
        }

        if (iinventory == null)
        {
            List<Entity> list = worldIn.getEntitiesInAABBexcluding((Entity)null, new AxisAlignedBB(x - 0.5D, y - 0.5D, z - 0.5D, x + 0.5D, y + 0.5D, z + 0.5D), EntitySelectors.HAS_INVENTORY);

            if (!list.isEmpty())
            {
                iinventory = (IInventory)list.get(worldIn.rand.nextInt(list.size()));
            }
        }

        return iinventory;
    }

    private static boolean canCombine(ItemStack stack1, ItemStack stack2)
    {
        return stack1.getItem() != stack2.getItem() ? false : (stack1.getMetadata() != stack2.getMetadata() ? false : (stack1.getCount() > stack1.getMaxStackSize() ? false : ItemStack.areItemStackTagsEqual(stack1, stack2)));
    }

    /**
     * Gets the world X position for this hopper entity.
     */
    public double getXPos()
    {
        return (double)this.pos.getX() + 0.5D;
    }

    /**
     * Gets the world Y position for this hopper entity.
     */
    public double getYPos()
    {
        return (double)this.pos.getY() + 0.5D;
    }

    /**
     * Gets the world Z position for this hopper entity.
     */
    public double getZPos()
    {
        return (double)this.pos.getZ() + 0.5D;
    }

    public void setTransferCooldown(int ticks)
    {
        this.transferCooldown = ticks;
    }

    private boolean isOnTransferCooldown()
    {
        return this.transferCooldown > 0;
    }

    public boolean mayTransfer()
    {
        return this.transferCooldown > 8;
    }

    public String getGuiID()
    {
        return "minecraft:hopper";
    }

    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn)
    {
        this.fillWithLoot(playerIn);
        return new ContainerHopper(playerInventory, this, playerIn);
    }

    protected NonNullList<ItemStack> getItems()
    {
        return this.inventory;
    }

    public long getLastUpdateTime() { return tickedGameTime; } // Forge
}