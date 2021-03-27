package re.domi.easyautocrafting;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointerImpl;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static net.minecraft.util.math.Direction.*;

public class CraftingDropper
{
    public static void dispense(ServerWorld world, BlockPos pos, CallbackInfo ci)
    {
        Direction facing = world.getBlockState(pos).get(DispenserBlock.FACING);
        Direction facingAway = facing.getOpposite();

        if (!hasTableNextToBlock(world, pos))
        {
            return;
        }

        Inventory inventoryBehind = HopperBlockEntity.getInventoryAt(world, pos.offset(facingAway));
        boolean patternMode = inventoryBehind != null;
        CraftingInventory craftingInventory = new CraftingInventory(new StubScreenHandler(), 3, 3);

        DropperBlockEntity dropper = (DropperBlockEntity)world.getBlockEntity(pos);
        List<ItemStack> ingredients = new ArrayList<>(9);

        for (int i = 0; i < 9; i++)
        {
            @SuppressWarnings("ConstantConditions") ItemStack stack = singleItemOf(dropper.getStack(i));
            addToMergedItemStackList(ingredients, stack);
            craftingInventory.setStack(i, stack);
        }

        if (patternMode && !checkInventoryContents(inventoryBehind, ingredients, facing, false))
        {
            ci.cancel();
            return;
        }

        Optional<CraftingRecipe> recipe = world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, craftingInventory, world);

        if (recipe.isPresent())
        {
            List<ItemStack> craftingResults = new ArrayList<>();

            addToMergedItemStackList(craftingResults, recipe.get().craft(craftingInventory));

            for (ItemStack remainingStack : world.getRecipeManager().getRemainingStacks(RecipeType.CRAFTING, craftingInventory, world))
            {
                addToMergedItemStackList(craftingResults, remainingStack);
            }

            Inventory inventoryInFront = HopperBlockEntity.getInventoryAt(world, pos.offset(facing));
            boolean hasCrafted = false;

            if (inventoryInFront != null)
            {
                if (checkInventorySpace(inventoryInFront, craftingResults, facing.getOpposite(), false))
                {
                    checkInventorySpace(inventoryInFront, craftingResults, facing.getOpposite(), true);
                    hasCrafted = true;
                }
            }
            else
            {
                for (ItemStack craftingResult : craftingResults)
                {
                    ItemDispenserBehavior.spawnItem(world, craftingResult, 6, facing,  DispenserBlock.getOutputLocation(new BlockPointerImpl(world, pos)));
                }

                world.syncWorldEvent(1000, pos, 0);
                world.syncWorldEvent(2000, pos, facing.getId());

                hasCrafted = true;
            }

            if (hasCrafted)
            {
                if (patternMode)
                {
                    checkInventoryContents(inventoryBehind, ingredients, facing, true);
                }
                else
                {
                    for (int i = 0; i < 9; i++)
                    {
                        dropper.getStack(i).decrement(1);
                    }

                    dropper.markDirty();
                }
            }
        }

        ci.cancel();
    }

    public static boolean hasTableNextToBlock(ServerWorld world, BlockPos pos)
    {
        Block table = Blocks.CRAFTING_TABLE;

        return world.getBlockState(pos.offset(UP)).getBlock().equals(table) ||
            world.getBlockState(pos.offset(DOWN)).getBlock().equals(table) ||
            world.getBlockState(pos.offset(NORTH)).getBlock().equals(table) ||
            world.getBlockState(pos.offset(EAST)).getBlock().equals(table) ||
            world.getBlockState(pos.offset(SOUTH)).getBlock().equals(table) ||
            world.getBlockState(pos.offset(WEST)).getBlock().equals(table);
    }

    private static ItemStack singleItemOf(ItemStack stack)
    {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static boolean checkInventoryContents(Inventory inventory, List<ItemStack> stacks, Direction side, boolean takeItems)
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
                if (checkInventoryContentsSlot(inventory, sidedInventory, availableSlot, stacks, side, takeItems))
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
                if (checkInventoryContentsSlot(inventory, fakeSidedInventory, i, stacks, side, takeItems))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean checkInventoryContentsSlot(Inventory inventory, SidedInventory sidedInventory, int slot, List<ItemStack> stacks, Direction side, boolean takeItems)
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

    private static boolean checkInventorySpace(Inventory inventory, List<ItemStack> stacks, Direction side, boolean putItems)
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
                    if (checkInventorySpaceSlot(inventory, sidedInventory, availableSlot, stacks, side, putItems, pass == 2))
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
                    if (checkInventorySpaceSlot(inventory, fakeSidedInventory, i, stacks, side, putItems, pass == 2))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean checkInventorySpaceSlot(Inventory inventory, SidedInventory sidedInventory, int slot, List<ItemStack> stacks, Direction side, boolean putItems, boolean considerEmptySlots)
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

    private static boolean itemsEqual(ItemStack first, ItemStack second)
    {
        return ItemStack.areItemsEqual(first, second) && ItemStack.areTagsEqual(first, second);
    }

    private static void addToMergedItemStackList(List<ItemStack> stackList, ItemStack newStack)
    {
        if (newStack.isEmpty())
        {
            return;
        }

        for (ItemStack stack : stackList)
        {
            if (itemsEqual(stack, newStack))
            {
                stack.setCount(stack.getCount() + newStack.getCount());
                return;
            }
        }

        stackList.add(newStack.copy());
    }
}
