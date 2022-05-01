package re.domi.easyautocrafting;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointerImpl;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

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
		List<ItemStack> listForCompare = new ArrayList<>(9);
        for (int i = 0; i < 9; i++)
        {
            @SuppressWarnings("ConstantConditions") ItemStack stack = InventoryUtil.singleItemOf(dropper.getStack(i));
            addToMergedItemStackList(ingredients, stack);
			listForCompare.add(i, stack);
            craftingInventory.setStack(i, stack);
        }

        if (craftingInventory.isEmpty() || patternMode && !InventoryUtil.takeItems(inventoryBehind, ingredients, facing, false))
        {
            ci.cancel();
            return;
        }

        DropperRecipeCache cache = (DropperRecipeCache)dropper;
	    DropperItemsCache itemsCache = (DropperItemsCache) dropper;
        CraftingRecipe recipe = cache.get();

        if (!InventoryUtil.compareList(itemsCache.getCachedList(), listForCompare) || (itemsCache.getCachedList().isEmpty() && (recipe == null || !recipe.matches(craftingInventory, world)) )) //check if inventory has changed
        {
	        recipe = world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, craftingInventory, world).orElse(null);
	        itemsCache.setCachedList(InventoryUtil.deepCopy(listForCompare));
		cache.set(recipe);
        }

        if (recipe != null)
        {
            cache.set(recipe);
            List<ItemStack> craftingResults = new ArrayList<>();

            addToMergedItemStackList(craftingResults, recipe.craft(craftingInventory));

            for (ItemStack remainingStack : recipe.getRemainder(craftingInventory))
            {
                addToMergedItemStackList(craftingResults, remainingStack);
            }

            Inventory inventoryInFront = HopperBlockEntity.getInventoryAt(world, pos.offset(facing));
            boolean hasCrafted = false;

            if (inventoryInFront != null)
            {
                if (InventoryUtil.putItems(inventoryInFront, craftingResults, facing.getOpposite(), false))
                {
                    InventoryUtil.putItems(inventoryInFront, craftingResults, facing.getOpposite(), true);
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
                    InventoryUtil.takeItems(inventoryBehind, ingredients, facing, true);
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

    private static void addToMergedItemStackList(List<ItemStack> stackList, ItemStack newStack)
    {
        if (newStack.isEmpty())
        {
            return;
        }

        for (ItemStack stack : stackList)
        {
            if (InventoryUtil.itemsEqual(stack, newStack))
            {
                stack.setCount(stack.getCount() + newStack.getCount());
                return;
            }
        }

        stackList.add(newStack.copy());
    }
}
