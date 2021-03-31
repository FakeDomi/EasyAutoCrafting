package re.domi.easyautocrafting;

import net.minecraft.recipe.CraftingRecipe;

public interface DropperRecipeCache
{
    CraftingRecipe get();
    void set(CraftingRecipe r);
}
