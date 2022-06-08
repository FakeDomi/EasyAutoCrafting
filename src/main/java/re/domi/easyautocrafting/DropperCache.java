package re.domi.easyautocrafting;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;

import java.util.List;

public interface DropperCache
{
    CraftingRecipe eac_getRecipe();
    void eac_setRecipe(CraftingRecipe r);

    List<ItemStack> eac_getIngredients();
    void eac_setIngredients(List<ItemStack> l);

    void eac_clearCache();
}
