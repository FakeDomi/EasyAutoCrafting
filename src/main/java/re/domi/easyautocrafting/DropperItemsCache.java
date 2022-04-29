package re.domi.easyautocrafting;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;

import java.util.List;

public interface DropperItemsCache
{
	List<ItemStack> getCachedList();
    void setCachedList(List<ItemStack> r);
}
