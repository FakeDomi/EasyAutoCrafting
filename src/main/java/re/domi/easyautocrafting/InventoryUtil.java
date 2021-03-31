package re.domi.easyautocrafting;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class InventoryUtil
{
    public static boolean itemsEqual(ItemStack first, ItemStack second)
    {
        return ItemStack.areItemsEqual(first, second) && ItemStack.areTagsEqual(first, second);
    }

    public static ItemStack singleItemOf(ItemStack stack)
    {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    public static boolean takeItems(Inventory inventory, List<ItemStack> stacks, Direction side, boolean takeItems)
    {
        if (!takeItems)
        {
            stacks = deepCopy(stacks);
        }

        if (inventory instanceof SidedInventory)
        {
            SidedInventory sidedInventory = (SidedInventory)inventory;
            int[] availableSlots = sidedInventory.getAvailableSlots(side);

            for (int availableSlot : availableSlots)
            {
                if (takeItemsFromSlot(inventory, sidedInventory, availableSlot, stacks, side, takeItems))
                {
                    return true;
                }
            }
        }
        else
        {
            SidedInventory fakeSidedInventory = new StubSidedInventory();

            for (int i = 0; i < inventory.size(); i++)
            {
                if (takeItemsFromSlot(inventory, fakeSidedInventory, i, stacks, side, takeItems))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean takeItemsFromSlot(Inventory inventory, SidedInventory sidedInventory, int slot, List<ItemStack> stacks, Direction side, boolean takeItems)
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

                if (sidedInventory.canExtract(slot, extractedStack, side))
                {
                    if (currentStack.getCount() > toTake)
                    {
                        currentStack.setCount(currentStack.getCount() - toTake);
                    }
                    else
                    {
                        stacksIterator.remove();
                    }

                    if (takeItems)
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

    public static boolean putItems(Inventory inventory, List<ItemStack> stacks, Direction side, boolean putItems)
    {
        if (!putItems)
        {
            stacks = deepCopy(stacks);
        }

        if (inventory instanceof SidedInventory)
        {
            SidedInventory sidedInventory = (SidedInventory)inventory;
            int[] availableSlots = sidedInventory.getAvailableSlots(side);

            for (int pass = 1; pass <= 2; pass++)
            {
                for (int availableSlot : availableSlots)
                {
                    if (putItemsToSlot(inventory, sidedInventory, availableSlot, stacks, side, putItems, pass == 2))
                    {
                        return true;
                    }
                }
            }
        }
        else
        {
            SidedInventory fakeSidedInventory = new StubSidedInventory();

            for (int pass = 1; pass <= 2; pass++)
            {
                for (int i = 0; i < inventory.size(); i++)
                {
                    if (putItemsToSlot(inventory, fakeSidedInventory, i, stacks, side, putItems, pass == 2))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean putItemsToSlot(Inventory inventory, SidedInventory sidedInventory, int slot, List<ItemStack> stacks, Direction side, boolean putItems, boolean considerEmptySlots)
    {
        ItemStack stackInSlot = inventory.getStack(slot);

        Iterator<ItemStack> stacksIterator = stacks.iterator();

        while (stacksIterator.hasNext())
        {
            ItemStack currentStack = stacksIterator.next();

            if (considerEmptySlots && stackInSlot.isEmpty())
            {
                if (inventory.isValid(slot, currentStack) && sidedInventory.canInsert(slot, currentStack, side))
                {
                    stackInSlot = currentStack;

                    if (putItems)
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

                if (inventory.isValid(slot, currentStack) && sidedInventory.canInsert(slot, currentStack, side))
                {
                    if (putItems)
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
