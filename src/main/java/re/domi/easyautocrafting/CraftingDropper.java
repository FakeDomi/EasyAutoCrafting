package re.domi.easyautocrafting;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.domi.easyautocrafting.mixin.CraftingInventoryMixin;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.util.math.Direction.*;

public class CraftingDropper
{
    public static void dispense(ServerWorld world, BlockState dispenserState, BlockPos dispenserPos, CallbackInfo ci)
    {
        if (!hasTableNextToBlock(world, dispenserPos))
        {
            return;
        }

        ci.cancel();

        Direction facing = dispenserState.get(DispenserBlock.FACING);

        DropperBlockEntity dropper = (DropperBlockEntity)world.getBlockEntity(dispenserPos);
        List<ItemStack> ingredients = new ArrayList<>(9);
        CraftingInventory craftingInventory = new CraftingInventory(new StubScreenHandler(), 3, 3);

        for (int i = 0; i < 9; i++)
        {
            @SuppressWarnings("ConstantConditions") ItemStack stack = InventoryUtil.singleItemOf(dropper.getStack(i));
            addToMergedItemStackList(ingredients, stack);
            craftingInventory.setStack(i, stack);
        }

        Storage<ItemVariant> ingredientStorage = Config.enable3x3InventorySearching ?
            InventoryUtil.getMerged3x3Storage(world, dispenserPos.offset(facing.getOpposite()), facing) :
            ItemStorage.SIDED.find(world, dispenserPos.offset(facing.getOpposite()), facing);

        boolean patternMode = ingredientStorage != null;

        if (craftingInventory.isEmpty() || patternMode && !InventoryUtil.tryTakeItems(ingredientStorage, ingredients, true))
        {
            return;
        }

        DropperCache cache = (DropperCache)dropper;
        CraftingRecipe recipe = cache.eac_getRecipe();

        //noinspection ConstantConditions
        List<ItemStack> craftingInventoryItems = ((CraftingInventoryMixin)craftingInventory).getStacks();

        if (!InventoryUtil.itemStackListsEqual(cache.eac_getIngredients(), craftingInventoryItems)
            || recipe != null && !recipe.matches(craftingInventory.createRecipeInput(), world))
        {
            RecipeEntry<CraftingRecipe> entry = world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, craftingInventory.createRecipeInput(), world).orElse(null);

            recipe = entry == null ? null : entry.value();

            cache.eac_setRecipe(recipe);
            cache.eac_setIngredients(craftingInventoryItems);
        }

        if (recipe != null)
        {
            List<ItemStack> craftingResults = new ArrayList<>();

            addToMergedItemStackList(craftingResults, recipe.craft(craftingInventory.createRecipeInput(), world.getRegistryManager()));

            for (ItemStack remainingStack : recipe.getRecipeRemainders(craftingInventory.createRecipeInput()))
            {
                addToMergedItemStackList(craftingResults, remainingStack);
            }

            Inventory inventoryInFront = HopperBlockEntity.getInventoryAt(world, dispenserPos.offset(facing));
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, dispenserPos.offset(facing), facing.getOpposite());
            boolean hasCrafted = false;

            if (inventoryInFront != null)
            {
                if (InventoryUtil.tryPutItems(inventoryInFront, craftingResults, facing.getOpposite(), true))
                {
                    InventoryUtil.tryPutItems(inventoryInFront, craftingResults, facing.getOpposite(), false);
                    hasCrafted = true;
                }
            }
            else if (storage != null)
            {
                if (InventoryUtil.tryPutItems(storage, craftingResults))
                {
                    hasCrafted = true;
                }
            }
            else
            {
                for (ItemStack craftingResult : craftingResults)
                {
                    ItemDispenserBehavior.spawnItem(world, craftingResult, 6, facing, DispenserBlock.getOutputLocation(new BlockPointer(world, dispenserPos, dispenserState, dropper)));
                }

                world.syncWorldEvent(1000, dispenserPos, 0);
                world.syncWorldEvent(2000, dispenserPos, facing.getId());

                hasCrafted = true;
            }

            if (hasCrafted)
            {
                if (patternMode)
                {
                    InventoryUtil.tryTakeItems(ingredientStorage, ingredients, false);
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
