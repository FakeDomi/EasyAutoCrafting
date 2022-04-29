package re.domi.easyautocrafting.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import re.domi.easyautocrafting.CraftingDropper;
import re.domi.easyautocrafting.DropperItemsCache;
import re.domi.easyautocrafting.DropperRecipeCache;

import java.util.List;

@Mixin(DropperBlockEntity.class)
public class DropperBlockEntityMixin extends DispenserBlockEntity implements DropperRecipeCache, DropperItemsCache
{
    private CraftingRecipe cachedRecipe;
	private List<ItemStack> cachedList;

    public DropperBlockEntityMixin(BlockPos pos, BlockState state)
    {
        super(pos, state);
    }

    @Override
    public boolean isValid(int slot, ItemStack stack)
    {
        if (this.world instanceof ServerWorld && CraftingDropper.hasTableNextToBlock((ServerWorld)this.world, this.pos))
        {
            return this.getStack(slot).isEmpty();
        }

        return super.isValid(slot, stack);
    }

    @Override
    public CraftingRecipe get()
    {
        return this.cachedRecipe;
    }

	@Override
	public void set(CraftingRecipe r)
	{
		this.cachedRecipe = r;
	}

	@Override
	public List<ItemStack> getCachedList()
	{
		return this.cachedList;
	}

    @Override
    public void setCachedList(List<ItemStack> r)
    {
        this.cachedList = r;
    }
}
