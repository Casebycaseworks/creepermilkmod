package milk.creeper.recipe;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.List;

public class CreeperMilkToMilkRecipe extends ShapelessRecipe {

    public CreeperMilkToMilkRecipe(String group, CraftingRecipeCategory category, ItemStack result, List<net.minecraft.recipe.Ingredient> ingredients) {
        super(group, category, result, ingredients);
    }

    @Override
    public DefaultedList<ItemStack> getRecipeRemainders(CraftingRecipeInput craftingRecipeInput) {
        DefaultedList<ItemStack> defaultedList = DefaultedList.ofSize(craftingRecipeInput.size(), ItemStack.EMPTY);
        
        // For this specific recipe, we don't want any remainders
        // This overrides the default behavior that would leave an empty bucket
        return defaultedList;
    }
} 