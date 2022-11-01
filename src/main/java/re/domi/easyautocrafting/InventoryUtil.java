package re.domi.easyautocrafting;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class InventoryUtil
{
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
    public static Storage<ItemVariant> getMerged3x3Storage(World world, BlockPos center, Direction extractionSide)
    {
        Direction.Axis first;
        Direction.Axis second;

        if (ItemStorage.SIDED.find(world, center, extractionSide) == null)
        {
            return null;
        }

        List<Storage<ItemVariant>> storages = new ArrayList<>(9);

        switch (extractionSide.getAxis())
        {
            case X ->
            {
                first = Direction.Axis.Y;
                second = Direction.Axis.Z;
            }
            case Y ->
            {
                first = Direction.Axis.X;
                second = Direction.Axis.Z;
            }
            case Z ->
            {
                first = Direction.Axis.Y;
                second = Direction.Axis.X;
            }
            default -> throw new IllegalStateException("Invalid axis: " + extractionSide.getAxis());
        }

        for (int a = -1; a <= 1; a++)
        {
            for (int b = -1; b <= 1; b++)
            {
                BlockPos checkPos = center.offset(first, a).offset(second, b);
                Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, checkPos, extractionSide);

                if(storage != null)
                {
                    storages.add(storage);
                }
            }
        }

        return new CombinedStorage<>(storages);
    }

    public static ItemStack singleItemOf(ItemStack stack)
    {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static boolean tryTakeItems(Storage<ItemVariant> storage, List<ItemStack> stacks, boolean simulate)
    {
        stacks = deepCopy(stacks);

        try (Transaction transaction = Transaction.openOuter())
        {
            for (StorageView<ItemVariant> view : storage)
            {
                ItemVariant variant = view.getResource();

                if (variant.isBlank())
                {
                    continue;
                }

                Iterator<ItemStack> i = stacks.iterator();

                while (i.hasNext())
                {
                    ItemStack current = i.next();

                    if (variant.matches(current))
                    {
                        int remaining = current.getCount() - (int)view.extract(variant, current.getCount(), transaction);

                        if (remaining > 0)
                        {
                            current.setCount(remaining);
                        }
                        else
                        {
                            i.remove();
                        }
                    }
                }

                if (stacks.isEmpty())
                {
                    if (!simulate)
                    {
                        transaction.commit();
                    }

                    return true;
                }
            }

            return false;
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static boolean tryPutItems(Storage<ItemVariant> storage, List<ItemStack> stacks)
    {
        try (Transaction transaction = Transaction.openOuter())
        {
            for (ItemStack stack : stacks)
            {
                if (storage.insert(ItemVariant.of(stack), stack.getCount(), transaction) < stack.getCount())
                {
                    return false;
                }
            }

            transaction.commit();
            return true;
        }
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
