package re.domi.easyautocrafting;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;

import java.util.List;

public interface DropperCache
{
    CraftingRecipe getRecipe();
    void setRecipe(CraftingRecipe r);

    List<ItemStack> getIngredients();
    void setIngredients(List<ItemStack> l);

    void clearCache();
}
