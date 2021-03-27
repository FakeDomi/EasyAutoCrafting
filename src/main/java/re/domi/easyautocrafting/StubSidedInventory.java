package re.domi.easyautocrafting;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class StubSidedInventory implements SidedInventory
{
    @Override
    public int[] getAvailableSlots(Direction side)
    {
        return new int[0];
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir)
    {
        return true;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir)
    {
        return true;
    }

    @Override
    public int size()
    {
        return 0;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public ItemStack getStack(int slot)
    {
        return null;
    }

    @Override
    public ItemStack removeStack(int slot, int amount)
    {
        return null;
    }

    @Override
    public ItemStack removeStack(int slot)
    {
        return null;
    }

    @Override
    public void setStack(int slot, ItemStack stack)
    {
    }

    @Override
    public void markDirty()
    {
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player)
    {
        return false;
    }

    @Override
    public void clear()
    {
    }
}
