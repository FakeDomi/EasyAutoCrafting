package re.domi.easyautocrafting;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InventoryUtil
{
	@SuppressWarnings("UnstableApiUsage")
	public static final Storage<ItemVariant> ALWAYS_EMPTY = new Storage<>() {
		@Override
		public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			return maxAmount;
		}

		@Override
		public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			return 0;
		}
		@Override
		public boolean supportsExtraction(){
			return false;
		}

		@Override
		public @NotNull Iterator<StorageView<ItemVariant>> iterator() {
			return Collections.emptyIterator();
		}
	};
    public static boolean itemsEqual(ItemStack first, ItemStack second)
    {
        return first == second || first.getItem() == second.getItem() && (Objects.equals(first.getNbt(), second.getNbt()));
    }

    public static boolean itemStackListsEqual(List<ItemStack> first, List<ItemStack> second)
    {
        if (first == null || second == null || first.size() != second.size())
        {
            return false;
        }

        for (int i = 0; i < first.size(); i++)
        {
            if (!itemsEqual(first.get(i), second.get(i)))
            {
                return false;
            }
        }

        return true;
    }
	@SuppressWarnings("UnstableApiUsage")
	public static List<Storage<ItemVariant>> getMerged3x3InventoryBehind(World world, Direction dropperFacing, BlockPos dropperPos){
		BlockPos posBehind = dropperPos.offset(dropperFacing.getOpposite());
		List<Storage<ItemVariant>> inventories = new ArrayList<>(9);
		//Now we got perpendicular directions
		for (int a = -1; a < 2; a++){
			for (int b = -1; b < 2; b++){
				Direction.Axis first;
				Direction.Axis second;
				switch (dropperFacing.getAxis()) {
					case X -> {
						first = Direction.Axis.Y;
						second = Direction.Axis.Z;
					}
					case Y -> {
						first = Direction.Axis.X;
						second = Direction.Axis.Z;
					}
					case Z -> {
						first = Direction.Axis.Y;
						second = Direction.Axis.X;
					}
					default -> throw new IllegalStateException("Dropper facing was null");
				}
				BlockPos checkPos = posBehind.offset(first, a).offset(second, b);
				//inventories[i] = HopperBlockEntity.getInventoryAt(world, checkPos);
				Storage<ItemVariant> variantStorage = ItemStorage.SIDED.find(world, checkPos, dropperFacing.getOpposite());
				if(variantStorage != null)
					inventories.add(variantStorage);
			}
		}
		return inventories;
	}

    public static ItemStack singleItemOf(ItemStack stack)
    {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }
	@SuppressWarnings("UnstableApiUsage")
	public static boolean tryTakeItems(List<Storage<ItemVariant>> inventories, List<ItemStack> stacks, boolean simulate)
	{
		if (simulate)
		{
			stacks = deepCopy(stacks);
		}
		boolean done = true;
		CombinedStorage<ItemVariant, Storage<ItemVariant>> storages = new CombinedStorage<>(inventories);
		try (Transaction transaction = Transaction.openOuter()){
			for (ItemStack itemStack : stacks){
				long movedAmount = StorageUtil.move(storages, ALWAYS_EMPTY, (stack)-> stack.toStack().isItemEqual(itemStack), itemStack.getCount(), transaction);
				if (movedAmount < itemStack.getCount()){
					done = false;
					break;
				}
			}
			if (!done || simulate){
				transaction.abort();
			}
			else {
				transaction.commit();
			}
		}
		catch (IllegalStateException ignored){
			return false;
		}
		return done;
	}
	@SuppressWarnings("unused")
    public static boolean tryTakeItems(Inventory inventory, List<ItemStack> stacks, Direction side, boolean simulate)
    {
        if (simulate)
        {
            stacks = deepCopy(stacks);
        }

        if (inventory instanceof SidedInventory sidedInventory)
        {
            for (int availableSlot : sidedInventory.getAvailableSlots(side))
            {
                if (takeItemsFromSlot(inventory, sidedInventory, availableSlot, stacks, side, simulate))
                {
                    return true;
                }
            }
        }
        else
        {
            for (int i = 0; i < inventory.size(); i++)
            {
                if (takeItemsFromSlot(inventory, null, i, stacks, side, simulate))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean takeItemsFromSlot(Inventory inventory, @Nullable SidedInventory sidedInventory, int slot, List<ItemStack> stacks, Direction side, boolean simulate)
    {
        ItemStack stackInSlot = inventory.getStack(slot);
        Iterator<ItemStack> stacksIterator = stacks.iterator();

        while (stacksIterator.hasNext())
        {
            ItemStack currentStack = stacksIterator.next();

            if (itemsEqual(stackInSlot, currentStack))
            {
                int toTake = Math.min(currentStack.getCount(), stackInSlot.getCount());
                ItemStack extractedStack = stackInSlot.copy();
                extractedStack.setCount(toTake);

                if (sidedInventory == null || sidedInventory.canExtract(slot, extractedStack, side))
                {
                    if (currentStack.getCount() > toTake)
                    {
                        currentStack.setCount(currentStack.getCount() - toTake);
                    }
                    else
                    {
                        stacksIterator.remove();
                    }

                    if (!simulate)
                    {
                        if (stackInSlot.getCount() > toTake)
                        {
                            stackInSlot.setCount(stackInSlot.getCount() - toTake);
                            inventory.setStack(slot, stackInSlot);
                        }
                        else
                        {
                            inventory.setStack(slot, ItemStack.EMPTY);
                        }
                    }
                }
            }
        }

        return stacks.isEmpty();
    }

    public static boolean tryPutItems(Inventory inventory, List<ItemStack> stacks, Direction side, boolean simulate)
    {
        if (simulate)
        {
            stacks = deepCopy(stacks);
        }

        if (inventory instanceof SidedInventory sidedInventory)
        {
            int[] availableSlots = sidedInventory.getAvailableSlots(side);

            for (int pass = 1; pass <= 2; pass++)
            {
                for (int availableSlot : availableSlots)
                {
                    if (putItemsToSlot(inventory, sidedInventory, availableSlot, stacks, side, simulate, pass == 2))
                    {
                        return true;
                    }
                }
            }
        }
        else
        {
            for (int pass = 1; pass <= 2; pass++)
            {
                for (int i = 0; i < inventory.size(); i++)
                {
                    if (putItemsToSlot(inventory, null, i, stacks, side, simulate, pass == 2))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean putItemsToSlot(Inventory inventory, @Nullable SidedInventory sidedInventory, int slot, List<ItemStack> stacks, Direction side, boolean simulate, boolean considerEmptySlots)
    {
        ItemStack stackInSlot = inventory.getStack(slot);

        Iterator<ItemStack> stacksIterator = stacks.iterator();

        while (stacksIterator.hasNext())
        {
            ItemStack currentStack = stacksIterator.next();

            if (considerEmptySlots && stackInSlot.isEmpty())
            {
                if (inventory.isValid(slot, currentStack) && (sidedInventory == null || sidedInventory.canInsert(slot, currentStack, side)))
                {
                    stackInSlot = currentStack;

                    if (!simulate)
                    {
                        inventory.setStack(slot, currentStack);
                    }

                    stacksIterator.remove();
                }
            }
            else if (itemsEqual(currentStack, stackInSlot))
            {
                int totalCount = currentStack.getCount() + stackInSlot.getCount();
                int maxCount = Math.min(inventory.getMaxCountPerStack(), currentStack.getMaxCount());
                int remainingCount = Math.max(0, totalCount - maxCount);

                if (inventory.isValid(slot, currentStack) && (sidedInventory == null || sidedInventory.canInsert(slot, currentStack, side)))
                {
                    if (!simulate)
                    {
                        stackInSlot.setCount(remainingCount == 0 ? totalCount : maxCount);
                        inventory.setStack(slot, stackInSlot);
                    }

                    if (remainingCount == 0)
                    {
                        stacksIterator.remove();
                    }
                    else
                    {
                        currentStack.setCount(remainingCount);
                    }
                }
            }
        }

        return stacks.isEmpty();
    }

    private static List<ItemStack> deepCopy(List<ItemStack> list)
    {
        List<ItemStack> copy = new ArrayList<>(list.size());

        for (ItemStack stack : list)
        {
            copy.add(stack.copy());
        }

        return copy;
    }
}
