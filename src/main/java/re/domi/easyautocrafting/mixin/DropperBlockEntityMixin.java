package re.domi.easyautocrafting.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import re.domi.easyautocrafting.DropperCache;
import re.domi.easyautocrafting.CraftingDropper;

import java.util.List;

@Mixin(DropperBlockEntity.class)
public class DropperBlockEntityMixin extends DispenserBlockEntity implements DropperCache
{
    private CraftingRecipe cachedRecipe;
	private List<ItemStack> cachedIngredients;

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
    public CraftingRecipe getRecipe()
    {
        return this.cachedRecipe;
    }

	@Override
	public void setRecipe(CraftingRecipe r)
	{
		this.cachedRecipe = r;
	}

	@Override
	public List<ItemStack> getIngredients()
	{
		return this.cachedIngredients;
	}

    @Override
    public void setIngredients(List<ItemStack> r)
    {
        this.cachedIngredients = r;
    }

    @Override
    public void clearCache()
    {
        this.cachedRecipe = null;
        this.cachedIngredients = null;
    }
}
