package milk.creeper.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import milk.creeper.ModRegistry;

public class CreeperMilkToMilkRecipe extends ShapelessRecipe {
    
    public CreeperMilkToMilkRecipe(String group, CraftingRecipeCategory category, ItemStack result, DefaultedList<Ingredient> ingredients) {
        super(group, category, result, ingredients);
    }
    
    public DefaultedList<ItemStack> getRecipeRemainders(CraftingRecipeInput input) {
        DefaultedList<ItemStack> remainders = DefaultedList.ofSize(input.getWidth() * input.getHeight(), ItemStack.EMPTY);
        
        for (int i = 0; i < remainders.size(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (stack.getItem() == ModRegistry.CREEPER_MILK_BUCKET) {
                // For creeper milk bucket in this specific recipe, don't leave any remainder
                remainders.set(i, ItemStack.EMPTY);
            } else {
                // For other items, use default remainder behavior
                ItemStack remainder = stack.getRecipeRemainder();
                remainders.set(i, remainder);
            }
        }
        
        return remainders;
    }
    
    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CREEPER_MILK_TO_MILK_SERIALIZER;
    }
} 