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
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPointerImpl;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.domi.easyautocrafting.mixin.CraftingInventoryMixin;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.util.math.Direction.*;

public class CraftingDropper
{
    public static void dispense(ServerWorld world, BlockPos pos, CallbackInfo ci)
    {
        if (!hasTableNextToBlock(world, pos))
        {
            return;
        }

        ci.cancel();

        Direction facing = world.getBlockState(pos).get(DispenserBlock.FACING);
        Direction facingAway = facing.getOpposite();

        DropperBlockEntity dropper = (DropperBlockEntity)world.getBlockEntity(pos);
        List<ItemStack> ingredients = new ArrayList<>(9);
        CraftingInventory craftingInventory = new CraftingInventory(new StubScreenHandler(), 3, 3);

        for (int i = 0; i < 9; i++)
        {
            @SuppressWarnings("ConstantConditions") ItemStack stack = InventoryUtil.singleItemOf(dropper.getStack(i));
            addToMergedItemStackList(ingredients, stack);
            craftingInventory.setStack(i, stack);
        }

        Inventory inventoryBehind = HopperBlockEntity.getInventoryAt(world, pos.offset(facingAway));
        boolean patternMode = inventoryBehind != null;

        if (craftingInventory.isEmpty() || patternMode && !InventoryUtil.tryTakeItems(inventoryBehind, ingredients, facing, true))
        {
            return;
        }

        DropperCache cache = (DropperCache)dropper;
        CraftingRecipe recipe = cache.getRecipe();

        //noinspection ConstantConditions
        List<ItemStack> craftingInventoryItems = ((CraftingInventoryMixin)craftingInventory).getStacks();

        if (!InventoryUtil.itemStackListsEqual(cache.getIngredients(), craftingInventoryItems)
            || recipe != null && !recipe.matches(craftingInventory, world))
        {
            world.getPlayers().forEach(p -> p.sendMessage(new LiteralText("lookup"), false));
            recipe = world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, craftingInventory, world).orElse(null);

            cache.setRecipe(recipe);
            cache.setIngredients(craftingInventoryItems);
        }

        if (recipe != null)
        {
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
                if (InventoryUtil.tryPutItems(inventoryInFront, craftingResults, facing.getOpposite(), true))
                {
                    InventoryUtil.tryPutItems(inventoryInFront, craftingResults, facing.getOpposite(), false);
                    hasCrafted = true;
                }
            }
            else
            {
                for (ItemStack craftingResult : craftingResults)
                {
                    ItemDispenserBehavior.spawnItem(world, craftingResult, 6, facing, DispenserBlock.getOutputLocation(new BlockPointerImpl(world, pos)));
                }

                world.syncWorldEvent(1000, pos, 0);
                world.syncWorldEvent(2000, pos, facing.getId());

                hasCrafted = true;
            }

            if (hasCrafted)
            {
                if (patternMode)
                {
                    InventoryUtil.tryTakeItems(inventoryBehind, ingredients, facing, false);
                }
                else
                {
                    for (int i = 0; i < 9; i++)
                    {
                        if (!dropper.getStack(i).isEmpty())
                        {
                            dropper.getStack(i).decrement(1);
                        }
                    }

                    dropper.markDirty();
                }
            }
        }
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
